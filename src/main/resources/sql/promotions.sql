CREATE TABLE IF NOT EXISTS promotions (
    promotion_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL, -- PERCENTAGE, FIXED_DISCOUNT, BUY_X_GET_Y
    value DOUBLE NOT NULL,
    start_date DATETIME,
    end_date DATETIME,
    active BOOLEAN DEFAULT TRUE,
    scope VARCHAR(50) NOT NULL -- GLOBAL, CATEGORY, PRODUCT
);

CREATE TABLE IF NOT EXISTS promotion_items (
    promotion_id INT NOT NULL,
    item_id INT NOT NULL, -- product_id or category_id
    PRIMARY KEY (promotion_id, item_id),
    FOREIGN KEY (promotion_id) REFERENCES promotions(promotion_id) ON DELETE CASCADE
);
