package com.mycompany.ventacontrolfx;

import org.junit.jupiter.api.Test;
import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class FixEncodingTest {

    @Test
    public void listCorruptStrings() throws Exception {
        System.out.println("\n\n=== BUSCANDO CADENAS CORRUPTAS CON '??' ===");
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement()) {

            System.out.println("\nCategorías con '??' (tabla categories):");
            ResultSet rs1 = stmt.executeQuery("SELECT DISTINCT name FROM categories WHERE name LIKE '%??%'");
            boolean found1 = false;
            while (rs1.next()) {
                System.out.println(" - " + rs1.getString(1));
                found1 = true;
            }
            if (!found1)
                System.out.println(" (Ninguna)");

            System.out.println("\nProductos con '??' en Nombre (tabla products):");
            ResultSet rs2 = stmt.executeQuery("SELECT DISTINCT name FROM products WHERE name LIKE '%??%'");
            boolean found2 = false;
            while (rs2.next()) {
                System.out.println(" - " + rs2.getString(1));
                found2 = true;
            }
            if (!found2)
                System.out.println(" (Ninguno)");
        }
        System.out.println("\n=== FIN DE LA BUSQUEDA ===\n\n");
    }
}
