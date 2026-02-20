package com.mycompany.ventacontrolfx.dao;

import com.mycompany.ventacontrolfx.model.Return;
import com.mycompany.ventacontrolfx.model.ReturnDetail;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReturnDAO {
    private Connection connection;

    public ReturnDAO(Connection connection) {
        this.connection = connection;
    }

    public int createReturn(Return returnObj) throws SQLException {
        String sql = "INSERT INTO returns (sale_id, user_id, return_datetime, total_refunded, reason) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, returnObj.getSaleId());
            if (returnObj.getUserId() > 0) {
                pstmt.setInt(2, returnObj.getUserId());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setTimestamp(3, Timestamp.valueOf(returnObj.getReturnDatetime()));
            pstmt.setDouble(4, returnObj.getTotalRefunded());
            pstmt.setString(5, returnObj.getReason());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating return failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    returnObj.setReturnId(generatedKeys.getInt(1));
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating return failed, no ID obtained.");
                }
            }
        }
    }

    public void createReturnDetails(List<ReturnDetail> details, int returnId) throws SQLException {
        String sql = "INSERT INTO return_details (return_id, product_id, quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (ReturnDetail detail : details) {
                pstmt.setInt(1, returnId);
                pstmt.setInt(2, detail.getProductId());
                pstmt.setInt(3, detail.getQuantity());
                pstmt.setDouble(4, detail.getUnitPrice());
                pstmt.setDouble(5, detail.getSubtotal());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    // Calculate total returns for today
    public double getTotalReturnsToday() throws SQLException {
        String sql = "SELECT SUM(total_refunded) FROM returns WHERE DATE(return_datetime) = CURRENT_DATE";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        }
        return 0.0;
    }

    public java.util.Map<String, Double> getReturnTotalsByDate(java.time.LocalDate date) throws SQLException {
        java.util.Map<String, Double> totals = new java.util.HashMap<>();
        String sql = "SELECT s.payment_method, SUM(r.total_refunded) as total_refunded " +
                "FROM returns r " +
                "JOIN sales s ON r.sale_id = s.sale_id " +
                "WHERE DATE(r.return_datetime) = ? " +
                "GROUP BY s.payment_method";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(date));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    totals.put(rs.getString("payment_method"), rs.getDouble("total_refunded"));
                }
            }
        }
        return totals;
    }

    public List<Return> getReturnsBySaleId(int saleId) throws SQLException {
        List<Return> returns = new ArrayList<>();
        String sql = "SELECT * FROM returns WHERE sale_id = ? ORDER BY return_datetime DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, saleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Return r = new Return();
                    r.setReturnId(rs.getInt("return_id"));
                    r.setSaleId(rs.getInt("sale_id"));
                    r.setUserId(rs.getInt("user_id"));
                    r.setReturnDatetime(rs.getTimestamp("return_datetime").toLocalDateTime());
                    r.setTotalRefunded(rs.getDouble("total_refunded"));
                    r.setReason(rs.getString("reason"));
                    returns.add(r);
                }
            }
        }
        return returns;
    }
}
