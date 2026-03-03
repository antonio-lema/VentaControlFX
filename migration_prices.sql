CREATE TABLE IF NOT EXISTS price_lists (
    price_list_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product_prices (
    price_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    price_list_id INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    start_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    end_date DATETIME NULL DEFAULT NULL,
    reason VARCHAR(255),
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    FOREIGN KEY (price_list_id) REFERENCES price_lists(price_list_id) ON DELETE CASCADE
);

CREATE INDEX idx_active_price ON product_prices(product_id, price_list_id, end_date);
CREATE INDEX idx_product_category ON products(category_id);

INSERT INTO price_lists (name, is_default) VALUES ('Tarifa Base Estándar', 1) ON DUPLICATE KEY UPDATE name=name;

INSERT INTO product_prices (product_id, price_list_id, price, start_date, reason)
SELECT product_id, 1, price, NOW(), 'Migración inicial'
FROM products
WHERE NOT EXISTS (
    SELECT 1 FROM product_prices WHERE product_prices.product_id = products.product_id AND product_prices.price_list_id = 1
);
