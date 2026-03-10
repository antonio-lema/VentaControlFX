package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.exception.BusinessException;
import com.mycompany.ventacontrolfx.domain.model.TaxRevision;
import com.mycompany.ventacontrolfx.domain.repository.ITaxRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Caso de uso: Gestión de cambios de IVA.
 *
 * Responsabilidad: Orchesta los cambios de IVA (globales, por categoría o
 * por producto), aplicando las reglas de negocio definidas en el dominio y
 * delegando la persistencia a ITaxRepository.
 *
 * CLEAN ARCHITECTURE: No hay SQL aquí. Solo lógica de negocio y orquestación.
 *
 * Casos de uso cubiertos:
 * - Programar una subida/bajada global de IVA (ej: legislación nueva)
 * - Cambiar el IVA de una categoría completa
 * - Cambiar el IVA de un producto específico
 * - Consultar el historial de IVA
 * - Obtener el IVA efectivo para un producto en una fecha dada
 */
public class ScheduleVatChangeUseCase {

    private final ITaxRepository taxRepository;

    // Tipos de IVA comunes en España (informativo, no restrictivo)
    private static final double IVA_GENERAL = 21.0;
    private static final double IVA_REDUCIDO = 10.0;
    private static final double IVA_SUPERREDUCIDO = 4.0;
    private static final double IVA_EXENTO = 0.0;

    public ScheduleVatChangeUseCase(ITaxRepository taxRepository) {
        this.taxRepository = taxRepository;
    }

    // -------------------------------------------------------------------------
    // CAMBIO GLOBAL DE IVA
    // -------------------------------------------------------------------------

    /**
     * Aplica un cambio global de IVA a partir de la fecha indicada.
     * Las ventas ANTERIORES a esa fecha no se ven afectadas porque
     * SaleDetail guarda el ivaRate en el momento de la venta (snapshot).
     *
     * @param newRate       Nueva tasa global (ej: 21.0 para 21%).
     * @param effectiveFrom Fecha de efectividad. Si es null, aplica ahora.
     *                      Puede ser futura (subida programada).
     * @param reason        Justificación del cambio (obligatorio para auditoría).
     */
    public void applyGlobalRateChange(double newRate, LocalDateTime effectiveFrom, String reason)
            throws SQLException {
        validateRate(newRate);
        validateReason(reason);

        LocalDateTime effectiveDate = (effectiveFrom != null) ? effectiveFrom : LocalDateTime.now();

        // No se permite programar un cambio con fecha anterior a ahora
        if (effectiveDate.isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new BusinessException(
                    "No se puede programar un cambio de IVA en el pasado. Usa la fecha actual o una futura.");
        }

        taxRepository.applyGlobalVatChange(newRate, effectiveDate, reason);
    }

    // -------------------------------------------------------------------------
    // CAMBIO DE IVA POR CATEGORÍA
    // -------------------------------------------------------------------------

    /**
     * Cambia el IVA de todos los productos de una categoría a partir de
     * la fecha indicada. Los productos con IVA propio NO se ven afectados.
     *
     * @param categoryId    ID de la categoría.
     * @param newRate       Nueva tasa (ej: 10.0 para IVA Reducido).
     * @param effectiveFrom Fecha de inicio de vigencia.
     * @param reason        Motivo del cambio.
     * @return Número de registros de tasa creados.
     */
    public int applyCategoryRateChange(int categoryId, double newRate,
            LocalDateTime effectiveFrom, String reason) throws SQLException {
        validateRate(newRate);
        validateReason(reason);
        if (categoryId <= 0) {
            throw new BusinessException("El ID de categoría no es válido.");
        }
        LocalDateTime effectiveDate = (effectiveFrom != null) ? effectiveFrom : LocalDateTime.now();
        return taxRepository.applyBulkVatChangeToCategory(categoryId, newRate, effectiveDate, reason);
    }

    // -------------------------------------------------------------------------
    // CAMBIO DE IVA DE UN PRODUCTO ESPECÍFICO
    // -------------------------------------------------------------------------

    /**
     * Asigna un IVA propio a un producto, sobreescribiendo el de su categoría.
     * Útil para productos con tipo especial (ej: libros al 4% en una librería
     * donde el resto va al 21%).
     *
     * @param productId     ID del producto.
     * @param categoryId    ID de la categoría del producto (necesario para scopes).
     * @param newRate       Nueva tasa.
     * @param effectiveFrom Fecha de inicio de vigencia.
     * @param reason        Motivo del cambio.
     */
    public void applyProductRateChange(int productId, int categoryId, double newRate,
            LocalDateTime effectiveFrom, String reason) throws SQLException {
        validateRate(newRate);
        validateReason(reason);
        if (productId <= 0) {
            throw new BusinessException("El ID de producto no es válido.");
        }
        LocalDateTime effectiveDate = (effectiveFrom != null) ? effectiveFrom : LocalDateTime.now();

        TaxRevision revision = new TaxRevision(
                productId, null, TaxRevision.Scope.PRODUCT,
                newRate, buildLabel(newRate),
                effectiveDate, reason);
        taxRepository.closeCurrentAndSave(revision);
    }

    // -------------------------------------------------------------------------
    // CONSULTA
    // -------------------------------------------------------------------------

    /**
     * Resuelve el IVA efectivo que aplicaría a un producto en una fecha dada.
     * Útil para calcular precios históricos o futuros antes de confirmar cambios.
     *
     * @param productId  ID del producto.
     * @param categoryId ID de la categoría del producto.
     * @param at         Fecha para la que se quiere saber el IVA.
     * @return Tasa de IVA aplicable.
     */
    public double getEffectiveRate(int productId, int categoryId, LocalDateTime at)
            throws SQLException {
        LocalDateTime queryDate = (at != null) ? at : LocalDateTime.now();
        return taxRepository.resolveActiveRate(productId, categoryId, queryDate);
    }

    /**
     * Obtiene el historial completo de tasas globales (todas las revisiones).
     */
    public List<TaxRevision> getGlobalHistory() throws SQLException {
        return taxRepository.findGlobalHistory();
    }

    /**
     * Obtiene el historial de tasas de una categoría.
     */
    public List<TaxRevision> getCategoryHistory(int categoryId) throws SQLException {
        return taxRepository.findCategoryHistory(categoryId);
    }

    /**
     * Obtiene el historial de tasas de un producto.
     */
    public List<TaxRevision> getProductHistory(int productId) throws SQLException {
        return taxRepository.findProductHistory(productId);
    }

    /**
     * Obtiene la tasa global actualmente vigente, si existe.
     */
    public Optional<TaxRevision> getCurrentGlobalRate() throws SQLException {
        return taxRepository.getActiveGlobalRate();
    }

    // -------------------------------------------------------------------------
    // REGLAS DE NEGOCIO / VALIDACIONES PRIVADAS
    // -------------------------------------------------------------------------

    private void validateRate(double rate) {
        if (rate < 0) {
            throw new BusinessException("La tasa de IVA no puede ser negativa. Valor: " + rate + "%");
        }
        if (rate > 100) {
            throw new BusinessException("La tasa de IVA no puede superar el 100%. Valor: " + rate + "%");
        }
    }

    private void validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("El motivo del cambio de IVA es obligatorio para auditoría.");
        }
    }

    private String buildLabel(double rate) {
        if (rate == IVA_GENERAL)
            return "IVA General (21%)";
        if (rate == IVA_REDUCIDO)
            return "IVA Reducido (10%)";
        if (rate == IVA_SUPERREDUCIDO)
            return "IVA Superreducido (4%)";
        if (rate == IVA_EXENTO)
            return "Exento de IVA (0%)";
        return "IVA " + rate + "%";
    }
}
