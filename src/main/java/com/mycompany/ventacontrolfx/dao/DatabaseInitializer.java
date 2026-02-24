package com.mycompany.ventacontrolfx.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 0. Clients Table
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

            // 3. Closures Table
            stmt.execute("CREATE TABLE IF NOT EXISTS cash_closures (" +
                    "closure_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "closure_date DATE NOT NULL, " +
                    "user_id INT, " +
                    "total_cash DECIMAL(10,2), " +
                    "total_card DECIMAL(10,2), " +
                    "total_all DECIMAL(10,2), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

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

            // 5. Return Details Table
            stmt.execute("CREATE TABLE IF NOT EXISTS return_details (" +
                    "return_detail_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "return_id INT, " +
                    "product_id INT, " +
                    "quantity INT, " +
                    "unit_price DECIMAL(10,2), " +
                    "subtotal DECIMAL(10,2), " +
                    "FOREIGN KEY (return_id) REFERENCES returns(return_id))");
            // Ensure email column in users table (if users table exists)
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN email VARCHAR(255)");
            } catch (SQLException e) {
                // Ignore if column already exists or table doesn't exist (though it should)
            }

            // 6. System Configuration Table
            stmt.execute("CREATE TABLE IF NOT EXISTS system_config (" +
                    "config_key VARCHAR(50) PRIMARY KEY, " +
                    "config_value VARCHAR(255))");

        }
    }
}
