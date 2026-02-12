package com.mycompany.ventacontrolfx.config;

public class DBConfig {

    // URL de conexión JDBC (ajusta "tpv_bazar" por el nombre de tu base de datos)
    public static final String URL = "jdbc:mysql://localhost:3306/tpv_bazar?useSSL=false&serverTimezone=UTC";
    
    // Usuario y contraseña de MySQL (ajusta aquí si cambias de usuario/clave)
    public static final String USER = "root";
    public static final String PASS = "";

    private DBConfig() {
        // Constructor privado para evitar instanciar esta clase
        // (porque solo contiene constantes de configuración)
    }
}
