package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.dto.PriceUpdateLogDTO;
import com.mycompany.ventacontrolfx.domain.dto.ProductPriceDTO;
import com.mycompany.ventacontrolfx.domain.model.Price;
import java.sql.SQLException;
import java.util.List;

public interface IPriceHistoryRepository {
    List<Price> findPriceHistory(int productId) throws SQLException;
    List<ProductPriceDTO> findAllPriceHistory(int priceListId) throws SQLException;
    List<PriceUpdateLogDTO> findBulkUpdateLog(int priceListId) throws SQLException;
    String getAveragePercentageDifference(int priceListId) throws SQLException;
}
