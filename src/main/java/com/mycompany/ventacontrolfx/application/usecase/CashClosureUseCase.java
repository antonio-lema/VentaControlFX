package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.CashClosure;
import com.mycompany.ventacontrolfx.domain.model.ProductSummary;
import com.mycompany.ventacontrolfx.domain.repository.ICashClosureRepository;
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
}
