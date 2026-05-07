-- 0. Categorías
CREATE TABLE IF NOT EXISTS categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    tax_rate DECIMAL(10,2) DEFAULT 21.0,
    default_iva DECIMAL(5,2) DEFAULT 21.0,
    tax_group_id INT DEFAULT NULL,
    parent_category_id INT DEFAULT NULL,
    is_favorite BOOLEAN DEFAULT 0,
    visible BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 1. Productos
CREATE TABLE IF NOT EXISTS products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    sku VARCHAR(50) UNIQUE DEFAULT NULL,
    price DECIMAL(10,2) DEFAULT 0.00,
    iva DECIMAL(5,2) DEFAULT NULL,
    tax_rate DECIMAL(10,2) DEFAULT NULL,
    tax_group_id INT DEFAULT NULL,
    cost_price DECIMAL(10,4) DEFAULT 0.00,
    stock_quantity INT DEFAULT 0,
    min_stock INT DEFAULT 0,
    manage_stock BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    visible BOOLEAN DEFAULT TRUE,
    image_path VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(category_id)
);

-- 2. Usuarios
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    role VARCHAR(50),
    email VARCHAR(255),
    company_id INT DEFAULT NULL,
    has_custom_permissions BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 1. Clientes
CREATE TABLE IF NOT EXISTS clients (
    client_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    is_company BOOLEAN DEFAULT FALSE,
    tax_id VARCHAR(50),
    address TEXT,
    postal_code VARCHAR(10),
    city VARCHAR(100),
    province VARCHAR(100),
    country VARCHAR(100) DEFAULT 'España',
    email VARCHAR(255),
    phone VARCHAR(50),
    price_list_id INT DEFAULT NULL,
    tax_exempt BOOLEAN DEFAULT FALSE,
    tax_regime VARCHAR(50) DEFAULT 'NORMAL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Ventas
CREATE TABLE IF NOT EXISTS sales (
    sale_id INT AUTO_INCREMENT PRIMARY KEY,
    sale_datetime DATETIME,
    user_id INT,
    client_id INT DEFAULT NULL,
    total DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_net DECIMAL(10,2) DEFAULT 0.00,
    total_tax DECIMAL(10,2) DEFAULT 0.00,
    payment_method VARCHAR(50),
    iva DECIMAL(10,2) DEFAULT 0,
    is_return BOOLEAN DEFAULT FALSE,
    returned_amount DECIMAL(10,2) DEFAULT 0.00,
    return_reason TEXT,
    closure_id INT DEFAULT NULL,
    customer_name_snapshot VARCHAR(255) DEFAULT NULL,
    discount_amount DECIMAL(10,2) DEFAULT 0.00,
    discount_reason VARCHAR(255) DEFAULT NULL,
    promo_code VARCHAR(100) DEFAULT NULL,
    reward_promo_code VARCHAR(100) DEFAULT NULL,
    doc_type VARCHAR(20) DEFAULT 'SIMPLIFICADA',
    doc_series VARCHAR(10) DEFAULT 'A',
    doc_number INT DEFAULT NULL,
    doc_status VARCHAR(20) DEFAULT 'EMITIDO',
    control_hash VARCHAR(64) DEFAULT NULL,
    prev_hash VARCHAR(64) DEFAULT NULL,
    signature TEXT DEFAULT NULL,
    fiscal_status VARCHAR(20) DEFAULT 'PENDING',
    fiscal_msg TEXT DEFAULT NULL,
    aeat_submission_id VARCHAR(100) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(client_id)
);

-- 3. Detalles de Venta
CREATE TABLE IF NOT EXISTS sale_details (
    detail_id INT AUTO_INCREMENT PRIMARY KEY,
    sale_id INT,
    product_id INT,
    quantity INT,
    returned_quantity INT DEFAULT 0,
    unit_price DECIMAL(10,2),
    net_unit_price DECIMAL(10,4) DEFAULT 0.00,
    tax_basis DECIMAL(10,2) DEFAULT 0.00,
    tax_amount DECIMAL(10,2) DEFAULT 0.00,
    iva_rate DECIMAL(5,2) DEFAULT 21.0,
    iva_amount DECIMAL(10,2) DEFAULT 0.00,
    line_total DECIMAL(10,2),
    gross_total DECIMAL(10,2) DEFAULT 0.00,
    applied_tax_group VARCHAR(100) DEFAULT NULL,
    sku_snapshot VARCHAR(50) DEFAULT NULL,
    category_name_snapshot VARCHAR(100) DEFAULT NULL,
    observations TEXT DEFAULT NULL,
    FOREIGN KEY (sale_id) REFERENCES sales(sale_id)
);

-- 4. Índices de Rendimiento
CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_sales_user_date ON sales(user_id, sale_datetime);
CREATE INDEX IF NOT EXISTS idx_sales_client_date ON sales(client_id, sale_datetime);
CREATE INDEX IF NOT EXISTS idx_sales_datetime ON sales(sale_datetime);

-- 5. Cierres de Caja
CREATE TABLE IF NOT EXISTS cash_closures (
    closure_id INT AUTO_INCREMENT PRIMARY KEY,
    closure_date DATE NOT NULL,
    opening_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    user_id INT,
    initial_fund DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_cash DECIMAL(10,2),
    total_card DECIMAL(10,2),
    total_all DECIMAL(10,2),
    cash_in DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    cash_out DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    expected_cash DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    counted_cash DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    actual_cash DECIMAL(10,2) DEFAULT 0,
    difference DECIMAL(10,2) DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'CUADRADO',
    notes TEXT,
    reviewed_by INT DEFAULT NULL,
    reviewed_at DATETIME DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Devoluciones
CREATE TABLE IF NOT EXISTS returns (
    return_id INT AUTO_INCREMENT PRIMARY KEY,
    sale_id INT,
    user_id INT,
    closure_id INT DEFAULT NULL,
    return_datetime DATETIME,
    total_refunded DECIMAL(10,2) NOT NULL DEFAULT 0,
    cash_amount DOUBLE DEFAULT 0,
    card_amount DOUBLE DEFAULT 0,
    payment_method VARCHAR(50),
    reason TEXT,
    doc_type VARCHAR(20) DEFAULT 'RECTIFICATIVA',
    doc_series VARCHAR(10) DEFAULT 'R',
    doc_number INT DEFAULT NULL,
    doc_status VARCHAR(20) DEFAULT 'EMITIDO',
    control_hash VARCHAR(64) DEFAULT NULL,
    prev_hash VARCHAR(64) DEFAULT NULL,
    signature TEXT DEFAULT NULL,
    fiscal_status VARCHAR(20) DEFAULT 'PENDING',
    fiscal_msg TEXT DEFAULT NULL,
    aeat_submission_id VARCHAR(100) DEFAULT NULL,
    customer_name_snapshot VARCHAR(255) DEFAULT NULL,
    issuer_name VARCHAR(255) DEFAULT NULL,
    issuer_tax_id VARCHAR(50) DEFAULT NULL,
    issuer_address TEXT DEFAULT NULL,
    FOREIGN KEY (sale_id) REFERENCES sales(sale_id)
);

-- 7. Promociones
CREATE TABLE IF NOT EXISTS promotions (
    promotion_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL,
    value DOUBLE NOT NULL,
    buy_qty INT DEFAULT 0,
    free_qty INT DEFAULT 0,
    min_order_value DECIMAL(10,2) DEFAULT 0.00,
    start_date DATETIME,
    end_date DATETIME,
    active BOOLEAN DEFAULT TRUE,
    scope VARCHAR(50) NOT NULL,
    code VARCHAR(50) UNIQUE DEFAULT NULL,
    max_uses INT DEFAULT 0,
    current_uses INT DEFAULT 0,
    uses_per_customer INT DEFAULT 1,
    customer_id INT DEFAULT NULL
);

-- 8. Configuración del Sistema
CREATE TABLE IF NOT EXISTS system_config (
    config_key VARCHAR(50) PRIMARY KEY,
    config_value VARCHAR(255)
);

-- 9. Sesiones de Trabajo
CREATE TABLE IF NOT EXISTS work_sessions (
    session_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    type VARCHAR(20) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    status VARCHAR(20) NOT NULL,
    notes TEXT,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 10. Auditoría
CREATE TABLE IF NOT EXISTS audit_logs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    event_type VARCHAR(50),
    resource_name VARCHAR(100),
    action_description TEXT,
    payload_before TEXT,
    payload_after TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    branch_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 11. Recuperación de Contraseñas
CREATE TABLE IF NOT EXISTS password_recoveries (
    recovery_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    attempts INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 12. Permisos
CREATE TABLE IF NOT EXISTS permissions (
    permission_id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255) NOT NULL
);

-- 13. Roles
CREATE TABLE IF NOT EXISTS roles (
    role_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    is_system BOOLEAN DEFAULT FALSE
);

-- 14. Roles-Permisos (Pivot)
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(permission_id) ON DELETE CASCADE
);

-- 15. Usuarios-Permisos (Personalización)
CREATE TABLE IF NOT EXISTS user_permissions (
    user_id INT NOT NULL,
    permission_id INT NOT NULL,
    PRIMARY KEY (user_id, permission_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(permission_id) ON DELETE CASCADE
);

-- 16. Sucursales
CREATE TABLE IF NOT EXISTS branches (
    branch_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address TEXT,
    is_active BOOLEAN DEFAULT TRUE
);

-- 17. Ajustes de Aplicación (Estética)
CREATE TABLE IF NOT EXISTS app_settings (
    setting_key VARCHAR(50) PRIMARY KEY,
    setting_value TEXT,
    description VARCHAR(255),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 18. Listas de Precios y Tarifas
CREATE TABLE IF NOT EXISTS price_lists (
    price_list_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_prices (
    price_id      INT AUTO_INCREMENT PRIMARY KEY,
    product_id    INT NOT NULL,
    price_list_id INT NOT NULL,
    price         DECIMAL(10,2) NOT NULL,
    start_date    DATETIME NOT NULL,
    end_date      DATETIME DEFAULT NULL,
    reason        VARCHAR(255),
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    FOREIGN KEY (price_list_id) REFERENCES price_lists(price_list_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS price_update_log (
    log_id      INT AUTO_INCREMENT PRIMARY KEY,
    applied_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id     INT,
    description TEXT,
    affected_count INT
);

-- 19. Carritos Suspendidos (Aparcar Ventas)
CREATE TABLE IF NOT EXISTS suspended_carts (
    cart_id      INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT,
    client_id    INT,
    cart_name    VARCHAR(100),
    total_amount DECIMAL(10,2),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS suspended_cart_items (
    item_id      INT AUTO_INCREMENT PRIMARY KEY,
    cart_id      INT NOT NULL,
    product_id   INT NOT NULL,
    quantity     INT NOT NULL,
    unit_price   DECIMAL(10,2),
    FOREIGN KEY (cart_id) REFERENCES suspended_carts(cart_id) ON DELETE CASCADE
);

-- 20. Gestión de Caja (Sesiones, Entradas y Salidas)
CREATE TABLE IF NOT EXISTS cash_fund_sessions (
    session_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    opening_fund DECIMAL(10,2),
    closing_fund DECIMAL(10,2),
    status VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cash_withdrawals (
    withdrawal_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    amount DECIMAL(10,2),
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cash_entries (
    entry_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    amount DECIMAL(10,2),
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cash_movements (
    movement_id INT AUTO_INCREMENT PRIMARY KEY,
    session_id INT,
    user_id INT,
    type ENUM('APERTURA', 'VENTA', 'RETIRADA', 'INGRESO', 'CIERRE', 'DEVOLUCION') NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    reason VARCHAR(255),
    closure_id INT DEFAULT NULL,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 21. Motor de Impuestos (V2) y Snapshots Fiscales
CREATE TABLE IF NOT EXISTS tax_rates (
    tax_rate_id  INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    rate         DECIMAL(5,2) NOT NULL,
    country      VARCHAR(50) DEFAULT 'España',
    region       VARCHAR(50) DEFAULT NULL,
    valid_from   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_to     DATETIME NULL,
    active       BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS tax_groups (
    tax_group_id INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    is_default   BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS tax_group_items (
    tax_group_id INT NOT NULL,
    tax_rate_id  INT NOT NULL,
    PRIMARY KEY (tax_group_id, tax_rate_id),
    FOREIGN KEY (tax_group_id) REFERENCES tax_groups(tax_group_id) ON DELETE CASCADE,
    FOREIGN KEY (tax_rate_id) REFERENCES tax_rates(tax_rate_id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS sale_tax_summary (
    summary_id   INT AUTO_INCREMENT PRIMARY KEY,
    sale_id      INT NOT NULL,
    tax_rate_id  INT NOT NULL DEFAULT 1,
    tax_name     VARCHAR(100) NOT NULL DEFAULT 'IVA',
    tax_rate     DECIMAL(5,2) NOT NULL DEFAULT 21.0,
    tax_basis    DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    tax_amount   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    FOREIGN KEY (sale_id) REFERENCES sales(sale_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS doc_issuer_snapshots (
    snapshot_id      INT AUTO_INCREMENT PRIMARY KEY,
    sale_id          INT NOT NULL UNIQUE,
    company_name     VARCHAR(255),
    tax_id           VARCHAR(50),
    address          TEXT,
    phone            VARCHAR(50),
    issued_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    receiver_name    VARCHAR(255),
    receiver_tax_id  VARCHAR(50),
    receiver_address TEXT,
    base_amount      DECIMAL(10,2) DEFAULT 0.00,
    vat_amount       DECIMAL(10,2) DEFAULT 0.00,
    total_amount     DECIMAL(10,2) DEFAULT 0.00,
    FOREIGN KEY (sale_id) REFERENCES sales(sale_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tax_revisions (
    revision_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT DEFAULT NULL,
    category_id INT DEFAULT NULL,
    scope VARCHAR(20) NOT NULL,
    rate DECIMAL(5,2) NOT NULL,
    label VARCHAR(100),
    start_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_date DATETIME DEFAULT NULL,
    reason VARCHAR(255),
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE CASCADE
);

-- Índices Finales de Rendimiento
CREATE INDEX IF NOT EXISTS idx_active_price ON product_prices(product_id, price_list_id, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_sales_closure ON sales(closure_id);
CREATE INDEX IF NOT EXISTS idx_ret_closure ON returns(closure_id);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_event ON audit_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_prices_list_product ON product_prices (price_list_id, product_id);
CREATE INDEX IF NOT EXISTS idx_prod_stock ON products(stock_quantity);
CREATE INDEX IF NOT EXISTS idx_sales_dt_only ON sales(sale_datetime);
