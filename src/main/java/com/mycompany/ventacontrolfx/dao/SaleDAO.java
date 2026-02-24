package com.mycompany.ventacontrolfx.dao;

import com.mycompany.ventacontrolfx.model.Sale;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class SaleDAO {
    private Connection connection;

    public SaleDAO(Connection connection) {
        this.connection = connection;
    }

    public Map<String, Double> getTotalsByDate(LocalDate date) throws SQLException {
        Map<String, Double> totals = new HashMap<>();
        totals.put("Efectivo", 0.0);
        totals.put("Tarjeta", 0.0);

        String sql = "SELECT payment_method, SUM(total) as semi_total FROM sales WHERE DATE(created_at) = ? GROUP BY payment_method";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(date));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    totals.put(rs.getString("payment_method"), rs.getDouble("semi_total"));
                }
            }
        }
        return totals;
    }

    public Map<String, Double> getPendingTotals() throws SQLException {
        Map<String, Double> totals = new HashMap<>();
        totals.put("Efectivo", 0.0);
        totals.put("Tarjeta", 0.0);

        String sql = "SELECT payment_method, SUM(total) as semi_total FROM sales WHERE closure_id IS NULL GROUP BY payment_method";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    totals.put(rs.getString("payment_method"), rs.getDouble("semi_total"));
                }
            }
        }
        return totals;
    }

    public int getTransactionCountByDate(LocalDate date) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sales WHERE DATE(created_at) = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(date));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public int getPendingTransactionCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM sales WHERE closure_id IS NULL";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public java.util.List<Sale> getSalesByRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        java.util.List<Sale> sales = new java.util.ArrayList<>();
        String sql = "SELECT s.*, u.username FROM sales s " +
                "JOIN users u ON s.user_id = u.user_id " +
                "WHERE DATE(s.sale_datetime) BETWEEN ? AND ? " +
                "ORDER BY s.sale_datetime DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(startDate));
            pstmt.setDate(2, Date.valueOf(endDate));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sales.add(mapResultSetToSale(rs));
                }
            }
        }
        return sales;
    }

    public Sale getSaleById(int saleId) throws SQLException {
        String sql = "SELECT s.*, u.username FROM sales s " +
                "JOIN users u ON s.user_id = u.user_id " +
                "WHERE s.sale_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, saleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSale(rs);
                }
            }
        }
        return null;
    }

    private Sale mapResultSetToSale(ResultSet rs) throws SQLException {
        Sale sale = new Sale();
        sale.setSaleId(rs.getInt("sale_id"));
        sale.setSaleDateTime(rs.getTimestamp("sale_datetime").toLocalDateTime());
        sale.setUserId(rs.getInt("user_id"));
        sale.setUserName(rs.getString("username"));
        sale.setClientId((Integer) rs.getObject("client_id"));
        sale.setTotal(rs.getDouble("total"));
        sale.setPaymentMethod(rs.getString("payment_method"));
        sale.setIva(rs.getDouble("iva"));
        sale.setReturn(rs.getBoolean("is_return"));
        sale.setReturnReason(rs.getString("return_reason"));
        // Safely try to get returned_amount, default to 0 if column doesn't exist yet
        try {
            sale.setReturnedAmount(rs.getDouble("returned_amount"));
        } catch (SQLException e) {
            sale.setReturnedAmount(0);
        }
        return sale;
    }

    public void updateReturnStatus(int saleId, boolean isReturn, String reason) throws SQLException {
        String sql = "UPDATE sales SET is_return = ?, return_reason = ? WHERE sale_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setBoolean(1, isReturn);
            pstmt.setString(2, reason);
            pstmt.setInt(3, saleId);
            pstmt.executeUpdate();
        }
    }

    public void updateReturnedAmount(int saleId, double amount) throws SQLException {
        String sql = "UPDATE sales SET returned_amount = ? WHERE sale_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setInt(2, saleId);
            pstmt.executeUpdate();
        }
    }

    public int getTotalCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM sales";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}
