package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.model.VisibilityFilter;
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
        String priceSubquery = buildPriceSubquery(priceListId);

        String sql = "SELECT p.*, c.name AS category_name, c.default_iva AS category_iva, c.decimals AS category_decimals, " +
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
        String priceSubquery = buildPriceSubquery(priceListId);

        String sql = "SELECT p.*, c.name AS category_name, c.default_iva AS category_iva, c.decimals AS category_decimals, " +
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
    public List<Product> getByCategory(int categoryId, int priceListId) throws SQLException {
        List<Product> products = new ArrayList<>();
        String priceSubquery = buildPriceSubquery(priceListId);

        String sql = "SELECT p.*, c.name AS category_name, c.default_iva AS category_iva, " +
                priceSubquery + " AS current_price " +
                "FROM products p LEFT JOIN categories c ON p.category_id = c.category_id " +
                "WHERE p.visible = TRUE AND p.category_id = ? " +
                "LIMIT 100";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int paramIdx = 1;
            if (priceListId > 0) {
                pstmt.setInt(paramIdx++, priceListId);
            }
            pstmt.setInt(paramIdx, categoryId);

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
        // Redirigir al paginado con un l\u00edmite razonable por defecto si se usa sin
        // paginar
        return getFavoritesPaginated(1000, 0, priceListId);
    }

    @Override
    public List<Product> getFavoritesPaginated(int limit, int offset, int priceListId) throws SQLException {
        List<Product> products = new ArrayList<>();
        String priceSubquery = buildPriceSubquery(priceListId);

        String sql = "SELECT p.*, c.name AS category_name, c.default_iva AS category_iva, " +
                priceSubquery + " AS current_price " +
                "FROM products p LEFT JOIN categories c ON p.category_id = c.category_id " +
                "WHERE p.visible = TRUE AND p.is_favorite = TRUE LIMIT ? OFFSET ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            if (priceListId > 0) {
                pstmt.setInt(paramIndex++, priceListId);
            }
            pstmt.setInt(paramIndex++, limit);
            pstmt.setInt(paramIndex++, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        }
        return products;
    }

    @Override
    public int countFavorites() throws SQLException {
        String sql = "SELECT COUNT(*) FROM products WHERE visible = TRUE AND is_favorite = TRUE";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private String buildPriceSubquery(int priceListId) {
        if (priceListId > 0) {
            return "COALESCE(" +
                    "(SELECT pp.price FROM product_prices pp WHERE pp.product_id = p.product_id AND pp.price_list_id = ? AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) ORDER BY pp.start_date DESC LIMIT 1), "
                    +
                    "(SELECT pp.price FROM product_prices pp JOIN price_lists pl ON pp.price_list_id = pl.price_list_id WHERE pp.product_id = p.product_id AND pl.is_default = 1 AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) ORDER BY pp.start_date DESC LIMIT 1), "
                    +
                    "0.0)";
        } else {
            return "COALESCE((SELECT pp.price FROM product_prices pp JOIN price_lists pl ON pp.price_list_id = pl.price_list_id WHERE pp.product_id = p.product_id AND pl.is_default = 1 AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) ORDER BY pp.start_date DESC LIMIT 1), 0.0)";
        }
    }

    @Override
    public void save(Product product) throws SQLException {
        // ... (existing save code remains unchanged)
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Transacci\u00f3n para doble escritura

            String sql = "INSERT INTO products (category_id, name, is_favorite, image_path, visible, iva, tax_group_id, sku, cost_price, is_active, stock_quantity, min_stock, manage_stock, decimals) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, product.getCategoryId());
                pstmt.setString(2, product.getName());
                pstmt.setBoolean(3, product.isFavorite());
                pstmt.setString(4, product.getImagePath());
                pstmt.setBoolean(5, product.isVisible());

                Double iva = product.getIva();
                if (iva != null) {
                    pstmt.setDouble(6, iva);
                } else {
                    pstmt.setNull(6, Types.DOUBLE);
                }
                if (product.getTaxGroupId() != null && product.getTaxGroupId() > 0) {
                    pstmt.setInt(7, product.getTaxGroupId());
                } else {
                    pstmt.setNull(7, Types.INTEGER);
                }
                pstmt.setString(8, product.getSku());
                pstmt.setDouble(9, product.getCostPrice());
                pstmt.setBoolean(10, product.isActive());
                pstmt.setInt(11, product.getStockQuantity());
                pstmt.setInt(12, product.getMinStock());
                pstmt.setBoolean(13, product.isManageStock());
                if (product.getDecimals() != null) {
                    pstmt.setInt(14, product.getDecimals());
                } else {
                    pstmt.setNull(14, Types.INTEGER);
                }
                pstmt.executeUpdate();

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        product.setId(generatedKeys.getInt(1));
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
            String sql = "INSERT INTO products (category_id, name, is_favorite, image_path, visible, iva, tax_group_id, sku, cost_price, is_active, stock_quantity, min_stock, manage_stock, decimals) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
                    } else {
                        pstmt.setNull(6, Types.DOUBLE);
                    }
                    if (product.getTaxGroupId() != null && product.getTaxGroupId() > 0) {
                        pstmt.setInt(7, product.getTaxGroupId());
                    } else {
                        pstmt.setNull(7, Types.INTEGER);
                    }
                    pstmt.setString(8, product.getSku());
                    pstmt.setDouble(9, product.getCostPrice());
                    pstmt.setBoolean(10, product.isActive());
                    pstmt.setInt(11, product.getStockQuantity());
                    pstmt.setInt(12, product.getMinStock());
                    pstmt.setBoolean(13, product.isManageStock());
                    if (product.getDecimals() != null) {
                        pstmt.setInt(14, product.getDecimals());
                    } else {
                        pstmt.setNull(14, Types.INTEGER);
                    }
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
            conn.setAutoCommit(false); // Transacci\u00f3n para doble escritura

            String sql = "UPDATE products SET category_id = ?, name = ?, is_favorite = ?, image_path = ?, visible = ?, iva = ?, tax_group_id = ?, sku = ?, cost_price = ?, is_active = ?, stock_quantity = ?, min_stock = ?, manage_stock = ?, decimals = ? WHERE product_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, product.getCategoryId());
                pstmt.setString(2, product.getName());
                pstmt.setBoolean(3, product.isFavorite());
                pstmt.setString(4, product.getImagePath());
                pstmt.setBoolean(5, product.isVisible());

                Double iva = product.getIva();
                if (iva != null) {
                    pstmt.setDouble(6, iva);
                } else {
                    pstmt.setNull(6, Types.DOUBLE);
                }
                if (product.getTaxGroupId() != null && product.getTaxGroupId() > 0) {
                    pstmt.setInt(7, product.getTaxGroupId());
                } else {
                    pstmt.setNull(7, Types.INTEGER);
                }
                pstmt.setString(8, product.getSku());
                pstmt.setDouble(9, product.getCostPrice());
                pstmt.setBoolean(10, product.isActive());
                pstmt.setInt(11, product.getStockQuantity());
                pstmt.setInt(12, product.getMinStock());
                pstmt.setBoolean(13, product.isManageStock());
                if (product.getDecimals() != null) {
                    pstmt.setInt(14, product.getDecimals());
                } else {
                    pstmt.setNull(14, Types.INTEGER);
                }
                pstmt.setInt(15, product.getId());
                pstmt.executeUpdate();

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
    public List<Product> getByCategoryPaginated(int categoryId, int limit, int offset, int priceListId)
            throws SQLException {
        List<Product> products = new ArrayList<>();
        String priceSubquery = buildPriceSubquery(priceListId);

        String sql = "SELECT p.*, c.name AS category_name, c.default_iva AS category_iva, " +
                priceSubquery + " AS current_price " +
                "FROM products p LEFT JOIN categories c ON p.category_id = c.category_id " +
                "WHERE p.visible = TRUE AND p.category_id = ? " +
                "LIMIT ? OFFSET ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int paramIdx = 1;
            if (priceListId > 0) {
                pstmt.setInt(paramIdx++, priceListId);
            }
            pstmt.setInt(paramIdx++, categoryId);
            pstmt.setInt(paramIdx++, limit);
            pstmt.setInt(paramIdx, offset);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        }
        return products;
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
        String sql = "UPDATE products SET iva = ? WHERE category_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, taxRate);
            pstmt.setInt(2, categoryId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void updateTaxRateToAll(double taxRate) throws SQLException {
        String sql = "UPDATE products SET iva = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, taxRate);
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
        // FIX: A\\u00f1adida validaci\\u00f3n at\\u00f3mica de stock suficiente en la
        // query para evitar stock negativo.
        String updateSql = "UPDATE products SET stock_quantity = stock_quantity + ? " +
                "WHERE product_id = ? AND manage_stock = TRUE " +
                "AND (stock_quantity + ? >= 0)";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setInt(1, quantityDelta);
            pstmt.setInt(2, productId);
            pstmt.setInt(3, quantityDelta);
            int updated = pstmt.executeUpdate();

            if (updated == 0) {
                // Verificar si es porque manage_stock es FALSE o por stock insuficiente
                String checkSql = "SELECT manage_stock, stock_quantity FROM products WHERE product_id = ?";
                try (PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
                    checkPstmt.setInt(1, productId);
                    try (ResultSet rs = checkPstmt.executeQuery()) {
                        if (rs.next() && rs.getBoolean("manage_stock") && quantityDelta < 0) {
                            throw new SQLException("ERROR_INSUFFICIENT_STOCK: " + rs.getInt("stock_quantity"));
                        }
                    }
                }
            }
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
        ResultSetMetaData rsmd = rs.getMetaData();
        
        // 1. IVA Handling (Unified)
        Double iva = null;
        if (hasColumn(rsmd, "iva")) {
            double val = rs.getDouble("iva");
            if (!rs.wasNull()) iva = val;
        }

        // 2. Category Snapshots
        Double categoryIva = null;
        if (hasColumn(rsmd, "category_iva")) {
            double val = rs.getDouble("category_iva");
            if (!rs.wasNull()) categoryIva = val;
        }

        Integer categoryDecimals = null;
        if (hasColumn(rsmd, "category_decimals")) {
            int val = rs.getInt("category_decimals");
            if (!rs.wasNull()) categoryDecimals = val;
        }

        // 3. Price Handling
        double calculatedPrice = 0.0;
        if (hasColumn(rsmd, "current_price")) {
            calculatedPrice = rs.getDouble("current_price");
        }

        // 4. Decimals Handling
        Integer decimals = null;
        if (hasColumn(rsmd, "decimals")) {
            int val = rs.getInt("decimals");
            if (!rs.wasNull()) decimals = val;
        }

        Product p = new Product(
                rs.getInt("product_id"),
                rs.getInt("category_id"),
                rs.getString("name"),
                calculatedPrice,
                rs.getBoolean("is_favorite"),
                rs.getBoolean("visible"),
                rs.getString("image_path"),
                hasColumn(rsmd, "category_name") ? rs.getString("category_name") : null,
                iva,
                categoryIva,
                hasColumn(rsmd, "sku") ? rs.getString("sku") : null,
                hasColumn(rsmd, "cost_price") ? rs.getDouble("cost_price") : 0.0,
                rs.getBoolean("is_active"),
                rs.getInt("stock_quantity"),
                rs.getInt("min_stock"),
                rs.getBoolean("manage_stock"),
                decimals);

        p.setCurrentPrice(calculatedPrice);
        p.setCategoryDecimals(categoryDecimals);

        if (hasColumn(rsmd, "tax_group_id")) {
            int tgId = rs.getInt("tax_group_id");
            if (!rs.wasNull()) p.setTaxGroupId(tgId);
        }

        return p;
    }

    private boolean hasColumn(ResultSetMetaData rsmd, String columnName) throws SQLException {
        int columns = rsmd.getColumnCount();
        for (int i = 1; i <= columns; i++) {
            if (columnName.equalsIgnoreCase(rsmd.getColumnLabel(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Product> searchPaginated(String query, int limit, int offset, int priceListId,
            VisibilityFilter visibility)
            throws SQLException {
        List<Product> products = new ArrayList<>();
        String priceSubquery = buildPriceSubquery(priceListId);

        StringBuilder whereClause = new StringBuilder();
        if (visibility == VisibilityFilter.VISIBLE) {
            whereClause.append(" WHERE p.visible = TRUE ");
        } else if (visibility == VisibilityFilter.DISABLED) {
            whereClause.append(" WHERE p.visible = FALSE ");
        }

        boolean hasQuery = query != null && !query.trim().isEmpty();
        String safeQuery = "";
        if (hasQuery) {
            if (whereClause.length() == 0)
                whereClause.append(" WHERE ");
            else
                whereClause.append(" AND ");

            whereClause.append(" (p.name LIKE ? OR c.name LIKE ? OR p.sku LIKE ?) ");
            safeQuery = "%" + query.trim() + "%";
        }

        String sql = "SELECT p.*, c.name AS category_name, c.default_iva AS category_iva, " +
                priceSubquery + " AS current_price " +
                "FROM products p LEFT JOIN categories c ON p.category_id = c.category_id " +
                whereClause.toString() +
                " ORDER BY p.name ASC LIMIT ? OFFSET ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            if (priceListId > 0) {
                pstmt.setInt(paramIndex++, priceListId);
            }
            if (hasQuery) {
                pstmt.setString(paramIndex++, safeQuery);
                pstmt.setString(paramIndex++, safeQuery);
                pstmt.setString(paramIndex++, safeQuery);
            }
            pstmt.setInt(paramIndex++, limit);
            pstmt.setInt(paramIndex++, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        }
        return products;
    }

    @Override
    public List<Product> getPromotedPaginated(List<Integer> productIds, List<Integer> categoryIds,
            boolean hasGlobalPromo, int limit, int offset, int priceListId) throws SQLException {
        if (hasGlobalPromo) {
            return searchPaginated("", limit, offset, priceListId, VisibilityFilter.VISIBLE);
        }

        if ((productIds == null || productIds.isEmpty()) && (categoryIds == null || categoryIds.isEmpty())) {
            return new ArrayList<>();
        }

        StringBuilder whereClause = new StringBuilder(" WHERE p.visible = TRUE AND (");
        boolean first = true;
        if (productIds != null && !productIds.isEmpty()) {
            whereClause.append("p.product_id IN (")
                    .append(productIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).get())
                    .append(")");
            first = false;
        }
        if (categoryIds != null && !categoryIds.isEmpty()) {
            if (!first)
                whereClause.append(" OR ");
            whereClause.append("p.category_id IN (")
                    .append(categoryIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).get())
                    .append(")");
        }
        whereClause.append(")");

        String priceSubquery = (priceListId > 0)
                ? "COALESCE((SELECT pp.price FROM product_prices pp WHERE pp.product_id = p.product_id AND pp.price_list_id = ? AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) ORDER BY pp.start_date DESC LIMIT 1), (SELECT pp.price FROM product_prices pp JOIN price_lists pl ON pp.price_list_id = pl.price_list_id WHERE pp.product_id = p.product_id AND pl.is_default = 1 AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) ORDER BY pp.start_date DESC LIMIT 1), 0.0)"
                : "COALESCE((SELECT pp.price FROM product_prices pp JOIN price_lists pl ON pp.price_list_id = pl.price_list_id WHERE pp.product_id = p.product_id AND pl.is_default = 1 AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) ORDER BY pp.start_date DESC LIMIT 1), 0.0)";

        String sql = "SELECT p.*, c.name AS category_name, c.default_iva AS category_iva, " +
                priceSubquery + " AS current_price " +
                "FROM products p LEFT JOIN categories c ON p.category_id = c.category_id " +
                whereClause.toString() +
                " ORDER BY p.name ASC LIMIT ? OFFSET ?";

        List<Product> products = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            if (priceListId > 0)
                pstmt.setInt(paramIndex++, priceListId);
            pstmt.setInt(paramIndex++, limit);
            pstmt.setInt(paramIndex++, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        }
        return products;
    }

    @Override
    public int countPromoted(List<Integer> productIds, List<Integer> categoryIds, boolean hasGlobalPromo)
            throws SQLException {
        if (hasGlobalPromo) {
            return countSearch("", VisibilityFilter.VISIBLE);
        }

        if ((productIds == null || productIds.isEmpty()) && (categoryIds == null || categoryIds.isEmpty())) {
            return 0;
        }

        StringBuilder whereClause = new StringBuilder(" WHERE p.visible = TRUE AND (");
        boolean first = true;
        if (productIds != null && !productIds.isEmpty()) {
            whereClause.append("p.product_id IN (")
                    .append(productIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).get())
                    .append(")");
            first = false;
        }
        if (categoryIds != null && !categoryIds.isEmpty()) {
            if (!first)
                whereClause.append(" OR ");
            whereClause.append("p.category_id IN (")
                    .append(categoryIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).get())
                    .append(")");
        }
        whereClause.append(")");

        String sql = "SELECT COUNT(*) FROM products p " + whereClause.toString();
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    @Override
    public int countSearch(String query, VisibilityFilter visibility) throws SQLException {
        StringBuilder whereClause = new StringBuilder();
        if (visibility == VisibilityFilter.VISIBLE) {
            whereClause.append(" WHERE p.visible = TRUE ");
        } else if (visibility == VisibilityFilter.DISABLED) {
            whereClause.append(" WHERE p.visible = FALSE ");
        }

        boolean hasQuery = query != null && !query.trim().isEmpty();
        String safeQuery = "";
        if (hasQuery) {
            if (whereClause.length() == 0)
                whereClause.append(" WHERE ");
            else
                whereClause.append(" AND ");

            whereClause.append(" (p.name LIKE ? OR c.name LIKE ? OR p.sku LIKE ?) ");
            safeQuery = "%" + query.trim() + "%";
        } else if (visibility == VisibilityFilter.ALL) {
            return count(); // R\u00c3\u0192\u00a1pido si no hay filtros y queremos todos
        }

        String sql = "SELECT COUNT(*) FROM products p LEFT JOIN categories c ON p.category_id = c.category_id "
                + whereClause.toString();

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (hasQuery) {
                pstmt.setString(1, safeQuery);
                pstmt.setString(2, safeQuery);
                pstmt.setString(3, safeQuery);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public int countByCategory(int categoryId, VisibilityFilter visibility) throws SQLException {
        String sql = "SELECT COUNT(*) FROM products WHERE category_id = ?";
        if (visibility == VisibilityFilter.VISIBLE) {
            sql += " AND visible = TRUE";
        } else if (visibility == VisibilityFilter.DISABLED) {
            sql += " AND visible = FALSE";
        }
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, categoryId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public Product findBySku(String sku) throws SQLException {
        String sql = "SELECT p.*, c.name AS category_name, c.default_iva AS category_iva, " +
                "COALESCE((SELECT pp.price FROM product_prices pp JOIN price_lists pl ON pp.price_list_id = pl.price_list_id WHERE pp.product_id = p.product_id AND pl.is_default = 1 AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) ORDER BY pp.start_date DESC LIMIT 1), 0.0) AS current_price "
                +
                "FROM products p LEFT JOIN categories c ON p.category_id = c.category_id WHERE p.sku = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sku);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduct(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Product> getBasicMetadata() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT product_id, category_id, name, sku, stock_quantity, image_path, is_favorite, visible, is_active FROM products WHERE visible = TRUE";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Product p = new Product();
                p.setId(rs.getInt("product_id"));
                p.setCategoryId(rs.getInt("category_id"));
                p.setName(rs.getString("name"));
                p.setSku(rs.getString("sku"));
                p.setStockQuantity(rs.getInt("stock_quantity"));
                p.setImagePath(rs.getString("image_path"));
                p.setFavorite(rs.getBoolean("is_favorite"));
                p.setVisible(rs.getBoolean("visible"));
                p.setActive(rs.getBoolean("is_active"));
                products.add(p);
            }
        }
        return products;
    }
}


