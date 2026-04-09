package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.FiscalDocument;
import com.mycompany.ventacontrolfx.domain.model.FiscalDocument.Status;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de acceso a la persistencia de documentos fiscales.
 * Clean Architecture \u00e2\u20ac\u201d Capa de Dominio (Interfaz / Port).
 * La implementaci\u00f3n concreta vive en la capa de Infraestructura.
 */
public interface IFiscalDocumentRepository {

    /**
     * Persiste el documento fiscal y actualiza la tabla sales con el n\u00famero
     * correlativo. Debe ejecutarse dentro de una transacci\u00f3n.
     */
    void save(FiscalDocument document, Connection conn) throws SQLException;

    /**
     * Busca el documento fiscal asociado a una venta concreta.
     */
    Optional<FiscalDocument> findBySaleId(int saleId) throws SQLException;

    /**
     * Busca por referencia completa: ej. "2026-T-00042"
     */
    Optional<FiscalDocument> findByReference(String reference) throws SQLException;

    /**
     * Listado de documentos fiscales filtrado por rango de fechas y estado.
     * Permite pasar null en cualquier par\u00e1metro para ignorar ese filtro.
     */
    List<FiscalDocument> findByFilters(LocalDate from, LocalDate to, Status status, String docType)
            throws SQLException;

    /**
     * Cambia el estado de un documento (ANULADO).
     * Prohibido cambiar importes. Solo estado.
     */
    void updateStatus(int saleId, Status newStatus, Connection conn) throws SQLException;

}
