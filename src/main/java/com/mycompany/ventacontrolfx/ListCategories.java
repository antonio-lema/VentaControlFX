package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ListCategories {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("=== LISTADO DE CATEGORIAS ===");
            try (ResultSet rs = stmt.executeQuery("SELECT category_id, name, tax_group_id, default_iva FROM categories")) {
                while (rs.next()) {
                    System.out.printf("ID: %d | Name: %s | GroupID: %s | IVA: %.2f\n",
                        rs.getInt("category_id"), rs.getString("name"), 
                        rs.getString("tax_group_id"), rs.getDouble("default_iva"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
