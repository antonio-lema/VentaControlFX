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
    public boolean isClosureDone(LocalDate date) throws SQLException {
        String sql = "SELECT COUNT(*) FROM cash_closures WHERE closure_date = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return rs.getInt(1);
        }
        return 0;
    }

    @Override
    public Map<String, Double> getPendingTotals() throws SQLException {
        Map<String, Double> totals = new HashMap<>();
        String sqlSales = "SELECT SUM(COALESCE(cash_amount, IF(payment_method = 'Efectivo', total, 0))) as cash_total, "
                +
                "SUM(COALESCE(card_amount, IF(payment_method = 'Tarjeta', total, 0))) as card_total, " +
                "SUM(IF(payment_method NOT IN ('Efectivo', 'Tarjeta', 'Mixed'), total, 0)) as others_total " +
                "FROM sales WHERE closure_id IS NULL";
        double cash = 0, card = 0, others = 0;
        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sqlSales)) {
            if (rs.next()) {
                cash = rs.getDouble("cash_total");
                card = rs.getDouble("card_total");
                others = rs.getDouble("others_total");
            }
        }
        String sqlReturns = "SELECT SUM(cash_amount) as returns_cash, SUM(card_amount) as returns_card FROM returns WHERE closure_id IS NULL";
        double returnsCash = 0, returnsCard = 0;
        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sqlReturns)) {
            if (rs.next()) {
                returnsCash = rs.getDouble("returns_cash");
                returnsCard = rs.getDouble("returns_card");
                cash -= returnsCash;
                card -= returnsCard;
            }
        }

        // Manual Movements
        double manualIn = 0, manualOut = 0;
        String sqlManual = "SELECT type, SUM(amount) as total FROM cash_movements WHERE closure_id IS NULL AND type IN ('INGRESO', 'RETIRADA') GROUP BY type";
        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sqlManual)) {
            while (rs.next()) {
                String type = rs.getString("type");
                double amount = rs.getDouble("total");
                if ("INGRESO".equalsIgnoreCase(type))
                    manualIn = amount;
                else if ("RETIRADA".equalsIgnoreCase(type))
                    manualOut = amount;
            }
        }

        double netCash = cash;
        double netCard = card;
        double netOthers = others;

        totals.put("cash", netCash);
        totals.put("card", netCard);
        totals.put("others", netOthers);
        totals.put("total", netCash + netCard + netOthers);
        totals.put("returns_cash", returnsCash);
        totals.put("returns_card", returnsCard);
        totals.put("returns_total", returnsCash + returnsCard);
        totals.put("manual_in", manualIn);
        totals.put("manual_out", manualOut);

        // Discounts tracking
        String sqlDiscounts = "SELECT SUM(discount_amount * GREATEST(0, 1 - COALESCE(returned_amount, 0) / IF(total <= 0, 1, total))) FROM sales WHERE closure_id IS NULL";
        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sqlDiscounts)) {
            if (rs.next()) {
                totals.put("total_discounts", rs.getDouble(1));
            }
        }

        return totals;
    }

    @Override
    public double getCurrentCashInDrawer() throws SQLException {
        double initialFund = getActiveFundAmount();
        Map<String, Double> totals = getPendingTotals();
        return initialFund + totals.getOrDefault("cash", 0.0) + totals.getOrDefault("manual_in", 0.0)
                - totals.getOrDefault("manual_out", 0.0);
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
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
    public int getPendingTransactionCount() throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM sales WHERE closure_id IS NULL) + " +
                "(SELECT COUNT(*) FROM returns WHERE closure_id IS NULL) + " +
                "(SELECT COUNT(*) FROM cash_fund_sessions WHERE is_closed = FALSE) + " +
                "(SELECT COUNT(*) FROM cash_movements WHERE closure_id IS NULL) as total_pending";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return rs.getInt(1);
        }
        return 0;
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
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
    public void save(CashClosure closure) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false); // Enable transaction
            String sql = "INSERT INTO cash_closures (closure_date, user_id, total_cash, total_card, total_all, actual_cash, difference, notes, created_at, opening_time, initial_fund, cash_in, cash_out, expected_cash, counted_cash, status, reviewed_by, reviewed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
                pstmt.setTimestamp(10, closure.getOpeningTime() != null ? Timestamp.valueOf(closure.getOpeningTime())
                        : Timestamp.valueOf(LocalDateTime.now()));
                pstmt.setDouble(11, closure.getInitialFund());
                pstmt.setDouble(12, closure.getCashIn());
                pstmt.setDouble(13, closure.getCashOut());
                pstmt.setDouble(14, closure.getExpectedCash());
                pstmt.setDouble(15, closure.getActualCash());
                pstmt.setString(16, closure.getStatus() != null ? closure.getStatus() : "CUADRADO");
                if (closure.getReviewedBy() != null)
                    pstmt.setInt(17, closure.getReviewedBy());
                else
                    pstmt.setNull(17, Types.INTEGER);
                pstmt.setTimestamp(18,
                        closure.getReviewedAt() != null ? Timestamp.valueOf(closure.getReviewedAt()) : null);
                pstmt.executeUpdate();
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int closureId = generatedKeys.getInt(1);
                        closure.setClosureId(closureId);
                        // Register movement BEFORE marking session as closed to ensure traceability
                        registerMovement(MovementType.CIERRE, closure.getTotalAll(), "Cierre de caja manual",
                                closure.getUserId(), connection);
                        linkTransactionsToClosure(connection, closureId);
                        connection.commit(); // Commit transaction
                    }
                }
            } catch (SQLException e) {
                if (connection != null && !connection.getAutoCommit()) {
                    connection.rollback();
                }
                throw e;
            } finally {
                if (connection != null) {
                    connection.setAutoCommit(true);
                }
            }
        }
    }

    private void linkTransactionsToClosure(Connection connection, int closureId) throws SQLException {
        String[] tables = { "sales", "returns", "cash_fund_sessions", "cash_movements" };
        for (String table : tables) {
            String sql = "UPDATE " + table + " SET closure_id = ? WHERE closure_id IS NULL";
            if ("cash_fund_sessions".equals(table)) {
                sql = "UPDATE cash_fund_sessions SET closure_id = ?, is_closed = TRUE WHERE is_closed = FALSE";
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
                    Timestamp ot = rs.getTimestamp("opening_time");
                    if (ot != null)
                        c.setOpeningTime(ot.toLocalDateTime());
                    c.setInitialFund(rs.getDouble("initial_fund"));
                    c.setCashIn(rs.getDouble("cash_in"));
                    c.setCashOut(rs.getDouble("cash_out"));
                    c.setExpectedCash(rs.getDouble("expected_cash"));
                    c.setStatus(rs.getString("status"));
                    int revBy = rs.getInt("reviewed_by");
                    if (!rs.wasNull())
                        c.setReviewedBy(revBy);
                    Timestamp ra = rs.getTimestamp("reviewed_at");
                    if (ra != null)
                        c.setReviewedAt(ra.toLocalDateTime());
                    closures.add(c);
                }
            }
        }
        return closures;
    }

    @Override
    public void registerMovement(MovementType type, double amount, String reason, int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            registerMovement(type, amount, reason, userId, conn);
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
    public void registerCashEntry(double amount, String reason, int userId) throws SQLException {
        registerMovement(MovementType.INGRESO, amount, reason, userId);
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
    public void openCashFund(double initialAmount, String notes, int userId) throws SQLException {
        String sql = "INSERT INTO cash_fund_sessions (session_date, user_id, initial_amount, notes, is_closed) VALUES (CURDATE(), ?, ?, ?, FALSE)";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setDouble(2, initialAmount);
            pstmt.setString(3, notes != null ? notes : "");
            pstmt.executeUpdate();

            String movementReason = (notes != null && !notes.trim().isEmpty())
                    ? "Apertura: " + notes
                    : "Apertura de caja";
            registerMovement(MovementType.APERTURA, initialAmount, movementReason, userId);
        }
    }

    @Override
    public boolean hasActiveFund() throws SQLException {
        // Unclosed session can be from any day (carrying forward)
        String sql = "SELECT COUNT(*) FROM cash_fund_sessions WHERE is_closed = FALSE";
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
        // Obtenemos solo el fondo de la sesión activa más reciente
        // Si hay varias abiertas por error, no las sumamos para no inflar el esperado
        String sql = "SELECT initial_amount FROM cash_fund_sessions WHERE is_closed = FALSE ORDER BY created_at DESC LIMIT 1";
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

    @Override
    public List<CashMovement> getMovementsByClosure(int closureId) throws SQLException {
        List<CashMovement> movements = new ArrayList<>();
        String sql = "SELECT m.*, u.username FROM cash_movements m LEFT JOIN users u ON m.user_id = u.user_id WHERE m.closure_id = ? ORDER BY m.created_at ASC";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, closureId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    movements.add(new CashMovement(rs.getInt("movement_id"), rs.getString("type"),
                            rs.getDouble("amount"), rs.getString("reason"),
                            rs.getTimestamp("created_at").toLocalDateTime(), rs.getString("username")));
                }
            }
        }
        return movements;
    }

    @Override
    public void markAsReviewed(int closureId, int reviewerId) throws SQLException {
        String sql = "UPDATE cash_closures SET status = 'REVISADO', reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP WHERE closure_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, reviewerId);
            pstmt.setInt(2, closureId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void markAsExcluded(int closureId, int reviewerId) throws SQLException {
        String sql = "UPDATE cash_closures SET status = 'EXCLUIDO', reviewed_by = ?, reviewed_at = CURRENT_TIMESTAMP WHERE closure_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, reviewerId);
            pstmt.setInt(2, closureId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void updateClosure(int closureId, double actualCash, String reason, int reviewerId, double previousCash)
            throws SQLException {
        String sql = "UPDATE cash_closures SET " +
                "actual_cash = ?, " +
                "difference = ? - expected_cash, " +
                "notes = CONCAT(IFNULL(notes, ''), '\n[Editado: ', ?, ' | Antiguo: ', ?, ' €]'), " +
                "reviewed_by = ?, " +
                "reviewed_at = CURRENT_TIMESTAMP, " +
                "status = CASE WHEN ABS(? - expected_cash) < 0.01 THEN 'CUADRADO' ELSE 'DESCUADRE' END " +
                "WHERE closure_id = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, actualCash);
            pstmt.setDouble(2, actualCash);
            pstmt.setString(3, reason);
            pstmt.setDouble(4, previousCash);
            pstmt.setInt(5, reviewerId);
            pstmt.setDouble(6, actualCash);
            pstmt.setInt(7, closureId);
            pstmt.executeUpdate();
        }
    }
}
