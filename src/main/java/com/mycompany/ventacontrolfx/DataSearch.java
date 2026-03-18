package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DataSearch {
    public static void main(String[] args) {
        String query = args.length > 0 ? args[0] : "descuento";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement()) {

            System.out.println("=== BUSCANDO '" + query + "' EN LA DB ===");

            String[] tables = { "categories", "products", "tax_groups", "tax_rates" };
            for (String table : tables) {
                System.out.println("\nTabla: " + table);
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)) {
                    int colCount = rs.getMetaData().getColumnCount();
                    while (rs.next()) {
                        StringBuilder row = new StringBuilder();
                        boolean found = false;
                        for (int i = 1; i <= colCount; i++) {
                            String val = String.valueOf(rs.getObject(i));
                            if (val.toLowerCase().contains(query.toLowerCase())) {
                                found = true;
                            }
                            row.append(rs.getMetaData().getColumnName(i)).append(": ").append(val).append(" | ");
                        }
                        if (found) {
                            System.out.println("MATCH FOUND: " + row);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
