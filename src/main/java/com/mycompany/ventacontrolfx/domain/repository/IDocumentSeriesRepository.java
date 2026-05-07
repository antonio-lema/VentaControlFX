package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.DocumentSeries;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Puerto para la gesti\u00f3n de series de numeraci\u00f3n correlativa.
 * Clean Architecture \u2014 Capa de Dominio (Interfaz / Port).
 *
 * CR\u00cdTICO: getAndIncrement debe ser at\u00f3mico (SELECT FOR UPDATE en SQL)
 * para garantizar unicidad del n\u00famero en entornos multi-caja.
 */
public interface IDocumentSeriesRepository {

    /**
     * Obtiene el siguiente n\u00famero disponible para la serie indicada
     * e incrementa el contador de forma AT\u00d3MICA dentro de la transacci\u00f3n.
     *
     * @param seriesCode "T" (Ticket), "F" (Factura)
     * @param conn       transacci\u00f3n activa \u2014 NUNCA commit aqu\u00ed
     * @return el n\u00famero asignado (ya incrementado en BD)
     */
    int getAndIncrement(String seriesCode, Connection conn) throws SQLException;

    /**
     * Devuelve los metadatos de la serie (prefix, lastNumber, etc.)
     * para construir la referencia humana legible.
     */
    DocumentSeries findByCode(String seriesCode) throws SQLException;
}

