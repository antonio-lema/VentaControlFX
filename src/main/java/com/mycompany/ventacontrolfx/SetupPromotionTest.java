package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class SetupPromotionTest {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                System.out.println("=== CONFIGURANDO DATOS DE PRUEBA DE PROMOCIONES ===");

                // 1. Categoría de prueba
                stmt.execute("INSERT IGNORE INTO categories (category_id, name, visible, is_favorite, default_iva) " +
                        "VALUES (999, 'CATEGORIA PRUEBAS', 1, 0, 21.0)");

                // 2. Producto de prueba
                stmt.execute(
                        "INSERT IGNORE INTO products (product_id, category_id, name, sku, is_active, visible, is_favorite) "
                                +
                                "VALUES (999, 999, 'Soda de Prueba (2x1)', 'PROMO-TEST-01', 1, 1, 0)");

                stmt.execute(
                        "UPDATE products SET category_id = 999, name = 'Soda de Prueba (2x1)', sku = 'PROMO-TEST-01' WHERE product_id = 999");

                // 3. Precio del producto (2.00€)
                stmt.execute("DELETE FROM product_prices WHERE product_id = 999");
                stmt.execute("INSERT INTO product_prices (product_id, price_list_id, price, start_date, reason) " +
                        "VALUES (999, 1, 2.00, NOW(), 'Precio de prueba para promociones')");

                // 4. Limpiar promociones previas
                stmt.execute("DELETE FROM promotion_items WHERE item_id = 999");
                stmt.execute("DELETE FROM promotions WHERE name LIKE 'TEST PROMO%'");

                // 5. Crear Promoción 2x1 (VOLUME_DISCOUNT)
                stmt.execute(
                        "INSERT INTO promotions (name, description, type, value, active, scope, buy_qty, free_qty) " +
                                "VALUES ('TEST PROMO 2x1', 'Llévate dos y paga solo una', 'VOLUME_DISCOUNT', 0, 1, 'PRODUCT', 1, 1)");

                // Obtener ID (usaremos una subquery para simplificar)
                stmt.execute("INSERT INTO promotion_items (promotion_id, item_id) " +
                        "SELECT promotion_id, 999 FROM promotions WHERE name = 'TEST PROMO 2x1' LIMIT 1");

                // 6. Crear Promoción de Categoría (10% extra)
                stmt.execute("INSERT INTO promotions (name, description, type, value, active, scope) " +
                        "VALUES ('TEST PROMO CAT 10%', '10% de descuento extra en toda la categoría pruebas', 'PERCENTAGE_DISCOUNT', 10.0, 1, 'CATEGORY')");

                stmt.execute("INSERT INTO promotion_items (promotion_id, item_id) " +
                        "SELECT promotion_id, 999 FROM promotions WHERE name = 'TEST PROMO CAT 10%' LIMIT 1");

                conn.commit();
                System.out.println("=== CONFIGURACIÓN COMPLETADA CON ÉXITO ===");
                System.out.println("Busca 'Soda de Prueba (2x1)' en el TPV y añade 2 unidades.");
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("!!! ERROR DURANTE LA CONFIGURACIÓN !!!");
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
