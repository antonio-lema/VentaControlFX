package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.exception.InsufficientCashInDrawerException;
import com.mycompany.ventacontrolfx.domain.exception.RefundLimitExceededException;
import com.mycompany.ventacontrolfx.application.ports.IFiscalPdfService;
import com.mycompany.ventacontrolfx.application.service.RefundCalculatorService;
import com.mycompany.ventacontrolfx.application.usecase.QueryFiscalDocumentUseCase.PrintData;
import com.mycompany.ventacontrolfx.domain.model.*;
import com.mycompany.ventacontrolfx.domain.repository.*;
import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Caso de Uso: Registro de Devoluciones (Refacturado por Swarm Agent).
 * Se encarga de la l\u00f3gica transaccional de abonos y rectificativas.
 */
public class ReturnUseCase {

    private final ISaleRepository saleRepository;
    private final IProductRepository productRepository;
    private final IDocumentSeriesRepository seriesRepository;
    private final ICompanyConfigRepository configRepository;
    private final RefundCalculatorService refundCalculator;
    private final CashClosureUseCase cashClosureUseCase;
    private IFiscalPdfService pdfService;

    public ReturnUseCase(ISaleRepository saleRepository,
            IProductRepository productRepository,
            IDocumentSeriesRepository seriesRepository,
            ICompanyConfigRepository configRepository,
            RefundCalculatorService refundCalculator,
            CashClosureUseCase cashClosureUseCase) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.seriesRepository = seriesRepository;
        this.configRepository = configRepository;
        this.refundCalculator = refundCalculator;
        this.cashClosureUseCase = cashClosureUseCase;
    }

    public void setPdfService(IFiscalPdfService pdfService) {
        this.pdfService = pdfService;
    }

    public void registerPartialReturn(int saleId, Map<Integer, Integer> quantitiesToReturn,
            String reason, int userId)
            throws SQLException, RefundLimitExceededException, InsufficientCashInDrawerException {

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Sale sale = saleRepository.getById(saleId);
                List<SaleDetail> saleDetails = saleRepository.getDetailsBySaleId(saleId);
                List<ReturnDetail> newReturnDetails = new ArrayList<>();
                BigDecimal totalRefund = BigDecimal.ZERO;
                boolean allReturned = true;

                // 1. Validar y Calcular l\u00edneas de devoluci\u00f3n
                for (SaleDetail d : saleDetails) {
                    int qtyToReturnNow = quantitiesToReturn.getOrDefault(d.getProductId(), 0);
                    int totalReturnedOnDetail = d.getReturnedQuantity() + qtyToReturnNow;

                    if (qtyToReturnNow > 0) {
                        BigDecimal unitPrice = BigDecimal.valueOf(d.getUnitPrice());
                        BigDecimal lineRefund = unitPrice.multiply(BigDecimal.valueOf(qtyToReturnNow));
                        totalRefund = totalRefund.add(lineRefund);

                        ReturnDetail rd = new ReturnDetail();
                        rd.setProductId(d.getProductId());
                        rd.setProductName(d.getProductName());
                        rd.setQuantity(qtyToReturnNow);
                        rd.setUnitPrice(d.getUnitPrice());
                        rd.setSubtotal(lineRefund.doubleValue());
                        newReturnDetails.add(rd);

                        // Actualizar Stock de producto
                        productRepository.updateStock(d.getProductId(), qtyToReturnNow, conn);

                        // FIX: Actualizar cantidad devuelta en el detalle de la venta original
                        saleRepository.updateDetailReturnedQuantity(d.getDetailId(), totalReturnedOnDetail, conn);
                    }

                    if (totalReturnedOnDetail < d.getQuantity()) {
                        allReturned = false;
                    }
                }

                if (totalRefund.compareTo(BigDecimal.ZERO) > 0) {
                    // 2. Prorrateo de pagos (Cash/Card)
                    BigDecimal grossTotalOnSale = BigDecimal.valueOf(sale.getTotal() + sale.getDiscountAmount());
                    BigDecimal cashPaidOnSale = BigDecimal.valueOf(sale.getCashAmount());

                    BigDecimal[] split = refundCalculator.calculateRefundAmounts(totalRefund, cashPaidOnSale,
                            grossTotalOnSale);
                    BigDecimal cashToRefund = split[0];
                    BigDecimal cardToRefund = split[1];

                    // 3. Validaci\u00f3n de Efectivo en Caja
                    if (cashToRefund.compareTo(BigDecimal.ZERO) > 0 && cashClosureUseCase != null) {
                        try {
                            cashClosureUseCase.validateCashAvailableForReturn(cashToRefund.doubleValue());
                        } catch (Exception e) {
                            throw new InsufficientCashInDrawerException(
                                    "No hay suficiente efectivo en el cierre actual para esta devoluci\u00f3n.");
                        }
                    }

                    // 4. Generaci\u00f3n de Factura Rectificativa (Documento)
                    String seriesCode = "R";
                    int nextNum = seriesRepository.getAndIncrement(seriesCode, conn);
                    SaleConfig company = configRepository.load();

                    Return newReturn = new Return.Builder(saleId)
                            .userId(userId)
                            .totalRefunded(totalRefund.doubleValue())
                            .cashAmount(cashToRefund.doubleValue())
                            .cardAmount(cardToRefund.doubleValue())
                            .reason(reason)
                            .docType("RECTIFICATIVA")
                            .docSeries(seriesCode)
                            .docNumber(nextNum)
                            .docStatus("EMITIDO")
                            .issuerName(company.getCompanyName())
                            .issuerTaxId(company.getCif())
                            .issuerAddress(company.getAddress())
                            .customerNameSnapshot(sale.getCustomerNameSnapshot())
                            .build();

                    int returnId = saleRepository.saveReturn(newReturn, conn);
                    saleRepository.saveReturnDetails(newReturnDetails, returnId, conn);

                    // 5. Actualizar estado de la venta original
                    double newTotalReturned = sale.getReturnedAmount() + totalRefund.doubleValue();
                    saleRepository.updateSaleReturnStatus(saleId, allReturned,
                            allReturned ? reason : reason + " (Parcial)", newTotalReturned, conn);

                    // 6. Registrar en caja
                    if (cashToRefund.compareTo(BigDecimal.ZERO) > 0 && cashClosureUseCase != null) {
                        String cashReason = String.format("[Devoluci\u00f3n Ticket #%d] %s", saleId, reason);
                        cashClosureUseCase.registerCashReturn(cashToRefund.doubleValue(), cashReason, userId, conn);
                    }

                    // 7. Archivatura PDF
                    archiveInPdf(newReturn, newReturnDetails, sale);
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof InsufficientCashInDrawerException || e instanceof RefundLimitExceededException) {
                    throw e;
                }
                throw new SQLException("Error durante la transacci\u00f3n de devoluci\u00f3n: " + e.getMessage(), e);
            }
        }
    }

    private void archiveInPdf(Return r, List<ReturnDetail> details, Sale sale) {
        if (pdfService == null)
            return;
        try {
            // Conversi\u00f3n para compatibilidad con IFiscalPdfService.PrintData
            FiscalDocument doc = FiscalDocument.builder()
                    .saleId(r.getSaleId())
                    .type(FiscalDocument.Type.RECTIFICATIVA)
                    .series(r.getDocSeries())
                    .number(r.getDocNumber())
                    .issuedAt(r.getReturnDatetime())
                    .issuer(r.getIssuerName(), r.getIssuerTaxId(), r.getIssuerAddress(), null)
                    .receiver(r.getCustomerNameSnapshot(), null, null)
                    .amounts(r.getTotalRefunded(), 0, r.getTotalRefunded())
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

            String logoPath = configRepository.getValue("logoPath");
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
            ex.printStackTrace();
        }
    }
}

