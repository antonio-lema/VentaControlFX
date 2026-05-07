package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.*;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import com.mycompany.ventacontrolfx.domain.repository.ISaleRepository;
import com.mycompany.ventacontrolfx.domain.repository.ICompanyConfigRepository;
import com.mycompany.ventacontrolfx.domain.repository.IClientRepository;
import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import com.mycompany.ventacontrolfx.infrastructure.security.AuthorizationService;
import com.mycompany.ventacontrolfx.domain.repository.IDocumentSeriesRepository;
import com.mycompany.ventacontrolfx.domain.service.TaxEngineService;

import com.mycompany.ventacontrolfx.shared.bus.GlobalEventBus;

import com.mycompany.ventacontrolfx.application.ports.IFiscalPdfService;
import com.mycompany.ventacontrolfx.application.usecase.QueryFiscalDocumentUseCase.PrintData;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Caso de uso para la gesti\u00f3n de ventas, incluyendo el procesamiento de
 * nuevas
 * ventas,
 * historial, detalles y devoluciones.
 */
public class SaleUseCase {

    private final ISaleRepository saleRepository;
    private final ICompanyConfigRepository configRepository;
    private final AuthorizationService authService;
    private final TaxEngineService taxEngineService;
    private final IClientRepository clientRepository;
    private final com.mycompany.ventacontrolfx.application.service.PromotionEngine promotionEngine;
    private final IProductRepository productRepository;
    private final IDocumentSeriesRepository seriesRepository;
    private final GlobalEventBus eventBus;
    private CashClosureUseCase cashClosureUseCase;
    private IFiscalPdfService pdfService;
    private final com.mycompany.ventacontrolfx.domain.service.FiscalEngineService fiscalEngine;

    public static class ProcessSaleResult {
        public int saleId;
        public String rewardPromoCode;
        public double rewardAmount;
        public LocalDateTime rewardExpiryDate;
    }

    public void setPdfService(IFiscalPdfService pdfService) {
        this.pdfService = pdfService;
    }

    public SaleUseCase(ISaleRepository saleRepository,
            ICompanyConfigRepository configRepository,
            AuthorizationService authService,
            TaxEngineService taxEngineService,
            IClientRepository clientRepository,
            com.mycompany.ventacontrolfx.application.service.PromotionEngine promotionEngine,
            IProductRepository productRepository,
            IDocumentSeriesRepository seriesRepository,
            GlobalEventBus eventBus) {
        this.saleRepository = saleRepository;
        this.configRepository = configRepository;
        this.authService = authService;
        this.taxEngineService = taxEngineService;
        this.clientRepository = clientRepository;
        this.promotionEngine = promotionEngine;
        this.productRepository = productRepository;
        this.seriesRepository = seriesRepository;
        this.eventBus = eventBus;
        this.fiscalEngine = new com.mycompany.ventacontrolfx.domain.service.FiscalEngineService();
    }

    /**
     * Inyecta el use case de caja para registrar movimientos de devoluci\u00f3n.
     */
    public void setCashClosureUseCase(CashClosureUseCase cashClosureUseCase) {
        this.cashClosureUseCase = cashClosureUseCase;
    }

    public int processSale(List<CartItem> cartItems, double total, String paymentMethod, Integer clientId, int userId)
            throws SQLException {
        return processSale(cartItems, total, paymentMethod, clientId, userId, 0.0, null, total, 0.0, null, null).saleId;
    }

    public int processSale(List<CartItem> cartItems, double total, String paymentMethod, Integer clientId, int userId,
            double discountAmount, String discountReason) throws SQLException {
        return processSale(cartItems, total, paymentMethod, clientId, userId, discountAmount, discountReason,
                total - discountAmount, 0.0, null, null).saleId;
    }

    public int processSale(List<CartItem> cartItems, double total, String paymentMethod, Integer clientId, int userId,
            double discountAmount, String discountReason, double cashAmount, double cardAmount, String observations)
            throws SQLException {
        return processSale(cartItems, total, paymentMethod, clientId, userId, discountAmount, discountReason,
                cashAmount, cardAmount, observations, null).saleId;
    }

    public ProcessSaleResult processSale(List<CartItem> cartItems, double total, String paymentMethod, Integer clientId,
            int userId,
            double discountAmount, String discountReason, double cashAmount, double cardAmount, String observations,
            String promoCode)
            throws SQLException {
        authService.checkPermission("VENTAS");

        // ──── REGLA DE NEGOCIO: Validar caja abierta ────
        if (cashClosureUseCase != null && !cashClosureUseCase.hasActiveFund()) {
            throw new SQLException(
                    "OPERACION_BLOQUEADA: La caja no ha sido abierta. Debe establecer un fondo de caja antes de realizar ventas.");
        }

        // VALIDACIÓN DE INTEGRIDAD FINANCIERA (Anti-descuadre)
        double totalToPay = total - discountAmount;
        double deliveredMoney = cashAmount + cardAmount;
        if (Math.abs(totalToPay - deliveredMoney) > 0.01) {
            throw new SQLException(
                    "ERROR_PAGO: El dinero entregado (" + String.format("%.2f", deliveredMoney)
                            + ") no coincide con el total a pagar (" + String.format("%.2f", totalToPay) + ").");
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
                .process(cartItems, promoCode, client);
        // double totalPromoDiscount = promoResult.getTotalDiscount(); // Resolving
        // lint: not needed here as we use itemDiscounts

        for (CartItem item : cartItems) {
            Product product = item.getProduct();

            double autoLineDiscount = promoResult.getItemDiscounts().getOrDefault(product.getId(), 0.0);
            double totalLineDiscount = autoLineDiscount + item.getManualDiscountAmount();

            // Precio unitario original (con tarifa de cliente ya aplicada)
            double unitPrice = item.getUnitPrice();

            // Calculamos el total de la línea YA con descuento para que el TaxEngine
            // recalcule la base e IVA
            double grossLineTotal = (unitPrice * item.getQuantity()) - totalLineDiscount;

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
            d.setUnitPrice(item.getUnitPrice());
            d.setObservations(item.getObservations());

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
            double autoLineDiscount = promoResult.getItemDiscounts().getOrDefault(item.getProduct().getId(), 0.0);
            double totalLineDiscount = autoLineDiscount + item.getManualDiscountAmount();

            double taxRate = item.getProduct().resolveEffectiveIva(config.getTaxRate());
            double taxMultiplier = isInclusive ? 1.0 : (1.0 + (taxRate / 100.0));
            grossSavingsTotal += totalLineDiscount * taxMultiplier;
        }
        double globalDiscount = promoResult.getItemDiscounts().getOrDefault(-1, 0.0);
        grossSavingsTotal += globalDiscount;

        // Total a pagar final
        double finalTotalPayable = total - discountAmount;
        // Total antes de descuentos manuales/globales pero después de promos de
        // item
        double preDiscountTotal = total;

        // --- FIX FISCAL: Prorratear descuento global en bases e impuestos ---
        double factor = (preDiscountTotal > 0) ? finalTotalPayable / preDiscountTotal : 1.0;

        double finalTotalTax = calculatedTotalIva * factor;
        double finalTotalNet = finalTotalPayable - finalTotalTax;

        // 2. Generación de Cupón de Recompensa (Reward) - Cálculo anticipado (V1.13)
        String finalRewardCode = null;
        if (finalTotalPayable >= 5.0) {
            finalRewardCode = generateRandomCode();
            // System.out.println("**************************************************");
            // System.out.println("[SALE_PROCESS] CODIGO GENERADO: " + finalRewardCode);
            // System.out.println("**************************************************");
        }

        Sale sale = new Sale();
        sale.setRewardPromoCode(finalRewardCode);
        sale.setSaleDateTime(LocalDateTime.now());
        sale.setUserId(userId);
        sale.setClientId(clientId);
        sale.setTotal(Math.round(finalTotalPayable * 100.0) / 100.0);
        sale.setPaymentMethod(paymentMethod);
        sale.setIva(Math.round(finalTotalTax * 100.0) / 100.0);
        sale.setTotalNet(Math.round(finalTotalNet * 100.0) / 100.0);
        sale.setTotalTax(Math.round(finalTotalTax * 100.0) / 100.0);
        sale.setCustomerNameSnapshot(client != null ? client.getName() : "Consumidor Final");
        sale.setDiscountAmount(Math.round((grossSavingsTotal + discountAmount) * 100.0) / 100.0);
        sale.setDiscountReason(((discountReason != null ? discountReason : "") + " "
                + String.join(", ", promoResult.getAppliedPromos())).trim());
        sale.setReturn(false);
        sale.setDetails(details);
        sale.setCashAmount(cashAmount);
        sale.setCardAmount(cardAmount);
        sale.setObservations(observations);
        sale.setPromoCode(promoCode);

        // 2. Generar el sumario fiscal (Tax Summary) de toda la venta
        List<SaleTaxSummary> taxSummaries = taxEngineService.summarizeTaxes(lineResults);

        // Aplicar el factor de descuento a cada línea del sumario fiscal
        if (factor != 1.0) {
            for (SaleTaxSummary sum : taxSummaries) {
                sum.setTaxBasis(Math.round(sum.getTaxBasis() * factor * 100.0) / 100.0);
                sum.setTaxAmount(Math.round(sum.getTaxAmount() * factor * 100.0) / 100.0);
            }
        }
        sale.setTaxSummaries(taxSummaries);

        // ──── PROCESAMIENTO ATÓMICO EN UNA SOLA TRANSACCIÓN ────
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 0. GESTIÓN FISCAL (VeriFactu Chaining)
                String series = (clientId != null) ? "F" : "A";
                String prevHash = saleRepository.getLastControlHash(series);
                int nextNumber = seriesRepository.getAndIncrement(series, conn);

                // AEAT Hashing
                String nifEmisor = configRepository.getValue("cif");
                if (nifEmisor == null || nifEmisor.isEmpty())
                    nifEmisor = "99999910G";

                String fechaExp = sale.getSaleDateTime()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                // Deterministic ISO: LocalDateTime + ZoneId + Pattern
                String ahoraIso = sale.getSaleDateTime()
                        .atZone(java.time.ZoneId.of("Europe/Madrid"))
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx"));

                String tipoFacturaCode = (clientId != null) ? "F1" : "F2";

                StringBuilder sbHuella = new StringBuilder();
                sbHuella.append("IDEmisorFactura=").append(nifEmisor);
                sbHuella.append("&NumSerieFactura=").append(
                        sale.getSaleDateTime().getYear() + "-" + series + "-" + String.format("%05d", nextNumber));
                sbHuella.append("&FechaExpedicionFactura=").append(fechaExp);
                sbHuella.append("&TipoFactura=").append(tipoFacturaCode);
                sbHuella.append("&CuotaTotal=").append(String.format(java.util.Locale.US, "%.2f", sale.getIva()));
                sbHuella.append("&ImporteTotal=").append(String.format(java.util.Locale.US, "%.2f", sale.getTotal()));
                sbHuella.append("&Huella=").append(prevHash != null ? prevHash.toUpperCase() : "");
                sbHuella.append("&FechaHoraHusoGenRegistro=").append(ahoraIso);

                String currentHash = sha256(sbHuella.toString()).toUpperCase();

                // 1. Preparar y Guardar Venta
                sale.setDocSeries(series);
                sale.setDocNumber(nextNumber);
                sale.setDocType(clientId != null ? "FACTURA" : "TICKET");
                sale.setDocStatus("EMITIDO");
                sale.setControlHash(currentHash);
                sale.setPrevHash(prevHash);

                int saleId = saleRepository.saveSale(sale, conn);
                sale.setSaleId(saleId);

                // 2. Guardar Detalles y Sumarios
                saleRepository.saveSaleDetails(details, saleId, conn);
                saleRepository.saveSaleTaxSummaries(taxSummaries, saleId, conn);

                // 3. Generar Registro Fiscal (Nace como EMITIDO en la DB local)
                com.mycompany.ventacontrolfx.domain.model.FiscalDocument fiscalDoc = com.mycompany.ventacontrolfx.domain.model.FiscalDocument
                        .builder()
                        .saleId(saleId)
                        .type(com.mycompany.ventacontrolfx.domain.model.FiscalDocument.Type.valueOf(sale.getDocType()))
                        .series(series)
                        .number(nextNumber)
                        .status(com.mycompany.ventacontrolfx.domain.model.FiscalDocument.Status.EMITIDO)
                        .issuedAt(sale.getSaleDateTime())
                        .issuer(
                                configRepository.getValue("companyName"),
                                nifEmisor,
                                configRepository.getValue("address"),
                                configRepository.getValue("phone"))
                        .receiver(sale.getCustomerNameSnapshot(), null, null)
                        .amounts(sale.getTotal() - sale.getIva(), sale.getIva(), sale.getTotal())
                        .controlHash(currentHash)
                        .build();

                com.mycompany.ventacontrolfx.infrastructure.persistence.JdbcFiscalDocumentRepository fiscalRepo = new com.mycompany.ventacontrolfx.infrastructure.persistence.JdbcFiscalDocumentRepository();
                fiscalRepo.save(fiscalDoc, conn);

                // 4. Actualizar Stock
                for (CartItem item : cartItems) {
                    if (item.getProduct().isManageStock()) {
                        int newStock = productRepository.updateStock(item.getProduct().getId(), -item.getQuantity(),
                                conn);

                        // Notificar si el stock ha bajado del mínimo (opcional: lanzar evento o
                        // log)
                        if (newStock <= item.getProduct().getMinStock()) {
                            System.out.println("ALERTA STOCK: El producto " + item.getProduct().getName() +
                                    " ha alcanzado el stock mínimo (" + newStock + ")");
                        }
                    }
                }

                // 5. Incrementar contadores de promociones
                com.mycompany.ventacontrolfx.domain.repository.IPromotionRepository promoRepo = new com.mycompany.ventacontrolfx.infrastructure.persistence.JdbcPromotionRepository();
                if (promoResult.getAppliedPromoCodes() != null) {
                    for (String promoCodeOrName : promoResult.getAppliedPromoCodes()) {
                        try {
                            Promotion p = promoRepo.findByCode(promoCodeOrName).orElse(null);
                            if (p == null) {
                                // Fallback by name if it wasn't a code
                                p = promoRepo.getAll().stream()
                                        .filter(p1 -> p1.getName().equals(promoCodeOrName))
                                        .findFirst().orElse(null);
                            }
                            if (p != null) {
                                p.setCurrentUses(p.getCurrentUses() + 1);
                                promoRepo.update(p);
                            }
                        } catch (Exception ex) {
                            System.err.println("[SaleUseCase] Error incrementing promo usage: " + ex.getMessage());
                        }
                    }
                }

                // 6. Persistir Cupón de Recompensa si existe
                if (sale.getRewardPromoCode() != null) {
                    Promotion reward = new Promotion();
                    reward.setName("Recompensa por compra > 100€");
                    reward.setCode(sale.getRewardPromoCode());
                    reward.setType(com.mycompany.ventacontrolfx.domain.model.PromotionType.FIXED_DISCOUNT);
                    reward.setValue(10.0); // 10€ de descuento
                    reward.setScope(com.mycompany.ventacontrolfx.domain.model.PromotionScope.GLOBAL);
                    reward.setMaxUses(1);
                    reward.setActive(true);
                    reward.setMinOrderValue(100.0); // Restaurado el valor de producción (100€)
                    reward.setStartDate(LocalDateTime.now());
                    reward.setEndDate(LocalDateTime.now().plusMonths(3));
                    // Eliminamos la vinculación al cliente para que sea más fácil de usar
                    // if (clientId != null) reward.setCustomerId(clientId);

                    promoRepo.save(reward);
                }

                // ———— ARCHIVADO PDF FISCAL ————
                archiveSaleInPdf(sale, details);

                conn.commit();

                // Notificar cambio de datos (para refrescar stock en UI si es necesario)
                if (eventBus != null) {
                    eventBus.publishDataChange();
                }

                ProcessSaleResult result = new ProcessSaleResult();
                result.saleId = saleId;
                result.rewardPromoCode = sale.getRewardPromoCode();
                result.rewardAmount = 10.0;
                result.rewardExpiryDate = LocalDateTime.now().plusMonths(3);
                return result;
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
     * Registra una devoluci\u00f3n parcial o total de una venta.
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
                double totalTaxRefunded = 0;
                double totalNetRefunded = 0;
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

                            // USAR PRECIO FINAL (CON IVA) PARA LA DEVOLUCI\u00d3N
                            double unitRefundPrice = d.getGrossTotal() / d.getQuantity();
                            double lineRefund = Math.round((qtyToReturnNow * unitRefundPrice) * 100.0) / 100.0;
                            refundAmountForThisTransaction += lineRefund;

                            // CALCULO PROPORCIONAL DE IMPUESTOS
                            double factor = (double) qtyToReturnNow / d.getQuantity();
                            double lineTaxRefund = Math.round((d.getTaxAmount() * factor) * 100.0) / 100.0;
                            double lineNetRefund = lineRefund - lineTaxRefund;

                            totalTaxRefunded += lineTaxRefund;
                            totalNetRefunded += lineNetRefund;

                            ReturnDetail rd = new ReturnDetail();
                            rd.setProductId(d.getProductId());
                            rd.setQuantity(qtyToReturnNow);
                            rd.setUnitPrice(unitRefundPrice);
                            rd.setSubtotal(lineRefund);
                            rd.setTaxAmount(lineTaxRefund);
                            rd.setNetAmount(lineNetRefund);
                            rd.setProductName(d.getProductName()); // Snapshot del nombre original
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
                    // Logic for Mixed Payment Returns: Refund Proportionally to what was paid
                    double currentGrossTotal = sale.getTotal() + sale.getDiscountAmount();
                    double cashRatio = (currentGrossTotal > 0) ? sale.getCashAmount() / currentGrossTotal : 1.0;

                    double cashToRefund = Math.round((refundAmountForThisTransaction * cashRatio) * 100.0) / 100.0;
                    double cardToRefund = Math.round((refundAmountForThisTransaction - cashToRefund) * 100.0) / 100.0;

                    // Ensure we don't refund more than what was actually paid in each method
                    List<Return> prevReturns = saleRepository.getReturnsBySaleId(saleId);
                    double totalCashReturnedSoFar = prevReturns.stream().mapToDouble(Return::getCashAmount).sum();
                    double totalCardReturnedSoFar = prevReturns.stream().mapToDouble(Return::getCardAmount).sum();

                    double availableCash = Math.max(0, sale.getCashAmount() - totalCashReturnedSoFar);
                    double availableCard = Math.max(0, sale.getCardAmount() - totalCardReturnedSoFar);

                    if (cashToRefund > availableCash) {
                        double excess = cashToRefund - availableCash;
                        cashToRefund = availableCash;
                        cardToRefund += excess;
                    }
                    if (cardToRefund > availableCard) {
                        double excess = cardToRefund - availableCard;
                        cardToRefund = availableCard;
                        cashToRefund += excess;
                    }

                    // Cap final refund
                    cashToRefund = Math.min(cashToRefund, availableCash);
                    cardToRefund = Math.min(cardToRefund, availableCard);

                    // Validaci\u00f3n de efectivo disponible para la devoluci\u00f3n (solo la parte
                    // que
                    // devolvemos en cash)
                    if (cashToRefund > 0 && cashClosureUseCase != null) {
                        cashClosureUseCase.validateCashAvailableForReturn(cashToRefund);
                    }

                    Return newReturn = new Return();
                    newReturn.setSaleId(saleId);
                    newReturn.setUserId(userId);
                    newReturn.setTotalRefunded(refundAmountForThisTransaction);
                    newReturn.setReason(reason);
                    newReturn.setReturnDatetime(LocalDateTime.now());
                    newReturn.setPaymentMethod(sale.getPaymentMethod());
                    newReturn.setCashAmount(cashToRefund);
                    newReturn.setCardAmount(cardToRefund);
                    newReturn.setTotalTax(totalTaxRefunded);
                    newReturn.setTaxBasis(totalNetRefunded);

                    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac GENERACI\u00d3N DE C\u00d3dIGO FISCAL
                    // \u00daNICO PARA LA DEVOLUCI\u00d3N \u00e2\u201d\u20ac\u00e2\u201d\u20ac
                    String seriesCode = "R";
                    int nextDocNumber = seriesRepository.getAndIncrement(seriesCode, conn);

                    // ———— GESTIÓN FISCAL (VeriFactu Chaining para Rectificativas) ————
                    String prevHash = saleRepository.getLastControlHash(seriesCode);

                    // AEAT Hashing deterministic
                    String nifEmisor = configRepository.getValue("cif");
                    if (nifEmisor == null || nifEmisor.isEmpty())
                        nifEmisor = "99999910G";

                    String fechaExp = newReturn.getReturnDatetime()
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                    String ahoraIso = newReturn.getReturnDatetime()
                            .atZone(java.time.ZoneId.of("Europe/Madrid"))
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx"));

                    String fullNumSerie = newReturn.getReturnDatetime().getYear() + "-" + seriesCode + "-"
                            + String.format("%05d", nextDocNumber);

                    StringBuilder sbHuella = new StringBuilder();
                    sbHuella.append("IDEmisorFactura=").append(nifEmisor);
                    sbHuella.append("&NumSerieFactura=").append(fullNumSerie);
                    sbHuella.append("&FechaExpedicionFactura=").append(fechaExp);
                    sbHuella.append("&TipoFactura=").append("R1"); // R1 por defecto para abonos de mostrador
                    sbHuella.append("&CuotaTotal=")
                            .append(String.format(java.util.Locale.US, "%.2f", newReturn.getTotalTax()));
                    sbHuella.append("&ImporteTotal=")
                            .append(String.format(java.util.Locale.US, "%.2f", newReturn.getTotalRefunded()));
                    sbHuella.append("&Huella=").append(prevHash != null ? prevHash.toUpperCase() : "");
                    sbHuella.append("&FechaHoraHusoGenRegistro=").append(ahoraIso);

                    String currentHash = sha256(sbHuella.toString()).toUpperCase();

                    newReturn.setControlHash(currentHash);
                    newReturn.setPrevHash(prevHash);
                    newReturn.setFiscalStatus("PENDING");
                    newReturn.setGenTimestamp(ahoraIso);
                    newReturn.setDocSeries(seriesCode);
                    newReturn.setDocNumber(nextDocNumber);
                    newReturn.setDocType("RECTIFICATIVA");
                    
                    // Snapshot del emisor para la rectificativa
                    SaleConfig companyData = configRepository.load();
                    newReturn.setIssuerName(companyData.getCompanyName());
                    newReturn.setIssuerTaxId(companyData.getCif());
                    newReturn.setIssuerAddress(companyData.getAddress());
                    newReturn.setCustomerNameSnapshot(sale.getCustomerNameSnapshot());

                    int returnId = saleRepository.saveReturn(newReturn, conn);
                    saleRepository.saveReturnDetails(newReturnDetails, returnId, conn);

                    double newTotalReturned = sale.getReturnedAmount() + refundAmountForThisTransaction;
                    saleRepository.updateSaleReturnStatus(saleId, allReturned,
                            allReturned ? reason : reason + " (Parcial)", newTotalReturned, conn);

                    // Registrar en caja SOLO la parte devuelta en efectivo
                    if (cashToRefund > 0 && cashClosureUseCase != null) {
                        String cashReason = String.format("[Devoluci\u00f3n Ticket #%d - Origen: %s] %s", saleId,
                                sale.getPaymentMethod(), reason);
                        cashClosureUseCase.registerCashReturn(cashToRefund, cashReason, userId, conn);
                    }

                    // Archivamos el PDF fiscal de la devoluci\u00f3n antes del commit (para
                    // asegurar
                    // integridad)
                    archiveReturnInPdf(newReturn, newReturnDetails);

                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof SQLException)
                    throw (SQLException) e;
                throw new SQLException("Error durante la transacci\u00f3n de devoluci\u00f3n: " + e.getMessage(), e);
            } finally {
                try {
                    if (conn != null && !conn.isClosed()) {
                        conn.setAutoCommit(true);
                    }
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public int getTotalSalesCount() throws SQLException {
        return saleRepository.count();
    }

    public List<Sale> getSalesByRange(LocalDate from, LocalDate to) throws SQLException {
        return saleRepository.getByRange(from, to, 1000); // Default safety limit
    }

    public List<Sale> getSalesByRange(LocalDate from, LocalDate to, int limit) throws SQLException {
        return saleRepository.getByRange(from, to, limit);
    }

    public com.mycompany.ventacontrolfx.domain.model.HistoryStats getHistoryStats(LocalDate start, LocalDate end)
            throws SQLException {
        return saleRepository.getStatsByRange(start, end);
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

    public List<Return> getReturnsByClient(int clientId, LocalDate from, LocalDate to) throws SQLException {
        return saleRepository.getReturnsByRange(from, to).stream()
                .filter(r -> {
                    try {
                        Sale s = saleRepository.getById(r.getSaleId());
                        return s != null && s.getClientId() != null && s.getClientId() == clientId;
                    } catch (SQLException e) {
                        return false;
                    }
                })
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Return> getReturnsBySaleId(int saleId) throws SQLException {
        return saleRepository.getReturnsBySaleId(saleId);
    }

    public List<ReturnDetail> getReturnDetails(int returnId) throws SQLException {
        return saleRepository.getReturnDetailsByReturnId(returnId);
    }

    public List<Return> getReturnsHistory(LocalDate start, LocalDate end) throws SQLException {
        authService.checkPermission("HISTORIAL");
        return saleRepository.getReturnsByRange(start, end);
    }

    public List<ProductSummary> getTopProductsByClient(int clientId, int limit) throws SQLException {
        return saleRepository.getTopProductsByClient(clientId, limit);
    }

    public List<com.mycompany.ventacontrolfx.domain.model.ClientSaleSummary> getClientSalesSummary(LocalDate start,
            LocalDate end) throws SQLException {
        return saleRepository.getClientSalesSummary(start, end);
    }

    public List<Sale> getSalesByClient(int clientId) throws SQLException {
        return saleRepository.getByClient(clientId);
    }

    /**
     * Genera y archiva el PDF de la devoluci\u00f3n (Factura Rectificativa).
     * Sincronizado con el motor de ReturnUseCase para mantener coherencia.
     */
    /**
     * Genera y archiva el PDF de la venta (Ticket o Factura).
     */
    private void archiveSaleInPdf(Sale s, List<SaleDetail> details) {
        if (this.pdfService == null)
            return;
        try {
            // Conversión para compatibilidad con IFiscalPdfService.PrintData
            FiscalDocument doc = FiscalDocument.builder()
                    .saleId(s.getSaleId())
                    .type(FiscalDocument.Type.valueOf(s.getDocType()))
                    .series(s.getDocSeries())
                    .number(s.getDocNumber())
                    .issuedAt(s.getSaleDateTime())
                    .issuer(
                            configRepository.getValue("companyName"),
                            configRepository.getValue("cif"),
                            configRepository.getValue("address"),
                            configRepository.getValue("phone"))
                    .receiver(s.getCustomerNameSnapshot(), null, null)
                    .amounts(s.getTotalNet(), s.getTotalTax(), s.getTotal())
                    .status(FiscalDocument.Status.EMITIDO)
                    .controlHash(s.getControlHash())
                    .build();

            SaleConfig companyData = configRepository.load();
            String logoPath = companyData != null ? companyData.getLogoPath() : null;
            PrintData data = new PrintData(doc, s, details, logoPath);

            String year = String.valueOf(doc.getIssuedAt().getYear());
            String month = String.format("%02d", doc.getIssuedAt().getMonthValue());
            String typeFolder = (doc.getDocType() == FiscalDocument.Type.FACTURA) ? "Facturas" : "Tickets";
            String dirPath = "archivos_fiscales/" + year + "/" + month + "/" + typeFolder;

            Path path = Paths.get(dirPath);
            if (!Files.exists(path))
                Files.createDirectories(path);

            String fileName = doc.getFullReference().replace("/", "_") + ".pdf";
            String fullPath = dirPath + "/" + fileName;

            pdfService.generateInvoicePdf(data, fullPath);
        } catch (Exception ex) {
            System.err.println("[SaleUseCase] Error archivando PDF de venta: " + ex.getMessage());
        }
    }

    private void archiveReturnInPdf(Return r, List<ReturnDetail> details) {
        if (this.pdfService == null)
            return;
        try {
            // Cargar la venta original para contexto
            Sale sale = saleRepository.getById(r.getSaleId());

            // Conversi\u00f3n para compatibilidad con IFiscalPdfService.PrintData
            FiscalDocument doc = FiscalDocument.builder()
                    .saleId(r.getSaleId())
                    .type(FiscalDocument.Type.RECTIFICATIVA)
                    .series(r.getDocSeries())
                    .number(r.getDocNumber())
                    .issuedAt(r.getReturnDatetime())
                    .issuer(r.getIssuerName(), r.getIssuerTaxId(), r.getIssuerAddress(), null)
                    .receiver(r.getCustomerNameSnapshot(), null, null)
                    .amounts(r.getTaxBasis(), r.getTotalTax(), r.getTotalRefunded())
                    .status(FiscalDocument.Status.EMITIDO)
                    .build();

            List<SaleDetail> lines = new ArrayList<>();
            for (ReturnDetail rd : details) {
                SaleDetail sd = new SaleDetail();
                sd.setProductName(rd.getProductName());
                sd.setQuantity(rd.getQuantity());
                sd.setUnitPrice(rd.getUnitPrice());
                sd.setLineTotal(rd.getSubtotal());
                lines.add(sd);
            }

            SaleConfig companyData = configRepository.load();
            String logoPath = companyData != null ? companyData.getLogoPath() : null;
            PrintData data = new PrintData(doc, sale, lines, logoPath);

            String year = String.valueOf(doc.getIssuedAt().getYear());
            String month = String.format("%02d", doc.getIssuedAt().getMonthValue());
            String dirPath = "archivos_fiscales/" + year + "/" + month + "/Devoluciones";

            Path path = Paths.get(dirPath);
            if (!Files.exists(path))
                Files.createDirectories(path);

            String fileName = doc.getFullReference().replace("/", "_") + ".pdf";
            String fullPath = dirPath + "/" + fileName;

            pdfService.generateInvoicePdf(data, fullPath);
        } catch (Exception ex) {
            System.err.println("[SaleUseCase] Error archivando PDF de devoluci\u00f3n: " + ex.getMessage());
        }
    }

    public java.util.Map<String, Double> getCategoryDistribution(LocalDate start, LocalDate end) throws SQLException {
        return saleRepository.getCategoryDistribution(start, end);
    }

    public java.util.Map<Integer, Integer> getHourlyDistribution(LocalDate start, LocalDate end) throws SQLException {
        return saleRepository.getHourlyDistribution(start, end);
    }

    public void registerCorrection(int saleId, String newName, String newNif) throws SQLException {
        authService.checkPermission("VENTAS");
        
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Obtener la venta actual
                Sale sale = saleRepository.getById(saleId);
                if (sale == null) throw new SQLException("Venta no encontrada: " + saleId);

                // 2. Actualizar los datos del cliente en la venta
                sale.setCustomerNameSnapshot(newName);
                sale.setCustomerNifSnapshot(newNif);
                
                // 3. Marcar como subsanación y pendiente
                sale.setFiscalStatus("PENDING");
                sale.setCorrection(true);
                sale.setCorrectionType("ERROR_REGISTRAL");

                // 4. Guardar cambios
                saleRepository.updateCorrectionData(saleId, newName, newNif, true, "ERROR_REGISTRAL", conn);
                
                conn.commit();
                
                if (eventBus != null) {
                    eventBus.publishDataChange();
                }
            } catch (Exception e) {
                conn.rollback();
                throw new SQLException("Error al registrar la subsanación: " + e.getMessage(), e);
            }
        }
    }

    public String resendToAeat(int saleId) throws SQLException {
        authService.checkPermission("VENTAS");
        
        // 1. Marcar como PENDING para que el OutboxManager lo recoja
        try (Connection conn = DBConnection.getConnection()) {
            saleRepository.updateFiscalStatus(saleId, "PENDING", "Puesto en cola para re-envío...", conn);
        }

        // 2. Notificar al bus para despertar al worker si está dormido
        if (eventBus != null) {
            eventBus.publishDataChange();
        }

        // 3. Pequeña espera para dar tiempo al worker
        try {
            Thread.sleep(2500); 
            Sale s = saleRepository.getById(saleId);
            if ("ACCEPTED".equals(s.getFiscalStatus())) {
                return "OK";
            } else if ("PENDING".equals(s.getFiscalStatus())) {
                // Si sigue pendiente, no es un error, es que el worker aún no ha llegado a él
                return "SOLICITADO"; 
            } else {
                return s.getFiscalMsg() != null ? s.getFiscalMsg() : "Error en proceso";
            }
        } catch (InterruptedException e) {
            return "SOLICITADO";
        }
    }

    private String generateRandomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder("GIFT-");
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String sha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}

