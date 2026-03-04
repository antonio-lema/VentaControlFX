package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.Price;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface IPriceRepository {

    /**
     * Recupera el precio actualmente vigente para un producto y una lista
     * específica.
     */
    Optional<Price> getActivePrice(int productId, int priceListId) throws SQLException;

    /**
     * Persiste un nuevo precio.
     */
    void save(Price price) throws SQLException;

    /**
     * Finaliza la vigencia del precio actual de un producto en una lista.
     * Útil antes de insertar uno nuevo.
     */
    void closeCurrentPrice(int productId, int priceListId) throws SQLException;

    /**
     * Operación atómica: Cierra el precio actual e inserta el nuevo en una
     * transacción.
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
}
