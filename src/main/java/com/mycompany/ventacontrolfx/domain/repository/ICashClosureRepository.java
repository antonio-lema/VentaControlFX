package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.CashClosure;
import com.mycompany.ventacontrolfx.domain.model.ProductSummary;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    void openCashFund(double initialAmount, String notes, int userId) throws SQLException;

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

    /**
     * Obtiene los movimientos (ingresos/retiradas) asociados a un cierre concreto.
     */
    List<CashMovement> getMovementsByClosure(int closureId) throws SQLException;

    /** Marca un cierre como revisado por un administrador. */
    void markAsReviewed(int closureId, int reviewerId) throws SQLException;

    /** Representa un movimiento de efectivo con detalles para auditoría. */
    class CashMovement {
        public int movementId;
        public String type;
        public double amount;
        public String reason;
        public LocalDateTime createdAt;
        public String username;

        public CashMovement(int id, String type, double amount, String reason, LocalDateTime createdAt,
                String username) {
            this.movementId = id;
            this.type = type;
            this.amount = amount;
            this.reason = reason;
            this.createdAt = createdAt;
            this.username = username;
        }

        public int getMovementId() {
            return movementId;
        }

        public String getType() {
            return type;
        }

        public double getAmount() {
            return amount;
        }

        public String getReason() {
            return reason;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public String getUsername() {
            return username;
        }
    }
}
