
package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.*;

public class CheckCashSessions {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement()) {

            System.out.println("--- UNCLOSED SESSIONS ---");
            ResultSet rs = stmt.executeQuery("SELECT * FROM cash_fund_sessions WHERE is_closed = FALSE");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("session_id") +
                        ", Date: " + rs.getDate("session_date") +
                        ", Amount: " + rs.getDouble("initial_amount") +
                        ", Closed: " + rs.getBoolean("is_closed"));
            }

            System.out.println("\n--- LAST CLOSURES ---");
            rs = stmt.executeQuery("SELECT * FROM cash_closures ORDER BY created_at DESC LIMIT 3");
            while (rs.next()) {
                System.out.println("Date: " + rs.getDate("closure_date") +
                        ", Actual: " + rs.getDouble("actual_cash") +
                        ", Expected: " + rs.getDouble("expected_cash") +
                        ", Diff: " + rs.getDouble("difference"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
