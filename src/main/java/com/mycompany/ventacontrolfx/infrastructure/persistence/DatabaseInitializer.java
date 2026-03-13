package com.mycompany.ventacontrolfx.infrastructure.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class DatabaseInitializer {

    public static void initialize(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 0. Users Table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(50) UNIQUE NOT NULL, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "full_name VARCHAR(100), " +
                    "role VARCHAR(50), " +
                    "email VARCHAR(255), " +
                    "company_id INT DEFAULT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            String[] userCols = {
                    "email VARCHAR(255)",
                    "role VARCHAR(50)",
                    "company_id INT DEFAULT NULL"
            };
            for (String col : userCols) {
                try {
                    stmt.execute("ALTER TABLE users ADD COLUMN " + col);
                } catch (SQLException e) {
                    // Ignore if exists
                }
            }

            // 1. Clients Table
            stmt.execute("CREATE TABLE IF NOT EXISTS clients (" +
                    "client_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "is_company BOOLEAN DEFAULT FALSE, " +
                    "tax_id VARCHAR(50), " +
                    "address TEXT, " +
                    "postal_code VARCHAR(10), " +
                    "city VARCHAR(100), " +
                    "province VARCHAR(100), " +
                    "country VARCHAR(100) DEFAULT 'España', " +
                    "email VARCHAR(255), " +
                    "phone VARCHAR(50), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Ensure columns exist if table was already there
            // Ensure columns exist if table was already there
            String[] clientCols = {
                    "is_company BOOLEAN DEFAULT FALSE",
                    "postal_code VARCHAR(10)",
                    "city VARCHAR(100)",
                    "province VARCHAR(100)",
                    "country VARCHAR(100) DEFAULT 'Spain'",
                    "phone VARCHAR(50)",
                    "price_list_id INT DEFAULT NULL",
                    "tax_exempt BOOLEAN DEFAULT FALSE",
                    "tax_regime VARCHAR(50) DEFAULT 'NORMAL'"
            };
            for (String col : clientCols) {
                try {
                    stmt.execute("ALTER TABLE clients ADD COLUMN " + col);
                } catch (SQLException e) {
                    // System.err.println("Column likely exists: " + col + " - " + e.getMessage());
                }
            }

            // --- Productos e IVA Flex (Nueva Estructura) ---
            try {
                // default_iva en categorías (para herencia)
                stmt.execute("ALTER TABLE categories ADD COLUMN default_iva DECIMAL(5,2) DEFAULT 21.0");
            } catch (SQLException e) {
            }
            try {
                // iva en productos (sobrescribe categoría)
                stmt.execute("ALTER TABLE products ADD COLUMN iva DECIMAL(5,2) DEFAULT NULL");
            } catch (SQLException e) {
            }
            try {
                // tax_group_id para el Tax Engine V2
                stmt.execute("ALTER TABLE products ADD COLUMN tax_group_id INT DEFAULT NULL");
            } catch (SQLException e) {
            }
            try {
                // tax_group_id en categorías (para herencia)
                stmt.execute("ALTER TABLE categories ADD COLUMN tax_group_id INT DEFAULT NULL");
            } catch (SQLException e) {
            }

            // Inicialización de datos (Expand): Copiar tax_rate actual si existía o usar
            // 21.0
            try {
                stmt.execute("UPDATE categories SET default_iva = tax_rate WHERE default_iva IS NULL");
                stmt.execute("UPDATE products SET iva = tax_rate WHERE iva IS NULL AND tax_rate IS NOT NULL");
            } catch (SQLException e) {
                // Si la columna tax_rate no existía, ignoramos el update
            }

            // --- Estructura Legacy (Mantener por compatibilidad temporal) ---
            try {
                stmt.execute("ALTER TABLE categories ADD COLUMN tax_rate DECIMAL(10,2) DEFAULT 21.0");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE products ADD COLUMN tax_rate DECIMAL(10,2) DEFAULT NULL");
            } catch (SQLException e) {
            }

            try {
                stmt.execute("ALTER TABLE products ADD COLUMN sku VARCHAR(50) UNIQUE DEFAULT NULL");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE products ADD COLUMN cost_price DECIMAL(10,4) DEFAULT 0.00");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE products ADD COLUMN is_active BOOLEAN DEFAULT TRUE");
            } catch (SQLException e) {
            }

            // --- Control de Stock ---
            try {
                stmt.execute("ALTER TABLE products ADD COLUMN stock_quantity INT DEFAULT 0");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE products ADD COLUMN min_stock INT DEFAULT 0");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE products ADD COLUMN manage_stock BOOLEAN DEFAULT FALSE");
            } catch (SQLException e) {
            }

            try {
                stmt.execute("ALTER TABLE price_lists ADD COLUMN priority INT DEFAULT 0");
            } catch (SQLException e) {
            }

            try {
                stmt.execute("ALTER TABLE categories ADD COLUMN parent_category_id INT DEFAULT NULL");
            } catch (SQLException e) {
            }

            // 1. Sales Table
            stmt.execute("CREATE TABLE IF NOT EXISTS sales (" +
                    "sale_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "sale_datetime DATETIME, " +
                    "user_id INT, " +
                    "client_id INT DEFAULT NULL, " +
                    "total DECIMAL(10,2) NOT NULL DEFAULT 0, " +
                    "payment_method VARCHAR(50), " +
                    "iva DECIMAL(10,2) DEFAULT 0, " +
                    "is_return BOOLEAN DEFAULT FALSE, " +
                    "return_reason TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (client_id) REFERENCES clients(client_id))");

            // Ensure columns exist and have the right size if table was already there
            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN sale_datetime DATETIME");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE sales MODIFY COLUMN sale_datetime DATETIME");
            } catch (SQLException e) {
            }

            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN user_id INT");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE sales MODIFY COLUMN user_id INT");
            } catch (SQLException e) {
            }

            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN client_id INT DEFAULT NULL");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE sales MODIFY COLUMN client_id INT DEFAULT NULL");
            } catch (SQLException e) {
            }

            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN total DECIMAL(10,2) NOT NULL DEFAULT 0");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE sales MODIFY COLUMN total DECIMAL(10,2) NOT NULL DEFAULT 0");
            } catch (SQLException e) {
            }

            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN payment_method VARCHAR(50)");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE sales MODIFY COLUMN payment_method VARCHAR(50)");
            } catch (SQLException e) {
            }

            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN iva DECIMAL(10,2) DEFAULT 0");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE sales MODIFY COLUMN iva DECIMAL(10,2) DEFAULT 0");
            } catch (SQLException e) {
            }

            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN is_return BOOLEAN DEFAULT FALSE");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE sales MODIFY COLUMN is_return BOOLEAN DEFAULT FALSE");
            } catch (SQLException e) {
            }

            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN return_reason TEXT");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE sales MODIFY COLUMN return_reason TEXT");
            } catch (SQLException e) {
            }

            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN closure_id INT DEFAULT NULL");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE sales MODIFY COLUMN closure_id INT DEFAULT NULL");
            } catch (SQLException e) {
            }

            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE sales MODIFY COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            } catch (SQLException e) {
            }

            // --- Snapshots Fiscales e Inmutabilidad (Fase 3) ---
            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN total_net DECIMAL(10,2) DEFAULT 0.00");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN total_tax DECIMAL(10,2) DEFAULT 0.00");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN customer_name_snapshot VARCHAR(255) DEFAULT NULL");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN discount_amount DECIMAL(10,2) DEFAULT 0.00");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN discount_reason VARCHAR(255) DEFAULT NULL");
            } catch (SQLException e) {
            }

            // 2. Sale Details Table
            stmt.execute("CREATE TABLE IF NOT EXISTS sale_details (" +
                    "detail_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "sale_id INT, " +
                    "product_id INT, " +
                    "quantity INT, " +
                    "unit_price DECIMAL(10,2), " +
                    "line_total DECIMAL(10,2), " +
                    "FOREIGN KEY (sale_id) REFERENCES sales(sale_id))");

            try {
                stmt.execute("ALTER TABLE sale_details ADD COLUMN unit_price DECIMAL(10,2)");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE sale_details MODIFY COLUMN unit_price DECIMAL(10,2)");
            } catch (SQLException e) {
            }

            try {
                stmt.execute("ALTER TABLE sale_details ADD COLUMN line_total DECIMAL(10,2)");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE sale_details MODIFY COLUMN line_total DECIMAL(10,2)");
            } catch (SQLException e) {
            }

            try {
                stmt.execute("ALTER TABLE sale_details ADD COLUMN returned_quantity INT DEFAULT 0");
            } catch (SQLException e) {
            }

            // Add returned_amount to sales table for easier tracking
            try {
                stmt.execute("ALTER TABLE sales ADD COLUMN returned_amount DECIMAL(10,2) DEFAULT 0.00");
            } catch (SQLException e) {
            }

            // --- Detalles de venta con desglose de IVA ---
            try {
                stmt.execute("ALTER TABLE sale_details ADD COLUMN iva_rate DECIMAL(5,2) DEFAULT 21.0");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE sale_details ADD COLUMN iva_amount DECIMAL(10,2) DEFAULT 0.00");
            } catch (SQLException e) {
            }

            // --- Campos para Snapshot Fiscal V2 (Tax Engine) ---
            String[] v2SaleDetailCols = {
                    "net_unit_price DECIMAL(10,4) DEFAULT 0.00",
                    "tax_basis DECIMAL(10,2) DEFAULT 0.00",
                    "tax_amount DECIMAL(10,2) DEFAULT 0.00",
                    "gross_total DECIMAL(10,2) DEFAULT 0.00",
                    "applied_tax_group VARCHAR(100) DEFAULT NULL"
            };
            for (String col : v2SaleDetailCols) {
                try {
                    stmt.execute("ALTER TABLE sale_details ADD COLUMN " + col);
                } catch (SQLException e) {
                }
            }

            try {
                stmt.execute("ALTER TABLE sale_details ADD COLUMN sku_snapshot VARCHAR(50) DEFAULT NULL");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE sale_details ADD COLUMN category_name_snapshot VARCHAR(100) DEFAULT NULL");
            } catch (SQLException e) {
            }

            // 3. Closures Table
            stmt.execute("CREATE TABLE IF NOT EXISTS cash_closures (" +
                    "closure_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "closure_date DATE NOT NULL, " +
                    "user_id INT, " +
                    "total_cash DECIMAL(10,2), " +
                    "total_card DECIMAL(10,2), " +
                    "total_all DECIMAL(10,2), " +
                    "actual_cash DECIMAL(10,2) DEFAULT 0, " +
                    "difference DECIMAL(10,2) DEFAULT 0, " +
                    "notes TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

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
                try {
                    stmt.execute("ALTER TABLE cash_closures ADD COLUMN " + col);
                } catch (SQLException e) {
                }
            }

            try {
                stmt.execute("ALTER TABLE cash_closures ADD COLUMN total_cash DECIMAL(10,2)");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE cash_closures MODIFY COLUMN total_cash DECIMAL(10,2)");
            } catch (SQLException e) {
            }

            try {
                stmt.execute("ALTER TABLE cash_closures ADD COLUMN total_card DECIMAL(10,2)");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE cash_closures MODIFY COLUMN total_card DECIMAL(10,2)");
            } catch (SQLException e) {
            }

            try {
                stmt.execute("ALTER TABLE cash_closures ADD COLUMN total_all DECIMAL(10,2)");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE cash_closures MODIFY COLUMN total_all DECIMAL(10,2)");
            } catch (SQLException e) {
            }

            // 4. Returns Table
            stmt.execute("CREATE TABLE IF NOT EXISTS returns (" +
                    "return_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "sale_id INT, " +
                    "user_id INT, " +
                    "return_datetime DATETIME, " +
                    "total_refunded DECIMAL(10,2) NOT NULL DEFAULT 0, " +
                    "reason TEXT, " +
                    "closure_id INT DEFAULT NULL, " +
                    "FOREIGN KEY (sale_id) REFERENCES sales(sale_id))");

            try {
                stmt.execute("ALTER TABLE returns ADD COLUMN closure_id INT DEFAULT NULL");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE returns ADD COLUMN payment_method VARCHAR(50)");
            } catch (SQLException e) {
            }

            // 5. Return Details Table
            stmt.execute("CREATE TABLE IF NOT EXISTS return_details (" +
                    "return_detail_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "return_id INT, " +
                    "product_id INT, " +
                    "quantity INT, " +
                    "unit_price DECIMAL(10,2), " +
                    "subtotal DECIMAL(10,2), " +
                    "FOREIGN KEY (return_id) REFERENCES returns(return_id))");
            // 6. System Configuration Table
            stmt.execute("CREATE TABLE IF NOT EXISTS system_config (" +
                    "config_key VARCHAR(50) PRIMARY KEY, " +
                    "config_value VARCHAR(255))");

            // 7. password_recoveries Table (Security Fix V-01)
            stmt.execute("CREATE TABLE IF NOT EXISTS password_recoveries (" +
                    "recovery_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "email VARCHAR(255) NOT NULL, " +
                    "code_hash VARCHAR(255) NOT NULL, " +
                    "expires_at DATETIME NOT NULL, " +
                    "is_used BOOLEAN DEFAULT FALSE, " +
                    "attempts INT DEFAULT 0, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Asegurarse de que la columna 'attempts' existe en instalaciones antiguas (Fix
            // V-01)
            try {
                stmt.execute("ALTER TABLE password_recoveries ADD COLUMN attempts INT DEFAULT 0");
            } catch (SQLException ignored) {
                /* Ya existe */ }

            // 8. Promotions table check
            try {
                stmt.execute("ALTER TABLE promotions ADD COLUMN buy_qty INT DEFAULT 0");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE promotions ADD COLUMN free_qty INT DEFAULT 0");
            } catch (SQLException e) {
            }
            String[][] companyDefaults = {
                    { "companyName", "MI EMPRESA S.L." },
                    { "cif", "B12345678" },
                    { "address", "Calle Falsa 123, 28001 Madrid" },
                    { "phone", "912 345 678" },
                    { "email", "info@miempresa.com" },
                    { "logoPath", "" },
                    { "currency", "EUR" },
                    { "roundingMethod", "LINE" } // LINE or GLOBAL
            };
            for (String[] row : companyDefaults) {
                stmt.execute(String.format(
                        "INSERT IGNORE INTO system_config (config_key, config_value) VALUES ('%s', '%s')",
                        row[0], row[1]));
            }

            // 7. Permissions Catalog Table
            stmt.execute("CREATE TABLE IF NOT EXISTS permissions (" +
                    "permission_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "code VARCHAR(50) UNIQUE NOT NULL, " +
                    "description VARCHAR(255) NOT NULL)");

            // 8. User-Permission pivot table
            stmt.execute("CREATE TABLE IF NOT EXISTS user_permissions (" +
                    "user_id INT NOT NULL, " +
                    "permission_id INT NOT NULL, " +
                    "PRIMARY KEY (user_id, permission_id), " +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (permission_id) REFERENCES permissions(permission_id) ON DELETE CASCADE)");

            // 9. Roles Table (Updated with is_system)
            stmt.execute("CREATE TABLE IF NOT EXISTS roles (" +
                    "role_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(50) UNIQUE NOT NULL, " +
                    "description VARCHAR(255), " +
                    "is_system BOOLEAN DEFAULT FALSE)");

            try {
                stmt.execute("ALTER TABLE roles ADD COLUMN is_system BOOLEAN DEFAULT FALSE");
            } catch (SQLException e) {
            }

            // 10. Role-Permission pivot table
            stmt.execute("CREATE TABLE IF NOT EXISTS role_permissions (" +
                    "role_id INT NOT NULL, " +
                    "permission_id INT NOT NULL, " +
                    "PRIMARY KEY (role_id, permission_id), " +
                    "FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (permission_id) REFERENCES permissions(permission_id) ON DELETE CASCADE)");

            // 11. Individual user permissions table
            stmt.execute("CREATE TABLE IF NOT EXISTS user_permissions (" +
                    "user_id INT NOT NULL, " +
                    "permission_id INT NOT NULL, " +
                    "PRIMARY KEY (user_id, permission_id), " +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (permission_id) REFERENCES permissions(permission_id) ON DELETE CASCADE)");

            // 12. Branches Table (Multi-branch support)
            stmt.execute("CREATE TABLE IF NOT EXISTS branches (" +
                    "branch_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "address TEXT, " +
                    "is_active BOOLEAN DEFAULT TRUE)");

            // 13. Contextual Branch Permissions
            stmt.execute("CREATE TABLE IF NOT EXISTS user_branch_permissions (" +
                    "user_id INT NOT NULL, " +
                    "branch_id INT NOT NULL, " +
                    "permission_id INT NOT NULL, " +
                    "PRIMARY KEY (user_id, branch_id, permission_id), " +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (branch_id) REFERENCES branches(branch_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (permission_id) REFERENCES permissions(permission_id) ON DELETE CASCADE)");

            // 14. Enterprise Audit Logs (Enhanced)
            stmt.execute("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "log_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT, " +
                    "event_type VARCHAR(50), " +
                    "resource_name VARCHAR(100), " +
                    "action_description TEXT, " +
                    "payload_before TEXT, " + // Simulating JSON with TEXT for compatibility
                    "payload_after TEXT, " +
                    "ip_address VARCHAR(45), " +
                    "user_agent TEXT, " +
                    "branch_id INT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL, " +
                    "FOREIGN KEY (branch_id) REFERENCES branches(branch_id) ON DELETE SET NULL)");

            // 15. App Settings (Persistencia de configuración y Estética)
            stmt.execute("CREATE TABLE IF NOT EXISTS app_settings (" +
                    "setting_key VARCHAR(50) PRIMARY KEY, " +
                    "setting_value TEXT, " +
                    "description VARCHAR(255), " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Seed default aesthetic settings if they don't exist
            String[][] defaultSettings = {
                    { "ui.primary_color", "#1e88e5", "Color principal de la aplicación" },
                    { "ui.secondary_color", "#64748b", "Color secundario de la aplicación" },
                    { "ui.bg_main", "#f1f5f9", "Color de fondo principal" },
                    { "ui.font_size", "14", "Tamaño de fuente base (px)" },
                    { "ui.border_radius", "8", "Redondeado de bordes base (px)" },
                    { "ui.theme_mode", "LIGHT", "Modo de tema activo (LIGHT/DARK)" }
            };

            for (String[] setting : defaultSettings) {
                stmt.execute(String.format(
                        "INSERT IGNORE INTO app_settings (setting_key, setting_value, description) VALUES ('%s', '%s', '%s')",
                        setting[0], setting[1], setting[2]));
            }

            // Actualizar bases de datos antiguas para que las tarjetas destaquen sobre el
            // fondo
            try {
                stmt.execute(
                        "UPDATE app_settings SET setting_value = '#f1f5f9' WHERE setting_key = 'ui.bg_main' AND setting_value = '#fafbfc'");
            } catch (SQLException e) {
            }

            // 12. Seed the permission catalog with ATOMIC permissions
            String[] permSeeds = {
                    // Ventas
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('venta.devolucion', 'Gestionar devoluciones de productos')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('venta.limpiar', 'Limpiar carrito de venta')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('venta.aplazar', 'Aplazar ventas en curso')",

                    // Productos
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('producto.crear', 'Crear nuevos productos')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('producto.editar', 'Editar productos existentes')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('producto.eliminar', 'Eliminar productos del catálogo')",

                    // Clientes
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('cliente.crear', 'Registrar nuevos clientes')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('cliente.editar', 'Actualizar datos de clientes')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('cliente.eliminar', 'Borrar clientes del sistema')",

                    // Caja
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('caja.abrir', 'Realizar apertura de caja')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('caja.cerrar', 'Realizar cierre de caja diario')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('caja.ingresar', 'Realizar ingresos manuales de efectivo')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('caja.retirada', 'Realizar retiradas de efectivo')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('caja.ver_totales', 'Ver efectivo esperado y fondo inicial en cierres')",

                    // Administración / Configuración
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('admin.iva', 'Gestionar tipos de IVA y tasas')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('admin.precios_masivo', 'Realizar actualizaciones masivas de precios')",

                    // Usuarios y Roles
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('usuario.crear', 'Gestionar usuarios del sistema')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('rol.editar', 'Crear y editar roles personalizados')",

                    // Informes y Analíticas
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('reporte.vendedores', 'Ver analíticas por vendedor')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('reporte.clientes', 'Ver informes de consumo de clientes')",
                    // Otros
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('CONFIGURACION', 'Ajustes técnicos de empresa')"
            };
            for (String seed : permSeeds) {
                stmt.execute(seed);
            }

            // Legacy support (older codes used in current UI)
            String[] legacySeeds = {
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('VENTAS', 'Acceso al punto de venta')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('HISTORIAL', 'Ver historial de ventas')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('PRODUCTOS', 'Gestionar productos')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('CLIENTES', 'Gestionar clientes')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('CIERRES', 'Gestionar cierres')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('USUARIOS', 'Gestionar usuarios')"
            };
            for (String seed : legacySeeds) {
                stmt.execute(seed);
            }

            // 13. Seed Default Roles
            String[][] rolesSeed = {
                    { "Administrador", "Acceso total al sistema", "1" },
                    { "Encargado", "Gestión de tienda, stock y cierres", "1" },
                    { "Supervisor", "Cajas, devoluciones e informes", "1" },
                    { "Vendedor", "Atención al cliente y ventas", "1" },
                    { "Almacenero", "Gestión de inventario y productos", "1" }
            };
            for (String[] role : rolesSeed) {
                stmt.execute(String.format(
                        "INSERT IGNORE INTO roles (name, description, is_system) VALUES ('%s', '%s', %s)",
                        role[0], role[1], role[2]));
            }

            // 14. Assign Permissions to Roles (Atomic and Legacy)
            // -- ADMINISTRADOR: All permissions
            stmt.execute("INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT r.role_id, p.permission_id FROM roles r, permissions p " +
                    "WHERE r.name = 'Administrador'");

            // -- ENCARGADO: Most permissions except user/config management
            stmt.execute("INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT r.role_id, p.permission_id FROM roles r, permissions p " +
                    "WHERE r.name = 'Encargado' AND p.code IN (" +
                    "'VENTAS', 'venta.devolucion', 'venta.limpiar', 'venta.aplazar', " +
                    "'PRODUCTOS', 'producto.crear', 'producto.editar', 'producto.eliminar', " +
                    "'admin.precios_masivo', " +
                    "'CLIENTES', 'cliente.crear', 'cliente.editar', 'cliente.eliminar', " +
                    "'CIERRES', 'HISTORIAL', " +
                    "'caja.abrir', 'caja.cerrar', 'caja.ingresar', 'caja.retirada', " +
                    "'reporte.vendedores', 'reporte.clientes')");

            // -- SUPERVISOR: Vendedor + Devoluciones + Cierres y Reportes básicos
            stmt.execute("INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT r.role_id, p.permission_id FROM roles r, permissions p " +
                    "WHERE r.name = 'Supervisor' AND p.code IN (" +
                    "'VENTAS', 'venta.aplazar', 'venta.devolucion', 'venta.limpiar', 'CLIENTES', " +
                    "'HISTORIAL', 'CIERRES', " +
                    "'caja.abrir', 'caja.cerrar', 'caja.ingresar', 'caja.retirada', 'reporte.vendedores')");

            // -- VENDEDOR: Only sales and basic operations
            stmt.execute("INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT r.role_id, p.permission_id FROM roles r, permissions p " +
                    "WHERE r.name = 'Vendedor' AND p.code IN (" +
                    "'VENTAS', 'venta.aplazar', 'venta.devolucion', 'CLIENTES', 'cliente.crear', 'cliente.editar', 'caja.abrir', 'caja.cerrar')");

            // -- ALMACENERO: Only product management
            stmt.execute("INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT r.role_id, p.permission_id FROM roles r, permissions p " +
                    "WHERE r.name = 'Almacenero' AND p.code IN ('PRODUCTOS', 'producto.crear', 'producto.editar', 'admin.precios_masivo')");

            // 10. Suspended Carts (Aplazamiento de carrito)
            stmt.execute("CREATE TABLE IF NOT EXISTS suspended_carts (" +
                    "cart_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "alias VARCHAR(100), " +
                    "user_id INT NOT NULL, " +
                    "client_id INT DEFAULT NULL, " +
                    "total DECIMAL(10,2) NOT NULL DEFAULT 0, " +
                    "suspended_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (client_id) REFERENCES clients(client_id) ON DELETE SET NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS suspended_cart_items (" +
                    "item_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "cart_id INT NOT NULL, " +
                    "product_id INT NOT NULL, " +
                    "quantity INT NOT NULL, " +
                    "price_at_suspension DECIMAL(10,2) NOT NULL, " +
                    "FOREIGN KEY (cart_id) REFERENCES suspended_carts(cart_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (product_id) REFERENCES products(product_id))");

            // 11. Pricing System (Gestión Flexible de Precios)
            stmt.execute("CREATE TABLE IF NOT EXISTS price_lists (" +
                    "price_list_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "is_default BOOLEAN DEFAULT FALSE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS product_prices (" +
                    "price_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "product_id INT NOT NULL, " +
                    "price_list_id INT NOT NULL, " +
                    "price DECIMAL(10,4) NOT NULL CHECK (price >= 0), " +
                    "start_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "end_date DATETIME DEFAULT NULL, " +
                    "reason VARCHAR(255), " +
                    "update_log_id INT NULL, " +
                    "FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (price_list_id) REFERENCES price_lists(price_list_id) ON DELETE CASCADE)");

            // Ensure update_log_id exists if table was already there
            try {
                stmt.execute("ALTER TABLE product_prices ADD COLUMN update_log_id INT NULL");
            } catch (SQLException e) {
            }

            // Seed default price list if empty
            stmt.execute(
                    "INSERT IGNORE INTO price_lists (price_list_id, name, is_default) VALUES (1, 'Tarifa General', 1)");

            // Índices para optimización de precios
            stmt.execute(
                    "CREATE INDEX IF NOT EXISTS idx_product_active_price ON product_prices (product_id, price_list_id, end_date)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_price_dates ON product_prices (start_date, end_date)");

            // 3. Migrar precio de cada producto que NO tenga aún una entrada activa en
            // product_prices.
            // Esto es idempotente: si el producto ya tiene precio activo, no hace nada.
            // Detectamos si existe la columna legacy 'price' para migrarla, si no usamos
            // 0.00.
            boolean hasPriceColumn = false;
            try (java.sql.ResultSet rsCol = conn.getMetaData().getColumns(null, null, "products", "price")) {
                if (rsCol.next()) {
                    hasPriceColumn = true;
                }
            }

            String migrationSql;
            if (hasPriceColumn) {
                migrationSql = "INSERT INTO product_prices (product_id, price_list_id, price, start_date, reason) " +
                        "SELECT p.product_id, 1, COALESCE(p.price, 0.00), CURRENT_TIMESTAMP, 'Migración inicial' " +
                        "FROM products p " +
                        "WHERE NOT EXISTS (SELECT 1 FROM product_prices pp WHERE pp.product_id = p.product_id AND pp.price_list_id = 1 AND pp.end_date IS NULL)";
            } else {
                migrationSql = "INSERT INTO product_prices (product_id, price_list_id, price, start_date, reason) " +
                        "SELECT p.product_id, 1, 0.00, CURRENT_TIMESTAMP, 'Inicialización automática' " +
                        "FROM products p " +
                        "WHERE NOT EXISTS (SELECT 1 FROM product_prices pp WHERE pp.product_id = p.product_id AND pp.price_list_id = 1 AND pp.end_date IS NULL)";
            }

            try {
                stmt.execute(migrationSql);
            } catch (SQLException e) {
                // Si falla por columna inexistente a pesar del check, simplemente no migramos
                if (e.getMessage().contains("Unknown column")) {
                    System.err.println(
                            "[INFO] Columna legacy 'price' no encontrada realmente, omitiendo migración de valores antiguos.");
                } else {
                    System.err.println("[WARN] Migración de precios fallida: " + e.getMessage());
                }
            }

            // ── 11. Tabla de Log de Actualizaciones Masivas de Precios ─────
            stmt.execute("CREATE TABLE IF NOT EXISTS price_update_log (" +
                    "  log_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "  update_type VARCHAR(50) NOT NULL COMMENT 'percentage, fixed, rounding'," +
                    "  scope VARCHAR(255) NOT NULL COMMENT 'La agrupación aplicada'," +
                    "  category_id INT NULL," +
                    "  value DECIMAL(10,4) NOT NULL," +
                    "  products_updated INT NOT NULL DEFAULT 0," +
                    "  reason VARCHAR(255)," +
                    "  applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            // Aseguramos tamaño por si se creó antes con tamaño pequeño
            try {
                stmt.execute("ALTER TABLE price_update_log MODIFY COLUMN scope VARCHAR(255) NOT NULL");
            } catch (SQLException e) {
                /* ignorar si falla */ }

            // 12. Seed default roles
            String[] roleSeeds = {
                    "INSERT IGNORE INTO roles (name, description) VALUES ('Cajero', 'Acceso básico a ventas')",
                    "INSERT IGNORE INTO roles (name, description) VALUES ('Administrador', 'Acceso total al sistema')",
                    "INSERT IGNORE INTO roles (name, description) VALUES ('Supervisor', 'Acceso a ventas, historial y productos')"
            };
            for (String seed : roleSeeds) {
                stmt.execute(seed);
            }

            // 13. Assign default permissions to roles
            // Cajero: solo VENTAS
            stmt.execute("INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT r.role_id, p.permission_id FROM roles r, permissions p " +
                    "WHERE r.name = 'Cajero' AND p.code = 'VENTAS'");

            // Supervisor: VENTAS, HISTORIAL, PRODUCTOS, CLIENTES, CIERRES
            String supervisorPerms = "'VENTAS', 'HISTORIAL', 'PRODUCTOS', 'CLIENTES', 'CIERRES'";
            stmt.execute("INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT r.role_id, p.permission_id FROM roles r, permissions p " +
                    "WHERE r.name = 'Supervisor' AND p.code IN (" + supervisorPerms + ")");

            // Administrador: TODO
            stmt.execute("INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT r.role_id, p.permission_id FROM roles r, permissions p " +
                    "WHERE r.name = 'Administrador'");

            // 7. Cash Fund Sessions Table (fondo de caja por turno)
            stmt.execute("CREATE TABLE IF NOT EXISTS cash_fund_sessions (" +
                    "session_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "session_date DATE NOT NULL, " +
                    "user_id INT, " +
                    "initial_amount DECIMAL(10,2) NOT NULL DEFAULT 0, " +
                    "is_closed BOOLEAN DEFAULT FALSE, " +
                    "closure_id INT DEFAULT NULL, " +
                    "notes TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            try {
                stmt.execute("ALTER TABLE cash_fund_sessions ADD COLUMN notes TEXT");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE cash_fund_sessions ADD COLUMN closure_id INT DEFAULT NULL");
            } catch (SQLException e) {
            }
            try {
                stmt.execute("ALTER TABLE cash_fund_sessions ADD COLUMN is_closed BOOLEAN DEFAULT FALSE");
            } catch (SQLException e) {
            }

            // 8. Cash Withdrawals Table (retiradas de efectivo)
            stmt.execute("CREATE TABLE IF NOT EXISTS cash_withdrawals (" +
                    "withdrawal_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "session_id INT, " +
                    "user_id INT, " +
                    "amount DECIMAL(10,2) NOT NULL, " +
                    "reason VARCHAR(255), " +
                    "closure_id INT DEFAULT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS cash_entries (" +
                    "entry_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "session_id INT, " +
                    "user_id INT, " +
                    "amount DECIMAL(10,2) NOT NULL, " +
                    "reason VARCHAR(255), " +
                    "closure_id INT DEFAULT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            try {
                stmt.execute("ALTER TABLE cash_withdrawals ADD COLUMN closure_id INT DEFAULT NULL");
            } catch (SQLException e) {
            }

            // ============================================================
            // ÍNDICES DE RENDIMIENTO
            // Seguros de ejecutar múltiples veces (IF NOT EXISTS).
            // ============================================================

            // --- sales ---
            // Reportes por vendedor en rango de fechas (SaleUseCase,
            // SellerReportController)
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_sales_user_date    ON sales(user_id, sale_datetime)");
            // Historial de compras por cliente (ClientReportController)
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_sales_client_date  ON sales(client_id, sale_datetime)");
            // Filtro por fecha en historial general
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_sales_datetime      ON sales(sale_datetime)");
            // Filtro por método de pago en cierres de caja
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_sales_payment       ON sales(payment_method)");
            // JOIN con cierres de caja
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_sales_closure       ON sales(closure_id)");
            // Filtro de devoluciones
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_sales_is_return     ON sales(is_return)");

            // --- sale_details ---
            // JOIN desde sales → detalles de línea (muy frecuente)
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_sd_sale             ON sale_details(sale_id)");
            // Consultas de ventas por producto (qué productos se han vendido más)
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_sd_product          ON sale_details(product_id)");

            // --- returns ---
            // JOIN desde sales → devoluciones
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_ret_sale            ON returns(sale_id)");
            // Devoluciones realizadas por vendedor
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_ret_user            ON returns(user_id)");
            // Devoluciones asociadas a un cierre
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_ret_closure         ON returns(closure_id)");
            // Filtro por fecha de devolución
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_ret_datetime        ON returns(return_datetime)");

            // --- return_details ---
            // JOIN desde returns → líneas devueltas
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_rd_return           ON return_details(return_id)");
            // Consultas de producto devuelto
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_rd_product          ON return_details(product_id)");

            // --- cash_closures ---
            // Cierres por usuario y fecha (histórico de cierres)
            tryIndex(stmt,
                    "CREATE INDEX IF NOT EXISTS idx_cc_user_date        ON cash_closures(user_id, closure_date)");
            // Filtro por fecha de cierre
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_cc_date             ON cash_closures(closure_date)");

            // --- cash_fund_sessions ---
            // Sesión activa de fondo de caja por usuario y fecha
            tryIndex(stmt,
                    "CREATE INDEX IF NOT EXISTS idx_cfs_user_date       ON cash_fund_sessions(user_id, session_date)");
            // Sesiones asociadas a un cierre
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_cfs_closure         ON cash_fund_sessions(closure_id)");
            // Búsqueda de sesiones abiertas
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_cfs_closed          ON cash_fund_sessions(is_closed)");

            // 8. Cash Movements (Evolución para trazabilidad total)
            stmt.execute("CREATE TABLE IF NOT EXISTS cash_movements (" +
                    "movement_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "session_id INT, " +
                    "user_id INT NOT NULL, " +
                    "type ENUM('APERTURA', 'VENTA', 'RETIRADA', 'INGRESO', 'CIERRE', 'DEVOLUCION') NOT NULL, " +
                    "amount DECIMAL(10,2) NOT NULL, " +
                    "reason VARCHAR(255), " +
                    "closure_id INT DEFAULT NULL, " +
                    "ip_address VARCHAR(45), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Migración: Asegurar que la columna 'type' acepte 'DEVOLUCION' si la tabla ya
            // existía
            try {
                stmt.execute(
                        "ALTER TABLE cash_movements MODIFY COLUMN type ENUM('APERTURA', 'VENTA', 'RETIRADA', 'INGRESO', 'CIERRE', 'DEVOLUCION') NOT NULL");
            } catch (SQLException e) {
                // Probablemente ya tenga el tipo correcto o sea compatible
            }

            // 9. Audit Log (Historial de acciones críticas de seguridad)
            // Ya definido como audit_logs en el bloque 14 para soporte enterprise.

            // Ya definido en el nuevo bloque de seeding de roles

            // --- Indices para optimización ---
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_cm_session ON cash_movements(session_id)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_cm_closure ON cash_movements(closure_id)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_audit_event ON audit_logs(event_type)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_audit_resource ON audit_logs(resource_name)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs(created_at)");

            // --- users ---
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_user_role ON users(role)");

            // --- password_recoveries ---
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_pwd_rec_email ON password_recoveries(email)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs(created_at)");

            // --- products ---
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_prod_category       ON products(category_id)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_prod_name           ON products(name)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_prod_visible        ON products(visible)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_prod_favorite       ON products(is_favorite)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_product_sku         ON products(sku)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_product_active      ON products(is_active)");

            // --- product_prices ---
            tryIndex(stmt,
                    "CREATE INDEX IF NOT EXISTS idx_active_price        ON product_prices(product_id, price_list_id, start_date, end_date)");

            // --- categories ---
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_cat_name            ON categories(name)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_cat_visible         ON categories(visible)");

            // --- suspended_carts ---
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_sus_user            ON suspended_carts(user_id)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_sus_items_cart      ON suspended_cart_items(cart_id)");

            // --- price_update_log ---
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_price_log_date      ON price_update_log(applied_at)");

            // --- clients ---
            // Búsqueda por nombre de cliente
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_cli_name            ON clients(name)");
            // Búsqueda por NIF/CIF
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_cli_tax_id          ON clients(tax_id)");
            // Búsqueda por email
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_cli_email           ON clients(email)");

            // --- USER_ID INTEGRITY (Fixing delete errors) ---
            String[] userRefs = { "sales", "cash_closures", "returns", "cash_fund_sessions", "cash_withdrawals",
                    "cash_entries", "cash_movements" };
            for (String table : userRefs) {
                try {
                    stmt.execute("ALTER TABLE " + table + " ADD CONSTRAINT fk_" + table
                            + "_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL");
                } catch (SQLException e) {
                    // Si ya existe la FK o la columna, simplemente seguimos
                }
            }

            // ─── SISTEMA DE HISTORIAL DE IVA (V2) ─────────────────────────
            stmt.execute("CREATE TABLE IF NOT EXISTS tax_rates ("
                    + "tax_rate_id  INT AUTO_INCREMENT PRIMARY KEY, "
                    + "name         VARCHAR(100) NOT NULL, "
                    + "rate         DECIMAL(5,2) NOT NULL, "
                    + "country      VARCHAR(50) DEFAULT 'España', "
                    + "region       VARCHAR(50) DEFAULT NULL, "
                    + "valid_from   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "valid_to     DATETIME NULL, "
                    + "active       BOOLEAN DEFAULT TRUE"
                    + ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS tax_groups ("
                    + "tax_group_id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "name         VARCHAR(100) NOT NULL, "
                    + "is_default   BOOLEAN DEFAULT FALSE"
                    + ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS tax_group_items ("
                    + "tax_group_id INT NOT NULL, "
                    + "tax_rate_id  INT NOT NULL, "
                    + "PRIMARY KEY (tax_group_id, tax_rate_id), "
                    + "FOREIGN KEY (tax_group_id) REFERENCES tax_groups(tax_group_id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (tax_rate_id) REFERENCES tax_rates(tax_rate_id) ON DELETE RESTRICT"
                    + ")");

            // Ensure columns exist for tax_rates and tax_groups (Fix for "name not found")
            String[] taxRateCols = {
                    "name VARCHAR(100) NOT NULL",
                    "rate DECIMAL(5,2) NOT NULL",
                    "country VARCHAR(50) DEFAULT 'España'",
                    "region VARCHAR(50) DEFAULT NULL",
                    "valid_from DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP",
                    "valid_to DATETIME NULL",
                    "active BOOLEAN DEFAULT TRUE"
            };
            for (String col : taxRateCols) {
                try {
                    stmt.execute("ALTER TABLE tax_rates ADD COLUMN " + col);
                } catch (SQLException e) {
                }
            }

            String[] taxGroupCols = {
                    "name VARCHAR(100) NOT NULL",
                    "is_default BOOLEAN DEFAULT FALSE"
            };
            for (String col : taxGroupCols) {
                try {
                    stmt.execute("ALTER TABLE tax_groups ADD COLUMN " + col);
                } catch (SQLException e) {
                }
            }

            stmt.execute("CREATE TABLE IF NOT EXISTS sale_tax_summary ("
                    + "summary_id   INT AUTO_INCREMENT PRIMARY KEY, "
                    + "sale_id      INT NOT NULL, "
                    + "tax_rate_id  INT NOT NULL DEFAULT 1, "
                    + "tax_name     VARCHAR(100) NOT NULL DEFAULT 'IVA', "
                    + "tax_rate     DECIMAL(5,2) NOT NULL DEFAULT 21.0, "
                    + "tax_basis    DECIMAL(10,2) NOT NULL DEFAULT 0.00, "
                    + "tax_amount   DECIMAL(10,2) NOT NULL DEFAULT 0.00, "
                    + "FOREIGN KEY (sale_id) REFERENCES sales(sale_id) ON DELETE CASCADE"
                    + ")");

            // Ensure columns exist for older installations
            String[] summaryCols = {
                    "tax_rate_id INT NOT NULL DEFAULT 1",
                    "tax_name VARCHAR(100) DEFAULT 'IVA'",
                    "tax_rate DECIMAL(5,2) DEFAULT 21.0"
            };
            for (String col : summaryCols) {
                try {
                    stmt.execute("ALTER TABLE sale_tax_summary ADD COLUMN " + col);
                } catch (SQLException e) {
                }
            }

            // Migración de tax_id a tax_rate_id si existiera la columna antigua
            try {
                stmt.execute(
                        "UPDATE sale_tax_summary SET tax_rate_id = tax_id WHERE tax_rate_id = 1 AND tax_id IS NOT NULL");
                // Limpieza de IDs 0 que rompen la FK
                stmt.execute(
                        "UPDATE sale_tax_summary SET tax_rate_id = 1 WHERE tax_rate_id = 0 OR tax_rate_id IS NULL");
            } catch (SQLException e) {
            }

            // Insertar datos por defecto (Tax Engine V2) si es primera ejecución
            try {
                // Crear tasas base
                stmt.execute("INSERT INTO tax_rates (tax_rate_id, name, rate) "
                        + "SELECT 1, 'IVA General (21%)', 21.00 "
                        + "FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tax_rates WHERE tax_rate_id = 1)");
                stmt.execute("INSERT INTO tax_rates (tax_rate_id, name, rate) "
                        + "SELECT 2, 'IVA Reducido (10%)', 10.00 "
                        + "FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tax_rates WHERE tax_rate_id = 2)");
                stmt.execute("INSERT INTO tax_rates (tax_rate_id, name, rate) "
                        + "SELECT 3, 'IVA Superreducido (4%)', 4.00 "
                        + "FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tax_rates WHERE tax_rate_id = 3)");
                stmt.execute("INSERT INTO tax_rates (tax_rate_id, name, rate) "
                        + "SELECT 4, 'Exento (0%)', 0.00 "
                        + "FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tax_rates WHERE tax_rate_id = 4)");

                // Crear grupos base
                stmt.execute("INSERT INTO tax_groups (tax_group_id, name, is_default) "
                        + "SELECT 1, 'Normal (IVA 21%)', 1 "
                        + "FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tax_groups WHERE tax_group_id = 1)");
                stmt.execute("INSERT INTO tax_groups (tax_group_id, name, is_default) "
                        + "SELECT 2, 'Reducido (IVA 10%)', 0 "
                        + "FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tax_groups WHERE tax_group_id = 2)");
                stmt.execute("INSERT INTO tax_groups (tax_group_id, name, is_default) "
                        + "SELECT 3, 'Superreducido (IVA 4%)', 0 "
                        + "FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tax_groups WHERE tax_group_id = 3)");
                stmt.execute("INSERT INTO tax_groups (tax_group_id, name, is_default) "
                        + "SELECT 4, 'Exento', 0 "
                        + "FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tax_groups WHERE tax_group_id = 4)");

                // Mapeo grupo -> tasas
                stmt.execute(
                        "INSERT IGNORE INTO tax_group_items (tax_group_id, tax_rate_id) VALUES (1, 1), (2, 2), (3, 3), (4, 4)");

                // Migrar productos hacia el grupo 1 si tienen iva nulo o 21
                stmt.execute(
                        "UPDATE products SET tax_group_id = 1 WHERE tax_group_id IS NULL AND (iva IS NULL OR iva = 21.0)");
                stmt.execute("UPDATE products SET tax_group_id = 2 WHERE tax_group_id IS NULL AND iva = 10.0");
                stmt.execute("UPDATE products SET tax_group_id = 3 WHERE tax_group_id IS NULL AND iva = 4.0");
                stmt.execute("UPDATE products SET tax_group_id = 4 WHERE tax_group_id IS NULL AND iva = 0.0");
                // Para productos con ivas raros, asignamos el default de momento para la
                // migración
                stmt.execute("UPDATE products SET tax_group_id = 1 WHERE tax_group_id IS NULL");
            } catch (SQLException e) {
                // Ignorar si duplicado
            }

            // ═══════════════════════════════════════════════════════════════
            // MÓDULO: PERSISTENCIA FISCAL DE TICKETS Y FACTURAS
            // Migración aditiva — no modifica ni elimina datos existentes.
            // ═══════════════════════════════════════════════════════════════

            // ── 1. Series de numeración correlativa ─────────────────────────
            stmt.execute("CREATE TABLE IF NOT EXISTS doc_series (" +
                    "series_id   INT AUTO_INCREMENT PRIMARY KEY, " +
                    "series_code VARCHAR(10) UNIQUE NOT NULL, " +
                    "prefix      VARCHAR(20) NOT NULL, " +
                    "last_number INT NOT NULL DEFAULT 0, " +
                    "year        INT NOT NULL DEFAULT 2026, " +
                    "description VARCHAR(100)" +
                    ")");

            // Seeds de las tres series (idempotente)
            int currentYear = java.time.LocalDate.now().getYear();
            stmt.execute("INSERT IGNORE INTO doc_series (series_code, prefix, last_number, year, description) VALUES " +
                    "('T', '" + currentYear + "-T-', 0, " + currentYear + ", 'Tickets / Facturas Simplificadas'), " +
                    "('F', '" + currentYear + "-F-', 0, " + currentYear + ", 'Facturas Completas')");

            // ── 2. Snapshot de emisor por documento ─────────────────────────
            stmt.execute("CREATE TABLE IF NOT EXISTS doc_issuer_snapshots (" +
                    "snapshot_id      INT AUTO_INCREMENT PRIMARY KEY, " +
                    "sale_id          INT NOT NULL UNIQUE, " +
                    "company_name     VARCHAR(255), " +
                    "tax_id           VARCHAR(50), " +
                    "address          TEXT, " +
                    "phone            VARCHAR(50), " +
                    "issued_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "receiver_name    VARCHAR(255), " +
                    "receiver_tax_id  VARCHAR(50), " +
                    "receiver_address TEXT, " +
                    "base_amount      DECIMAL(10,2) DEFAULT 0.00, " +
                    "vat_amount       DECIMAL(10,2) DEFAULT 0.00, " +
                    "total_amount     DECIMAL(10,2) DEFAULT 0.00, " +
                    "FOREIGN KEY (sale_id) REFERENCES sales(sale_id) ON DELETE CASCADE" +
                    ")");

            try {
                stmt.execute("ALTER TABLE doc_issuer_snapshots ADD COLUMN phone VARCHAR(50)");
            } catch (SQLException e) {
                /* columna ya existe */ }

            // ── 3. Columnas fiscales en la tabla sales (aditivo) ────────────
            String[] fiscalCols = {
                    "doc_type       VARCHAR(20)  DEFAULT NULL", // TICKET / FACTURA
                    "doc_series     VARCHAR(10)  DEFAULT NULL", // T / F
                    "doc_number     INT          DEFAULT NULL", // correlativo
                    "doc_status     VARCHAR(20)  DEFAULT NULL", // EMITIDO / ANULADO
                    "control_hash   VARCHAR(64)  DEFAULT NULL" // SHA-256 integridad
            };
            for (String col : fiscalCols) {
                try {
                    stmt.execute("ALTER TABLE sales ADD COLUMN " + col);
                } catch (SQLException e) {
                    /* columna ya existe */ }
            }

            // ── 4. Columna snapshot de nombre de producto en sale_details ───
            // Garantiza que los detalles de línea son inmutables ante cambios de catálogo.
            String[] detailSnapCols = {
                    "product_name_snapshot VARCHAR(255) DEFAULT NULL",
                    "discount_pct          DECIMAL(5,2)  DEFAULT 0.00"
            };
            for (String col : detailSnapCols) {
                try {
                    stmt.execute("ALTER TABLE sale_details ADD COLUMN " + col);
                } catch (SQLException e) {
                    /* columna ya existe */ }
            }

            // ── 5. Migración inicial: rellenar snapshot con nombre actual ───
            // Sólo rellena filas donde product_name_snapshot todavía es NULL
            try {
                stmt.execute(
                        "UPDATE sale_details sd " +
                                "JOIN products p ON sd.product_id = p.product_id " +
                                "SET sd.product_name_snapshot = p.name " +
                                "WHERE sd.product_name_snapshot IS NULL");
            } catch (SQLException e) {
                // Si la tabla products no tiene columna name aún, ignorar
            }

            // ── 6. Índices de rendimiento para documentos fiscales ──────────
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_sales_doc_series  ON sales(doc_series)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_sales_doc_number  ON sales(doc_series, doc_number)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_sales_doc_status  ON sales(doc_status)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_sales_doc_type    ON sales(doc_type)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_snapshot_sale     ON doc_issuer_snapshots(sale_id)");

            // ── 7. Migración de seguridad: BCrypt ───────────────────────────
            BCryptMigrationService.migratePasswords(conn);

            // ── 8. Sincronización de impuestos (Motor V2) ──────────────────
            // Esto asegura que cualquier cambio programado que ya deba estar activo
            // se refleje en los campos espejo (mirrored fields) al arrancar.
            try {
                new JdbcTaxRepository().syncMirroredValues();
            } catch (Exception e) {
                // Si las tablas nuevas no existen aún (primera ejecución), ignorar
            }

            // ── 9. Promotions Module ───────────────────────────────────────
            stmt.execute("CREATE TABLE IF NOT EXISTS promotions (" +
                    "promotion_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "description TEXT, " +
                    "type VARCHAR(50) NOT NULL, " +
                    "value DOUBLE NOT NULL, " +
                    "start_date DATETIME, " +
                    "end_date DATETIME, " +
                    "active BOOLEAN DEFAULT TRUE, " +
                    "scope VARCHAR(50) NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS promotion_items (" +
                    "promotion_id INT NOT NULL, " +
                    "item_id INT NOT NULL, " +
                    "PRIMARY KEY (promotion_id, item_id), " +
                    "FOREIGN KEY (promotion_id) REFERENCES promotions(promotion_id) ON DELETE CASCADE)");
        }
    }

    /**
     * Crea un índice de forma segura. Si ya existe o el motor no soporta
     * IF NOT EXISTS, simplemente lo ignora sin detener la inicialización.
     */
    private static void tryIndex(Statement stmt, String sql) {
        try {
            stmt.execute(sql);
        } catch (SQLException e) {
            // El índice ya existe o no es soportado — se ignora
        }
    }
}
