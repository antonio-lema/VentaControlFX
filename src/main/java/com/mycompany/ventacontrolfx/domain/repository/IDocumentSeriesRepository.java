package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.DocumentSeries;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Puerto para la gesti\u00c3\u00b3n de series de numeraci\u00c3\u00b3n correlativa.
 * Clean Architecture \u00e2\u20ac\u201d Capa de Dominio (Interfaz / Port).
 *
 * CR\u00c3\u008dTICO: getAndIncrement debe ser at\u00c3\u00b3mico (SELECT FOR UPDATE en SQL)
 * para garantizar unicidad del n\u00c3\u00bamero en entornos multi-caja.
 */
public interface IDocumentSeriesRepository {

    /**
     * Obtiene el siguiente n\u00c3\u00bamero disponible para la serie indicada
     * e incrementa el contador de forma AT\u00c3\u201cMICA dentro de la transacci\u00c3\u00b3n.
     *
     * @param seriesCode "T" (Ticket), "F" (Factura)
     * @param conn       transacci\u00c3\u00b3n activa \u00e2\u20ac\u201d NUNCA commit aqu\u00c3\u00ad
     * @return el n\u00c3\u00bamero asignado (ya incrementado en BD)
     */
    int getAndIncrement(String seriesCode, Connection conn) throws SQLException;

    /**
     * Devuelve los metadatos de la serie (prefix, lastNumber, etc.)
     * para construir la referencia humana legible.
     */
    DocumentSeries findByCode(String seriesCode) throws SQLException;
}
