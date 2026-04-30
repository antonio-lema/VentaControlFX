package com.mycompany.ventacontrolfx.util;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.*;

public class DatabaseMigrator {
    public static void main(String[] args) {
        System.out.println("Starting VeriFactu Migrations...");
        try (Connection conn = DBConnection.getConnection()) {
            Statement stmt = conn.createStatement();
            
            addColumn(stmt, "sales", "incident_reason");
            addColumn(stmt, "returns", "incident_reason");
            
            System.out.println("Migrations completed successfully.");
        } catch (Exception e) {
            System.err.println("Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addColumn(Statement stmt, String table, String column) {
        try {
            System.out.println("Adding " + column + " to " + table + "...");
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " VARCHAR(255) DEFAULT NULL");
        } catch (SQLException e) {
            if (e.getErrorCode() == 1060) { // Duplicate column name
                System.out.println("Column " + column + " already exists in " + table + ".");
            } else {
                System.err.println("Error adding column to " + table + ": " + e.getMessage());
            }
        }
    }
}
