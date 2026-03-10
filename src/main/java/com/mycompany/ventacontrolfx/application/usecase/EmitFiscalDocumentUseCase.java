package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.FiscalDocument;
import com.mycompany.ventacontrolfx.domain.model.FiscalDocument.Status;
import com.mycompany.ventacontrolfx.domain.model.FiscalDocument.Type;
import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.model.SaleDetail;
import com.mycompany.ventacontrolfx.domain.repository.IDocumentSeriesRepository;
import com.mycompany.ventacontrolfx.domain.repository.IFiscalDocumentRepository;
import com.mycompany.ventacontrolfx.domain.repository.ISaleRepository;
import com.mycompany.ventacontrolfx.domain.repository.ICompanyConfigRepository;
import com.mycompany.ventacontrolfx.domain.service.FiscalIntegrityService;
import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.mycompany.ventacontrolfx.application.ports.IFiscalPdfService;
import com.mycompany.ventacontrolfx.application.usecase.QueryFiscalDocumentUseCase.PrintData;

/**
 * Caso de uso: Emisión de documentos fiscales (Ticket y Factura).
 * Clean Architecture — Capa de Aplicación.
 *
 * Coordina sin contener lógica de negocio propia:
 * 1. Recoge los datos de la venta (snapshot).
 * 2. Asigna número correlativo de forma atómica.
 * 3. Calcula hash de integridad (vía Domain Service).
 * 4. Persiste el documento fiscal.
 */
public class EmitFiscalDocumentUseCase {

    private final ISaleRepository saleRepository;
    private final IFiscalDocumentRepository fiscalRepository;
    private final IDocumentSeriesRepository seriesRepository;
    private final ICompanyConfigRepository configRepository;
    private final FiscalIntegrityService integrityService;
    private IFiscalPdfService pdfService; // Opcional, inyectado vía setter

    public EmitFiscalDocumentUseCase(
            ISaleRepository saleRepository,
            IFiscalDocumentRepository fiscalRepository,
            IDocumentSeriesRepository seriesRepository,
            ICompanyConfigRepository configRepository) {
        this.saleRepository = saleRepository;
        this.fiscalRepository = fiscalRepository;
        this.seriesRepository = seriesRepository;
        this.configRepository = configRepository;
        this.integrityService = new FiscalIntegrityService();
    }

    public void setPdfService(IFiscalPdfService pdfService) {
        this.pdfService = pdfService;
    }

    // ── PUBLIC API ─────────────────────────────────────────────────────

    /**
     * Emite un Ticket (Factura Simplificada) para una venta ya procesada.
     * Si la venta ya tiene un documento emitido, lo retorna sin crear duplicados.
     *
     * @param saleId ID de la venta en la tabla `sales`
     * @return El documento fiscal emitido con su número correlativo.
     */
    public FiscalDocument emitTicket(int saleId) throws SQLException {
        // Idempotencia: si ya tiene documento, retornarlo
        Optional<FiscalDocument> existing = fiscalRepository.findBySaleId(saleId);
        if (existing.isPresent()) {
            return existing.get();
        }
        return emitDocument(saleId, Type.TICKET, "T", null, null, null);
    }

    /**
     * Emite una Factura Completa. Requiere los datos fiscales del receptor.
     *
     * @param saleId          ID de la venta
     * @param receiverName    Nombre o razón social del cliente
     * @param receiverTaxId   NIF/CIF del cliente
     * @param receiverAddress Dirección fiscal del cliente
     */
    public FiscalDocument emitInvoice(int saleId,
            String receiverName, String receiverTaxId, String receiverAddress)
            throws SQLException {
        Optional<FiscalDocument> existing = fiscalRepository.findBySaleId(saleId);
        if (existing.isPresent() && existing.get().getDocType() == Type.FACTURA) {
            return existing.get();
        }
        return emitDocument(saleId, Type.FACTURA, "F", receiverName, receiverTaxId, receiverAddress);
    }

    /**
     * Anula un documento emitido. No elimina datos; solo cambia el estado.
     */
    public void cancelDocument(int saleId) throws SQLException {
        Optional<FiscalDocument> doc = fiscalRepository.findBySaleId(saleId);
        if (doc.isEmpty()) {
            throw new IllegalStateException("No existe documento fiscal para la venta #" + saleId);
        }
        if (!doc.get().canTransitionTo(Status.ANULADO)) {
            throw new IllegalStateException("El documento ya está en estado: " + doc.get().getDocStatus());
        }
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                fiscalRepository.updateStatus(saleId, Status.ANULADO, conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new SQLException("Error anulando documento: " + e.getMessage(), e);
            }
        }
    }

    // ── PRIVATE HELPERS ────────────────────────────────────────────────

    private FiscalDocument emitDocument(int saleId, Type type, String seriesCode,
            String receiverName, String receiverTaxId, String receiverAddress)
            throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                FiscalDocument doc = buildDocument(saleId, type, seriesCode,
                        receiverName, receiverTaxId, receiverAddress, conn);
                integrityService.stampHash(doc);
                fiscalRepository.save(doc, conn);
                conn.commit();

                // ── ARCHIVADO AUTOMÁTICO EN PDF ──
                if (pdfService != null) {
                    archiveInPdf(doc);
                }

                return doc;
            } catch (Exception e) {
                conn.rollback();
                throw new SQLException("Error emitiendo documento fiscal: " + e.getMessage(), e);
            }
        }
    }

    private void archiveInPdf(FiscalDocument doc) {
        try {
            // 1. Obtener datos para el PDF
            Sale sale = saleRepository.getById(doc.getSaleId());
            List<SaleDetail> lines = saleRepository.getDetailsBySaleId(doc.getSaleId());
            String logoPath = configRepository.getValue("logoPath");
            PrintData data = new PrintData(doc, sale, lines, logoPath);

            // 2. Definir ruta de guardado: archivos_fiscales/YYYY/MM/
            String year = String.valueOf(doc.getIssuedAt().getYear());
            String month = String.format("%02d", doc.getIssuedAt().getMonthValue());

            // Clasificación por tipo de documento
            String typeFolder;
            if (doc.getDocType() == Type.FACTURA)
                typeFolder = "Facturas";
            else
                typeFolder = "Tickets";

            String dirPath = "archivos_fiscales/" + year + "/" + month + "/" + typeFolder;

            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }

            String fileName = doc.getFullReference().replace("/", "_") + ".pdf";
            String fullPath = dirPath + "/" + fileName;

            // 3. Generar PDF
            pdfService.generateInvoicePdf(data, fullPath);
            System.out.println("PDF FISCAL ARCHIVADO: " + fullPath);

        } catch (Exception e) {
            System.err.println("CRÍTICO: Fallo al archivar PDF fiscal de seguridad: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private FiscalDocument buildDocument(int saleId, Type type, String seriesCode,
            String receiverName, String receiverTaxId, String receiverAddress,
            Connection conn) throws SQLException {

        Sale sale = saleRepository.getById(saleId);
        if (sale == null) {
            throw new IllegalArgumentException("Venta no encontrada: #" + saleId);
        }

        // Número correlativo (ATÓMICO dentro de la transacción)
        int docNumber = seriesRepository.getAndIncrement(seriesCode, conn);

        FiscalDocument doc = new FiscalDocument();

        // Snapshot del emisor desde la configuración actual
        doc.setIssuerName(orEmpty(configRepository.getValue("companyName")));
        doc.setIssuerTaxId(orEmpty(configRepository.getValue("cif")));
        doc.setIssuerAddress(orEmpty(configRepository.getValue("address")));
        doc.setIssuerPhone(orEmpty(configRepository.getValue("phone")));

        // Desgloses de IVA
        double totalAmount = sale.getTotal();
        double vatAmount = sale.getIva();
        double baseAmount = totalAmount - vatAmount;

        doc.setSaleId(saleId);
        doc.setDocType(type);
        doc.setDocSeries(seriesCode);
        doc.setDocNumber(docNumber);
        doc.setDocStatus(Status.EMITIDO);
        doc.setIssuedAt(LocalDateTime.now());
        doc.setBaseAmount(baseAmount);
        doc.setVatAmount(vatAmount);
        doc.setTotalAmount(totalAmount);

        if (receiverName != null) {
            doc.setReceiverName(receiverName);
            doc.setReceiverTaxId(receiverTaxId);
            doc.setReceiverAddress(receiverAddress);
        }

        return doc;
    }

    private String orEmpty(String val) {
        return val != null ? val : "";
    }
}
