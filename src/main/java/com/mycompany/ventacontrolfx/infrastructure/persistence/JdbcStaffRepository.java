package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.StaffVacation;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcStaffRepository {

    public JdbcStaffRepository() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS staff_vacations (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "start_date DATE NOT NULL, " +
                "end_date DATE NOT NULL, " +
                "type VARCHAR(50), " +
                "notes TEXT)";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<StaffVacation> getAllVacations() {
        List<StaffVacation> list = new ArrayList<>();
        String sql = "SELECT * FROM staff_vacations";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                StaffVacation sv = new StaffVacation(
                        rs.getInt("user_id"),
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate(),
                        rs.getString("type"),
                        rs.getString("notes"));
                sv.setId(rs.getInt("id"));
                list.add(sv);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void addVacation(StaffVacation sv) {
        String sql = "INSERT INTO staff_vacations (user_id, start_date, end_date, type, notes) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sv.getUserId());
            pstmt.setDate(2, Date.valueOf(sv.getStartDate()));
            pstmt.setDate(3, Date.valueOf(sv.getEndDate()));
            pstmt.setString(4, sv.getType());
            pstmt.setString(5, sv.getNotes());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
