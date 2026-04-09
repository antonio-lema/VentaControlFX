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
 * Caso de uso: Emisi\u00f3n de documentos fiscales (Ticket y Factura).
 * Clean Architecture \u00e2\u20ac\u201d Capa de Aplicaci\u00f3n.
 *
 * Coordina sin contener l\u00f3gica de negocio propia:
 * 1. Recoge los datos de la venta (snapshot).
 * 2. Asigna n\u00famero correlativo de forma at\u00f3mica.
 * 3. Calcula hash de integridad (v\u00eda Domain Service).
 * 4. Persiste el documento fiscal.
 */
public class EmitFiscalDocumentUseCase {

    private final ISaleRepository saleRepository;
    private final IFiscalDocumentRepository fiscalRepository;
    private final IDocumentSeriesRepository seriesRepository;
    private final ICompanyConfigRepository configRepository;
    private final FiscalIntegrityService integrityService;
    private IFiscalPdfService pdfService; // Opcional, inyectado v\u00eda setter

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

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac PUBLIC API \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac

    /**
     * Emite un Ticket (Factura Simplificada) para una venta ya procesada.
     * Si la venta ya tiene un documento emitido, lo retorna sin crear duplicados.
     *
     * @param saleId ID de la venta en la tabla `sales`
     * @return El documento fiscal emitido con su n\u00famero correlativo.
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
     * @param receiverName    Nombre o raz\u00f3n social del cliente
     * @param receiverTaxId   NIF/CIF del cliente
     * @param receiverAddress Direcci\u00f3n fiscal del cliente
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
            throw new IllegalStateException("El documento ya est\u00e1 en estado: " + doc.get().getDocStatus());
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

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac PRIVATE HELPERS \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac

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

                // \u00e2\u201d\u20ac\u00e2\u201d\u20ac ARCHIVADO AUTOM\u00c1TICO EN PDF \u00e2\u201d\u20ac\u00e2\u201d\u20ac
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

            // Clasificaci\u00f3n por tipo de documento
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
            System.err.println("CR\u00cdTICO: Fallo al archivar PDF fiscal de seguridad: " + e.getMessage());
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

        // N\u00famero correlativo (AT\u00c3\u201cMICO dentro de la transacci\u00f3n)
        int docNumber = seriesRepository.getAndIncrement(seriesCode, conn);

        // Desgloses de IVA
        double totalAmount = sale.getTotal();
        double vatAmount = sale.getIva();
        double baseAmount = totalAmount - vatAmount;

        return FiscalDocument.builder()
                .saleId(saleId)
                .type(type)
                .series(seriesCode)
                .number(docNumber)
                .status(Status.EMITIDO)
                .issuedAt(LocalDateTime.now())
                .issuer(
                        orEmpty(configRepository.getValue("companyName")),
                        orEmpty(configRepository.getValue("cif")),
                        orEmpty(configRepository.getValue("address")),
                        orEmpty(configRepository.getValue("phone")))
                .receiver(receiverName, receiverTaxId, receiverAddress)
                .amounts(baseAmount, vatAmount, totalAmount)
                .build();
    }

    private String orEmpty(String val) {
        return val != null ? val : "";
    }
}
