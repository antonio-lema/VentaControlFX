package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.exception.BusinessException;
import com.mycompany.ventacontrolfx.domain.repository.IPriceRepository;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import com.mycompany.ventacontrolfx.domain.repository.IPriceUpdateLogRepository;
import com.mycompany.ventacontrolfx.domain.model.PriceUpdateLog;

import java.sql.SQLException;

/**
 * Caso de uso: Actualización masiva de precios.
 *
 * Responsabilidad: Orquestar la lógica de negocio (validación, cálculo del
 * multiplicador) y delegar la persistencia al repositorio via interfaz.
 *
 * IMPORTANTE (Clean Architecture): Esta clase NO debe contener SQL, Connection,
 * PreparedStatement ni ninguna referencia a infraestructura. Solo habla con
 * interfaces de dominio.
 */
public class MassivePriceUpdateUseCase {

    private final IPriceRepository priceRepository;
    private final IProductRepository productRepository;
    private final IPriceUpdateLogRepository priceLogRepository;

    public MassivePriceUpdateUseCase(IPriceRepository priceRepository,
            IProductRepository productRepository,
            IPriceUpdateLogRepository priceLogRepository) {
        this.priceRepository = priceRepository;
        this.productRepository = productRepository;
        this.priceLogRepository = priceLogRepository;
    }

    /**
     * Aplica una subida (o bajada) porcentual a TODOS los productos activos
     * de una lista de precios.
     *
     * @param priceListId ID de la lista de precios a actualizar.
     * @param percentage  Porcentaje a aplicar (ej: 5.0 = +5%, -3.0 = -3%).
     * @param reason      Motivo del cambio (para trazabilidad en base de datos).
     * @return Número de productos actualizados.
     */
    public int applyPercentageIncreaseToAll(int priceListId, double percentage, String reason)
            throws SQLException {
        validatePercentage(percentage);
        double multiplier = 1.0 + (percentage / 100.0);
        int updated = priceRepository.applyBulkMultiplier(priceListId, null, multiplier, reason);
        saveLog("percentage", "Todos los Artículos", percentage, updated, null, reason);
        return updated;
    }

    /**
     * Aplica una subida (o bajada) porcentual a todos los productos de una
     * categoría específica.
     *
     * @param priceListId ID de la lista de precios.
     * @param categoryId  ID de la categoría cuyos productos serán afectados.
     * @param percentage  Porcentaje a aplicar.
     * @param reason      Motivo del cambio.
     * @return Número de productos actualizados.
     */
    public int applyPercentageIncreaseToCategory(int priceListId, int categoryId,
            double percentage, String reason) throws SQLException {
        validatePercentage(percentage);
        double multiplier = 1.0 + (percentage / 100.0);
        int updated = priceRepository.applyBulkMultiplier(priceListId, categoryId, multiplier, reason);
        saveLog("percentage", "Por Categoría", percentage, updated, categoryId, reason);
        return updated;
    }

    /**
     * Aplica una subida (o bajada) por importe fijo a TODOS los productos.
     */
    public int applyFixedAmountIncreaseToAll(int priceListId, double amount, String reason)
            throws SQLException {
        validateAmount(amount);
        int updated = priceRepository.applyBulkFixedAmount(priceListId, null, amount, reason);
        saveLog("fixed_amount", "Todos los Artículos", amount, updated, null, reason);
        return updated;
    }

    /**
     * Aplica una subida (o bajada) por importe fijo a una categoría.
     */
    public int applyFixedAmountIncreaseToCategory(int priceListId, int categoryId,
            double amount, String reason) throws SQLException {
        validateAmount(amount);
        int updated = priceRepository.applyBulkFixedAmount(priceListId, categoryId, amount, reason);
        saveLog("fixed_amount", "Por Categoría", amount, updated, categoryId, reason);
        return updated;
    }

    /**
     * Redondea precios masivamente a un decimal objetivo (ej: .99).
     */
    public int applyRoundingToAll(int priceListId, double targetDecimal, String reason) throws SQLException {
        int updated = priceRepository.applyBulkRounding(priceListId, null, targetDecimal, reason);
        saveLog("rounding", "Todos los Artículos", targetDecimal, updated, null, reason);
        return updated;
    }

    /**
     * Redondea precios de una categoría a un decimal objetivo (ej: .99).
     */
    public int applyRoundingToCategory(int priceListId, int categoryId, double targetDecimal, String reason)
            throws SQLException {
        int updated = priceRepository.applyBulkRounding(priceListId, categoryId, targetDecimal, reason);
        return updated;
    }

    /**
     * Aplica un ajuste (porcentual o fijo) a los N artículos más vendidos.
     */
    public int applyToTopSellers(int priceListId, int topN, int daysBack, double value, String reason,
            boolean isPercentage)
            throws SQLException {
        if (isPercentage)
            validatePercentage(value);
        else
            validateAmount(value);
        if (topN <= 0)
            throw new BusinessException("El número de artículos Top debe ser mayor que 0.");
        if (daysBack <= 0)
            throw new BusinessException("Los días de análisis deben ser mayores que 0.");

        double repoValue = isPercentage ? (1.0 + (value / 100.0)) : value;
        int updated = priceRepository.applyBulkMultiplierToTopSellers(priceListId, topN, daysBack, repoValue, reason,
                isPercentage);
        saveLog(isPercentage ? "percentage" : "fixed_amount", "Top " + topN + " Vendidos", value, updated, null,
                reason);
        return updated;
    }

    /**
     * Aplica un ajuste (porcentual o fijo) a artículos sin ventas (slow-movers).
     */
    public int applyToSlowMovers(int priceListId, int daysWithoutSale, double value, String reason,
            boolean isPercentage)
            throws SQLException {
        if (isPercentage)
            validatePercentage(value);
        else
            validateAmount(value);
        if (daysWithoutSale <= 0)
            throw new BusinessException("Los días sin venta deben ser mayores que 0.");

        double repoValue = isPercentage ? (1.0 + (value / 100.0)) : value;
        int updated = priceRepository.applyBulkMultiplierToSlowMovers(priceListId, daysWithoutSale, repoValue, reason,
                isPercentage);
        saveLog(isPercentage ? "percentage" : "fixed_amount", "Sin Ventas (" + daysWithoutSale + "d)", value, updated,
                null, reason);
        return updated;
    }

    /**
     * Aplica un ajuste (porcentual o fijo) dentro de un rango de precio.
     */
    public int applyToPriceRange(int priceListId, double minPrice, double maxPrice, double value, String reason,
            boolean isPercentage)
            throws SQLException {
        if (isPercentage)
            validatePercentage(value);
        else
            validateAmount(value);
        if (minPrice < 0)
            throw new BusinessException("El precio mínimo no puede ser negativo.");
        if (maxPrice <= minPrice)
            throw new BusinessException("El precio máximo debe ser mayor que el mínimo.");

        double repoValue = isPercentage ? (1.0 + (value / 100.0)) : value;
        int updated = priceRepository.applyBulkMultiplierToPriceRange(priceListId, minPrice, maxPrice, repoValue,
                reason,
                isPercentage);
        saveLog(isPercentage ? "percentage" : "fixed_amount", "Rango [" + minPrice + "-" + maxPrice + "]", value,
                updated, null, reason);
        return updated;
    }

    /**
     * Aplica un ajuste (porcentual o fijo) a favoritos.
     */
    public int applyToFavorites(int priceListId, double value, String reason, boolean isPercentage)
            throws SQLException {
        if (isPercentage)
            validatePercentage(value);
        else
            validateAmount(value);

        double repoValue = isPercentage ? (1.0 + (value / 100.0)) : value;
        int updated = priceRepository.applyBulkMultiplierToFavorites(priceListId, repoValue, reason, isPercentage);
        saveLog(isPercentage ? "percentage" : "fixed_amount", "Favoritos ★", value, updated, null, reason);
        return updated;
    }

    /**
     * Aplica un ajuste (porcentual o fijo) a los N artículos MENOS vendidos.
     */
    public int applyToBottomSellers(int priceListId, int bottomN, int daysBack, double value, String reason,
            boolean isPercentage)
            throws SQLException {
        if (isPercentage)
            validatePercentage(value);
        else
            validateAmount(value);
        if (bottomN <= 0)
            throw new BusinessException("El número de artículos a actualizar debe ser mayor que 0.");
        if (daysBack <= 0)
            throw new BusinessException("Los días de análisis deben ser mayores que 0.");

        double repoValue = isPercentage ? (1.0 + (value / 100.0)) : value;
        int updated = priceRepository.applyBulkMultiplierToBottomSellers(priceListId, bottomN, daysBack, repoValue,
                reason,
                isPercentage);
        saveLog(isPercentage ? "percentage" : "fixed_amount", "Bottom " + bottomN + " Vendidos", value, updated, null,
                reason);
        return updated;
    }

    /**
     * Clona una lista de precios en otra aplicando un multiplicador masivo.
     * Útil para crear tarifas de mayoristas a partir de la general.
     */
    public void cloneAndAdjustPrices(int sourceListId, int targetListId, double percentage, String reason)
            throws SQLException {
        double multiplier = 1.0 + (percentage / 100.0);
        priceRepository.cloneAndAdjustPriceList(sourceListId, targetListId, multiplier, reason);
        saveLog("clonacion", "Desde Tarifa " + sourceListId, percentage, -1, null, reason);
    }

    /**
     * Aplica un nuevo tipo de IVA a todos los productos.
     */
    public int applyTaxToAll(double taxRate, String reason) throws SQLException {
        productRepository.updateTaxRateToAll(taxRate);
        int updated = productRepository.count(); // Aproximado o exacto según repo
        saveLog("tax_change", "Todos los Artículos", taxRate, updated, null, reason);
        return updated;
    }

    /**
     * Aplica un nuevo tipo de IVA a todos los productos de una categoría.
     */
    public int applyTaxToCategory(int categoryId, double taxRate, String reason) throws SQLException {
        productRepository.updateTaxRateByCategory(categoryId, taxRate);
        // Aquí no tenemos un count exacto fácil sin query extra, pero guardamos el log.
        saveLog("tax_change", "Por Categoría", taxRate, -1, categoryId, reason);
        return 1; // Indicativo
    }

    /**
     * Redondea precios de los artículos Top Sellers.
     */
    public int applyRoundingToTopSellers(int priceListId, int topN, int daysBack, double targetDecimal, String reason)
            throws SQLException {
        int updated = priceRepository.applyBulkRoundingToTopSellers(priceListId, topN, daysBack, targetDecimal, reason);
        saveLog("rounding", "Top " + topN + " Vendidos", targetDecimal, updated, null, reason);
        return updated;
    }

    /**
     * Redondea precios de los artículos Bottom Sellers.
     */
    public int applyRoundingToBottomSellers(int priceListId, int bottomN, int daysBack, double targetDecimal,
            String reason)
            throws SQLException {
        int updated = priceRepository.applyBulkRoundingToBottomSellers(priceListId, bottomN, daysBack, targetDecimal,
                reason);
        saveLog("rounding", "Bottom " + bottomN + " Vendidos", targetDecimal, updated, null, reason);
        return updated;
    }

    /**
     * Redondea precios de los artículos Sin Movimiento (Slow-movers).
     */
    public int applyRoundingToSlowMovers(int priceListId, int daysWithoutSale, double targetDecimal, String reason)
            throws SQLException {
        int updated = priceRepository.applyBulkRoundingToSlowMovers(priceListId, daysWithoutSale, targetDecimal,
                reason);
        saveLog("rounding", "Sin Ventas (" + daysWithoutSale + "d)", targetDecimal, updated, null, reason);
        return updated;
    }

    /**
     * Redondea precios de artículos en un rango de precio.
     */
    public int applyRoundingToPriceRange(int priceListId, double minPrice, double maxPrice, double targetDecimal,
            String reason) throws SQLException {
        int updated = priceRepository.applyBulkRoundingToPriceRange(priceListId, minPrice, maxPrice, targetDecimal,
                reason);
        saveLog("rounding", "Rango [" + minPrice + "-" + maxPrice + "]", targetDecimal, updated, null, reason);
        return updated;
    }

    /**
     * Redondea precios de artículos favoritos.
     */
    public int applyRoundingToFavorites(int priceListId, double targetDecimal, String reason) throws SQLException {
        return priceRepository.applyBulkRoundingToFavorites(priceListId, targetDecimal, reason);
    }

    private void validateAmount(double amount) {
        if (amount == 0) {
            throw new BusinessException("El importe de cambio no puede ser cero.");
        }
    }

    /**
     * Reglas de negocio sobre el porcentaje permitido.
     * El dominio decide qué valores son aceptables, no la UI.
     */
    private void validatePercentage(double percentage) {
        if (percentage == 0) {
            throw new BusinessException("El porcentaje de cambio no puede ser cero.");
        }
        if (percentage < -99) {
            throw new BusinessException(
                    "No se puede reducir el precio más de un 99%. Valor recibido: " + percentage + "%");
        }
        if (percentage > 200) {
            throw new BusinessException(
                    "Una subida del " + percentage + "% parece incorrecta. "
                            + "Confirme manualmente si es un cambio intencionado superior al 200%.");
        }
    }

    private void saveLog(String type, String scope, double value, int updated, Integer categoryId, String reason) {
        try {
            PriceUpdateLog log = new PriceUpdateLog();
            log.setUpdateType(type);
            log.setScope(scope);
            log.setValue(value);
            log.setProductsUpdated(updated);
            log.setCategoryId(categoryId);
            log.setReason(reason);
            priceLogRepository.save(log);
        } catch (Exception ex) {
            // Log simple, no interrumpir la operación principal
            System.err.println("Error guardando log de auditoría: " + ex.getMessage());
        }
    }
}
