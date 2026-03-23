package com.mycompany.ventacontrolfx;

import org.junit.jupiter.api.Test;
import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class ApplyFixEncodingTest {

    @Test
    public void applyFixes() throws Exception {
        System.out.println("\n\n=== APLICANDO CORRECCIONES DE CODIFICACIÓN ===");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                String sqlCat = "UPDATE categories SET name = ? WHERE name = ?";
                try (PreparedStatement PS = conn.prepareStatement(sqlCat)) {
                    // List of fixes for categories
                    String[][] fixes = {
                            { "Accesorios Móviles", "Accesorios M??viles" },
                            { "Alimentación y Bebidas", "Alimentaci??n y Bebidas" },
                            { "Baterías y Cargadores", "Bater??as y Cargadores" },
                            { "Cámaras y Fotografía", "C??maras y Fotograf??a" },
                            { "Fuentes de alimentación", "Fuentes de alimentaci??n" },
                            { "Móviles", "M??viles" },
                            { "Micrófonos", "Micr??fonos" },
                            { "Periféricos", "Perif??ricos" },
                            { "Portátiles", "Port??tiles" },
                            { "Refrigeración", "Refrigeraci??n" },
                            { "Tarjetas gráficas", "Tarjetas gr??ficas" },
                            { "afsd", "afsd??" }
                    };

                    int countCat = 0;
                    for (String[] fix : fixes) {
                        PS.setString(1, fix[0]);
                        PS.setString(2, fix[1]);
                        countCat += PS.executeUpdate();
                    }
                    System.out.println("Categorías actualizadas: " + countCat);
                }

                String sqlProd = "UPDATE products SET name = ? WHERE name = ?";
                try (PreparedStatement PS = conn.prepareStatement(sqlProd)) {
                    PS.setString(1, "toñín");
                    PS.setString(2, "to??in");
                    int countProd = PS.executeUpdate();
                    System.out.println("Productos actualizados: " + countProd);
                }

                conn.commit();
                System.out.println("\n=== CORRECCIONES APLICADAS CON ÉXITO ===");

            } catch (Exception e) {
                conn.rollback();
                System.err.println("\n=== ERROR APLICANDO CORRECCIONES: " + e.getMessage());
                throw e;
            }
        }
    }
}
