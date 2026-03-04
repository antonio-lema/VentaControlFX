package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.CashClosure;
import com.mycompany.ventacontrolfx.domain.model.ProductSummary;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface ICashClosureRepository {
    void save(CashClosure closure) throws SQLException;

    List<CashClosure> getByRange(LocalDate start, LocalDate end) throws SQLException;

    List<ProductSummary> getProductSummary(int closureId) throws SQLException;

    List<ProductSummary> getPendingProductSummary() throws SQLException;

    boolean isClosureDone(LocalDate date) throws SQLException;

    int count() throws SQLException;

    java.util.Map<String, Double> getPendingTotals() throws SQLException;

    int getPendingTransactionCount() throws SQLException;

    // ── Gestión de fondo de caja ────────────────────────────────────────
    /** Abre una sesión de caja con el fondo inicial indicado. */
    void openCashFund(double initialAmount, int userId) throws SQLException;

    enum MovementType {
        APERTURA, VENTA, RETIRADA, INGRESO, CIERRE, DEVOLUCION
    }

    /** Registra un movimiento de efectivo con trazabilidad completa. */
    void registerMovement(MovementType type, double amount, String reason, int userId) throws SQLException;

    /** Registra una retirada de efectivo de la caja. */
    void withdrawCash(double amount, String reason, int userId) throws SQLException;

    /** Registra una entrada manual de efectivo a la caja. */
    void registerCashEntry(double amount, String reason, int userId) throws SQLException;

    /**
     * Registra una devolución de efectivo en el libro mayor de caja.
     * Requiere que haya una sesión de caja activa.
     */
    void registerCashReturn(double amount, String reason, int userId) throws SQLException;

    void registerCashReturn(double amount, String reason, int userId, Connection conn) throws SQLException;

    /**
     * Devuelve el efectivo actual en caja:
     * fondo_inicial + ventas_efectivo – devoluciones_efectivo – retiradas.
     */
    double getCurrentCashInDrawer() throws SQLException;

    /** Indica si hay una sesión de caja activa (abierta hoy sin cerrar). */
    boolean hasActiveFund() throws SQLException;

    /** Devuelve el fondo inicial de la sesión activa (0 si no hay sesión). */
    double getActiveFundAmount() throws SQLException;

    /** Devuelve el importe real con el que se cerró la última vez. */
    double getLastClosureAmount() throws SQLException;

    /** Comprueba si hay sesiones de días anteriores sin cerrar. */
    boolean hasUnclosedPreviousSession() throws SQLException;
}
