package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.exception.BusinessException;
import com.mycompany.ventacontrolfx.domain.repository.IPriceRepository;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import com.mycompany.ventacontrolfx.domain.repository.IPriceUpdateLogRepository;
import com.mycompany.ventacontrolfx.domain.model.PriceUpdateLog;

import java.sql.SQLException;

/**
 * Caso de uso: Actualizaci\u00c3\u00b3n masiva de precios.
 *
 * Responsabilidad: Orquestar la l\u00c3\u00b3gica de negocio (validaci\u00c3\u00b3n, c\u00c3\u00a1lculo del
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
     * @return N\u00c3\u00bamero de productos actualizados.
     */
    public int applyPercentageIncreaseToAll(int priceListId, double percentage, String reason,
            java.time.LocalDateTime startDate)
            throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        validatePercentage(percentage);
        double multiplier = 1.0 + (percentage / 100.0);
        int updated = priceRepository.applyBulkMultiplier(priceListId, null, multiplier, reason, startDate);
        saveLog("percentage", "Todos los Art\u00c3\u00adculos", percentage, updated, null, reason, startDate);
        return updated;
    }

    /**
     * Aplica una subida (o bajada) porcentual a todos los productos de una
     * categor\u00c3\u00ada espec\u00c3\u00adfica.
     *
     * @param priceListId ID de la lista de precios.
     * @param categoryId  ID de la categor\u00c3\u00ada cuyos productos ser\u00c3\u00a1n afectados.
     * @param percentage  Porcentaje a aplicar.
     * @param reason      Motivo del cambio.
     * @return N\u00c3\u00bamero de productos actualizados.
     */
    public int applyPercentageIncreaseToCategory(int priceListId, int categoryId,
            double percentage, String reason, java.time.LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        validatePercentage(percentage);
        double multiplier = 1.0 + (percentage / 100.0);
        int updated = priceRepository.applyBulkMultiplier(priceListId, categoryId, multiplier, reason, startDate);
        saveLog("percentage", "Por Categor\u00c3\u00ada", percentage, updated, categoryId, reason, startDate);
        return updated;
    }

    /**
     * Aplica una subida (o bajada) por importe fijo a TODOS los productos.
     */
    public int applyFixedAmountIncreaseToAll(int priceListId, double amount, String reason,
            java.time.LocalDateTime startDate)
            throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        validateAmount(amount);
        int updated = priceRepository.applyBulkFixedAmount(priceListId, null, amount, reason, startDate);
        saveLog("fixed_amount", "Todos los Art\u00c3\u00adculos", amount, updated, null, reason, startDate);
        return updated;
    }

    /**
     * Aplica una subida (o bajada) por importe fijo a una categor\u00c3\u00ada.
     */
    public int applyFixedAmountIncreaseToCategory(int priceListId, int categoryId,
            double amount, String reason, java.time.LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        validateAmount(amount);
        int updated = priceRepository.applyBulkFixedAmount(priceListId, categoryId, amount, reason, startDate);
        saveLog("fixed_amount", "Por Categor\u00c3\u00ada", amount, updated, categoryId, reason, startDate);
        return updated;
    }

    /**
     * Redondea precios masivamente a un decimal objetivo (ej: .99).
     */
    public int applyRoundingToAll(int priceListId, double targetDecimal, String reason,
            java.time.LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        int updated = priceRepository.applyBulkRounding(priceListId, null, targetDecimal, reason, startDate);
        saveLog("rounding", "Todos los Art\u00c3\u00adculos", targetDecimal, updated, null, reason, startDate);
        return updated;
    }

    /**
     * Redondea precios de una categor\u00c3\u00ada a un decimal objetivo (ej: .99).
     */
    public int applyRoundingToCategory(int priceListId, int categoryId, double targetDecimal, String reason,
            java.time.LocalDateTime startDate)
            throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        int updated = priceRepository.applyBulkRounding(priceListId, categoryId, targetDecimal, reason, startDate);
        saveLog("rounding", "Por Categor\u00c3\u00ada", targetDecimal, updated, categoryId, reason, startDate);
        return updated;
    }

    /**
     * Aplica un ajuste (porcentual o fijo) a los N art\u00c3\u00adculos m\u00c3\u00a1s vendidos.
     */
    public int applyToTopSellers(int priceListId, int topN, int daysBack, double value, String reason,
            boolean isPercentage, java.time.LocalDateTime startDate)
            throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        if (isPercentage)
            validatePercentage(value);
        else
            validateAmount(value);
        if (topN <= 0)
            throw new BusinessException("El n\u00c3\u00bamero de art\u00c3\u00adculos Top debe ser mayor que 0.");
        if (daysBack <= 0)
            throw new BusinessException("Los d\u00c3\u00adas de an\u00c3\u00a1lisis deben ser mayores que 0.");

        double repoValue = isPercentage ? (1.0 + (value / 100.0)) : value;
        int updated = priceRepository.applyBulkMultiplierToTopSellers(priceListId, topN, daysBack, repoValue, reason,
                isPercentage, startDate);
        saveLog(isPercentage ? "percentage" : "fixed_amount", "Top " + topN + " Vendidos", value, updated, null,
                reason, startDate);
        return updated;
    }

    /**
     * Aplica un ajuste (porcentual o fijo) a art\u00c3\u00adculos sin ventas (slow-movers).
     */
    public int applyToSlowMovers(int priceListId, int daysWithoutSale, double value, String reason,
            boolean isPercentage, java.time.LocalDateTime startDate)
            throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        if (isPercentage)
            validatePercentage(value);
        else
            validateAmount(value);
        if (daysWithoutSale <= 0)
            throw new BusinessException("Los d\u00c3\u00adas sin venta deben ser mayores que 0.");

        double repoValue = isPercentage ? (1.0 + (value / 100.0)) : value;
        int updated = priceRepository.applyBulkMultiplierToSlowMovers(priceListId, daysWithoutSale, repoValue, reason,
                isPercentage, startDate);
        saveLog(isPercentage ? "percentage" : "fixed_amount", "Sin Ventas (" + daysWithoutSale + "d)", value, updated,
                null, reason, startDate);
        return updated;
    }

    /**
     * Aplica un ajuste (porcentual o fijo) dentro de un rango de precio.
     */
    public int applyToPriceRange(int priceListId, double minPrice, double maxPrice, double value, String reason,
            boolean isPercentage, java.time.LocalDateTime startDate)
            throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        if (isPercentage)
            validatePercentage(value);
        else
            validateAmount(value);
        if (minPrice < 0)
            throw new BusinessException("El precio m\u00c3\u00adnimo no puede ser negativo.");
        if (maxPrice <= minPrice)
            throw new BusinessException("El precio m\u00c3\u00a1ximo debe ser mayor que el m\u00c3\u00adnimo.");

        double repoValue = isPercentage ? (1.0 + (value / 100.0)) : value;
        int updated = priceRepository.applyBulkMultiplierToPriceRange(priceListId, minPrice, maxPrice, repoValue,
                reason, isPercentage, startDate);
        saveLog(isPercentage ? "percentage" : "fixed_amount", "Rango [" + minPrice + "-" + maxPrice + "]", value,
                updated, null, reason, startDate);
        return updated;
    }

    /**
     * Aplica un ajuste (porcentual o fijo) a favoritos.
     */
    public int applyToFavorites(int priceListId, double value, String reason, boolean isPercentage,
            java.time.LocalDateTime startDate)
            throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        if (isPercentage)
            validatePercentage(value);
        else
            validateAmount(value);

        double repoValue = isPercentage ? (1.0 + (value / 100.0)) : value;
        int updated = priceRepository.applyBulkMultiplierToFavorites(priceListId, repoValue, reason, isPercentage,
                startDate);
        saveLog(isPercentage ? "percentage" : "fixed_amount", "Favoritos \u2605", value, updated, null, reason, startDate);
        return updated;
    }

    /**
     * Aplica un ajuste (porcentual o fijo) a los N art\u00c3\u00adculos MENOS vendidos.
     */
    public int applyToBottomSellers(int priceListId, int bottomN, int daysBack, double value, String reason,
            boolean isPercentage, java.time.LocalDateTime startDate)
            throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        if (isPercentage)
            validatePercentage(value);
        else
            validateAmount(value);
        if (bottomN <= 0)
            throw new BusinessException("El n\u00c3\u00bamero de art\u00c3\u00adculos a actualizar debe ser mayor que 0.");
        if (daysBack <= 0)
            throw new BusinessException("Los d\u00c3\u00adas de an\u00c3\u00a1lisis deben ser mayores que 0.");

        double repoValue = isPercentage ? (1.0 + (value / 100.0)) : value;
        int updated = priceRepository.applyBulkMultiplierToBottomSellers(priceListId, bottomN, daysBack, repoValue,
                reason, isPercentage, startDate);
        saveLog(isPercentage ? "percentage" : "fixed_amount", "Bottom " + bottomN + " Vendidos", value, updated, null,
                reason, startDate);
        return updated;
    }

    /**
     * Clona una lista de precios en otra aplicando un multiplicador masivo.
     * \u00c3\u0161til para crear tarifas de mayoristas a partir de la general.
     */
    public void cloneAndAdjustPrices(int sourceListId, int targetListId, double percentage, String reason,
            java.time.LocalDateTime startDate)
            throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        double multiplier = 1.0 + (percentage / 100.0);
        priceRepository.cloneAndAdjustPriceList(sourceListId, targetListId, multiplier, reason, startDate);
        saveLog("clonacion", "Desde Tarifa " + sourceListId, percentage, -1, null, reason, startDate);
    }

    /**
     * Aplica un nuevo tipo de IVA a todos los productos.
     */
    public int applyTaxToAll(double taxRate, String reason) throws SQLException {
        productRepository.updateTaxRateToAll(taxRate);
        int updated = productRepository.count(); // Aproximado o exacto seg\u00c3\u00ban repo
        saveLog("tax_change", "Todos los Art\u00c3\u00adculos", taxRate, updated, null, reason, java.time.LocalDateTime.now());
        return updated;
    }

    /**
     * Aplica un nuevo tipo de IVA a todos los productos de una categor\u00c3\u00ada.
     */
    public int applyTaxToCategory(int categoryId, double taxRate, String reason) throws SQLException {
        productRepository.updateTaxRateByCategory(categoryId, taxRate);
        // Aqu\u00c3\u00ad no tenemos un count exacto f\u00c3\u00a1cil sin query extra, pero guardamos el log.
        saveLog("tax_change_legacy", "Por Categor\u00c3\u00ada", taxRate, -1, categoryId, reason, java.time.LocalDateTime.now());
        return 1; // Indicativo
    }

    /**
     * Aplica un grupo de impuestos V2 (TaxEngine) a todos los productos.
     */
    public int applyTaxGroupToAll(int taxGroupId, String reason) throws SQLException {
        productRepository.updateTaxGroupToAll(taxGroupId);
        int updated = productRepository.count();
        saveLog("tax_group_change", "Todos los Art\u00c3\u00adculos (V2)", taxGroupId, updated, null, reason,
                java.time.LocalDateTime.now());
        return updated;
    }

    /**
     * Aplica un grupo de impuestos V2 (TaxEngine) a una categor\u00c3\u00ada.
     */
    public int applyTaxGroupToCategory(int categoryId, int taxGroupId, String reason) throws SQLException {
        productRepository.updateTaxGroupByCategory(categoryId, taxGroupId);
        saveLog("tax_group_change", "Por Categor\u00c3\u00ada (V2)", taxGroupId, -1, categoryId, reason,
                java.time.LocalDateTime.now());
        return 1;
    }

    /**
     * Redondea precios de los art\u00c3\u00adculos Top Sellers.
     */
    public int applyRoundingToTopSellers(int priceListId, int topN, int daysBack, double targetDecimal, String reason,
            java.time.LocalDateTime startDate)
            throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        int updated = priceRepository.applyBulkRoundingToTopSellers(priceListId, topN, daysBack, targetDecimal, reason,
                startDate);
        saveLog("rounding", "Top " + topN + " Vendidos", targetDecimal, updated, null, reason, startDate);
        return updated;
    }

    /**
     * Redondea precios de los art\u00c3\u00adculos Bottom Sellers.
     */
    public int applyRoundingToBottomSellers(int priceListId, int bottomN, int daysBack, double targetDecimal,
            String reason, java.time.LocalDateTime startDate)
            throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        int updated = priceRepository.applyBulkRoundingToBottomSellers(priceListId, bottomN, daysBack, targetDecimal,
                reason, startDate);
        saveLog("rounding", "Bottom " + bottomN + " Vendidos", targetDecimal, updated, null, reason, startDate);
        return updated;
    }

    /**
     * Redondea precios de los art\u00c3\u00adculos Sin Movimiento (Slow-movers).
     */
    public int applyRoundingToSlowMovers(int priceListId, int daysWithoutSale, double targetDecimal, String reason,
            java.time.LocalDateTime startDate)
            throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        int updated = priceRepository.applyBulkRoundingToSlowMovers(priceListId, daysWithoutSale, targetDecimal,
                reason, startDate);
        saveLog("rounding", "Sin Ventas (" + daysWithoutSale + "d)", targetDecimal, updated, null, reason, startDate);
        return updated;
    }

    /**
     * Redondea precios de art\u00c3\u00adculos en un rango de precio.
     */
    public int applyRoundingToPriceRange(int priceListId, double minPrice, double maxPrice, double targetDecimal,
            String reason, java.time.LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        int updated = priceRepository.applyBulkRoundingToPriceRange(priceListId, minPrice, maxPrice, targetDecimal,
                reason, startDate);
        saveLog("rounding", "Rango [" + minPrice + "-" + maxPrice + "]", targetDecimal, updated, null, reason,
                startDate);
        return updated;
    }

    /**
     * Redondea precios de art\u00c3\u00adculos favoritos.
     */
    public int applyRoundingToFavorites(int priceListId, double targetDecimal, String reason,
            java.time.LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        int updated = priceRepository.applyBulkRoundingToFavorites(priceListId, targetDecimal, reason, startDate);
        saveLog("rounding", "Favoritos \u2605", targetDecimal, updated, null, reason, startDate);
        return updated;
    }

    private void validateAmount(double amount) {
        if (amount == 0) {
            throw new BusinessException("El importe de cambio no puede ser cero.");
        }
    }

    /**
     * Reglas de negocio sobre el porcentaje permitido.
     * El dominio decide qu\u00c3\u00a9 valores son aceptables, no la UI.
     */
    private void validatePercentage(double percentage) {
        if (percentage == 0) {
            throw new BusinessException("El porcentaje de cambio no puede ser cero.");
        }
        if (percentage < -99) {
            throw new BusinessException(
                    "No se puede reducir el precio m\u00c3\u00a1s de un 99%. Valor recibido: " + percentage + "%");
        }
        if (percentage > 200) {
            throw new BusinessException(
                    "Una subida del " + percentage + "% parece incorrecta. "
                            + "Confirme manualmente si es un cambio intencionado superior al 200%.");
        }
    }

    private void saveLog(String type, String scope, double value, int updated, Integer categoryId, String reason,
            java.time.LocalDateTime appliedAt) {
        try {
            PriceUpdateLog log = new PriceUpdateLog();
            log.setUpdateType(type);
            log.setScope(scope);
            log.setValue(value);
            log.setProductsUpdated(updated);
            log.setCategoryId(categoryId);
            log.setReason(reason);
            // Ensure we use the exact same timestamp rounded to seconds if needed,
            // but for now just use the passed one without nanos to be safe
            log.setAppliedAt(appliedAt != null ? appliedAt.withNano(0) : java.time.LocalDateTime.now().withNano(0));
            priceLogRepository.save(log);
        } catch (Exception ex) {
            // Log simple, no interrumpir la operaci\u00c3\u00b3n principal
            System.err.println("Error guardando log de auditor\u00c3\u00ada: " + ex.getMessage());
        }
    }

    public int applyPercentageIncreaseToProducts(int priceListId, java.util.List<Integer> productIds, double percentage,
            String reason, java.time.LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        validatePercentage(percentage);
        double multiplier = 1.0 + (percentage / 100.0);
        int updated = priceRepository.applyBulkMultiplierToProducts(priceListId, productIds, multiplier, reason,
                startDate);
        saveLog("percentage", "Lista de Productos (" + (productIds != null ? productIds.size() : 0) + ")", percentage,
                updated, null, reason, startDate);
        return updated;
    }

    public int applyFixedAmountIncreaseToProducts(int priceListId, java.util.List<Integer> productIds, double amount,
            String reason, java.time.LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        validateAmount(amount);
        int updated = priceRepository.applyBulkFixedAmountToProducts(priceListId, productIds, amount, reason,
                startDate);
        saveLog("fixed_amount", "Lista de Productos (" + (productIds != null ? productIds.size() : 0) + ")", amount,
                updated, null, reason, startDate);
        return updated;
    }

    public int applyRoundingToProducts(int priceListId, java.util.List<Integer> productIds, double targetDecimal,
            String reason, java.time.LocalDateTime startDate) throws SQLException {
        startDate = (startDate != null) ? startDate.withNano(0) : java.time.LocalDateTime.now().withNano(0);
        int updated = priceRepository.applyBulkRoundingToProducts(priceListId, productIds, targetDecimal, reason,
                startDate);
        saveLog("rounding", "Lista de Productos (" + (productIds != null ? productIds.size() : 0) + ")", targetDecimal,
                updated, null, reason, startDate);
        return updated;
    }
}
