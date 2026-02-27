package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.domain.repository.ICategoryRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcCategoryRepository implements ICategoryRepository {

    private void ensureFavoriteColumnExists(Connection connection) {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE categories ADD COLUMN is_favorite BOOLEAN DEFAULT 0");
        } catch (SQLException e) {
            // Column likely exists or other harmless error
        }
    }

    @Override
    public List<Category> getAll() throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM categories";

        try (Connection connection = DBConnection.getConnection()) {
            ensureFavoriteColumnExists(connection);
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
        }
        return categories;
    }

    @Override
    public List<Category> getAllVisible() throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM categories WHERE visible = TRUE";
        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
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

    @Override
    public List<Category> getFavorites() throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM categories WHERE visible = 1 AND is_favorite = 1";
        try (Connection connection = DBConnection.getConnection()) {
            ensureFavoriteColumnExists(connection);
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
        }
        return categories;
    }

    @Override
    public void save(Category category) throws SQLException {
        String sql = "INSERT INTO categories (name, visible, is_favorite) VALUES (?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, category.getName());
            pstmt.setBoolean(2, category.isVisible());
            pstmt.setBoolean(3, category.isFavorite());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void update(Category category) throws SQLException {
        String sql = "UPDATE categories SET name = ?, visible = ?, is_favorite = ? WHERE category_id = ?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, category.getName());
            pstmt.setBoolean(2, category.isVisible());
            pstmt.setBoolean(3, category.isFavorite());
            pstmt.setInt(4, category.getId());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM categories WHERE category_id = ?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM categories";
        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}
