package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.exception.BusinessException;
import com.mycompany.ventacontrolfx.domain.repository.IMassivePriceUpdateRepository;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import com.mycompany.ventacontrolfx.domain.repository.IPriceUpdateLogRepository;
import com.mycompany.ventacontrolfx.domain.model.PriceUpdateLog;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Caso de uso: Actualizaci\u00f3n masiva de precios.
 *
 * Responsabilidad: Orquestar la l\u00f3gica de negocio y delegar la persistencia.
 * Implementa un flujo at\u00f3mico de logId para asegurar la trazabilidad.
 */
public class MassivePriceUpdateUseCase {

    private final IMassivePriceUpdateRepository priceRepository;
    private final IProductRepository productRepository;
    private final IPriceUpdateLogRepository priceLogRepository;

    public MassivePriceUpdateUseCase(IMassivePriceUpdateRepository priceRepository,
            IProductRepository productRepository,
            IPriceUpdateLogRepository priceLogRepository) {
        this.priceRepository = priceRepository;
        this.productRepository = productRepository;
        this.priceLogRepository = priceLogRepository;
    }

    private int initLog(String type, String scope, double value, Integer categoryId, Integer priceListId, String reason, LocalDateTime appliedAt) throws SQLException {
        PriceUpdateLog log = new PriceUpdateLog();
        log.setUpdateType(type);
        log.setScope(scope);
        log.setValue(value);
        log.setProductsUpdated(0);
        log.setCategoryId(categoryId);
        log.setReason(reason);
        log.setPriceListId(priceListId);
        log.setAppliedAt(appliedAt.withNano(0));
        priceLogRepository.save(log);
        return log.getLogId();
    }

    private void finalizeLog(int logId, int count) throws SQLException {
        if (logId > 0) {
            priceLogRepository.updateProductsUpdatedCount(logId, count);
        }
    }

    public int applyPercentageIncreaseToAll(int priceListId, double percentage, String reason, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        validatePercentage(percentage);
        double multiplier = 1.0 + (percentage / 100.0);
        int logId = initLog("percentage", "Todos los Art\u00edculos", percentage, null, priceListId, reason, startDate);
        int updated = priceRepository.applyBulkMultiplier(priceListId, null, multiplier, reason, startDate, logId);
        finalizeLog(logId, updated);
        return updated;
    }

    public int applyPercentageIncreaseToCategory(int priceListId, int categoryId, double percentage, String reason, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        validatePercentage(percentage);
        double multiplier = 1.0 + (percentage / 100.0);
        int logId = initLog("percentage", "Por Categor\u00eda", percentage, categoryId, priceListId, reason, startDate);
        int updated = priceRepository.applyBulkMultiplier(priceListId, categoryId, multiplier, reason, startDate, logId);
        finalizeLog(logId, updated);
        return updated;
    }

    public int applyFixedAmountIncreaseToAll(int priceListId, double amount, String reason, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        validateAmount(amount);
        int logId = initLog("fixed_amount", "Todos los Art\u00edculos", amount, null, priceListId, reason, startDate);
        int updated = priceRepository.applyBulkFixedAmount(priceListId, null, amount, reason, startDate, logId);
        finalizeLog(logId, updated);
        return updated;
    }

    public int applyFixedAmountIncreaseToCategory(int priceListId, int categoryId, double amount, String reason, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        validateAmount(amount);
        int logId = initLog("fixed_amount", "Por Categor\u00eda", amount, categoryId, priceListId, reason, startDate);
        int updated = priceRepository.applyBulkFixedAmount(priceListId, categoryId, amount, reason, startDate, logId);
        finalizeLog(logId, updated);
        return updated;
    }

    public int applyRoundingToAll(int priceListId, double targetDecimal, String reason, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        int logId = initLog("rounding", "Todos los Art\u00edculos", targetDecimal, null, priceListId, reason, startDate);
        int updated = priceRepository.applyBulkRounding(priceListId, null, targetDecimal, reason, startDate, logId);
        finalizeLog(logId, updated);
        return updated;
    }

    public int applyRoundingToCategory(int priceListId, int categoryId, double targetDecimal, String reason, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        int logId = initLog("rounding", "Por Categor\u00eda", targetDecimal, categoryId, priceListId, reason, startDate);
        int updated = priceRepository.applyBulkRounding(priceListId, categoryId, targetDecimal, reason, startDate, logId);
        finalizeLog(logId, updated);
        return updated;
    }

    public int applyToTopSellers(int priceListId, int topN, int daysBack, double value, String reason, boolean isPercentage, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        if (isPercentage) validatePercentage(value); else validateAmount(value);
        double repoValue = isPercentage ? (1.0 + (value / 100.0)) : value;
        int logId = initLog(isPercentage ? "percentage" : "fixed_amount", "Top " + topN + " Vendidos", value, null, priceListId, reason, startDate);
        int updated = priceRepository.applyBulkMultiplierToTopSellers(priceListId, topN, daysBack, repoValue, reason, isPercentage, startDate, logId);
        finalizeLog(logId, updated);
        return updated;
    }

    public int applyToSlowMovers(int priceListId, int daysWithoutSale, double value, String reason, boolean isPercentage, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        if (isPercentage) validatePercentage(value); else validateAmount(value);
        double repoValue = isPercentage ? (1.0 + (value / 100.0)) : value;
        int logId = initLog(isPercentage ? "percentage" : "fixed_amount", "Sin Ventas (" + daysWithoutSale + "d)", value, null, priceListId, reason, startDate);
        int updated = priceRepository.applyBulkMultiplierToSlowMovers(priceListId, daysWithoutSale, repoValue, reason, isPercentage, startDate, logId);
        finalizeLog(logId, updated);
        return updated;
    }

    public int applyToPriceRange(int priceListId, double minPrice, double maxPrice, double value, String reason, boolean isPercentage, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        if (isPercentage) validatePercentage(value); else validateAmount(value);
        double repoValue = isPercentage ? (1.0 + (value / 100.0)) : value;
        int logId = initLog(isPercentage ? "percentage" : "fixed_amount", "Rango [" + minPrice + "-" + maxPrice + "]", value, null, priceListId, reason, startDate);
        int updated = priceRepository.applyBulkMultiplierToPriceRange(priceListId, minPrice, maxPrice, repoValue, reason, isPercentage, startDate, logId);
        finalizeLog(logId, updated);
        return updated;
    }

    public int applyToFavorites(int priceListId, double value, String reason, boolean isPercentage, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        if (isPercentage) validatePercentage(value); else validateAmount(value);
        double repoValue = isPercentage ? (1.0 + (value / 100.0)) : value;
        int logId = initLog(isPercentage ? "percentage" : "fixed_amount", "Favoritos \u2605", value, null, priceListId, reason, startDate);
        int updated = priceRepository.applyBulkMultiplierToFavorites(priceListId, repoValue, reason, isPercentage, startDate, logId);
        finalizeLog(logId, updated);
        return updated;
    }

    public int applyToBottomSellers(int priceListId, int bottomN, int daysBack, double value, String reason, boolean isPercentage, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        if (isPercentage) validatePercentage(value); else validateAmount(value);
        double repoValue = isPercentage ? (1.0 + (value / 100.0)) : value;
        int logId = initLog(isPercentage ? "percentage" : "fixed_amount", "Bottom " + bottomN + " Vendidos", value, null, priceListId, reason, startDate);
        int updated = priceRepository.applyBulkMultiplierToBottomSellers(priceListId, bottomN, daysBack, repoValue, reason, isPercentage, startDate, logId);
        finalizeLog(logId, updated);
        return updated;
    }

    public void cloneAndAdjustPrices(int sourceListId, int targetListId, double percentage, String reason, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        double multiplier = 1.0 + (percentage / 100.0);
        int logId = initLog("clonacion", "Desde Tarifa " + sourceListId, percentage, null, targetListId, reason, startDate);
        priceRepository.cloneAndAdjustPriceList(sourceListId, targetListId, multiplier, reason, startDate, logId);
        // updated count isn't easily returned for clone, setting as -1 or estimate
        finalizeLog(logId, -1);
    }

    public int applyRoundingToTopSellers(int priceListId, int topN, int daysBack, double targetDecimal, String reason, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        int logId = initLog("rounding", "Top " + topN + " Vendidos", targetDecimal, null, priceListId, reason, startDate);
        int updated = priceRepository.applyBulkRoundingToTopSellers(priceListId, topN, daysBack, targetDecimal, reason, startDate, logId);
        finalizeLog(logId, updated);
        return updated;
    }

    public int applyRoundingToBottomSellers(int priceListId, int bottomN, int daysBack, double targetDecimal, String reason, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        int logId = initLog("rounding", "Bottom " + bottomN + " Vendidos", targetDecimal, null, priceListId, reason, startDate);
        int updated = priceRepository.applyBulkRoundingToBottomSellers(priceListId, bottomN, daysBack, targetDecimal, reason, startDate, logId);
        finalizeLog(logId, updated);
        return updated;
    }

    public int applyRoundingToSlowMovers(int priceListId, int daysWithoutSale, double targetDecimal, String reason, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        int logId = initLog("rounding", "Sin Ventas (" + daysWithoutSale + "d)", targetDecimal, null, priceListId, reason, startDate);
        int updated = priceRepository.applyBulkRoundingToSlowMovers(priceListId, daysWithoutSale, targetDecimal, reason, startDate, logId);
        finalizeLog(logId, updated);
        return updated;
    }

    public int applyRoundingToPriceRange(int priceListId, double minPrice, double maxPrice, double targetDecimal, String reason, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        int logId = initLog("rounding", "Rango [" + minPrice + "-" + maxPrice + "]", targetDecimal, null, priceListId, reason, startDate);
        int updated = priceRepository.applyBulkRoundingToPriceRange(priceListId, minPrice, maxPrice, targetDecimal, reason, startDate, logId);
        finalizeLog(logId, updated);
        return updated;
    }

    public int applyRoundingToFavorites(int priceListId, double targetDecimal, String reason, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        int logId = initLog("rounding", "Favoritos \u2605", targetDecimal, null, priceListId, reason, startDate);
        int updated = priceRepository.applyBulkRoundingToFavorites(priceListId, targetDecimal, reason, startDate, logId);
        finalizeLog(logId, updated);
        return updated;
    }

    public int applyPercentageIncreaseToProducts(int priceListId, List<Integer> productIds, double percentage, String reason, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        validatePercentage(percentage);
        double multiplier = 1.0 + (percentage / 100.0);
        int logId = initLog("percentage", "Lista de Productos (" + (productIds != null ? productIds.size() : 0) + ")", percentage, null, priceListId, reason, startDate);
        int updated = priceRepository.applyBulkMultiplierToProducts(priceListId, productIds, multiplier, reason, startDate, logId);
        finalizeLog(logId, updated);
        return updated;
    }

    public int applyFixedAmountIncreaseToProducts(int priceListId, List<Integer> productIds, double amount, String reason, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        validateAmount(amount);
        int logId = initLog("fixed_amount", "Lista de Productos (" + (productIds != null ? productIds.size() : 0) + ")", amount, null, priceListId, reason, startDate);
        int updated = priceRepository.applyBulkFixedAmountToProducts(priceListId, productIds, amount, reason, startDate, logId);
        finalizeLog(logId, updated);
        return updated;
    }

    public int applyBulkRoundingToProducts(int priceListId, List<Integer> productIds, double targetDecimal, String reason, LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : LocalDateTime.now().withNano(0);
        int logId = initLog("rounding", "Lista de Productos (" + (productIds != null ? productIds.size() : 0) + ")", targetDecimal, null, priceListId, reason, startDate);
        int updated = priceRepository.applyBulkRoundingToProducts(priceListId, productIds, targetDecimal, reason, startDate, logId);
        finalizeLog(logId, updated);
        return updated;
    }

    public int applyTaxToAll(double taxRate, String reason) throws SQLException {
        productRepository.updateTaxRateToAll(taxRate);
        int updated = productRepository.count();
        // Tax changes are not currently linked to price logs via update_log_id, keeping legacy log for now
        return updated;
    }

    public int applyTaxToCategory(int categoryId, double taxRate, String reason) throws SQLException {
        productRepository.updateTaxRateByCategory(categoryId, taxRate);
        return 1;
    }

    private void validateAmount(double amount) {
        if (amount == 0) throw new BusinessException("El importe de cambio no puede ser cero.");
    }

    private void validatePercentage(double percentage) {
        if (percentage == 0) throw new BusinessException("El porcentaje de cambio no puede ser cero.");
        if (percentage < -99) throw new BusinessException("No se puede reducir el precio m\u00e1s de un 99%.");
        if (percentage > 500) throw new BusinessException("Una subida del " + percentage + "% parece excesiva.");
    }
}
