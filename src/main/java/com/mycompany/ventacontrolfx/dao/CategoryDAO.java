package com.mycompany.ventacontrolfx.dao;

import com.mycompany.ventacontrolfx.model.Category;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {
    private Connection connection;

    public CategoryDAO(Connection connection) {
        this.connection = connection;
    }

    private void ensureFavoriteColumnExists() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE categories ADD COLUMN is_favorite BOOLEAN DEFAULT 0");
        } catch (SQLException e) {
            // Column likely exists or other harmless error
        }
    }

    public List<Category> getAllCategories() throws SQLException {
        ensureFavoriteColumnExists();
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM categories";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                boolean isFavorite = false;
                try {
                    isFavorite = rs.getBoolean("is_favorite");
                } catch (SQLException e) {
                    // ignore if column issue, default false
                }

                Category category = new Category(
                        rs.getInt("category_id"),
                        rs.getString("name"),
                        rs.getBoolean("visible"),
                        isFavorite);
                categories.add(category);
            }
        }
        return categories;
    }

    public List<Category> getFavoriteCategories() throws SQLException {
        ensureFavoriteColumnExists();
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM categories WHERE visible = 1 AND is_favorite = 1";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                categories.add(new Category(
                        rs.getInt("category_id"),
                        rs.getString("name"),
                        rs.getBoolean("visible"),
                        rs.getBoolean("is_favorite")));
            }
        }
        return categories;
    }

    public void addCategory(Category category) throws SQLException {
        String sql = "INSERT INTO categories (name, visible, is_favorite) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, category.getName());
            pstmt.setBoolean(2, category.isVisible());
            pstmt.setBoolean(3, category.isFavorite());
            pstmt.executeUpdate();
        }
    }

    public void updateCategory(Category category) throws SQLException {
        String sql = "UPDATE categories SET name = ?, visible = ?, is_favorite = ? WHERE category_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, category.getName());
            pstmt.setBoolean(2, category.isVisible());
            pstmt.setBoolean(3, category.isFavorite());
            pstmt.setInt(4, category.getId());
            pstmt.executeUpdate();
        }
    }

    public List<Category> getAllVisibleCategories() throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM categories WHERE visible = TRUE";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                boolean isFavorite = false;
                try {
                    isFavorite = rs.getBoolean("is_favorite");
                } catch (SQLException e) {
                    // ignore
                }

                Category category = new Category(
                        rs.getInt("category_id"),
                        rs.getString("name"),
                        rs.getBoolean("visible"),
                        isFavorite);
                categories.add(category);
            }
        }
        return categories;
    }

    public void deleteCategory(int categoryId) throws SQLException {
        String sql = "DELETE FROM categories WHERE category_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, categoryId);
            pstmt.executeUpdate();
        }
    }
}
