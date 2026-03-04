package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Price;
import com.mycompany.ventacontrolfx.domain.repository.IPriceRepository;
import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class MassivePriceUpdateUseCase {

    private final IPriceRepository priceRepository;

    public MassivePriceUpdateUseCase(IPriceRepository priceRepository) {
        this.priceRepository = priceRepository;
    }

    /**
     * Applies a percentage increase (or decrease) to all current active
     * prices in a given price list.
     *
     * @param priceListId The target price list to update.
     * @param percentage  The percentage to apply (e.g., 10.5 for +10.5%, -5 for
     *                    -5%).
     * @param reason      The reason for this massive update (e.g., "Revisión anual
     *                    2026").
     * @return The number of products updated.
     */
    public int applyPercentageIncreaseToAll(int priceListId, double percentage, String reason) throws SQLException {
        return executeMassiveUpdate(priceListId, null, percentage, reason);
    }

    /**
     * Applies a percentage increase (or decrease) to all current active
     * prices belonging to a specific category.
     */
    public int applyPercentageIncreaseToCategory(int priceListId, int categoryId, double percentage, String reason)
            throws SQLException {
        return executeMassiveUpdate(priceListId, categoryId, percentage, reason);
    }

    /**
     * Core logic for bulk updating prices. Uses a database transaction to close old
     * prices
     * and open new ones atomicly to prevent inconsistent data.
     */
    private int executeMassiveUpdate(int priceListId, Integer categoryId, double percentage, String reason)
            throws SQLException {
        if (percentage == 0)
            return 0;

        int updatedCount = 0;
        double multiplier = 1.0 + (percentage / 100.0);
        LocalDateTime now = LocalDateTime.now();

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // 1. Identify which products to update
                StringBuilder queryBuilder = new StringBuilder();
                queryBuilder.append("SELECT pp.product_id, pp.price FROM product_prices pp ");
                if (categoryId != null) {
                    queryBuilder.append("JOIN products p ON p.product_id = pp.product_id ");
                }
                queryBuilder.append("WHERE pp.price_list_id = ? AND pp.end_date IS NULL ");
                if (categoryId != null) {
                    queryBuilder.append("AND p.category_id = ? ");
                }

                String selectSql = queryBuilder.toString();
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setInt(1, priceListId);
                    if (categoryId != null) {
                        selectStmt.setInt(2, categoryId);
                    }

                    try (ResultSet rs = selectStmt.executeQuery()) {
                        String closeSql = "UPDATE product_prices SET end_date = ? WHERE product_id = ? AND price_list_id = ? AND end_date IS NULL";
                        String insertSql = "INSERT INTO product_prices (product_id, price_list_id, price, start_date, end_date, reason) VALUES (?, ?, ?, ?, NULL, ?)";

                        try (PreparedStatement closeStmt = conn.prepareStatement(closeSql);
                                PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

                            while (rs.next()) {
                                int productId = rs.getInt("product_id");
                                double currentPrice = rs.getDouble("price");
                                double newPrice = currentPrice * multiplier;

                                // Close old price
                                closeStmt.setTimestamp(1, java.sql.Timestamp.valueOf(now));
                                closeStmt.setInt(2, productId);
                                closeStmt.setInt(3, priceListId);
                                closeStmt.addBatch();

                                // Insert new price
                                insertStmt.setInt(1, productId);
                                insertStmt.setInt(2, priceListId);
                                insertStmt.setDouble(3, newPrice);
                                insertStmt.setTimestamp(4, java.sql.Timestamp.valueOf(now));
                                insertStmt.setString(5, reason);
                                insertStmt.addBatch();

                                updatedCount++;
                            }

                            if (updatedCount > 0) {
                                closeStmt.executeBatch();
                                insertStmt.executeBatch();
                            }
                        }
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }

        return updatedCount;
    }
}
