-- Datos Iniciales - VentaControlFX

-- 1. Series de Documentos
INSERT IGNORE INTO doc_series (series_code, prefix, last_number, year, description) VALUES ('T', 'TCK-', 0, 2026, 'Tickets / Facturas Simplificadas');
INSERT IGNORE INTO doc_series (series_code, prefix, last_number, year, description) VALUES ('F', 'FAC-', 0, 2026, 'Facturas Completas');
INSERT IGNORE INTO doc_series (series_code, prefix, last_number, year, description) VALUES ('R', 'REC-', 0, 2026, 'Facturas Rectificativas (Abonos)');

-- 2. Configuración por Defecto
INSERT IGNORE INTO system_config (config_key, config_value) VALUES ('companyName', 'MI EMPRESA S.L.');
INSERT IGNORE INTO system_config (config_key, config_value) VALUES ('cif', 'B12345678');
INSERT IGNORE INTO system_config (config_key, config_value) VALUES ('currency', 'EUR');
INSERT IGNORE INTO system_config (config_key, config_value) VALUES ('db.schema_version', '1.14');

-- 3. Ajustes Estéticos por Defecto
INSERT IGNORE INTO app_settings (setting_key, setting_value, description) VALUES ('ui.primary_color', '#1e88e5', 'Color principal de la aplicación');
INSERT IGNORE INTO app_settings (setting_key, setting_value, description) VALUES ('ui.bg_main', '#f1f5f9', 'Color de fondo principal');
INSERT IGNORE INTO app_settings (setting_key, setting_value, description) VALUES ('ui.theme_mode', 'LIGHT', 'Modo de tema activo');

-- 4. Catálogo de Permisos
INSERT IGNORE INTO permissions (code, description) VALUES ('VENTAS', 'Acceso al punto de venta');
INSERT IGNORE INTO permissions (code, description) VALUES ('HISTORIAL', 'Ver historial de ventas');
INSERT IGNORE INTO permissions (code, description) VALUES ('PRODUCTOS', 'Gestionar productos');
INSERT IGNORE INTO permissions (code, description) VALUES ('CLIENTES', 'Gestionar clientes');
INSERT IGNORE INTO permissions (code, description) VALUES ('CIERRES', 'Gestionar cierres');
INSERT IGNORE INTO permissions (code, description) VALUES ('USUARIOS', 'Gestionar usuarios');
INSERT IGNORE INTO permissions (code, description) VALUES ('venta.devolucion', 'Gestionar devoluciones');
INSERT IGNORE INTO permissions (code, description) VALUES ('admin.iva', 'Gestionar tipos de IVA');

-- 5. Roles del Sistema
INSERT IGNORE INTO roles (name, description, is_system) VALUES ('Administrador', 'Acceso total al sistema', 1);
INSERT IGNORE INTO roles (name, description, is_system) VALUES ('Encargado', 'Gestión de tienda y stock', 1);
INSERT IGNORE INTO roles (name, description, is_system) VALUES ('Vendedor', 'Atención al cliente y ventas', 1);

-- 6. Asignación de Permisos a Roles
-- Administrador tiene todo
INSERT IGNORE INTO role_permissions (role_id, permission_id) 
SELECT r.role_id, p.permission_id FROM roles r, permissions p WHERE r.name = 'Administrador';

-- Vendedor tiene solo ventas y clientes
INSERT IGNORE INTO role_permissions (role_id, permission_id) 
SELECT r.role_id, p.permission_id FROM roles r, permissions p 
WHERE r.name = 'Vendedor' AND p.code IN ('VENTAS', 'CLIENTES', 'venta.devolucion');
