package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.TaxRevision;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de repositorio para el historial de tasas de IVA.
 *
 * Nota de Clean Architecture: Esta interfaz vive en el Dominio y NO
 * debe importar nada de infraestructura (java.sql, JDBC, etc.).
 * El único motivo por el que se include SQLException es por la convención
 * del proyecto (el equipo decidió no usar una excepción de dominio propia
 * para persistencia en esta versión).
 */
public interface ITaxRepository {

    /**
     * Resuelve la tasa de IVA efectiva para un producto en una fecha dada,
     * aplicando la cascada: PRODUCT → CATEGORY → GLOBAL.
     *
     * @param productId  ID del producto.
     * @param categoryId ID de la categoría del producto.
     * @param at         Momento para el que se quiere conocer el IVA.
     * @return Tasa de IVA aplicable (ej: 21.0 para 21%).
     */
    double resolveActiveRate(int productId, int categoryId, LocalDateTime at) throws SQLException;

    /**
     * Persiste una nueva revisión de IVA. No cierra la anterior; eso debe
     * hacerse explícitamente antes si se quiere versionar correctamente.
     */
    void save(TaxRevision revision) throws SQLException;

    /**
     * Operación atómica: cierra el IVA vigente para el mismo scope/producto/
     * categoría e inserta el nuevo, garantizando que no hay solapamientos.
     */
    void closeCurrentAndSave(TaxRevision newRevision) throws SQLException;

    /**
     * Obtiene todas las revisiones de IVA de un producto específico
     * (scope PRODUCT), ordenadas de más reciente a más antigua.
     */
    List<TaxRevision> findProductHistory(int productId) throws SQLException;

    /**
     * Obtiene todas las revisiones de IVA de una categoría (scope CATEGORY).
     */
    List<TaxRevision> findCategoryHistory(int categoryId) throws SQLException;

    /**
     * Obtiene el historial de tasas globales (scope GLOBAL).
     */
    List<TaxRevision> findGlobalHistory() throws SQLException;

    /**
     * Obtiene la tasa global actualmente vigente, si existe.
     */
    Optional<TaxRevision> getActiveGlobalRate() throws SQLException;

    /**
     * Aplica un cambio de IVA masivo a todos los productos de una categoría.
     * Crea nuevas revisiones con start_date = effectiveFrom y cierra las antiguas.
     *
     * @param categoryId    ID de la categoría afectada.
     * @param newRate       Nueva tasa de IVA (ej: 10.0 para 10%).
     * @param effectiveFrom Fecha de inicio de vigencia (puede ser futura).
     * @param reason        Motivo del cambio.
     * @return Número de revisiones creadas.
     */
    int applyBulkVatChangeToCategory(int categoryId, double newRate,
            LocalDateTime effectiveFrom, String reason) throws SQLException;

    /**
     * Aplica un cambio de IVA global (afecta al fallback cuando no hay IVA
     * propio de producto o categoría).
     *
     * @param newRate       Nueva tasa global.
     * @param effectiveFrom Fecha de inicio de vigencia.
     * @param reason        Motivo del cambio.
     */
    void applyGlobalVatChange(double newRate, LocalDateTime effectiveFrom, String reason)
            throws SQLException;
}
