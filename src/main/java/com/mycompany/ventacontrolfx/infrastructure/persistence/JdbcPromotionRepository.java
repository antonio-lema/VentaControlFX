package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.Promotion;
import com.mycompany.ventacontrolfx.domain.model.PromotionScope;
import com.mycompany.ventacontrolfx.domain.model.PromotionType;
import com.mycompany.ventacontrolfx.domain.repository.IPromotionRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcPromotionRepository implements IPromotionRepository {

    @Override
    public List<Promotion> getAll() throws SQLException {
        List<Promotion> promotions = new ArrayList<>();
        String sql = "SELECT * FROM promotions";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                promotions.add(mapResultSetToPromotion(rs));
            }
        }
        for (Promotion p : promotions) {
            p.setAffectedIds(getAffectedIds(p.getId()));
        }
        return promotions;
    }

    @Override
    public List<Promotion> getActive() throws SQLException {
        List<Promotion> promotions = new ArrayList<>();
        String sql = "SELECT * FROM promotions WHERE active = 1 AND (start_date IS NULL OR start_date <= NOW()) AND (end_date IS NULL OR end_date >= NOW())";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                promotions.add(mapResultSetToPromotion(rs));
            }
        }
        for (Promotion p : promotions) {
            p.setAffectedIds(getAffectedIds(p.getId()));
        }
        return promotions;
    }

    @Override
    public Optional<Promotion> getById(int id) throws SQLException {
        String sql = "SELECT * FROM promotions WHERE promotion_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Promotion p = mapResultSetToPromotion(rs);
                    p.setAffectedIds(getAffectedIds(id));
                    return Optional.of(p);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Promotion> findByCode(String code) throws SQLException {
        String sql = "SELECT * FROM promotions WHERE code = ? AND active = 1 AND (start_date IS NULL OR start_date <= DATE_ADD(NOW(), INTERVAL 1 SECOND)) AND (end_date IS NULL OR end_date >= NOW())";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Promotion p = mapResultSetToPromotion(rs);
                    p.setAffectedIds(getAffectedIds(p.getId()));
                    return Optional.of(p);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Promotion save(Promotion p) throws SQLException {
        String sql = "INSERT INTO promotions (name, description, type, value, start_date, end_date, active, scope, buy_qty, free_qty, code, max_uses, current_uses, uses_per_customer, customer_id, min_order_value) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, p.getName());
            pstmt.setString(2, p.getDescription());
            pstmt.setString(3, p.getType().name());
            pstmt.setDouble(4, p.getValue());
            pstmt.setTimestamp(5, p.getStartDate() != null ? Timestamp.valueOf(p.getStartDate()) : null);
            pstmt.setTimestamp(6, p.getEndDate() != null ? Timestamp.valueOf(p.getEndDate()) : null);
            pstmt.setBoolean(7, p.isActive());
            pstmt.setString(8, p.getScope().name());
            pstmt.setInt(9, p.getBuyQty());
            pstmt.setInt(10, p.getFreeQty());
            pstmt.setString(11, p.getCode());
            pstmt.setInt(12, p.getMaxUses());
            pstmt.setInt(13, p.getCurrentUses());
            pstmt.setInt(14, p.getUsesPerCustomer());
            if (p.getCustomerId() != null)
                pstmt.setInt(15, p.getCustomerId());
            else
                pstmt.setNull(15, java.sql.Types.INTEGER);
            pstmt.setDouble(16, p.getMinOrderValue());
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setId(rs.getInt(1));
                }
            }
            saveAffectedIds(p.getId(), p.getAffectedIds());
        }
        return p;
    }

    @Override
    public void update(Promotion p) throws SQLException {
        String sql = "UPDATE promotions SET name = ?, description = ?, type = ?, value = ?, start_date = ?, end_date = ?, active = ?, scope = ?, buy_qty = ?, free_qty = ?, code = ?, max_uses = ?, current_uses = ?, uses_per_customer = ?, customer_id = ?, min_order_value = ? WHERE promotion_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, p.getName());
            pstmt.setString(2, p.getDescription());
            pstmt.setString(3, p.getType().name());
            pstmt.setDouble(4, p.getValue());
            pstmt.setTimestamp(5, p.getStartDate() != null ? Timestamp.valueOf(p.getStartDate()) : null);
            pstmt.setTimestamp(6, p.getEndDate() != null ? Timestamp.valueOf(p.getEndDate()) : null);
            pstmt.setBoolean(7, p.isActive());
            pstmt.setString(8, p.getScope().name());
            pstmt.setInt(9, p.getBuyQty());
            pstmt.setInt(10, p.getFreeQty());
            pstmt.setString(11, p.getCode());
            pstmt.setInt(12, p.getMaxUses());
            pstmt.setInt(13, p.getCurrentUses());
            pstmt.setInt(14, p.getUsesPerCustomer());
            if (p.getCustomerId() != null)
                pstmt.setInt(15, p.getCustomerId());
            else
                pstmt.setNull(15, java.sql.Types.INTEGER);
            pstmt.setDouble(16, p.getMinOrderValue());
            pstmt.setInt(17, p.getId());
            pstmt.executeUpdate();

            saveAffectedIds(p.getId(), p.getAffectedIds());
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM promotions WHERE promotion_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    private List<Integer> getAffectedIds(int promotionId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT item_id FROM promotion_items WHERE promotion_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, promotionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }
        return ids;
    }

    private void saveAffectedIds(int promotionId, List<Integer> ids) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Delete old ones
                String deleteSql = "DELETE FROM promotion_items WHERE promotion_id = ?";
                try (PreparedStatement delStmt = conn.prepareStatement(deleteSql)) {
                    delStmt.setInt(1, promotionId);
                    delStmt.executeUpdate();
                }
                // Insert new ones
                if (ids != null && !ids.isEmpty()) {
                    String insertSql = "INSERT INTO promotion_items (promotion_id, item_id) VALUES (?, ?)";
                    try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                        for (Integer id : ids) {
                            insStmt.setInt(1, promotionId);
                            insStmt.setInt(2, id);
                            insStmt.addBatch();
                        }
                        insStmt.executeBatch();
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
    }

    @Override
    public void toggleActive(int id, boolean active) throws SQLException {
        String sql = "UPDATE promotions SET active = ? WHERE promotion_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, active);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    private Promotion mapResultSetToPromotion(ResultSet rs) throws SQLException {
        Promotion p = new Promotion();
        p.setId(rs.getInt("promotion_id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setType(PromotionType.valueOf(rs.getString("type")));
        p.setValue(rs.getDouble("value"));
        Timestamp start = rs.getTimestamp("start_date");
        if (start != null)
            p.setStartDate(start.toLocalDateTime());
        Timestamp end = rs.getTimestamp("end_date");
        if (end != null)
            p.setEndDate(end.toLocalDateTime());
        p.setActive(rs.getBoolean("active"));
        p.setScope(PromotionScope.valueOf(rs.getString("scope")));
        p.setBuyQty(rs.getInt("buy_qty"));
        p.setFreeQty(rs.getInt("free_qty"));
        p.setCode(rs.getString("code"));
        p.setMaxUses(rs.getInt("max_uses"));
        p.setCurrentUses(rs.getInt("current_uses"));
        p.setUsesPerCustomer(rs.getInt("uses_per_customer"));
        int customerId = rs.getInt("customer_id");
        p.setCustomerId(rs.wasNull() ? null : customerId);
        p.setMinOrderValue(rs.getDouble("min_order_value"));
        return p;
    }
}
