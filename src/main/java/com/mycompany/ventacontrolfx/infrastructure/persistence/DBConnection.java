package com.mycompany.ventacontrolfx.infrastructure.persistence;

/**
 *
 * @author PracticasSoftware1
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // Variables loaded from properties file
    private static String URL;
    private static String USER;
    private static String PASS;

    static {
        try (java.io.InputStream input = DBConnection.class.getClassLoader()
                .getResourceAsStream("config/db.properties")) {
            java.util.Properties prop = new java.util.Properties();
            if (input == null) {
                System.err.println("Sorry, unable to find config/db.properties");
                // Default fallback values just in case
                URL = "jdbc:mysql://localhost:3306/tpv_bazar?useSSL=false&serverTimezone=UTC";
                USER = "root";
                PASS = "";
            } else {
                prop.load(input);
                URL = prop.getProperty("db.url");
                USER = prop.getProperty("db.user");
                PASS = prop.getProperty("db.password");
            }
        } catch (java.io.IOException ex) {
            System.err.println("Error reading db.properties: " + ex.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            // Cargar el driver de MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: no se encontró el driver de MySQL");
        }
        // Devolver la conexión
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
