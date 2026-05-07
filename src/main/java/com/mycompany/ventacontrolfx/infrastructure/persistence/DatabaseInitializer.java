package com.mycompany.ventacontrolfx.infrastructure.persistence;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Motor de inicialización de Base de Datos.
 * Carga el esquema y datos iniciales desde recursos SQL y gestiona migraciones aditivas.
 */
public class DatabaseInitializer {

    private static final String SCHEMA_PATH = "/sql/schema.sql";
    private static final String DATA_PATH = "/sql/initial_data.sql";
    private static final Map<String, Set<String>> columnCache = new HashMap<>();

    public static void initialize(Connection conn) throws SQLException {
        if (isAlreadyInitialized(conn)) return;

        loadMetadata(conn);
        
        // 1. Ejecutar Esquema Base
        executeSqlResource(conn, SCHEMA_PATH);

        // 2. Ejecutar Parches Manuales (Migraciones complejas que requieren Java)
        applyManualMigrations(conn);

        // 3. Cargar Datos Iniciales (Seeds)
        executeSqlResource(conn, DATA_PATH);

        setSchemaVersion(conn, "1.16");
    }

    private static void executeSqlResource(Connection conn, String path) throws SQLException {
        InputStream is = DatabaseInitializer.class.getResourceAsStream(path);
        if (is == null) return;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
             Statement stmt = conn.createStatement()) {
            
            String content = reader.lines().collect(Collectors.joining("\n"));
            String[] queries = content.split(";");
            for (String query : queries) {
                if (!query.trim().isEmpty()) {
                    stmt.execute(query.trim());
                }
            }
        } catch (Exception e) {
            System.err.println("[DB-INIT] Error ejecutando recurso SQL " + path + ": " + e.getMessage());
        }
    }

    private static void applyManualMigrations(Connection conn) throws SQLException {
        // 1. Parches de compatibilidad histórica (Restaurados todos)
        DatabaseMigrator.applyLegacyPatches(conn, columnCache);
        
        // 2. Migraciones complejas de datos
        DatabaseMigrator.applyComplexMigrations(conn);

        try (Statement stmt = conn.createStatement()) {
            try { new JdbcTaxRepository().syncMirroredValues(); } 
            catch (Exception e) { }

            // 3. Casos especiales de inicialización
            seedGenericProduct(stmt);
            seedVerifactuConfig(conn);
        }
    }

    private static void seedVerifactuConfig(Connection conn) throws SQLException {
        String sql = "INSERT INTO system_config (config_key, config_value) VALUES (?, ?) ON DUPLICATE KEY UPDATE config_value=config_value";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String[][] defaults = {
                {"verifactu.nif", "99999910G"},
                {"verifactu.cert_name", "(VERI*FACTU) CERTIFICADO FISICA PRUEBAS"},
                {"verifactu.cert_path", "/certs/99999910G_prueba.pfx"},
                {"verifactu.cert_pass", "1234"},
                {"verifactu.url", "https://prewww1.aeat.es/wlpl/TIKE-CONT/ws/SistemaFacturacion/VerifactuSOAP"}
            };
            for (String[] def : defaults) {
                pstmt.setString(1, def[0]);
                pstmt.setString(2, def[1]);
                pstmt.executeUpdate();
            }
        }
    }

    private static void seedGenericProduct(Statement stmt) throws SQLException {
        try {
            stmt.execute("INSERT INTO categories (name, visible) SELECT 'SISTEMA', 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'SISTEMA')");
        } catch (Exception e) {}
    }

    private static void loadMetadata(Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), conn.getSchema(), null, null)) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME").toLowerCase();
                String columnName = rs.getString("COLUMN_NAME").toLowerCase();
                columnCache.computeIfAbsent(tableName, k -> new HashSet<>()).add(columnName);
            }
        }
    }

    private static boolean isAlreadyInitialized(Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT config_value FROM system_config WHERE config_key = 'db.schema_version'")) {
            return rs.next() && "1.16".equals(rs.getString(1));
        } catch (Exception e) { return false; }
    }

    private static void setSchemaVersion(Connection conn, String version) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO system_config (config_key, config_value) VALUES ('db.schema_version', ?) ON DUPLICATE KEY UPDATE config_value=?")) {
            pstmt.setString(1, version);
            pstmt.setString(2, version);
            pstmt.executeUpdate();
        }
    }
}

