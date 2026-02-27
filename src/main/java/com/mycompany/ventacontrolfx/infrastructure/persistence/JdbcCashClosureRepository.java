package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.CashClosure;
import com.mycompany.ventacontrolfx.domain.model.ProductSummary;
import com.mycompany.ventacontrolfx.domain.repository.ICashClosureRepository;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class JdbcCashClosureRepository implements ICashClosureRepository {

    @Override
    public void save(CashClosure closure) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
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
                        linkTransactionsToClosure(connection, closureId);
                    }
                }
            }
        }
    }

    private void linkTransactionsToClosure(Connection connection, int closureId) throws SQLException {
        String sqlSales = "UPDATE sales SET closure_id = ? WHERE closure_id IS NULL";
        try (PreparedStatement pstmt = connection.prepareStatement(sqlSales)) {
            pstmt.setInt(1, closureId);
            pstmt.executeUpdate();
        }

        String sqlReturns = "UPDATE returns SET closure_id = ? WHERE closure_id IS NULL";
        try (PreparedStatement pstmt = connection.prepareStatement(sqlReturns)) {
            pstmt.setInt(1, closureId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<CashClosure> getByRange(LocalDate start, LocalDate end) throws SQLException {
        List<CashClosure> closures = new ArrayList<>();
        String sql = "SELECT c.*, u.username FROM cash_closures c LEFT JOIN users u ON c.user_id = u.user_id WHERE c.closure_date >= ? AND c.closure_date <= ? ORDER BY c.created_at DESC";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(start));
            pstmt.setDate(2, Date.valueOf(end));
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

    @Override
    public List<ProductSummary> getProductSummary(int closureId) throws SQLException {
        List<ProductSummary> summary = new ArrayList<>();
        String sql = "SELECT name, SUM(quantity) as total_qty, SUM(amount) as total_amount FROM (" +
                "  SELECT p.name, sd.quantity, sd.line_total as amount " +
                "  FROM sale_details sd " +
                "  JOIN sales s ON sd.sale_id = s.sale_id " +
                "  JOIN products p ON sd.product_id = p.product_id " +
                "  WHERE s.closure_id = ? " +
                "  UNION ALL " +
                "  SELECT p.name, -rd.quantity, -rd.subtotal as amount " +
                "  FROM return_details rd " +
                "  JOIN returns r ON rd.return_id = r.return_id " +
                "  JOIN products p ON rd.product_id = p.product_id " +
                "  WHERE r.closure_id = ?" +
                ") as combined GROUP BY name HAVING total_qty <> 0 OR total_amount <> 0 ORDER BY total_qty DESC";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, closureId);
            pstmt.setInt(2, closureId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    summary.add(new ProductSummary(
                            rs.getString("name"),
                            rs.getInt("total_qty"),
                            rs.getDouble("total_amount")));
                }
            }
        }
        return summary;
    }

    @Override
    public List<ProductSummary> getPendingProductSummary() throws SQLException {
        List<ProductSummary> summary = new ArrayList<>();
        String sql = "SELECT name, SUM(quantity) as total_qty, SUM(amount) as total_amount FROM (" +
                "  SELECT p.name, sd.quantity, sd.line_total as amount " +
                "  FROM sale_details sd " +
                "  JOIN sales s ON sd.sale_id = s.sale_id " +
                "  JOIN products p ON sd.product_id = p.product_id " +
                "  WHERE s.closure_id IS NULL " +
                "  UNION ALL " +
                "  SELECT p.name, -rd.quantity, -rd.subtotal as amount " +
                "  FROM return_details rd " +
                "  JOIN returns r ON rd.return_id = r.return_id " +
                "  JOIN products p ON rd.product_id = p.product_id " +
                "  WHERE r.closure_id IS NULL" +
                ") as combined GROUP BY name HAVING total_qty <> 0 OR total_amount <> 0 ORDER BY total_qty DESC";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    summary.add(new ProductSummary(
                            rs.getString("name"),
                            rs.getInt("total_qty"),
                            rs.getDouble("total_amount")));
                }
            }
        }
        return summary;
    }

    @Override
    public boolean isClosureDone(LocalDate date) throws SQLException {
        String sql = "SELECT COUNT(*) FROM cash_closures WHERE closure_date = ?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(date));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    @Override
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM cash_closures";
        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    @Override
    public Map<String, Double> getPendingTotals() throws SQLException {
        Map<String, Double> totals = new HashMap<>();
        String sql = "SELECT payment_method, SUM(total) as amount FROM sales WHERE closure_id IS NULL GROUP BY payment_method";

        double cash = 0, card = 0;
        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String method = rs.getString("payment_method");
                double amount = rs.getDouble("amount");
                if ("Efectivo".equalsIgnoreCase(method))
                    cash = amount;
                else if ("Tarjeta".equalsIgnoreCase(method))
                    card = amount;
            }
        }

        String sqlReturns = "SELECT s.payment_method, SUM(r.total_refunded) as amount " +
                "FROM returns r " +
                "JOIN sales s ON r.sale_id = s.sale_id " +
                "WHERE r.closure_id IS NULL " +
                "GROUP BY s.payment_method";
        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sqlReturns)) {
            while (rs.next()) {
                String method = rs.getString("payment_method");
                double amount = rs.getDouble("amount");
                if ("Efectivo".equalsIgnoreCase(method))
                    cash -= amount;
                else if ("Tarjeta".equalsIgnoreCase(method))
                    card -= amount;
            }
        }

        totals.put("cash", cash);
        totals.put("card", card);
        totals.put("total", cash + card);
        return totals;
    }

    @Override
    public int getPendingTransactionCount() throws SQLException {
        String sql = "SELECT " +
                " (SELECT COUNT(*) FROM sales WHERE closure_id IS NULL) + " +
                " (SELECT COUNT(*) FROM returns WHERE closure_id IS NULL) as total_pending";
        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}
