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
            String sql = "INSERT INTO cash_closures (closure_date, user_id, total_cash, total_card, total_all, actual_cash, difference, notes, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setDate(1, Date.valueOf(closure.getClosureDate()));
                pstmt.setInt(2, closure.getUserId());
                pstmt.setDouble(3, closure.getTotalCash());
                pstmt.setDouble(4, closure.getTotalCard());
                pstmt.setDouble(5, closure.getTotalAll());
                pstmt.setDouble(6, closure.getActualCash());
                pstmt.setDouble(7, closure.getDifference());
                pstmt.setString(8, closure.getNotes());
                pstmt.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
                pstmt.executeUpdate();

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int closureId = generatedKeys.getInt(1);
                        closure.setClosureId(closureId);
                        linkTransactionsToClosure(connection, closureId);
                        registerMovement(MovementType.CIERRE, closure.getTotalAll(), "Cierre de caja manual",
                                closure.getUserId());
                    }
                }
            }
        }
    }

    private void linkTransactionsToClosure(Connection connection, int closureId) throws SQLException {
        String[] tables = { "sales", "returns", "cash_fund_sessions", "cash_movements" };
        for (String table : tables) {
            String sql = "UPDATE " + table + " SET closure_id = ? WHERE closure_id IS NULL";
            if ("cash_fund_sessions".equals(table)) {
                sql = "UPDATE cash_fund_sessions SET closure_id = ?, is_closed = TRUE WHERE closure_id IS NULL";
            }
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, closureId);
                pstmt.executeUpdate();
            }
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
                    c.setActualCash(rs.getDouble("actual_cash"));
                    c.setDifference(rs.getDouble("difference"));
                    c.setNotes(rs.getString("notes"));
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
                "  SELECT COALESCE(p.name, 'Producto Eliminado') as name, sd.quantity, sd.line_total as amount " +
                "  FROM sale_details sd JOIN sales s ON sd.sale_id = s.sale_id " +
                "  LEFT JOIN products p ON sd.product_id = p.product_id WHERE s.closure_id = ? " +
                "  UNION ALL " +
                "  SELECT CONCAT('[DEV] ', COALESCE(p.name, 'Producto Eliminado')) as name, -rd.quantity, -rd.subtotal as amount "
                +
                "  FROM return_details rd JOIN returns r ON rd.return_id = r.return_id " +
                "  LEFT JOIN products p ON rd.product_id = p.product_id WHERE r.closure_id = ?" +
                ") as combined GROUP BY name HAVING total_qty <> 0 OR total_amount <> 0 ORDER BY total_qty DESC";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, closureId);
            pstmt.setInt(2, closureId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    summary.add(new ProductSummary(rs.getString("name"), rs.getInt("total_qty"),
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
                "  SELECT COALESCE(p.name, 'Producto Eliminado') as name, sd.quantity, sd.line_total as amount " +
                "  FROM sale_details sd JOIN sales s ON sd.sale_id = s.sale_id " +
                "  LEFT JOIN products p ON sd.product_id = p.product_id WHERE s.closure_id IS NULL " +
                "  UNION ALL " +
                "  SELECT CONCAT('[DEV] ', COALESCE(p.name, 'Producto Eliminado')) as name, -rd.quantity, -rd.subtotal as amount "
                +
                "  FROM return_details rd JOIN returns r ON rd.return_id = r.return_id " +
                "  LEFT JOIN products p ON rd.product_id = p.product_id WHERE r.closure_id IS NULL" +
                ") as combined GROUP BY name HAVING total_qty <> 0 OR total_amount <> 0 ORDER BY total_qty DESC";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    summary.add(new ProductSummary(rs.getString("name"), rs.getInt("total_qty"),
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
                if (rs.next())
                    return rs.getInt(1) > 0;
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
            if (rs.next())
                return rs.getInt(1);
        }
        return 0;
    }

    @Override
    public void registerMovement(MovementType type, double amount, String reason, int userId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            registerMovement(type, amount, reason, userId, connection);
        }
    }

    public void registerMovement(MovementType type, double amount, String reason, int userId, Connection connection)
            throws SQLException {
        String sql = "INSERT INTO cash_movements (session_id, user_id, type, amount, reason, ip_address) " +
                "VALUES ((SELECT session_id FROM cash_fund_sessions WHERE is_closed = FALSE ORDER BY created_at DESC LIMIT 1), ?, ?, ?, ?, ?)";
        String ip = "127.0.0.1";
        try {
            ip = java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
        }

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, type.toString());
            pstmt.setDouble(3, amount);
            pstmt.setString(4, reason != null ? reason : "");
            pstmt.setString(5, ip);
            pstmt.executeUpdate();

            // Auditoría para movimientos críticos (Manuales y Devoluciones)
            if (type == MovementType.INGRESO || type == MovementType.RETIRADA || type == MovementType.DEVOLUCION) {
                registerAudit(connection, userId, "CASH_" + type.toString(),
                        String.format("Importe: %.2f€. Motivo: %s", amount, reason), ip);
            }
        }
    }

    private void registerAudit(Connection conn, int userId, String action, String description, String ip)
            throws SQLException {
        String sql = "INSERT INTO audit_logs (user_id, event_type, action_description, ip_address) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, action);
            pstmt.setString(3, description);
            pstmt.setString(4, ip);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void withdrawCash(double amount, String reason, int userId) throws SQLException {
        registerMovement(MovementType.RETIRADA, amount, reason, userId);
    }

    @Override
    public void registerCashReturn(double amount, String reason, int userId) throws SQLException {
        registerMovement(MovementType.DEVOLUCION, amount, reason, userId);
    }

    @Override
    public void registerCashReturn(double amount, String reason, int userId, Connection conn) throws SQLException {
        registerMovement(MovementType.DEVOLUCION, amount, reason, userId, conn);
    }

    @Override
    public void registerCashEntry(double amount, String reason, int userId) throws SQLException {
        registerMovement(MovementType.INGRESO, amount, reason, userId);
    }

    @Override
    public double getCurrentCashInDrawer() throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            double initialFund = 0;
            String sqlFund = "SELECT COALESCE(SUM(initial_amount), 0) FROM cash_fund_sessions WHERE (session_date = CURDATE() OR is_closed = FALSE) AND closure_id IS NULL";
            try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sqlFund)) {
                if (rs.next())
                    initialFund = rs.getDouble(1);
            }

            double cashSales = 0;
            String sqlSales = "SELECT COALESCE(SUM(total), 0) FROM sales WHERE LOWER(payment_method) = 'efectivo' AND closure_id IS NULL AND is_return = FALSE";
            try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sqlSales)) {
                if (rs.next())
                    cashSales = rs.getDouble(1);
            }

            double cashReturns = 0;
            String sqlReturns = "SELECT COALESCE(SUM(r.total_refunded), 0) FROM returns r JOIN sales s ON r.sale_id = s.sale_id WHERE LOWER(s.payment_method) = 'efectivo' AND r.closure_id IS NULL";
            try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sqlReturns)) {
                if (rs.next())
                    cashReturns = rs.getDouble(1);
            }

            double manualBalance = 0;
            String sqlManual = "SELECT SUM(CASE WHEN type = 'INGRESO' THEN amount ELSE -amount END) FROM cash_movements WHERE closure_id IS NULL AND type IN ('INGRESO', 'RETIRADA')";
            try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sqlManual)) {
                if (rs.next())
                    manualBalance = rs.getDouble(1);
            }

            return initialFund + cashSales - cashReturns + manualBalance;
        }
    }

    @Override
    public Map<String, Double> getPendingTotals() throws SQLException {
        Map<String, Double> totals = new HashMap<>();
        String sqlSales = "SELECT COALESCE(payment_method, 'Efectivo') as method, SUM(total) as amount FROM sales WHERE closure_id IS NULL AND is_return = FALSE GROUP BY method";
        double cash = 0, card = 0, others = 0;
        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sqlSales)) {
            while (rs.next()) {
                String method = rs.getString("method");
                double amount = rs.getDouble("amount");
                if ("Efectivo".equalsIgnoreCase(method))
                    cash += amount;
                else if ("Tarjeta".equalsIgnoreCase(method))
                    card += amount;
                else
                    others += amount;
            }
        }
        String sqlReturns = "SELECT COALESCE(s.payment_method, 'Efectivo') as method, SUM(r.total_refunded) as amount FROM returns r JOIN sales s ON r.sale_id = s.sale_id WHERE r.closure_id IS NULL GROUP BY method";
        double returnsCash = 0, returnsCard = 0;
        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sqlReturns)) {
            while (rs.next()) {
                String method = rs.getString("method");
                double amount = rs.getDouble("amount");
                if ("Efectivo".equalsIgnoreCase(method)) {
                    returnsCash += amount;
                    cash -= amount;
                } else if ("Tarjeta".equalsIgnoreCase(method)) {
                    returnsCard += amount;
                    card -= amount;
                } else {
                    others -= amount;
                }
            }
        }
        totals.put("cash", cash);
        totals.put("card", card);
        totals.put("total", cash + card + others);
        // Devoluciones como valores positivos para mostrar desglosado en UI
        totals.put("returns_cash", returnsCash);
        totals.put("returns_card", returnsCard);
        totals.put("returns_total", returnsCash + returnsCard);
        return totals;
    }

    @Override
    public int getPendingTransactionCount() throws SQLException {
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM sales WHERE closure_id IS NULL) + " +
                "(SELECT COUNT(*) FROM returns WHERE closure_id IS NULL) + " +
                "(SELECT COUNT(*) FROM cash_fund_sessions WHERE closure_id IS NULL) + " +
                "(SELECT COUNT(*) FROM cash_movements WHERE closure_id IS NULL) as total_pending";
        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return rs.getInt(1);
        }
        return 0;
    }

    @Override
    public void openCashFund(double initialAmount, int userId) throws SQLException {
        String sql = "INSERT INTO cash_fund_sessions (session_date, user_id, initial_amount, is_closed) VALUES (CURDATE(), ?, ?, FALSE)";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setDouble(2, initialAmount);
            pstmt.executeUpdate();
            registerMovement(MovementType.APERTURA, initialAmount, "Apertura de caja", userId);
        }
    }

    @Override
    public boolean hasActiveFund() throws SQLException {
        String sql = "SELECT COUNT(*) FROM cash_fund_sessions WHERE session_date = CURDATE() AND is_closed = FALSE";
        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return rs.getInt(1) > 0;
        }
        return false;
    }

    @Override
    public double getActiveFundAmount() throws SQLException {
        String sql = "SELECT COALESCE(SUM(initial_amount), 0) FROM cash_fund_sessions WHERE session_date = CURDATE() AND is_closed = FALSE";
        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return rs.getDouble(1);
        }
        return 0;
    }

    @Override
    public double getLastClosureAmount() throws SQLException {
        String sql = "SELECT actual_cash FROM cash_closures ORDER BY created_at DESC LIMIT 1";
        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return rs.getDouble("actual_cash");
        }
        return 0;
    }

    @Override
    public boolean hasUnclosedPreviousSession() throws SQLException {
        String sql = "SELECT COUNT(*) FROM cash_fund_sessions WHERE session_date < CURDATE() AND is_closed = FALSE";
        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return rs.getInt(1) > 0;
        }
        return false;
    }
}
