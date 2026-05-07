package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.PriceUpdateLog;
import com.mycompany.ventacontrolfx.domain.repository.IPriceUpdateLogRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio JDBC para el historial de actualizaciones masivas de precios.
 */
public class JdbcPriceUpdateLogRepository implements IPriceUpdateLogRepository {

    /**
     * Guarda un nuevo registro de actualizaci\u00f3n masiva.
     */
    @Override
    public void save(PriceUpdateLog log) throws SQLException {
        String sql = "INSERT INTO price_update_log " +
                "(update_type, scope, category_id, value, products_updated, reason, price_list_id, applied_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, log.getUpdateType());
            ps.setString(2, log.getScope());
            if (log.getCategoryId() != null)
                ps.setInt(3, log.getCategoryId());
            else
                ps.setNull(3, Types.INTEGER);
            ps.setDouble(4, log.getValue());
            ps.setInt(5, log.getProductsUpdated());
            ps.setString(6, log.getReason());
            if (log.getPriceListId() != null)
                ps.setInt(7, log.getPriceListId());
            else
                ps.setNull(7, Types.INTEGER);
            ps.setTimestamp(8, Timestamp.valueOf(log.getAppliedAt()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    log.setLogId(rs.getInt(1));
            }
        }
    }

    /**
     * Obtiene el historial de actualizaciones de precios.
     */
    @Override
    public List<PriceUpdateLog> getAll() throws SQLException {
        List<PriceUpdateLog> result = new ArrayList<>();
        String sql = "SELECT l.*, c.name as category_name " +
                "FROM price_update_log l " +
                "LEFT JOIN categories c ON l.category_id = c.category_id " +
                "ORDER BY l.applied_at DESC";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                PriceUpdateLog log = new PriceUpdateLog();
                log.setLogId(rs.getInt("log_id"));
                log.setUpdateType(rs.getString("update_type"));
                log.setScope(rs.getString("scope"));
                int catId = rs.getInt("category_id");
                if (!rs.wasNull())
                    log.setCategoryId(catId);
                log.setValue(rs.getDouble("value"));
                log.setProductsUpdated(rs.getInt("products_updated"));
                log.setReason(rs.getString("reason"));
                Timestamp ts = rs.getTimestamp("applied_at");
                if (ts != null)
                    log.setAppliedAt(ts.toLocalDateTime());
                int plId = rs.getInt("price_list_id");
                if (!rs.wasNull())
                    log.setPriceListId(plId);
                log.setCategoryName(rs.getString("category_name"));
                result.add(log);
            }
        }
        return result;
    }

    @Override
    public void updateProductsUpdatedCount(int logId, int count) throws SQLException {
        String sql = "UPDATE price_update_log SET products_updated = ? WHERE log_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, count);
            ps.setInt(2, logId);
            ps.executeUpdate();
        }
    }
}

