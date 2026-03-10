package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.DocumentSeries;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Puerto para la gestión de series de numeración correlativa.
 * Clean Architecture — Capa de Dominio (Interfaz / Port).
 *
 * CRÍTICO: getAndIncrement debe ser atómico (SELECT FOR UPDATE en SQL)
 * para garantizar unicidad del número en entornos multi-caja.
 */
public interface IDocumentSeriesRepository {

    /**
     * Obtiene el siguiente número disponible para la serie indicada
     * e incrementa el contador de forma ATÓMICA dentro de la transacción.
     *
     * @param seriesCode "T" (Ticket), "F" (Factura)
     * @param conn       transacción activa — NUNCA commit aquí
     * @return el número asignado (ya incrementado en BD)
     */
    int getAndIncrement(String seriesCode, Connection conn) throws SQLException;

    /**
     * Devuelve los metadatos de la serie (prefix, lastNumber, etc.)
     * para construir la referencia humana legible.
     */
    DocumentSeries findByCode(String seriesCode) throws SQLException;
}
