package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.*;
import java.time.LocalDate;

public class TestSessions {
    public static void main(String[] args) {
        String sql = "SELECT * FROM work_sessions WHERE DATE(start_time) = CURRENT_DATE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            System.out.println("--- SESSIONS TODAY ---");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("session_id") + 
                                   ", UserID: " + rs.getInt("user_id") + 
                                   ", Type: " + rs.getString("type") + 
                                   ", Start: " + rs.getTimestamp("start_time") + 
                                   ", Status: " + rs.getString("status"));
            }
            
            System.out.println("--- USERS ---");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs2 = stmt.executeQuery("SELECT user_id, username FROM users")) {
                while (rs2.next()) {
                    System.out.println("ID: " + rs2.getInt("user_id") + ", Name: " + rs2.getString("username"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
