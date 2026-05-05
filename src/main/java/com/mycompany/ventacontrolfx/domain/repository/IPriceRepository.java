package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.dto.ProductPriceDTO;
import com.mycompany.ventacontrolfx.domain.model.Price;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface IPriceRepository {
    Optional<Price> getActivePrice(int productId, int priceListId) throws SQLException;
    void save(Price price) throws SQLException;
    void closeCurrentPrice(int productId, int priceListId) throws SQLException;
    void updateCurrentAndSave(Price newPrice) throws SQLException;
    
    List<PriceList> getAllPriceLists() throws SQLException;
    PriceList getDefaultPriceList() throws SQLException;
    
    List<ProductPriceDTO> findPricesByList(int priceListId) throws SQLException;
    List<ProductPriceDTO> findPricesByListPaginated(int priceListId, String search, java.time.LocalDateTime startDate, int limit, int offset) throws SQLException;
    int countPricesByList(int priceListId, String search, java.time.LocalDateTime startDate) throws SQLException;
}
