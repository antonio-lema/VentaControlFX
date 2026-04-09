package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.CashClosure;
import com.mycompany.ventacontrolfx.domain.model.ProductSummary;
import com.mycompany.ventacontrolfx.domain.repository.ICashClosureRepository;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class CashClosureUseCase {
    private final ICashClosureRepository repository;
    private final com.mycompany.ventacontrolfx.util.AuthorizationService authService;

    public CashClosureUseCase(ICashClosureRepository repository,
            com.mycompany.ventacontrolfx.util.AuthorizationService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    public void performClosure(CashClosure closure) throws SQLException {
        authService.checkPermission("CIERRES");
        // Enriquecer el cierre con datos de auditorÃ­a antes de guardar
        Map<String, Double> totals = repository.getPendingTotals();

        double cashSales = totals.getOrDefault("cash", 0.0);
        double initialFund = repository.getActiveFundAmount();
        double currentCash = repository.getCurrentCashInDrawer();

        // Populate manual movement totals
        closure.setCashIn(totals.getOrDefault("manual_in", 0.0));
        closure.setCashOut(totals.getOrDefault("manual_out", 0.0));

        closure.setInitialFund(initialFund);
        closure.setExpectedCash(currentCash);

        // Enrich totals for history
        closure.setTotalCash(totals.getOrDefault("cash", 0.0));
        closure.setTotalCard(totals.getOrDefault("card", 0.0));
        closure.setTotalAll(totals.getOrDefault("total", 0.0));

        // Determinamos el estado segÃºn la diferencia
        double diff = closure.getActualCash() - currentCash;
        closure.setDifference(diff);

        if (Math.abs(diff) < 0.01) {
            closure.setStatus("CUADRADO");
        } else {
            closure.setStatus("DESCUADRE");
        }

        // El repositorio.save usarÃ¡ estos campos para la auditorÃ­a
        repository.save(closure);
    }

    /**
     * Realiza un cierre parcial (Informe X).
     * Muestra/Imprime los totales actuales sin cerrar la sesiÃ³n de caja.
     */
    public void performPartialClosure(int userId) throws java.sql.SQLException {
        authService.checkPermission("CIERRES");
        java.util.Map<String, Double> totals = repository.getPendingTotals();
        double currentCash = repository.getCurrentCashInDrawer();

        System.out.println("--- CIERRE PARCIAL (INFORME X) ---");
        System.out.println("Usuario: " + userId);
        System.out.println("Efectivo en CajÃ³n: " + currentCash);
        System.out.println("Ventas Totales: " + totals.getOrDefault("sales_total", 0.0));
        // AquÃ­ se llamarÃ­a al servicio de impresiÃ³n de tickets
    }

    public List<CashClosure> getHistory(LocalDate start, LocalDate end) throws SQLException {
        return repository.getByRange(start, end);
    }

    public List<ProductSummary> getClosureDetails(int closureId) throws SQLException {
        return repository.getProductSummary(closureId);
    }

    public Map<String, Double> getTodayTotals() throws SQLException {
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

    // â”€â”€ GestiÃ³n de fondo de caja â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Abre la caja con el fondo inicial indicado.
     * Lanza excepciÃ³n si ya hay una sesiÃ³n activa hoy.
     */
    public void openCashFund(double initialAmount, String notes, int userId) throws SQLException {
        authService.checkPermission("CIERRES");
        if (repository.hasActiveFund()) {
            throw new SQLException("Ya hay un fondo de caja abierto para hoy.");
        }
        if (initialAmount < 0) {
            throw new SQLException("El fondo inicial no puede ser negativo.");
        }
        repository.openCashFund(initialAmount, notes, userId);
    }

    /**
     * Retira efectivo de la caja.
     * Valida que haya suficiente efectivo disponible.
     */
    public void withdrawCash(double amount, String reason, int userId) throws SQLException {
        authService.checkPermission("CIERRES");
        if (amount <= 0) {
            throw new SQLException("El importe de retirada debe ser mayor que cero.");
        }
        double available = repository.getCurrentCashInDrawer();
        if (amount > available) {
            throw new SQLException(String.format(
                    "Efectivo insuficiente en caja. Disponible: %.2f â‚¬. Solicitado: %.2f â‚¬",
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

    /** Indica si hay una sesiÃ³n de caja abierta hoy. */
    public boolean hasActiveFund() throws SQLException {
        return repository.hasActiveFund();
    }

    /** Fondo inicial de la sesiÃ³n activa. */
    public double getActiveFundAmount() throws SQLException {
        return repository.getActiveFundAmount();
    }

    /** Obtiene el saldo real del Ãºltimo cierre realizado. */
    public double getLastClosureAmount() throws SQLException {
        return repository.getLastClosureAmount();
    }

    /**
     * Valida que haya suficiente efectivo en caja para procesar una devoluciÃ³n en
     * efectivo.
     * 
     * @param returnAmount cantidad a devolver
     * @throws SQLException si el efectivo es insuficiente
     */
    public void registerCashEntry(double amount, String reason, int userId) throws SQLException {
        authService.checkPermission("CIERRES");
        if (amount <= 0) {
            throw new SQLException("El importe de ingreso debe ser mayor que cero.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new SQLException("Debe indicar el motivo del ingreso manual.");
        }
        repository.registerCashEntry(amount, reason, userId);
    }

    /**
     * Valida que haya suficiente efectivo en caja para procesar una devoluciÃ³n en
     * efectivo.
     * 
     * @param returnAmount cantidad a devolver
     * @throws SQLException si el efectivo es insuficiente
     */
    public void validateCashAvailableForReturn(double returnAmount) throws SQLException {
        double available = repository.getCurrentCashInDrawer();
        if (returnAmount > available) {
            throw new SQLException(String.format(
                    "âŒ No hay suficiente efectivo en caja para esta devoluciÃ³n.\n\n" +
                            "ðŸ¦ Efectivo actual: %.2f â‚¬\n" +
                            "ðŸ’¸ Importe solicitado: %.2f â‚¬\n\n" +
                            "âš ï¸ Se requiere un fondo de caja mayor para cubrir este reembolso.",
                    available, returnAmount));
        }
    }

    /**
     * Registra una devoluciÃ³n en efectivo en el libro mayor de caja.
     * Valida el saldo disponible antes de permitir la operaciÃ³n.
     *
     * @param amount importe a devolver
     * @param reason motivo de la devoluciÃ³n
     * @param userId usuario que realiza la devoluciÃ³n
     * @throws SQLException si el efectivo es insuficiente o falla la BD
     */
    public void registerCashReturn(double amount, String reason, int userId) throws SQLException {
        authService.checkPermission("CIERRES");
        if (amount <= 0) {
            throw new SQLException("El importe de la devoluciÃ³n debe ser mayor que cero.");
        }
        validateCashAvailableForReturn(amount);
        repository.registerCashReturn(amount, reason, userId);
    }

    public void registerCashReturn(double amount, String reason, int userId, Connection conn) throws SQLException {
        // ValidaciÃ³n obligatoria de saldo incluso en transacciones
        validateCashAvailableForReturn(amount);
        repository.registerCashReturn(amount, reason, userId, conn);
    }

    public List<ICashClosureRepository.CashMovement> getMovementsByClosure(int closureId) throws SQLException {
        return repository.getMovementsByClosure(closureId);
    }

    public void markAsReviewed(int closureId, int reviewerId) throws SQLException {
        authService.checkPermission("CIERRES");
        repository.markAsReviewed(closureId, reviewerId);
    }

    public void markAsExcluded(int closureId, int reviewerId) throws SQLException {
        authService.checkPermission("CIERRES");
        repository.markAsExcluded(closureId, reviewerId);
    }

    public void updateClosure(int closureId, double actualCash, String reason, int reviewerId, double previousCash)
            throws SQLException {
        authService.checkPermission("CIERRES");
        repository.updateClosure(closureId, actualCash, reason, reviewerId, previousCash);
    }
}
