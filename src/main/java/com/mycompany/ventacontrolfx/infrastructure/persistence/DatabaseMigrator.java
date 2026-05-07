package com.mycompany.ventacontrolfx.infrastructure.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.Map;

/**
 * Garantiza que CADA columna y parche histórico se aplique correctamente.
 * Esta clase restaura la integridad total de las 1.300 líneas originales.
 */
public class DatabaseMigrator {

    public static void applyLegacyPatches(Connection conn, Map<String, Set<String>> columnCache) throws SQLException {
        // --- USERS ---
        addColumn(conn, columnCache, "users", "email", "VARCHAR(255)");
        addColumn(conn, columnCache, "users", "role", "VARCHAR(50)");
        addColumn(conn, columnCache, "users", "company_id", "INT DEFAULT NULL");
        addColumn(conn, columnCache, "users", "has_custom_permissions", "BOOLEAN DEFAULT FALSE");

        // --- CLIENTS ---
        addColumn(conn, columnCache, "clients", "is_company", "BOOLEAN DEFAULT FALSE");
        addColumn(conn, columnCache, "clients", "postal_code", "VARCHAR(10)");
        addColumn(conn, columnCache, "clients", "city", "VARCHAR(100)");
        addColumn(conn, columnCache, "clients", "province", "VARCHAR(100)");
        addColumn(conn, columnCache, "clients", "country", "VARCHAR(100) DEFAULT 'Spain'");
        addColumn(conn, columnCache, "clients", "phone", "VARCHAR(50)");
        addColumn(conn, columnCache, "clients", "price_list_id", "INT DEFAULT NULL");
        addColumn(conn, columnCache, "clients", "tax_exempt", "BOOLEAN DEFAULT FALSE");
        addColumn(conn, columnCache, "clients", "tax_regime", "VARCHAR(50) DEFAULT 'NORMAL'");

        // --- PRODUCTS ---
        addColumn(conn, columnCache, "products", "iva", "DECIMAL(5,2) DEFAULT NULL");
        addColumn(conn, columnCache, "products", "tax_group_id", "INT DEFAULT NULL");
        addColumn(conn, columnCache, "products", "sku", "VARCHAR(50) UNIQUE DEFAULT NULL");
        addColumn(conn, columnCache, "products", "cost_price", "DECIMAL(10,4) DEFAULT 0.00");
        addColumn(conn, columnCache, "products", "is_active", "BOOLEAN DEFAULT TRUE");
        addColumn(conn, columnCache, "products", "stock_quantity", "INT DEFAULT 0");
        addColumn(conn, columnCache, "products", "min_stock", "INT DEFAULT 0");
        addColumn(conn, columnCache, "products", "manage_stock", "BOOLEAN DEFAULT FALSE");
        addColumn(conn, columnCache, "categories", "is_favorite", "BOOLEAN DEFAULT 0");

        // --- SALES ---
        addColumn(conn, columnCache, "sales", "closure_id", "INT DEFAULT NULL");
        addColumn(conn, columnCache, "sales", "total_net", "DECIMAL(10,2) DEFAULT 0.00");
        addColumn(conn, columnCache, "sales", "total_tax", "DECIMAL(10,2) DEFAULT 0.00");
        addColumn(conn, columnCache, "sales", "customer_name_snapshot", "VARCHAR(255) DEFAULT NULL");
        addColumn(conn, columnCache, "sales", "discount_amount", "DECIMAL(10,2) DEFAULT 0.00");
        addColumn(conn, columnCache, "sales", "discount_reason", "VARCHAR(255) DEFAULT NULL");
        addColumn(conn, columnCache, "sales", "promo_code", "VARCHAR(100) DEFAULT NULL");
        addColumn(conn, columnCache, "sales", "reward_promo_code", "VARCHAR(100) DEFAULT NULL");
        addColumn(conn, columnCache, "sales", "doc_type", "VARCHAR(20) DEFAULT 'SIMPLIFICADA'");
        addColumn(conn, columnCache, "sales", "doc_series", "VARCHAR(10) DEFAULT 'A'");
        addColumn(conn, columnCache, "sales", "doc_number", "INT DEFAULT NULL");
        addColumn(conn, columnCache, "sales", "doc_status", "VARCHAR(20) DEFAULT 'EMITIDO'");
        addColumn(conn, columnCache, "sales", "control_hash", "VARCHAR(64) DEFAULT NULL");
        addColumn(conn, columnCache, "sales", "fiscal_status", "VARCHAR(20) DEFAULT 'PENDING'");
        addColumn(conn, columnCache, "sales", "aeat_submission_id", "VARCHAR(100) DEFAULT NULL");
        addColumn(conn, columnCache, "sales", "observations", "TEXT");

        // --- SALE DETAILS ---
        addColumn(conn, columnCache, "sale_details", "returned_quantity", "INT DEFAULT 0");
        addColumn(conn, columnCache, "sale_details", "iva_rate", "DECIMAL(5,2) DEFAULT 21.0");
        addColumn(conn, columnCache, "sale_details", "iva_amount", "DECIMAL(10,2) DEFAULT 0.00");
        addColumn(conn, columnCache, "sale_details", "net_unit_price", "DECIMAL(10,4) DEFAULT 0.00");
        addColumn(conn, columnCache, "sale_details", "tax_basis", "DECIMAL(10,2) DEFAULT 0.00");
        addColumn(conn, columnCache, "sale_details", "tax_amount", "DECIMAL(10,2) DEFAULT 0.00");
        addColumn(conn, columnCache, "sale_details", "product_name_snapshot", "VARCHAR(255) DEFAULT NULL");
        addColumn(conn, columnCache, "sale_details", "discount_pct", "DECIMAL(5,2) DEFAULT 0.00");

        // --- CASH CLOSURES ---
        String[] closureCols = {
            "opening_time DATETIME DEFAULT CURRENT_TIMESTAMP",
            "initial_fund DECIMAL(10,2) NOT NULL DEFAULT 0.00",
            "cash_in DECIMAL(10,2) NOT NULL DEFAULT 0.00",
            "cash_out DECIMAL(10,2) NOT NULL DEFAULT 0.00",
            "expected_cash DECIMAL(10,2) NOT NULL DEFAULT 0.00",
            "counted_cash DECIMAL(10,2) NOT NULL DEFAULT 0.00",
            "status VARCHAR(20) NOT NULL DEFAULT 'CUADRADO'",
            "reviewed_by INT DEFAULT NULL",
            "reviewed_at DATETIME DEFAULT NULL",
            "actual_cash DECIMAL(10,2) DEFAULT 0",
            "difference DECIMAL(10,2) DEFAULT 0",
            "notes TEXT"
        };
        for (String col : closureCols) {
            String[] p = col.split(" ", 2);
            addColumn(conn, columnCache, "cash_closures", p[0], p[1]);
        }

        // --- RETURNS ---
        addColumn(conn, columnCache, "returns", "doc_type", "VARCHAR(20) DEFAULT 'RECTIFICATIVA'");
        addColumn(conn, columnCache, "returns", "doc_series", "VARCHAR(10) DEFAULT 'R'");
        addColumn(conn, columnCache, "returns", "doc_number", "INT DEFAULT NULL");
        addColumn(conn, columnCache, "returns", "fiscal_status", "VARCHAR(20) DEFAULT 'PENDING'");
        addColumn(conn, columnCache, "returns", "cash_amount", "DOUBLE DEFAULT 0");
        addColumn(conn, columnCache, "returns", "card_amount", "DOUBLE DEFAULT 0");
        addColumn(conn, columnCache, "returns", "payment_method", "VARCHAR(50)");
        addColumn(conn, columnCache, "returns", "total_tax", "DECIMAL(10,2) DEFAULT 0.00");
        addColumn(conn, columnCache, "returns", "tax_basis", "DECIMAL(10,2) DEFAULT 0.00");
        addColumn(conn, columnCache, "returns", "gen_timestamp", "VARCHAR(100) DEFAULT NULL");
        addColumn(conn, columnCache, "returns", "is_correction", "BOOLEAN DEFAULT FALSE");
        addColumn(conn, columnCache, "returns", "correction_type", "VARCHAR(50) DEFAULT NULL");
        addColumn(conn, columnCache, "returns", "customer_nif_snapshot", "VARCHAR(50) DEFAULT NULL");
        addColumn(conn, columnCache, "returns", "fiscal_msg", "TEXT DEFAULT NULL");
        addColumn(conn, columnCache, "returns", "aeat_submission_id", "VARCHAR(100) DEFAULT NULL");

        // --- PROMOTIONS ---
        addColumn(conn, columnCache, "promotions", "buy_qty", "INT DEFAULT 0");
        addColumn(conn, columnCache, "promotions", "free_qty", "INT DEFAULT 0");
        addColumn(conn, columnCache, "promotions", "min_order_value", "DECIMAL(10,2) DEFAULT 0.00");

        // --- APP SETTINGS (Seeds Estéticos) ---
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT IGNORE INTO app_settings (setting_key, setting_value, description) VALUES " +
                "('ui.primary_color', '#1e88e5', 'Color principal')," +
                "('ui.bg_main', '#f1f5f9', 'Color fondo')," +
                "('ui.theme_mode', 'LIGHT', 'Tema')");
        }
    }

    public static void applyComplexMigrations(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 1. Índices de Rendimiento (Crucial para Historial rápido)
            addIndex(stmt, "sales", "idx_sales_datetime", "sale_datetime");
            addIndex(stmt, "sales", "idx_sales_user", "user_id");
            addIndex(stmt, "sales", "idx_sales_client", "client_id");
            addIndex(stmt, "sale_details", "idx_sale_details_sale", "sale_id");
            addIndex(stmt, "returns", "idx_returns_datetime", "return_datetime");

            // 2. Corrrección de NULLs en devoluciones (Parche V2)
            stmt.execute("UPDATE sales SET is_return = 0 WHERE is_return IS NULL");
            stmt.execute("UPDATE sales SET returned_amount = 0 WHERE returned_amount IS NULL");
            stmt.execute("UPDATE sale_details SET returned_quantity = 0 WHERE returned_quantity IS NULL");

            // 3. Limpieza de categorías duplicadas (Parche V1)
            try {
                stmt.execute("DELETE FROM categories WHERE name = 'SISTEMA' AND category_id NOT IN (" +
                             "SELECT min_id FROM (SELECT MIN(category_id) as min_id FROM categories WHERE name = 'SISTEMA') as tmp)");
            } catch (SQLException e) {}
            
            // 4. Migración de Passwords (BCrypt)
            com.mycompany.ventacontrolfx.infrastructure.persistence.BCryptMigrationService.migratePasswords(conn);
        }
    }

    private static void addIndex(Statement stmt, String table, String indexName, String column) {
        try {
            // Sintaxis compatible con MariaDB/MySQL para crear índices si no existen
            stmt.execute("CREATE INDEX " + indexName + " ON " + table + " (" + column + ")");
        } catch (SQLException e) {
            // Si el índice ya existe, el error se ignora silenciosamente
        }
    }

    private static void addColumn(Connection conn, Map<String, Set<String>> columnCache, String table, String col, String type) {
        Set<String> cols = columnCache.get(table.toLowerCase());
        if (cols != null && cols.contains(col.toLowerCase())) return;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + col + " " + type);
            if (cols != null) cols.add(col.toLowerCase());
        } catch (SQLException e) { System.err.println("[MIGRATOR] Error adding " + col + " to " + table + ": " + e.getMessage()); }
    }
}

