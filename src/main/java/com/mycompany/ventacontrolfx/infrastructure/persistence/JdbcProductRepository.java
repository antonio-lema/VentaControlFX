package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcProductRepository implements IProductRepository {

    @Override
    public List<Product> getAll() throws SQLException {
        return getAll(-1); // Use default
    }

    @Override
    public List<Product> getAll(int priceListId) throws SQLException {
        List<Product> products = new ArrayList<>();
        String priceSubquery;
        if (priceListId > 0) {
            priceSubquery = "COALESCE((SELECT pp.price FROM product_prices pp WHERE pp.product_id = p.product_id AND pp.price_list_id = ? AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) ORDER BY pp.start_date DESC LIMIT 1), 0.0)";
        } else {
            priceSubquery = "COALESCE((SELECT pp.price FROM product_prices pp JOIN price_lists pl ON pp.price_list_id = pl.price_list_id WHERE pp.product_id = p.product_id AND pl.is_default = 1 AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) ORDER BY pp.start_date DESC LIMIT 1), 0.0)";
        }

        String sql = "SELECT p.*, c.name AS category_name, c.default_iva AS category_iva, " +
                priceSubquery + " AS current_price " +
                "FROM products p LEFT JOIN categories c ON p.category_id = c.category_id";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (priceListId > 0) {
                pstmt.setInt(1, priceListId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        }
        return products;
    }

    @Override
    public List<Product> getAllVisible() throws SQLException {
        return getAllVisible(-1);
    }

    @Override
    public List<Product> getAllVisible(int priceListId) throws SQLException {
        List<Product> products = new ArrayList<>();
        String priceSubquery;
        if (priceListId > 0) {
            priceSubquery = "COALESCE(" +
                    "(SELECT pp.price FROM product_prices pp WHERE pp.product_id = p.product_id AND pp.price_list_id = ? AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) ORDER BY pp.start_date DESC LIMIT 1), "
                    +
                    "(SELECT pp.price FROM product_prices pp JOIN price_lists pl ON pp.price_list_id = pl.price_list_id WHERE pp.product_id = p.product_id AND pl.is_default = 1 AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) ORDER BY pp.start_date DESC LIMIT 1), "
                    +
                    "0.0)";
        } else {
            priceSubquery = "COALESCE((SELECT pp.price FROM product_prices pp JOIN price_lists pl ON pp.price_list_id = pl.price_list_id WHERE pp.product_id = p.product_id AND pl.is_default = 1 AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) ORDER BY pp.start_date DESC LIMIT 1), 0.0)";
        }

        String sql = "SELECT p.*, c.name AS category_name, c.default_iva AS category_iva, " +
                priceSubquery + " AS current_price " +
                "FROM products p LEFT JOIN categories c ON p.category_id = c.category_id WHERE p.visible = TRUE";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (priceListId > 0) {
                pstmt.setInt(1, priceListId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        }
        return products;
    }

    @Override
    public List<Product> getFavorites() throws SQLException {
        return getFavorites(-1);
    }

    @Override
    public List<Product> getFavorites(int priceListId) throws SQLException {
        List<Product> products = new ArrayList<>();
        String priceSubquery;
        if (priceListId > 0) {
            priceSubquery = "COALESCE(" +
                    "(SELECT pp.price FROM product_prices pp WHERE pp.product_id = p.product_id AND pp.price_list_id = ? AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) ORDER BY pp.start_date DESC LIMIT 1), "
                    +
                    "(SELECT pp.price FROM product_prices pp JOIN price_lists pl ON pp.price_list_id = pl.price_list_id WHERE pp.product_id = p.product_id AND pl.is_default = 1 AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) ORDER BY pp.start_date DESC LIMIT 1), "
                    +
                    "0.0)";
        } else {
            priceSubquery = "COALESCE((SELECT pp.price FROM product_prices pp JOIN price_lists pl ON pp.price_list_id = pl.price_list_id WHERE pp.product_id = p.product_id AND pl.is_default = 1 AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) ORDER BY pp.start_date DESC LIMIT 1), 0.0)";
        }

        String sql = "SELECT p.*, c.name AS category_name, c.default_iva AS category_iva, " +
                priceSubquery + " AS current_price " +
                "FROM products p LEFT JOIN categories c ON p.category_id = c.category_id WHERE p.visible = TRUE AND p.is_favorite = TRUE";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (priceListId > 0) {
                pstmt.setInt(1, priceListId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        }
        return products;
    }

    @Override
    public void save(Product product) throws SQLException {
        // ... (existing save code remains unchanged)
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Transacción para doble escritura

            String sql = "INSERT INTO products (category_id, name, is_favorite, image_path, visible, iva, tax_rate, tax_group_id, sku, cost_price, is_active, stock_quantity, min_stock, manage_stock) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, product.getCategoryId());
                pstmt.setString(2, product.getName());
                pstmt.setBoolean(3, product.isFavorite());
                pstmt.setString(4, product.getImagePath());
                pstmt.setBoolean(5, product.isVisible());

                Double iva = product.getIva();
                if (iva != null) {
                    pstmt.setDouble(6, iva);
                    pstmt.setDouble(7, iva); // Compatibility
                } else {
                    pstmt.setNull(6, Types.DOUBLE);
                    pstmt.setNull(7, Types.DOUBLE);
                }
                if (product.getTaxGroupId() != null && product.getTaxGroupId() > 0) {
                    pstmt.setInt(8, product.getTaxGroupId());
                } else {
                    pstmt.setNull(8, Types.INTEGER);
                }
                pstmt.setString(9, product.getSku());
                pstmt.setDouble(10, product.getCostPrice());
                pstmt.setBoolean(11, product.isActive());
                pstmt.setInt(12, product.getStockQuantity());
                pstmt.setInt(13, product.getMinStock());
                pstmt.setBoolean(14, product.isManageStock());
                pstmt.executeUpdate();

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        product.setId(generatedKeys.getInt(1));
                    }
                }

                // Insertar en la estructura nueva product_prices de forma transparente
                if (product.getId() > 0) {
                    // Obtener ID lista default (fallback 1)
                    int defaultPriceListId = 1;
                    try (Statement stmtList = conn.createStatement();
                            ResultSet rsList = stmtList.executeQuery(
                                    "SELECT price_list_id FROM price_lists WHERE is_default = 1 LIMIT 1")) {
                        if (rsList.next()) {
                            defaultPriceListId = rsList.getInt(1);
                        }
                    } catch (Exception ignored) {
                    }

                    String priceSql = "INSERT INTO product_prices (product_id, price_list_id, price, start_date, reason) VALUES (?, ?, ?, NOW(), 'Creación de producto')";
                    try (PreparedStatement pstmtPrice = conn.prepareStatement(priceSql)) {
                        pstmtPrice.setInt(1, product.getId());
                        pstmtPrice.setInt(2, defaultPriceListId);
                        pstmtPrice.setDouble(3, product.getPrice());
                        pstmtPrice.executeUpdate();
                    }
                }
            } // Close pstmt
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public void saveAll(List<Product> products) throws SQLException {
        if (products == null || products.isEmpty())
            return;

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Insertar productos en lote
            String sql = "INSERT INTO products (category_id, name, is_favorite, image_path, visible, iva, tax_rate, tax_group_id, sku, cost_price, is_active, stock_quantity, min_stock, manage_stock) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (Product product : products) {
                    pstmt.setInt(1, product.getCategoryId());
                    pstmt.setString(2, product.getName());
                    pstmt.setBoolean(3, product.isFavorite());
                    pstmt.setString(4, product.getImagePath());
                    pstmt.setBoolean(5, product.isVisible());

                    Double iva = product.getIva();
                    if (iva != null) {
                        pstmt.setDouble(6, iva);
                        pstmt.setDouble(7, iva);
                    } else {
                        pstmt.setNull(6, Types.DOUBLE);
                        pstmt.setNull(7, Types.DOUBLE);
                    }
                    if (product.getTaxGroupId() != null && product.getTaxGroupId() > 0) {
                        pstmt.setInt(8, product.getTaxGroupId());
                    } else {
                        pstmt.setNull(8, Types.INTEGER);
                    }
                    pstmt.setString(9, product.getSku());
                    pstmt.setDouble(10, product.getCostPrice());
                    pstmt.setBoolean(11, product.isActive());
                    pstmt.setInt(12, product.getStockQuantity());
                    pstmt.setInt(13, product.getMinStock());
                    pstmt.setBoolean(14, product.isManageStock());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();

                // Obtener IDs generados
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    int i = 0;
                    while (generatedKeys.next() && i < products.size()) {
                        products.get(i).setId(generatedKeys.getInt(1));
                        i++;
                    }
                }
            }

            // 2. Obtener ID de lista de precios por defecto una sola vez
            int defaultPriceListId = 1;
            try (Statement stmtList = conn.createStatement();
                    ResultSet rsList = stmtList.executeQuery(
                            "SELECT price_list_id FROM price_lists WHERE is_default = 1 LIMIT 1")) {
                if (rsList.next()) {
                    defaultPriceListId = rsList.getInt(1);
                }
            } catch (Exception ignored) {
            }

            // 3. Insertar precios en lote
            String priceSql = "INSERT INTO product_prices (product_id, price_list_id, price, start_date, reason) VALUES (?, ?, ?, NOW(), 'Importación masiva')";
            try (PreparedStatement pstmtPrice = conn.prepareStatement(priceSql)) {
                for (Product product : products) {
                    if (product.getId() > 0) {
                        pstmtPrice.setInt(1, product.getId());
                        pstmtPrice.setInt(2, defaultPriceListId);
                        pstmtPrice.setDouble(3, product.getPrice());
                        pstmtPrice.addBatch();
                    }
                }
                pstmtPrice.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null)
                conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    @Override
    public void update(Product product) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Transacción para doble escritura

            String sql = "UPDATE products SET category_id = ?, name = ?, is_favorite = ?, image_path = ?, visible = ?, iva = ?, tax_rate = ?, tax_group_id = ?, sku = ?, cost_price = ?, is_active = ?, stock_quantity = ?, min_stock = ?, manage_stock = ? WHERE product_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, product.getCategoryId());
                pstmt.setString(2, product.getName());
                pstmt.setBoolean(3, product.isFavorite());
                pstmt.setString(4, product.getImagePath());
                pstmt.setBoolean(5, product.isVisible());

                Double iva = product.getIva();
                if (iva != null) {
                    pstmt.setDouble(6, iva);
                    pstmt.setDouble(7, iva); // Compatibility
                } else {
                    pstmt.setNull(6, Types.DOUBLE);
                    pstmt.setNull(7, Types.DOUBLE);
                }
                if (product.getTaxGroupId() != null && product.getTaxGroupId() > 0) {
                    pstmt.setInt(8, product.getTaxGroupId());
                } else {
                    pstmt.setNull(8, Types.INTEGER);
                }
                pstmt.setString(9, product.getSku());
                pstmt.setDouble(10, product.getCostPrice());
                pstmt.setBoolean(11, product.isActive());
                pstmt.setInt(12, product.getStockQuantity());
                pstmt.setInt(13, product.getMinStock());
                pstmt.setBoolean(14, product.isManageStock());
                pstmt.setInt(15, product.getId());
                pstmt.executeUpdate();
            }

            // --- LÓGICA DE DOBLE ESCRITURA PARA product_prices ---
            // 1. Obtener ID de la lista por defecto
            int defaultPriceListId = 1;
            try (Statement stmtList = conn.createStatement();
                    ResultSet rsList = stmtList
                            .executeQuery("SELECT price_list_id FROM price_lists WHERE is_default = 1 LIMIT 1")) {
                if (rsList.next()) {
                    defaultPriceListId = rsList.getInt(1);
                }
            } catch (Exception ignored) {
            }

            // 2. Comprobar si el precio actual es distinto (o simplemente cerrar el actual
            // e insertar nuevo)
            // Para evitar basura, vamos a comprobar qué precio activo hay
            double activePrice = -1;
            boolean hasActive = false;
            String selectSql = "SELECT price FROM product_prices WHERE product_id = ? AND price_list_id = ? AND end_date IS NULL LIMIT 1";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, product.getId());
                selectStmt.setInt(2, defaultPriceListId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        hasActive = true;
                        activePrice = rs.getDouble(1);
                    }
                }
            }

            // Si el precio cambió, generamos el histórico
            if (!hasActive || activePrice != product.getPrice()) {
                if (hasActive) {
                    String updateOld = "UPDATE product_prices SET end_date = NOW(), reason = 'Cambio individual de tarifa' WHERE product_id = ? AND price_list_id = ? AND end_date IS NULL";
                    try (PreparedStatement updStmt = conn.prepareStatement(updateOld)) {
                        updStmt.setInt(1, product.getId());
                        updStmt.setInt(2, defaultPriceListId);
                        updStmt.executeUpdate();
                    }
                }
                String insertNew = "INSERT INTO product_prices (product_id, price_list_id, price, start_date, end_date, reason) VALUES (?, ?, ?, NOW(), NULL, 'Actualización de producto')";
                try (PreparedStatement insStmt = conn.prepareStatement(insertNew)) {
                    insStmt.setInt(1, product.getId());
                    insStmt.setInt(2, defaultPriceListId);
                    insStmt.setDouble(3, product.getPrice());
                    insStmt.executeUpdate();
                }
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public Product getById(int id) throws SQLException {
        String sql = "SELECT p.*, c.name AS category_name, c.default_iva AS category_iva, " +
                "COALESCE((SELECT pp.price FROM product_prices pp JOIN price_lists pl ON pp.price_list_id = pl.price_list_id WHERE pp.product_id = p.product_id AND pl.is_default = 1 AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) ORDER BY pp.start_date DESC LIMIT 1), 0.0) AS current_price "
                +
                "FROM products p LEFT JOIN categories c ON p.category_id = c.category_id WHERE p.product_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduct(rs);
                }
            }
        }
        return null;
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM products WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void updateVisibilityByCategory(int categoryId, boolean visible) throws SQLException {
        String sql = "UPDATE products SET visible = ? WHERE category_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, visible);
            pstmt.setInt(2, categoryId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void updateVisibility(int id, boolean visible) throws SQLException {
        String sql = "UPDATE products SET visible = ? WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, visible);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void updateFavorite(int id, boolean favorite) throws SQLException {
        String sql = "UPDATE products SET is_favorite = ? WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, favorite);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void updateTaxRateByCategory(int categoryId, double taxRate) throws SQLException {
        String sql = "UPDATE products SET iva = ?, tax_rate = ? WHERE category_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, taxRate);
            pstmt.setDouble(2, taxRate); // Compatibility
            pstmt.setInt(3, categoryId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void updateTaxRateToAll(double taxRate) throws SQLException {
        String sql = "UPDATE products SET iva = ?, tax_rate = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, taxRate);
            pstmt.setDouble(2, taxRate); // Compatibility
            pstmt.executeUpdate();
        }
    }

    @Override
    public void updateTaxGroupByCategory(int categoryId, int taxGroupId) throws SQLException {
        String sql = "UPDATE products SET tax_group_id = ? WHERE category_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, taxGroupId);
            pstmt.setInt(2, categoryId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void updateTaxGroupToAll(int taxGroupId) throws SQLException {
        String sql = "UPDATE products SET tax_group_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, taxGroupId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void updateTaxGroupForProducts(List<Integer> productIds, int taxGroupId) throws SQLException {
        if (productIds == null || productIds.isEmpty())
            return;

        String sql = "UPDATE products SET tax_group_id = ? WHERE product_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                for (Integer id : productIds) {
                    pstmt.setInt(1, taxGroupId);
                    pstmt.setInt(2, id);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public int updateStock(int productId, int quantityDelta, Connection conn) throws SQLException {
        String updateSql = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE product_id = ? AND manage_stock = TRUE";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setInt(1, quantityDelta);
            pstmt.setInt(2, productId);
            pstmt.executeUpdate();
        }

        // Recuperar el nuevo stock para devolverlo (MySQL no soporta UPDATE ...
        // RETURNING)
        String selectSql = "SELECT stock_quantity FROM products WHERE product_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock_quantity");
                }
            }
        }
        return 0;
    }

    @Override
    public List<Product> getLowStock() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name, c.default_iva AS category_iva, " +
                "COALESCE((SELECT pp.price FROM product_prices pp JOIN price_lists pl ON pp.price_list_id = pl.price_list_id WHERE pp.product_id = p.product_id AND pl.is_default = 1 AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) ORDER BY pp.start_date DESC LIMIT 1), 0.0) AS current_price "
                +
                "FROM products p LEFT JOIN categories c ON p.category_id = c.category_id " +
                "WHERE p.manage_stock = TRUE AND p.stock_quantity <= p.min_stock";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        }
        return products;
    }

    @Override
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM products";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        double ivaVal = rs.getDouble("iva");
        Double iva = rs.wasNull() ? null : ivaVal;

        if (iva == null) {
            try {
                double taxRateVal = rs.getDouble("tax_rate");
                if (!rs.wasNull()) {
                    iva = taxRateVal;
                }
            } catch (SQLException e) {
                // tax_rate doesn't exist
            }
        }

        Double categoryIva = null;
        try {
            double catIvaVal = rs.getDouble("category_iva");
            if (!rs.wasNull()) {
                categoryIva = catIvaVal;
            }
        } catch (SQLException e) {
            // column missing in legacy queries or joins
        }

        Double calculatedPrice = 0.0;
        try {
            double currPriceVal = rs.getDouble("current_price");
            if (!rs.wasNull()) {
                calculatedPrice = currPriceVal;
            }
        } catch (SQLException e) {
            // column missing
        }

        Product p = new Product(
                rs.getInt("product_id"),
                rs.getInt("category_id"),
                rs.getString("name"),
                calculatedPrice != null ? calculatedPrice.doubleValue() : 0.0,
                rs.getBoolean("is_favorite"),
                rs.getBoolean("visible"),
                rs.getString("image_path"),
                rs.getString("category_name"),
                iva,
                categoryIva,
                rs.getString("sku"),
                rs.getDouble("cost_price"),
                rs.getBoolean("is_active"),
                rs.getInt("stock_quantity"),
                rs.getInt("min_stock"),
                rs.getBoolean("manage_stock"));
        p.setCurrentPrice(calculatedPrice != null ? calculatedPrice.doubleValue() : 0.0);

        try {
            int taxGroupId = rs.getInt("tax_group_id");
            if (!rs.wasNull())
                p.setTaxGroupId(taxGroupId);
        } catch (SQLException e) {
        }
        try {
            p.setSku(rs.getString("sku"));
        } catch (SQLException e) {
        }
        try {
            p.setCostPrice(rs.getDouble("cost_price"));
        } catch (SQLException e) {
        }

        return p;
    }

}
