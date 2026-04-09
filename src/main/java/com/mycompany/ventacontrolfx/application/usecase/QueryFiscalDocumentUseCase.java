package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.FiscalDocument;
import com.mycompany.ventacontrolfx.domain.model.FiscalDocument.Status;
import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.model.SaleDetail;
import com.mycompany.ventacontrolfx.domain.repository.IFiscalDocumentRepository;
import com.mycompany.ventacontrolfx.domain.repository.ISaleRepository;
import com.mycompany.ventacontrolfx.domain.service.FiscalIntegrityService;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Caso de uso: Consulta y verificaci\u00f3n de documentos fiscales emitidos.
 * Clean Architecture \u00e2\u20ac\u201d Capa de Aplicaci\u00f3n.
 *
 * Responsabilidades:
 * - Buscar documentos por referencia, venta, cliente o rango de fechas.
 * - Verificar integridad de los documentos (hash control).
 * - Proporcionar los datos para reimpresi\u00f3n/regeneraci\u00f3n de PDF.
 */
public class QueryFiscalDocumentUseCase {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final IFiscalDocumentRepository fiscalRepository;
    private final ISaleRepository saleRepository;
    private final FiscalIntegrityService integrityService;

    public QueryFiscalDocumentUseCase(
            IFiscalDocumentRepository fiscalRepository,
            ISaleRepository saleRepository) {
        this.fiscalRepository = fiscalRepository;
        this.saleRepository = saleRepository;
        this.integrityService = new FiscalIntegrityService();
    }

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac B\u00c3\u0161SQUEDAS \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac

    /** Busca el documento de una venta concreta. */
    public Optional<FiscalDocument> findBySaleId(int saleId) throws SQLException {
        return fiscalRepository.findBySaleId(saleId);
    }

    /** Busca por referencia legible: ej. "2026-T-00042" */
    public Optional<FiscalDocument> findByReference(String reference) throws SQLException {
        return fiscalRepository.findByReference(reference);
    }

    /**
     * Listado filtrado para la pantalla de consulta.
     * Cualquier par\u00e1metro puede ser null para ignorar ese filtro.
     */
    public List<FiscalDocument> search(LocalDate from, LocalDate to,
            Status status, String docType) throws SQLException {
        return fiscalRepository.findByFilters(from, to, status, docType);
    }

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac INTEGRIDAD \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac

    /**
     * Verifica la integridad de un documento.
     * Retorna false si el hash no coincide \u00e2\u2020\u2019 posible manipulaci\u00f3n en BD.
     */
    public boolean verifyIntegrity(FiscalDocument doc) {
        return integrityService.verify(doc);
    }

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac DATOS PARA REIMPRESI\u00c3\u201cN \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac

    /**
     * Re\u00fane todos los datos necesarios para regenerar el PDF/ticket
     * desde los snapshots almacenados. El resultado es reproducible
     * en cualquier momento futuro con exactitud.
     *
     * @return PrintData con toda la informaci\u00f3n necesaria para imprimir.
     */
    public PrintData getDataForReprint(int saleId) throws SQLException {
        Optional<FiscalDocument> docOpt = fiscalRepository.findBySaleId(saleId);
        if (docOpt.isEmpty()) {
            throw new IllegalStateException("No hay documento fiscal para la venta #" + saleId);
        }
        FiscalDocument doc = docOpt.get();
        Sale sale = saleRepository.getById(saleId);
        List<SaleDetail> lines = saleRepository.getDetailsBySaleId(saleId);

        return new PrintData(doc, sale, lines);
    }

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac DTO interno para reimpresi\u00f3n \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac

    public static class PrintData {
        public final FiscalDocument document;
        public final Sale sale;
        public final List<SaleDetail> lines;
        public final String formattedDate;
        public String logoPath; // Ruta al logotipo de la empresa

        public PrintData(FiscalDocument document, Sale sale, List<SaleDetail> lines) {
            this.document = document;
            this.sale = sale;
            this.lines = lines;
            this.formattedDate = document.getIssuedAt() != null
                    ? document.getIssuedAt().format(DISPLAY_FMT)
                    : "-";
        }

        public PrintData(FiscalDocument document, Sale sale, List<SaleDetail> lines, String logoPath) {
            this(document, sale, lines);
            this.logoPath = logoPath;
        }

        /** Referencia corta para la cabecera del ticket: "2026-T-00042" */
        public String getReference() {
            return document.getFullReference();
        }
    }
}
