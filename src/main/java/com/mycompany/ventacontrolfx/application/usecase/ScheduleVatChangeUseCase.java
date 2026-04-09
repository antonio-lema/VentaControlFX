package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.exception.BusinessException;
import com.mycompany.ventacontrolfx.domain.repository.ITaxRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Caso de uso: Gesti\u00f3n de cambios de IVA.
 * OBSOLETO: Reemplazado por el Motor Fiscal V2. Se mantiene por
 * compatibilidad temporal hasta la fase de refactorizaci\u00f3n de UI.
 */
public class ScheduleVatChangeUseCase {

    private final ITaxRepository taxRepository;

    public ScheduleVatChangeUseCase(ITaxRepository taxRepository) {
        this.taxRepository = taxRepository;
    }

    public void applyGlobalRateChange(double newRate, LocalDateTime effectiveFrom, String reason) throws SQLException {
        throw new UnsupportedOperationException("Operaci\u00f3n obsoleta en V2. Utilizar TaxGroups y TaxRates.");
    }

    public int applyCategoryRateChange(int categoryId, double newRate, LocalDateTime effectiveFrom, String reason)
            throws SQLException {
        throw new UnsupportedOperationException("Operaci\u00f3n obsoleta en V2.");
    }

    public void applyProductRateChange(int productId, int categoryId, double newRate, LocalDateTime effectiveFrom,
            String reason) throws SQLException {
        throw new UnsupportedOperationException("Operaci\u00f3n obsoleta en V2.");
    }

    public double getEffectiveRate(int productId, int categoryId, LocalDateTime at) throws SQLException {
        return 21.0; // Fallback
    }

    // Retorna listas vac\u00edas temporalmente
    public List<Object> getGlobalHistory() throws SQLException {
        return new ArrayList<>();
    }

    public List<Object> getCategoryHistory(int categoryId) throws SQLException {
        return new ArrayList<>();
    }

    public List<Object> getProductHistory(int productId) throws SQLException {
        return new ArrayList<>();
    }

    public Optional<Object> getCurrentGlobalRate() throws SQLException {
        return Optional.empty();
    }
}
