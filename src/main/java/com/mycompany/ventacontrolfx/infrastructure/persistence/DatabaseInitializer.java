package com.mycompany.ventacontrolfx.infrastructure.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

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
                    "email VARCHAR(255)",
                    "phone VARCHAR(50)"
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
                    { "ui.bg_main", "#fafbfc", "Color de fondo principal" },
                    { "ui.font_size", "14", "Tamaño de fuente base (px)" },
                    { "ui.border_radius", "8", "Redondeado de bordes base (px)" },
                    { "ui.theme_mode", "LIGHT", "Modo de tema activo (LIGHT/DARK)" }
            };

            for (String[] setting : defaultSettings) {
                stmt.execute(String.format(
                        "INSERT IGNORE INTO app_settings (setting_key, setting_value, description) VALUES ('%s', '%s', '%s')",
                        setting[0], setting[1], setting[2]));
            }

            // 12. Seed the permission catalog with ATOMIC permissions
            String[] permSeeds = {
                    // Ventas
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('venta.crear', 'Crear nuevas ventas')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('venta.anular', 'Anular ventas realizadas')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('venta.descuento', 'Aplicar descuentos en ventas')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('venta.ticket', 'Reimprimir tickets de venta')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('venta.devolucion', 'Gestionar devoluciones de productos')",
                    // Productos
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('producto.crear', 'Crear nuevos productos')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('producto.editar', 'Editar productos existentes')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('producto.precios', 'Gestionar listas de precios')",
                    // Caja
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('caja.abrir', 'Realizar apertura de caja')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('caja.cerrar', 'Realizar cierre de caja diario')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('caja.retirada', 'Realizar retiradas de efectivo')",
                    // Usuarios y Roles
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('usuario.crear', 'Gestionar usuarios del sistema')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('rol.editar', 'Crear y editar roles personalizados')",
                    // Informes
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('informe.ver_global', 'Ver informes generales y KPI')",
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('informe.ver_propio', 'Ver historial de ventas propias')",
                    // Otros
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('CONFIGURACION', 'Configuración técnica del sistema')"
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
                    "price DECIMAL(10,2) NOT NULL CHECK (price >= 0), " +
                    "start_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "end_date DATETIME DEFAULT NULL, " +
                    "reason VARCHAR(255), " +
                    "FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (price_list_id) REFERENCES price_lists(price_list_id) ON DELETE CASCADE)");

            // Índices para optimización de precios
            stmt.execute(
                    "CREATE INDEX IF NOT EXISTS idx_product_active_price ON product_prices (product_id, price_list_id, end_date)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_price_dates ON product_prices (start_date, end_date)");

            // --- Migración Inicial de Precios (Idempotente) ---
            // 1. Asegurar lista por defecto
            stmt.execute(
                    "INSERT IGNORE INTO price_lists (price_list_id, name, is_default) VALUES (1, 'PVP General', TRUE)");

            // 2. Migrar precios desde la tabla products si la tabla product_prices está
            // vacía
            try {
                stmt.execute("INSERT INTO product_prices (product_id, price_list_id, price, start_date, reason) " +
                        "SELECT product_id, 1, price, CURRENT_TIMESTAMP, 'Migración inicial' " +
                        "FROM products p " +
                        "WHERE NOT EXISTS (SELECT 1 FROM product_prices)");
            } catch (SQLException e) {
                // Columna price puede no existir en nuevas instalaciones
            }

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
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

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

            // 9. Audit Log (Historial de acciones críticas de seguridad)
            // Ya definido como audit_logs en el bloque 14 para soporte enterprise.

            // Nuevo permiso caja.ingresar
            stmt.execute(
                    "INSERT IGNORE INTO permissions (code, description) VALUES ('caja.ingresar', 'Realizar ingresos manuales de efectivo')");

            // Asignar caja.ingresar al Administrador por defecto
            stmt.execute("INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT r.role_id, p.permission_id FROM roles r, permissions p " +
                    "WHERE r.name = 'Administrador' AND p.code = 'caja.ingresar'");

            stmt.execute("INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT r.role_id, p.permission_id FROM roles r, permissions p " +
                    "WHERE r.name = 'Administrador' AND p.code = 'venta.devolucion'");

            // --- Indices para optimización ---
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_cm_session ON cash_movements(session_id)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_cm_closure ON cash_movements(closure_id)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id)");
            tryIndex(stmt, "CREATE INDEX IF NOT EXISTS idx_audit_event ON audit_logs(event_type)");

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
