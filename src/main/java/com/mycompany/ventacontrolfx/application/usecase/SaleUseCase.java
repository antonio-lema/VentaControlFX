package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.*;
import com.mycompany.ventacontrolfx.domain.repository.ISaleRepository;
import com.mycompany.ventacontrolfx.domain.repository.ICompanyConfigRepository;
import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SaleUseCase {
    private final ISaleRepository saleRepository;
    private final ICompanyConfigRepository configRepository;
    private final com.mycompany.ventacontrolfx.util.AuthorizationService authService;
    private final com.mycompany.ventacontrolfx.domain.service.TaxEngineService taxEngineService;
    private final com.mycompany.ventacontrolfx.domain.repository.IClientRepository clientRepository;
    private CashClosureUseCase cashClosureUseCase;

    public SaleUseCase(ISaleRepository saleRepository, ICompanyConfigRepository configRepository,
            com.mycompany.ventacontrolfx.util.AuthorizationService authService,
            com.mycompany.ventacontrolfx.domain.service.TaxEngineService taxEngineService,
            com.mycompany.ventacontrolfx.domain.repository.IClientRepository clientRepository) {
        this.saleRepository = saleRepository;
        this.configRepository = configRepository;
        this.authService = authService;
        this.taxEngineService = taxEngineService;
        this.clientRepository = clientRepository;
    }

    /** Inyecta el use case de caja para registrar movimientos de devolución. */
    public void setCashClosureUseCase(CashClosureUseCase cashClosureUseCase) {
        this.cashClosureUseCase = cashClosureUseCase;
    }

    public int processSale(List<CartItem> cartItems, double total, String paymentMethod, Integer clientId, int userId)
            throws SQLException {
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

        for (CartItem item : cartItems) {
            Product product = item.getProduct();

            // Usar el motor fiscal para calcular impuestos de esta línea
            TaxCalculationResult result = taxEngineService.calculateLine(
                    product, client, product.getPrice(), item.getQuantity(), isInclusive);

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
            d.setLineTotal(result.getGrossTotal()); // Mantenemos lineTotal compatible

            // Guardamos el snapshot descriptivo de los impuestos aplicados
            String appliedGroups = result.getAppliedTaxes().stream()
                    .map(AppliedTax::toString)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("Exento/0%");
            d.setAppliedTaxGroup(appliedGroups);

            // Legacy fallbacks (to maintain backward compatibility with old UI while
            // transitioning)
            d.setIvaAmount(result.getTotalTaxAmount());
            if (result.getAppliedTaxes().size() == 1) {
                d.setIvaRate(result.getAppliedTaxes().get(0).getTaxRate());
            } else {
                d.setIvaRate(0); // Mixed or 0
            }

            // Snapshot fiscal: nombre del producto en el momento de la venta
            d.setProductName(product.getName());
            details.add(d);
        }

        Sale sale = new Sale();
        sale.setSaleDateTime(LocalDateTime.now());
        sale.setUserId(userId);
        sale.setClientId(clientId);
        sale.setTotal(total); // En el futuro se validará con la suma de grossTotals
        sale.setPaymentMethod(paymentMethod);
        sale.setIva(calculatedTotalIva);
        sale.setReturn(false);
        sale.setDetails(details);

        // 2. Generar el sumario fiscal (Tax Summary) de toda la venta
        List<SaleTaxSummary> taxSummaries = taxEngineService.summarizeTaxes(lineResults);
        sale.setTaxSummaries(taxSummaries);

        // 3. Persistir en la base de datos (Sale, SaleDetails y sale_tax_summary)
        int saleId = saleRepository.saveSale(sale);
        saleRepository.saveSaleDetails(details, saleId);
        saleRepository.saveSaleTaxSummaries(taxSummaries, saleId);

        return saleId;
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
     * Implementa consistencia transaccional: si el registro en caja falla, se
     * revierte todo.
     */
    public void registerPartialReturn(int saleId, Map<Integer, Integer> returnItems, String reason,
            int userId) throws SQLException {
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
                            allReturned ? reason : reason + " (Parcial)",
                            newTotalReturned, conn);

                    // Las devoluciones se hacen SIEMPRE en efectivo, independientemente de cómo se
                    // pagó
                    if (cashClosureUseCase != null) {
                        String cashReason = String.format("[Devolución Ticket #%d - Origen: %s] %s",
                                saleId, sale.getPaymentMethod(), reason);
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
