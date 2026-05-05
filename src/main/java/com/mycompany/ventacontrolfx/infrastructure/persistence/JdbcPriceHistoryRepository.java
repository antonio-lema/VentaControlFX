package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.dto.PriceUpdateLogDTO;
import com.mycompany.ventacontrolfx.domain.dto.ProductPriceDTO;
import com.mycompany.ventacontrolfx.domain.model.Price;
import com.mycompany.ventacontrolfx.domain.repository.IPriceHistoryRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcPriceHistoryRepository implements IPriceHistoryRepository {

    @Override
    public List<Price> findPriceHistory(int productId) throws SQLException {
        String sql = "SELECT * FROM product_prices WHERE product_id = ? ORDER BY start_date DESC";
        List<Price> history = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(mapPrice(rs));
                }
            }
        }
        return history;
    }

    @Override
    public List<ProductPriceDTO> findAllPriceHistory(int priceListId) throws SQLException {
        List<ProductPriceDTO> results = new ArrayList<>();
        String sql = "SELECT p.product_id, p.name as product_name, c.name as category_name, "
                + "pp.price, pp.start_date, pp.end_date, pp.reason "
                + "FROM product_prices pp "
                + "JOIN products p ON pp.product_id = p.product_id "
                + "LEFT JOIN categories c ON p.category_id = c.category_id "
                + "WHERE pp.price_list_id = ? AND pp.end_date IS NOT NULL "
                + "ORDER BY pp.end_date DESC LIMIT 200";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, priceListId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProductPriceDTO dto = new ProductPriceDTO(
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getString("category_name"),
                            rs.getDouble("price"),
                            0.0,
                            0.0);
                    Timestamp endTs = rs.getTimestamp("end_date");
                    if (endTs != null) {
                        dto.setEndDate(endTs.toLocalDateTime());
                    }
                    dto.setReason(rs.getString("reason"));
                    results.add(dto);
                }
            }
        }
        return results;
    }

    @Override
    public List<PriceUpdateLogDTO> findBulkUpdateLog(int priceListId) throws SQLException {
        List<PriceUpdateLogDTO> logs = new ArrayList<>();
        String sql = "SELECT * FROM price_update_log WHERE price_list_id = ? OR price_list_id IS NULL ORDER BY applied_at DESC LIMIT 100";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, priceListId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                int plId = rs.getInt("price_list_id");
                Integer plIdObj = rs.wasNull() ? null : plId;
                logs.add(new PriceUpdateLogDTO(
                        rs.getInt("log_id"),
                        rs.getString("update_type"),
                        rs.getString("scope"),
                        rs.getDouble("value"),
                        rs.getInt("products_updated"),
                        rs.getString("reason"),
                        plIdObj,
                        rs.getTimestamp("applied_at").toLocalDateTime()));
                }
            }
        }
        return logs;
    }

    @Override
    public String getAveragePercentageDifference(int priceListId) throws SQLException {
        String sql = "SELECT SUM(pp.price) as current_total, SUM(pp_def.price) as default_total " +
                "FROM product_prices pp " +
                "JOIN product_prices pp_def ON pp.product_id = pp_def.product_id " +
                "  AND pp_def.price_list_id = (SELECT price_list_id FROM price_lists WHERE is_default = 1 LIMIT 1) " +
                "  AND pp_def.start_date <= CURRENT_TIMESTAMP AND (pp_def.end_date IS NULL OR pp_def.end_date > CURRENT_TIMESTAMP) "
                +
                "WHERE pp.price_list_id = ? " +
                "  AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP)";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, priceListId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double currTotal = rs.getDouble("current_total");
                    double defTotal = rs.getDouble("default_total");
                    if (defTotal > 0) {
                        double diff = ((currTotal - defTotal) / defTotal) * 100.0;
                        if (Math.abs(diff) < 0.01)
                            return "Media igual (0%)";
                        if (diff > 0)
                            return String.format("+%.1f%% media", diff);
                        return String.format("%.1f%% media", diff);
                    }
                }
            }
        }
        return "Sin precios";
    }

    private Price mapPrice(ResultSet rs) throws SQLException {
        Price p = new Price();
        p.setId(rs.getInt("price_id"));
        p.setProductId(rs.getInt("product_id"));
        p.setPriceListId(rs.getInt("price_list_id"));
        p.setValue(rs.getDouble("price"));
        p.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
        Timestamp endTs = rs.getTimestamp("end_date");
        if (endTs != null)
            p.setEndDate(endTs.toLocalDateTime());
        p.setReason(rs.getString("reason"));
        int logId = rs.getInt("update_log_id");
        if (!rs.wasNull())
            p.setUpdateLogId(logId);
        return p;
    }
}
