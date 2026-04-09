package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.DocumentSeries;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Puerto para la gestiÃ³n de series de numeraciÃ³n correlativa.
 * Clean Architecture â€” Capa de Dominio (Interfaz / Port).
 *
 * CRÃTICO: getAndIncrement debe ser atÃ³mico (SELECT FOR UPDATE en SQL)
 * para garantizar unicidad del nÃºmero en entornos multi-caja.
 */
public interface IDocumentSeriesRepository {

    /**
     * Obtiene el siguiente nÃºmero disponible para la serie indicada
     * e incrementa el contador de forma ATÃ“MICA dentro de la transacciÃ³n.
     *
     * @param seriesCode "T" (Ticket), "F" (Factura)
     * @param conn       transacciÃ³n activa â€” NUNCA commit aquÃ­
     * @return el nÃºmero asignado (ya incrementado en BD)
     */
    int getAndIncrement(String seriesCode, Connection conn) throws SQLException;

    /**
     * Devuelve los metadatos de la serie (prefix, lastNumber, etc.)
     * para construir la referencia humana legible.
     */
    DocumentSeries findByCode(String seriesCode) throws SQLException;
}
