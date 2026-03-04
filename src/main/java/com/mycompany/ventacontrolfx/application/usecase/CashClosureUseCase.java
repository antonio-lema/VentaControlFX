package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.CashClosure;
import com.mycompany.ventacontrolfx.domain.model.ProductSummary;
import com.mycompany.ventacontrolfx.domain.repository.ICashClosureRepository;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class CashClosureUseCase {
    private final ICashClosureRepository repository;

    public CashClosureUseCase(ICashClosureRepository repository) {
        this.repository = repository;
    }

    public void performClosure(CashClosure closure) throws SQLException {
        // Permitir múltiples cierres si hay transacciones pendientes.
        // El repositorio ya se encarga de cerrar solo lo que tiene closure_id IS NULL.
        if (repository.getPendingTransactionCount() == 0) {
            throw new SQLException("No hay transacciones pendientes para realizar un cierre.");
        }
        repository.save(closure);
    }

    public List<CashClosure> getHistory(LocalDate start, LocalDate end) throws SQLException {
        return repository.getByRange(start, end);
    }

    public List<ProductSummary> getClosureDetails(int closureId) throws SQLException {
        return repository.getProductSummary(closureId);
    }

    public java.util.Map<String, Double> getTodayTotals() throws SQLException {
        return repository.getPendingTotals();
    }

    public int getTodayTransactionCount() throws SQLException {
        return repository.getPendingTransactionCount();
    }

    public boolean isClosureDoneToday() throws SQLException {
        return repository.isClosureDone(LocalDate.now());
    }

    public List<ProductSummary> getPendingSummary() throws SQLException {
        return repository.getPendingProductSummary();
    }

    // ── Gestión de fondo de caja ────────────────────────────────────────────

    /**
     * Abre la caja con el fondo inicial indicado.
     * Lanza excepción si ya hay una sesión activa hoy.
     */
    public void openCashFund(double initialAmount, int userId) throws SQLException {
        if (repository.hasActiveFund()) {
            throw new SQLException("Ya hay un fondo de caja abierto para hoy.");
        }
        if (initialAmount < 0) {
            throw new SQLException("El fondo inicial no puede ser negativo.");
        }
        repository.openCashFund(initialAmount, userId);
    }

    /**
     * Retira efectivo de la caja.
     * Valida que haya suficiente efectivo disponible.
     */
    public void withdrawCash(double amount, String reason, int userId) throws SQLException {
        if (amount <= 0) {
            throw new SQLException("El importe de retirada debe ser mayor que cero.");
        }
        double available = repository.getCurrentCashInDrawer();
        if (amount > available) {
            throw new SQLException(String.format(
                    "Efectivo insuficiente en caja. Disponible: %.2f €. Solicitado: %.2f €",
                    available, amount));
        }
        repository.withdrawCash(amount, reason, userId);
    }

    /**
     * Devuelve el efectivo actual en caja (fondo + ventas - devoluciones -
     * retiradas).
     */
    public double getCurrentCashInDrawer() throws SQLException {
        return repository.getCurrentCashInDrawer();
    }

    /** Indica si hay una sesión de caja abierta hoy. */
    public boolean hasActiveFund() throws SQLException {
        return repository.hasActiveFund();
    }

    /** Fondo inicial de la sesión activa. */
    public double getActiveFundAmount() throws SQLException {
        return repository.getActiveFundAmount();
    }

    /** Obtiene el saldo real del último cierre realizado. */
    public double getLastClosureAmount() throws SQLException {
        return repository.getLastClosureAmount();
    }

    /**
     * Valida que haya suficiente efectivo en caja para procesar una devolución en
     * efectivo.
     * 
     * @param returnAmount cantidad a devolver
     * @throws SQLException si el efectivo es insuficiente
     */
    public void registerCashEntry(double amount, String reason, int userId) throws SQLException {
        if (amount <= 0) {
            throw new SQLException("El importe de ingreso debe ser mayor que cero.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new SQLException("Debe indicar el motivo del ingreso manual.");
        }
        repository.registerCashEntry(amount, reason, userId);
    }

    /**
     * Valida que haya suficiente efectivo en caja para procesar una devolución en
     * efectivo.
     * 
     * @param returnAmount cantidad a devolver
     * @throws SQLException si el efectivo es insuficiente
     */
    public void validateCashAvailableForReturn(double returnAmount) throws SQLException {
        double available = repository.getCurrentCashInDrawer();
        if (returnAmount > available) {
            throw new SQLException(String.format(
                    "No hay suficiente efectivo en caja para esta devolución.\n" +
                            "Efectivo disponible: %.2f €\nImporte a devolver: %.2f €\n\n" +
                            "Debe retirar menos dinero o añadir más fondo de caja.",
                    available, returnAmount));
        }
    }

    /**
     * Registra una devolución en efectivo en el libro mayor de caja.
     * Valida el saldo disponible antes de permitir la operación.
     *
     * @param amount   importe a devolver
     * @param reason   motivo de la devolución
     * @param userId   usuario que realiza la devolución
     * @throws SQLException si el efectivo es insuficiente o falla la BD
     */
    public void registerCashReturn(double amount, String reason, int userId) throws SQLException {
        if (amount <= 0) {
            throw new SQLException("El importe de la devolución debe ser mayor que cero.");
        }
        double available = repository.getCurrentCashInDrawer();
        if (amount > available) {
            throw new SQLException(String.format(
                    "❌ No hay suficiente efectivo en caja para esta devolución.\n" +
                            "💵 Disponible en caja: %.2f €\n" +
                            "💸 Importe a devolver: %.2f €\n\n" +
                            "Añade más fondo de caja antes de procesar esta devolución.",
                    available, amount));
        }
        repository.registerCashReturn(amount, reason, userId);
    }

    public void registerCashReturn(double amount, String reason, int userId, Connection conn) throws SQLException {
        // En transacciones, asumimos que la validación de saldo ya se hizo o se hará
        repository.registerCashReturn(amount, reason, userId, conn);
    }
}
