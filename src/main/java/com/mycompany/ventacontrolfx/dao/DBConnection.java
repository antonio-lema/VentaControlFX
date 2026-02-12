/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.ventacontrolfx.dao;

/**
 *
 * @author PracticasSoftware1
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    // Cambia estos valores según tu configuración de XAMPP/MySQL
    private static final String URL = "jdbc:mysql://localhost:3306/tpv_bazar?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "";

    public static Connection getConnection() throws SQLException {
        try {
            // Cargar el driver de MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: no se encontró el driver de MySQL");
            e.printStackTrace();
        }
        // Devolver la conexión
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
