package com.mycompany.ventacontrolfx.dao;

import com.mycompany.ventacontrolfx.model.Product;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    private Connection connection;

    public ProductDAO(Connection connection) {
        this.connection = connection;
    }

    public List<Product> getAllProducts() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name FROM products p LEFT JOIN categories c ON p.category_id = c.category_id";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Product product = new Product(
                        rs.getInt("product_id"),
                        rs.getInt("category_id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getBoolean("is_favorite"),
                        rs.getBoolean("visible"),
                        rs.getString("image_path"),
                        rs.getString("category_name"));
                products.add(product);
            }
        }
        return products;
    }

    public void addProduct(Product product) throws SQLException {
        String sql = "INSERT INTO products (category_id, name, price, is_favorite, image_path, visible) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, product.getCategoryId());
            pstmt.setString(2, product.getName());
            pstmt.setDouble(3, product.getPrice());
            pstmt.setBoolean(4, product.isFavorite());
            pstmt.setString(5, product.getImagePath());
            pstmt.setBoolean(6, product.isVisible());
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    product.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public void updateProduct(Product product) throws SQLException {
        String sql = "UPDATE products SET category_id = ?, name = ?, price = ?, is_favorite = ?, image_path = ?, visible = ? WHERE product_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, product.getCategoryId());
            pstmt.setString(2, product.getName());
            pstmt.setDouble(3, product.getPrice());
            pstmt.setBoolean(4, product.isFavorite());
            pstmt.setString(5, product.getImagePath());
            pstmt.setBoolean(6, product.isVisible());
            pstmt.setInt(7, product.getId());
            pstmt.executeUpdate();
        }
    }

    public List<Product> getAllVisibleProducts() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name FROM products p LEFT JOIN categories c ON p.category_id = c.category_id WHERE p.visible = TRUE";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Product product = new Product(
                        rs.getInt("product_id"),
                        rs.getInt("category_id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getBoolean("is_favorite"),
                        rs.getBoolean("visible"),
                        rs.getString("image_path"),
                        rs.getString("category_name"));
                products.add(product);
            }
        }
        return products;
    }

    public void deleteProduct(int productId) throws SQLException {
        String sql = "DELETE FROM products WHERE product_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            pstmt.executeUpdate();
        }
    }

    public List<Product> getFavoriteVisibleProducts() throws SQLException {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name FROM products p LEFT JOIN categories c ON p.category_id = c.category_id WHERE p.visible = TRUE AND p.is_favorite = TRUE";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Product product = new Product(
                        rs.getInt("product_id"),
                        rs.getInt("category_id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getBoolean("is_favorite"),
                        rs.getBoolean("visible"),
                        rs.getString("image_path"),
                        rs.getString("category_name"));
                products.add(product);
            }
        }
        return products;
    }
}
