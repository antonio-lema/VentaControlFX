package com.mycompany.ventacontrolfx.domain.repository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public interface IMassivePriceUpdateRepository {
    int applyBulkMultiplier(int priceListId, Integer categoryId, double multiplier, String reason, LocalDateTime startDate) throws SQLException;
    int applyBulkFixedAmount(int priceListId, Integer categoryId, double amount, String reason, LocalDateTime startDate) throws SQLException;
    int applyBulkRounding(int priceListId, Integer categoryId, double roundingTarget, String reason, LocalDateTime startDate) throws SQLException;
    
    int applyBulkMultiplierToTopSellers(int priceListId, int topN, int daysBack, double value, String reason, boolean isPercentage, LocalDateTime startDate) throws SQLException;
    int applyBulkMultiplierToSlowMovers(int priceListId, int daysWithoutSale, double value, String reason, boolean isPercentage, LocalDateTime startDate) throws SQLException;
    int applyBulkMultiplierToBottomSellers(int priceListId, int bottomN, int daysBack, double value, String reason, boolean isPercentage, LocalDateTime startDate) throws SQLException;
    int applyBulkMultiplierToPriceRange(int priceListId, double minPrice, double maxPrice, double value, String reason, boolean isPercentage, LocalDateTime startDate) throws SQLException;
    int applyBulkMultiplierToFavorites(int priceListId, double value, String reason, boolean isPercentage, LocalDateTime startDate) throws SQLException;
    
    int applyBulkRoundingToTopSellers(int priceListId, int topN, int daysBack, double roundingTarget, String reason, LocalDateTime startDate) throws SQLException;
    int applyBulkRoundingToBottomSellers(int priceListId, int bottomN, int daysBack, double roundingTarget, String reason, LocalDateTime startDate) throws SQLException;
    int applyBulkRoundingToSlowMovers(int priceListId, int daysWithoutSale, double roundingTarget, String reason, LocalDateTime startDate) throws SQLException;
    int applyBulkRoundingToPriceRange(int priceListId, double minPrice, double maxPrice, double roundingTarget, String reason, LocalDateTime startDate) throws SQLException;
    int applyBulkRoundingToFavorites(int priceListId, double roundingTarget, String reason, LocalDateTime startDate) throws SQLException;
    
    int applyBulkMultiplierToProducts(int priceListId, List<Integer> productIds, double multiplier, String reason, LocalDateTime startDate) throws SQLException;
    int applyBulkFixedAmountToProducts(int priceListId, List<Integer> productIds, double amount, String reason, LocalDateTime startDate) throws SQLException;
    int applyBulkRoundingToProducts(int priceListId, List<Integer> productIds, double targetDecimal, String reason, LocalDateTime startDate) throws SQLException;
    
    void clonePriceList(int sourceId, int targetId) throws SQLException;
    void cloneAndAdjustPriceList(int sourceId, int targetId, double multiplier, String reason, LocalDateTime startDate) throws SQLException;
}
