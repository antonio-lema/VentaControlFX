package com.mycompany.ventacontrolfx.domain.model;

/**
 * Gestión de series de numeración correlativas de documentos fiscales.
 * Clean Architecture — Capa de Dominio.
 */
public class DocumentSeries {

    private int seriesId;
    private String seriesCode; // "T", "F", "R"
    private String prefix; // "2026-T-"
    private int lastNumber; // último número asignado
    private int year;
    private String description;

    public DocumentSeries() {
    }

    // ── Getters & Setters ──────────────────────────────────────────────

    public int getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(int seriesId) {
        this.seriesId = seriesId;
    }

    public String getSeriesCode() {
        return seriesCode;
    }

    public void setSeriesCode(String seriesCode) {
        this.seriesCode = seriesCode;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public int getLastNumber() {
        return lastNumber;
    }

    public void setLastNumber(int lastNumber) {
        this.lastNumber = lastNumber;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** Retorna el siguiente número SIN incrementar (solo lectura). */
    public int peekNextNumber() {
        return lastNumber + 1;
    }

    /** Genera la referencia formateada para el número dado. */
    public String format(int number) {
        return String.format("%s%05d", prefix, number);
    }
}
