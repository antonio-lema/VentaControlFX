package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.*;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import com.mycompany.ventacontrolfx.domain.repository.ISaleRepository;
import com.mycompany.ventacontrolfx.domain.repository.ICompanyConfigRepository;
import com.mycompany.ventacontrolfx.domain.repository.IClientRepository;
import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import com.mycompany.ventacontrolfx.util.AuthorizationService;
import com.mycompany.ventacontrolfx.domain.service.TaxEngineService;
import com.mycompany.ventacontrolfx.application.service.PromotionService;
import com.mycompany.ventacontrolfx.shared.bus.GlobalEventBus;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Caso de uso para la gestión de ventas, incluyendo el procesamiento de nuevas
 * ventas,
 * historial, detalles y devoluciones.
 */
public class SaleUseCase {

    private final ISaleRepository saleRepository;
    private final ICompanyConfigRepository configRepository;
    private final AuthorizationService authService;
    private final TaxEngineService taxEngineService;
    private final IClientRepository clientRepository;
    private final PromotionService promotionService;
    private final com.mycompany.ventacontrolfx.application.service.PromotionEngine promotionEngine;
    private final IProductRepository productRepository;
    private final GlobalEventBus eventBus;
    private CashClosureUseCase cashClosureUseCase;

    public SaleUseCase(ISaleRepository saleRepository,
            ICompanyConfigRepository configRepository,
            AuthorizationService authService,
            TaxEngineService taxEngineService,
            IClientRepository clientRepository,
            PromotionService promotionService,
            com.mycompany.ventacontrolfx.application.service.PromotionEngine promotionEngine,
            IProductRepository productRepository,
            GlobalEventBus eventBus) {
        this.saleRepository = saleRepository;
        this.configRepository = configRepository;
        this.authService = authService;
        this.taxEngineService = taxEngineService;
        this.clientRepository = clientRepository;
        this.promotionService = promotionService;
        this.promotionEngine = promotionEngine;
        this.productRepository = productRepository;
        this.eventBus = eventBus;
    }

    /** Inyecta el use case de caja para registrar movimientos de devolución. */
    public void setCashClosureUseCase(CashClosureUseCase cashClosureUseCase) {
        this.cashClosureUseCase = cashClosureUseCase;
    }

    public int processSale(List<CartItem> cartItems, double total, String paymentMethod, Integer clientId, int userId)
            throws SQLException {
        return processSale(cartItems, total, paymentMethod, clientId, userId, 0.0, null);
    }

    public int processSale(List<CartItem> cartItems, double total, String paymentMethod, Integer clientId, int userId,
            double discountAmount, String discountReason) throws SQLException {
        authService.checkPermission("VENTAS");

        // ── REGLA DE NEGOCIO: Validar caja abierta ──
        if (cashClosureUseCase != null && !cashClosureUseCase.hasActiveFund()) {
            throw new SQLException(
                    "OPERACION_BLOQUEADA: La caja no ha sido abierta. Debe establecer un fondo de caja antes de realizar ventas.");
        }

        SaleConfig config = configRepository.load();

        // 1. Obtener cliente (si existe) para considerar exenciones
        Client client = null;
        if (clientId != null && clientId > 0 && clientRepository != null) {
            client = clientRepository.getById(clientId);
        }

        double calculatedTotalIva = 0.0;
        List<SaleDetail> details = new ArrayList<>();
        List<TaxCalculationResult> lineResults = new ArrayList<>();
        boolean isInclusive = config.isPricesIncludeTax();

        // APLICAR MOTOR DE PROMOCIONES (PIPELINE) - Consistent with CartUseCase
        com.mycompany.ventacontrolfx.application.service.PromotionResult promoResult = promotionEngine
                .process(cartItems);
        double autoDiscount = promoResult.getTotalDiscount();

        for (CartItem item : cartItems) {
            Product product = item.getProduct();

            // Obtener descuento específico para este producto desde el motor
            double lineDiscount = promoResult.getItemDiscounts().getOrDefault(product.getId(), 0.0);

            // Precio unitario original (con tarifa de cliente ya aplicada)
            double unitPrice = item.getUnitPrice();

            // Calculamos el total de la línea YA con descuento para que el TaxEngine
            // recalcule la base e IVA
            double grossLineTotal = (unitPrice * item.getQuantity()) - lineDiscount;

            // Simulamos un precio unitario efectivo para el motor de impuestos
            double effectiveUnitPrice = item.getQuantity() > 0 ? (grossLineTotal / item.getQuantity()) : 0.0;

            // Usar el motor fiscal para calcular impuestos de esta línea
            TaxCalculationResult result = taxEngineService.calculateLine(product, client, effectiveUnitPrice,
                    item.getQuantity(), isInclusive);
            lineResults.add(result);
            calculatedTotalIva += result.getTotalTaxAmount();

            SaleDetail d = new SaleDetail();
            d.setProductId(product.getId());
            d.setQuantity(item.getQuantity());
            d.setUnitPrice(product.getPrice());

            // Tax Engine V2 fields
            d.setNetUnitPrice(result.getNetTotal() / item.getQuantity());
            d.setTaxBasis(result.getNetTotal());
            d.setTaxAmount(result.getTotalTaxAmount());
            d.setGrossTotal(result.getGrossTotal());
            d.setLineTotal(result.getGrossTotal());

            // Guardamos el snapshot descriptivo de los impuestos aplicados
            String appliedGroups = (result.getAppliedTaxes() != null)
                    ? result.getAppliedTaxes().stream()
                            .map(AppliedTax::toString)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("Exento/0%")
                    : "Exento/0%";
            d.setAppliedTaxGroup(appliedGroups);

            // Legacy fallbacks (to maintain backward compatibility with old UI while
            // transitioning)
            d.setIvaAmount(result.getTotalTaxAmount());
            if (result.getAppliedTaxes() != null && result.getAppliedTaxes().size() == 1) {
                d.setIvaRate(result.getAppliedTaxes().get(0).getTaxRate());
            } else {
                d.setIvaRate(0); // Mixed or 0
            }

            // Snapshot fiscal: nombre del producto en el momento de la venta
            d.setProductName(product.getName());
            d.setSkuSnapshot(product.getSku());
            d.setCategoryNameSnapshot(product.getCategoryName());
            details.add(d);
        }

        // Calcular el ahorro bruto (con impuestos) para guardarlo en discount_amount
        double grossSavingsTotal = 0.0;
        for (CartItem item : cartItems) {
            double lineDiscount = promoResult.getItemDiscounts().getOrDefault(item.getProduct().getId(), 0.0);
            double taxRate = item.getProduct().resolveEffectiveIva(config.getTaxRate());
            double taxMultiplier = isInclusive ? 1.0 : (1.0 + (taxRate / 100.0));
            grossSavingsTotal += lineDiscount * taxMultiplier;
        }
        double globalDiscount = promoResult.getItemDiscounts().getOrDefault(-1, 0.0);
        grossSavingsTotal += globalDiscount;

        double finalTotalTax = calculatedTotalIva;
        double finalTotalNet = (total - discountAmount) - finalTotalTax;

        Sale sale = new Sale();
        sale.setSaleDateTime(LocalDateTime.now());
        sale.setUserId(userId);
        sale.setClientId(clientId);
        sale.setTotal(total - discountAmount); // Descuento total = Auto (ya en total) + Manual
        sale.setPaymentMethod(paymentMethod);
        sale.setIva(calculatedTotalIva);
        sale.setTotalNet(finalTotalNet);
        sale.setTotalTax(finalTotalTax);
        sale.setCustomerNameSnapshot(client != null ? client.getName() : "Consumidor Final");
        sale.setDiscountAmount(grossSavingsTotal + discountAmount);
        sale.setDiscountReason(((discountReason != null ? discountReason : "") + " "
                + String.join(", ", promoResult.getAppliedPromos())).trim());
        sale.setReturn(false);
        sale.setDetails(details);

        // 2. Generar el sumario fiscal (Tax Summary) de toda la venta
        List<SaleTaxSummary> taxSummaries = taxEngineService.summarizeTaxes(lineResults);
        sale.setTaxSummaries(taxSummaries);

        // ── PROCESAMIENTO ATÓMICO EN UNA SOLA TRANSACCIÓN ──
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Guardar Cabecera
                int saleId = saleRepository.saveSale(sale, conn);

                // 2. Guardar Detalles
                saleRepository.saveSaleDetails(details, saleId, conn);

                // 3. Guardar Resúmenes de Impuestos
                saleRepository.saveSaleTaxSummaries(taxSummaries, saleId, conn);

                // 4. Actualizar Stock
                for (CartItem item : cartItems) {
                    if (item.getProduct().isManageStock()) {
                        int newStock = productRepository.updateStock(item.getProduct().getId(), -item.getQuantity(),
                                conn);

                        // Notificar si el stock ha bajado del mínimo (opcional: lanzar evento o log)
                        if (newStock <= item.getProduct().getMinStock()) {
                            System.out.println("ALERTA STOCK: El producto " + item.getProduct().getName() +
                                    " ha alcanzado el stock mínimo (" + newStock + ")");
                        }
                    }
                }

                conn.commit();

                // Notificar cambio de datos (para refrescar stock en UI si es necesario)
                if (eventBus != null) {
                    eventBus.publishDataChange();
                }

                return saleId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<Sale> getHistory(LocalDate start, LocalDate end) throws SQLException {
        authService.checkPermission("HISTORIAL");
        return saleRepository.getByRange(start, end);
    }

    public Sale getSaleDetails(int saleId) throws SQLException {
        return saleRepository.getById(saleId);
    }

    /**
     * Registra una devolución parcial o total de una venta.
     */
    public void registerPartialReturn(int saleId, Map<Integer, Integer> returnItems, String reason, int userId)
            throws SQLException {
        authService.checkPermission("VENTAS");
        Sale sale = saleRepository.getById(saleId);
        if (sale == null)
            return;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                double refundAmountForThisTransaction = 0;
                boolean allReturned = true;
                List<ReturnDetail> newReturnDetails = new ArrayList<>();

                for (SaleDetail d : sale.getDetails()) {
                    if (returnItems.containsKey(d.getDetailId())) {
                        int qtyToReturnNow = returnItems.get(d.getDetailId());
                        int maxReturnable = d.getQuantity() - d.getReturnedQuantity();

                        if (qtyToReturnNow > maxReturnable)
                            qtyToReturnNow = maxReturnable;

                        if (qtyToReturnNow > 0) {
                            int newTotalReturn = d.getReturnedQuantity() + qtyToReturnNow;
                            saleRepository.updateDetailReturnedQuantity(d.getDetailId(), newTotalReturn, conn);
                            d.setReturnedQuantity(newTotalReturn);

                            // USAR PRECIO FINAL (CON IVA) PARA LA DEVOLUCIÓN
                            double unitRefundPrice = d.getGrossTotal() / d.getQuantity();
                            double lineRefund = qtyToReturnNow * unitRefundPrice;
                            refundAmountForThisTransaction += lineRefund;

                            ReturnDetail rd = new ReturnDetail();
                            rd.setProductId(d.getProductId());
                            rd.setQuantity(qtyToReturnNow);
                            rd.setUnitPrice(unitRefundPrice);
                            rd.setSubtotal(lineRefund);
                            newReturnDetails.add(rd);

                            // INCREMENTAR STOCK AL DEVOLVER
                            com.mycompany.ventacontrolfx.infrastructure.persistence.JdbcProductRepository productRepo = new com.mycompany.ventacontrolfx.infrastructure.persistence.JdbcProductRepository();
                            productRepo.updateStock(d.getProductId(), qtyToReturnNow, conn);
                        }
                    }
                    if (d.getReturnedQuantity() < d.getQuantity())
                        allReturned = false;
                }

                if (refundAmountForThisTransaction > 0) {
                    // Validación de efectivo disponible para la devolución
                    if (cashClosureUseCase != null) {
                        cashClosureUseCase.validateCashAvailableForReturn(refundAmountForThisTransaction);
                    }

                    Return newReturn = new Return();
                    newReturn.setSaleId(saleId);
                    newReturn.setUserId(userId);
                    newReturn.setTotalRefunded(refundAmountForThisTransaction);
                    newReturn.setReason(reason);
                    newReturn.setReturnDatetime(LocalDateTime.now());
                    newReturn.setPaymentMethod(sale.getPaymentMethod());

                    int returnId = saleRepository.saveReturn(newReturn, conn);
                    saleRepository.saveReturnDetails(newReturnDetails, returnId, conn);

                    double newTotalReturned = sale.getReturnedAmount() + refundAmountForThisTransaction;
                    saleRepository.updateSaleReturnStatus(saleId, allReturned,
                            allReturned ? reason : reason + " (Parcial)", newTotalReturned, conn);

                    // Las devoluciones se hacen SIEMPRE en efectivo
                    if (cashClosureUseCase != null) {
                        String cashReason = String.format("[Devolución Ticket #%d - Origen: %s] %s", saleId,
                                sale.getPaymentMethod(), reason);
                        cashClosureUseCase.registerCashReturn(refundAmountForThisTransaction, cashReason, userId, conn);
                    }
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof SQLException)
                    throw (SQLException) e;
                throw new SQLException("Error durante la transacción de devolución: " + e.getMessage(), e);
            }
        }
    }

    public int getTotalSalesCount() throws SQLException {
        return saleRepository.count();
    }

    public List<Sale> getSalesByRange(LocalDate from, LocalDate to) throws SQLException {
        return saleRepository.getByRange(from, to);
    }

    public List<Sale> getSalesByUser(int userId, LocalDate from, LocalDate to) throws SQLException {
        return saleRepository.getByRange(from, to).stream()
                .filter(s -> s.getUserId() == userId && !s.isReturn())
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Sale> getSalesByClient(int clientId, LocalDate from, LocalDate to) throws SQLException {
        return saleRepository.getByRange(from, to).stream()
                .filter(s -> s.getClientId() != null && s.getClientId() == clientId && !s.isReturn())
                .collect(java.util.stream.Collectors.toList());
    }

    public List<ReturnDetail> getReturnDetails(int returnId) throws SQLException {
        return saleRepository.getReturnDetailsByReturnId(returnId);
    }

    public List<Return> getReturnsHistory(LocalDate start, LocalDate end) throws SQLException {
        authService.checkPermission("HISTORIAL");
        return saleRepository.getReturnsByRange(start, end);
    }
}
