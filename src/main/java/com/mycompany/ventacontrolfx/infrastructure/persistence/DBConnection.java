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
                System.err.println("CRITICAL: unable to find config/db.properties");
                // No fallback for security reasons
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
            System.err.println("Error: no se encontr\u00f3 el driver de MySQL");
        }
        // Devolver la conexi\u00f3n
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
