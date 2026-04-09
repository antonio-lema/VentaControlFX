package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.Price;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface IPriceRepository {

        /**
         * Recupera el precio actualmente vigente para un producto y una lista
         * espec\u00edfica.
         */
        Optional<Price> getActivePrice(int productId, int priceListId) throws SQLException;

        /**
         * Persiste un nuevo precio.
         */
        void save(Price price) throws SQLException;

        /**
         * Finaliza la vigencia del precio actual de un producto en una lista.
         * \u00c3\u0161til antes de insertar uno nuevo.
         */
        void closeCurrentPrice(int productId, int priceListId) throws SQLException;

        /**
         * Operaci\u00f3n at\u00f3mica: Cierra el precio actual e inserta el nuevo en una
         * transacci\u00f3n.
         */
        void updateCurrentAndSave(Price newPrice) throws SQLException;

        /**
         * Obtiene todo el historial de precios para un producto (todas las listas).
         */
        List<Price> findPriceHistory(int productId) throws SQLException;

        /**
         * Obtiene las definiciones de listas de precios disponibles.
         */
        List<PriceList> getAllPriceLists() throws SQLException;

        /**
         * Obtiene la lista de precios marcada como predeterminada para el TPV.
         */
        PriceList getDefaultPriceList() throws SQLException;

        /**
         * Operaci\u00f3n masiva y at\u00f3mica: cierra todos los precios activos de la lista
         * (opcionalmente filtrados por categor\u00eda) y crea nuevos aplicando un
         * multiplicador. El motivo queda trazado en la columna 'reason'.
         *
         * @param priceListId ID de la lista de precios a actualizar.
         * @param categoryId  Filtra por categor\u00eda; null = todos los productos.
         * @param multiplier  Factor de multiplicaci\u00f3n (ej. 1.05 = +5%, 0.95 = -5%).
         * @param reason      Motivo del cambio para trazabilidad.
         * @return N\u00famero de productos actualizados.
         */
        int applyBulkMultiplier(int priceListId, Integer categoryId, double multiplier, String reason,
                        java.time.LocalDateTime startDate)
                        throws SQLException;

        /**
         * Operaci\u00f3n masiva y at\u00f3mica: cierra todos los precios activos y crea nuevos
         * sumando/restando
         * un importe fijo.
         *
         * @param priceListId ID de la lista de precios a actualizar.
         * @param categoryId  Filtra por categor\u00eda; null = todos los productos.
         * @param amount      Importe a sumar (positivo) o restar (negativo).
         * @param reason      Motivo del cambio.
         * @return N\u00famero de productos actualizados.
         */
        int applyBulkFixedAmount(int priceListId, Integer categoryId, double amount, String reason,
                        java.time.LocalDateTime startDate)
                        throws SQLException;

        /**
         * Operaci\u00f3n masiva y at\u00f3mica: redondea los precios actuales al decimal deseado.
         * Ejemplo: roundingTarget = 0.99 redondear\u00e1 10.45 -> 10.99.
         *
         * @param priceListId    ID de la lista de precios.
         * @param categoryId     Filtra por categor\u00eda; null = todos.
         * @param roundingTarget Valor decimal objetivo (ej. 0.99, 0.50, 0.00).
         * @param reason         Motivo del cambio.
         * @return N\u00famero de productos actualizados.
         */
        int applyBulkRounding(int priceListId, Integer categoryId, double roundingTarget, String reason,
                        java.time.LocalDateTime startDate)
                        throws SQLException;

        /**
         * Aplica un ajuste (multiplicador o importe fijo) a los N productos m\u00e1s
         * vendidos.
         *
         * @param priceListId  ID de la lista de precios.
         * @param topN         N\u00famero de productos top a actualizar.
         * @param daysBack     D\u00edas hacia atr\u00e1s para calcular ventas.
         * @param value        Valor (multiplicador si isPercentage=true, incremento si
         *                     false).
         * @param reason       Motivo del cambio.
         * @param isPercentage True si 'value' es un multiplicador porcentual, false si
         *                     es importe fijo.
         * @return N\u00famero de productos actualizados.
         */
        int applyBulkMultiplierToTopSellers(int priceListId, int topN, int daysBack, double value, String reason,
                        boolean isPercentage, java.time.LocalDateTime startDate)
                        throws SQLException;

        /**
         * Aplica un ajuste a productos sin ventas (slow-movers).
         */
        int applyBulkMultiplierToSlowMovers(int priceListId, int daysWithoutSale, double value, String reason,
                        boolean isPercentage, java.time.LocalDateTime startDate)
                        throws SQLException;

        /**
         * Aplica un ajuste a los N productos MENOS vendidos (Bottom N).
         */
        int applyBulkMultiplierToBottomSellers(int priceListId, int bottomN, int daysBack, double value, String reason,
                        boolean isPercentage, java.time.LocalDateTime startDate)
                        throws SQLException;

        /**
         * Aplica un ajuste a productos cuyo precio activo est\u00e1 en el rango [minPrice,
         * maxPrice].
         */
        int applyBulkMultiplierToPriceRange(int priceListId, double minPrice, double maxPrice, double value,
                        String reason, boolean isPercentage, java.time.LocalDateTime startDate)
                        throws SQLException;

        /**
         * Aplica un ajuste s\u00f3lo a los productos marcados como favoritos.
         */
        int applyBulkMultiplierToFavorites(int priceListId, double value, String reason, boolean isPercentage,
                        java.time.LocalDateTime startDate)
                        throws SQLException;

        /**
         * Redondea precios de los N productos m\u00e1s vendidos.
         */
        int applyBulkRoundingToTopSellers(int priceListId, int topN, int daysBack, double roundingTarget, String reason,
                        java.time.LocalDateTime startDate)
                        throws SQLException;

        /**
         * Redondea precios de los N productos menos vendidos.
         */
        int applyBulkRoundingToBottomSellers(int priceListId, int bottomN, int daysBack, double roundingTarget,
                        String reason, java.time.LocalDateTime startDate) throws SQLException;

        /**
         * Redondea precios de productos sin ventas.
         */
        int applyBulkRoundingToSlowMovers(int priceListId, int daysWithoutSale, double roundingTarget, String reason,
                        java.time.LocalDateTime startDate)
                        throws SQLException;

        /**
         * Redondea precios dentro de un rango.
         */
        int applyBulkRoundingToPriceRange(int priceListId, double minPrice, double maxPrice, double roundingTarget,
                        String reason, java.time.LocalDateTime startDate) throws SQLException;

        /**
         * Redondea precios de favoritos.
         */
        int applyBulkRoundingToFavorites(int priceListId, double roundingTarget, String reason,
                        java.time.LocalDateTime startDate) throws SQLException;

        void clonePriceList(int sourceId, int targetId) throws SQLException;

        /**
         * Clona precios de una lista a otra aplicando un multiplicador (ej. 0.90 para
         * -10%).
         */
        void cloneAndAdjustPriceList(int sourceId, int targetId, double multiplier, String reason,
                        java.time.LocalDateTime startDate) throws SQLException;

        List<com.mycompany.ventacontrolfx.domain.dto.ProductPriceDTO> findPricesByList(int priceListId)
                        throws SQLException;

        List<com.mycompany.ventacontrolfx.domain.dto.ProductPriceDTO> findPricesByListPaginated(int priceListId,
                        String search, java.time.LocalDateTime startDate, int limit, int offset) throws SQLException;

        int countPricesByList(int priceListId, String search, java.time.LocalDateTime startDate) throws SQLException;

        String getAveragePercentageDifference(int priceListId) throws SQLException;

        int applyBulkMultiplierToProducts(int priceListId, java.util.List<Integer> productIds, double multiplier,
                        String reason, java.time.LocalDateTime startDate) throws SQLException;

        int applyBulkFixedAmountToProducts(int priceListId, java.util.List<Integer> productIds, double amount,
                        String reason, java.time.LocalDateTime startDate) throws SQLException;

        int applyBulkRoundingToProducts(int priceListId, java.util.List<Integer> productIds, double targetDecimal,
                        String reason, java.time.LocalDateTime startDate) throws SQLException;

        /**
         * Obtiene el log de actualizaciones masivas para una lista espec\u00edfica.
         */
        List<com.mycompany.ventacontrolfx.domain.dto.PriceUpdateLogDTO> findBulkUpdateLog(int priceListId)
                        throws SQLException;

        /**
         * Obtiene todos los precios hist\u00f3ricos (no vigentes) de una lista.
         */
        List<com.mycompany.ventacontrolfx.domain.dto.ProductPriceDTO> findAllPriceHistory(int priceListId)
                        throws SQLException;
}
