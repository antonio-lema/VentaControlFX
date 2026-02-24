package com.mycompany.ventacontrolfx.dao;

import com.mycompany.ventacontrolfx.model.CashClosure;
import com.mycompany.ventacontrolfx.dao.DatabaseInitializer;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CashClosureDAO {
    private Connection connection;

    public CashClosureDAO(Connection connection) {
        this.connection = connection;
    }

    public void save(CashClosure closure) throws SQLException {
        DatabaseInitializer.initialize(connection);
        String sql = "INSERT INTO cash_closures (closure_date, user_id, total_cash, total_card, total_all, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setDate(1, Date.valueOf(closure.getClosureDate()));
            pstmt.setInt(2, closure.getUserId());
            pstmt.setDouble(3, closure.getTotalCash());
            pstmt.setDouble(4, closure.getTotalCard());
            pstmt.setDouble(5, closure.getTotalAll());
            pstmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int closureId = generatedKeys.getInt(1);
                    closure.setClosureId(closureId);
                    // Link all pending sales to this closure
                    linkSalesToClosure(closureId);
                }
            }
        }
    }

    private void linkSalesToClosure(int closureId) throws SQLException {
        String sql = "UPDATE sales SET closure_id = ? WHERE closure_id IS NULL";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, closureId);
            pstmt.executeUpdate();
        }
    }

    public List<com.mycompany.ventacontrolfx.model.ProductSummary> getProductSummary(int closureId)
            throws SQLException {
        List<com.mycompany.ventacontrolfx.model.ProductSummary> summary = new ArrayList<>();
        String sql = "SELECT p.name, SUM(sd.quantity) as total_qty, SUM(sd.line_total) as total_amount " +
                "FROM sale_details sd " +
                "JOIN sales s ON sd.sale_id = s.sale_id " +
                "JOIN products p ON sd.product_id = p.product_id " +
                "WHERE s.closure_id = ? " +
                "GROUP BY p.name " +
                "ORDER BY total_qty DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, closureId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    summary.add(new com.mycompany.ventacontrolfx.model.ProductSummary(
                            rs.getString("name"),
                            rs.getInt("total_qty"),
                            rs.getDouble("total_amount")));
                }
            }
        }
        return summary;
    }

    public List<com.mycompany.ventacontrolfx.model.ProductSummary> getPendingProductSummary()
            throws SQLException {
        List<com.mycompany.ventacontrolfx.model.ProductSummary> summary = new ArrayList<>();
        String sql = "SELECT p.name, SUM(sd.quantity) as total_qty, SUM(sd.line_total) as total_amount " +
                "FROM sale_details sd " +
                "JOIN sales s ON sd.sale_id = s.sale_id " +
                "JOIN products p ON sd.product_id = p.product_id " +
                "WHERE s.closure_id IS NULL " +
                "GROUP BY p.name " +
                "ORDER BY total_qty DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    summary.add(new com.mycompany.ventacontrolfx.model.ProductSummary(
                            rs.getString("name"),
                            rs.getInt("total_qty"),
                            rs.getDouble("total_amount")));
                }
            }
        }
        return summary;
    }

    public boolean isClosureDone(LocalDate date) throws SQLException {
        String sql = "SELECT COUNT(*) FROM cash_closures WHERE closure_date = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(date));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public List<CashClosure> getAllClosures(LocalDate startDate, LocalDate endDate) throws SQLException {
        List<CashClosure> closures = new ArrayList<>();
        String sql = "SELECT c.*, u.username FROM cash_closures c LEFT JOIN users u ON c.user_id = u.user_id WHERE c.closure_date >= ? AND c.closure_date <= ? ORDER BY c.created_at DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(startDate));
            pstmt.setDate(2, Date.valueOf(endDate));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    CashClosure c = new CashClosure();
                    c.setClosureId(rs.getInt("closure_id"));
                    c.setClosureDate(rs.getDate("closure_date").toLocalDate());
                    c.setUserId(rs.getInt("user_id"));
                    c.setTotalCash(rs.getDouble("total_cash"));
                    c.setTotalCard(rs.getDouble("total_card"));
                    c.setTotalAll(rs.getDouble("total_all"));
                    c.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    c.setUsername(rs.getString("username"));
                    closures.add(c);
                }
            }
        }
        return closures;
    }

    public int getCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM cash_closures";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}
