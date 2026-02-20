package com.mycompany.ventacontrolfx.dao;

import com.mycompany.ventacontrolfx.model.CashClosure;
import com.mycompany.ventacontrolfx.dao.DatabaseInitializer;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
                    closure.setClosureId(generatedKeys.getInt(1));
                }
            }
        }
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
}
