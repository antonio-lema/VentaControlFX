-- Add new roles
INSERT IGNORE INTO roles (name, description, is_system) VALUES 
('Administrador', 'Acceso total al sistema', 1),
('Cajera', 'Operaciones de venta y cierres', 1),
('Supervisor', 'Gestión de inventario y devoluciones', 0),
('Almacen', 'Solo gestión de productos y stock', 0);

-- Assign some permissions to roles (assuming common permission codes exist)
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id 
FROM roles r, permissions p 
WHERE r.name = 'Administrador'; -- Admin gets everything

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id 
FROM roles r, permissions p 
WHERE r.name = 'Cajera' AND p.code IN ('VENTAS', 'CAJA_ABRIR', 'CAJA_CERRAR', 'CLIENTES_LISTAR');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id 
FROM roles r, permissions p 
WHERE r.name = 'Supervisor' AND p.code IN ('VENTAS', 'PRODUCTOS', 'CATEGORIAS', 'INFORMES');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id 
FROM roles r, permissions p 
WHERE r.name = 'Almacen' AND p.code IN ('PRODUCTOS', 'CATEGORIAS');
