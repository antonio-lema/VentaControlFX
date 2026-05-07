package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.domain.repository.ICategoryRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcCategoryRepository implements ICategoryRepository {

    @Override
    public List<Category> getAll() throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT c.*, tg.name as tax_group_name FROM categories c LEFT JOIN tax_groups tg ON c.tax_group_id = tg.tax_group_id";

        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                boolean isFavorite = rs.getBoolean("is_favorite");
                double defaultIva = rs.getDouble("default_iva");

                Integer parentCategoryId = rs.getInt("parent_category_id");
                if (rs.wasNull())
                    parentCategoryId = null;

                Category category = new Category(
                        rs.getInt("category_id"),
                        rs.getString("name"),
                        rs.getBoolean("visible"),
                        isFavorite,
                        defaultIva);
                category.setParentCategoryId(parentCategoryId);

                int taxGroupId = rs.getInt("tax_group_id");
                if (!rs.wasNull()) {
                    category.setTaxGroupId(taxGroupId);
                    category.setTaxGroupName(rs.getString("tax_group_name"));
                }
                categories.add(category);
            }
        }
        return categories;
    }

    @Override
    public List<Category> getAllVisible() throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT c.*, tg.name as tax_group_name FROM categories c LEFT JOIN tax_groups tg ON c.tax_group_id = tg.tax_group_id WHERE c.visible = TRUE";
        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                boolean isFavorite = rs.getBoolean("is_favorite");
                double defaultIva = rs.getDouble("default_iva");

                Integer parentCategoryId = rs.getInt("parent_category_id");
                if (rs.wasNull())
                    parentCategoryId = null;

                Category category = new Category(
                        rs.getInt("category_id"),
                        rs.getString("name"),
                        rs.getBoolean("visible"),
                        isFavorite,
                        defaultIva);
                category.setParentCategoryId(parentCategoryId);

                int taxGroupId = rs.getInt("tax_group_id");
                if (!rs.wasNull()) {
                    category.setTaxGroupId(taxGroupId);
                    category.setTaxGroupName(rs.getString("tax_group_name"));
                }
                categories.add(category);
            }
        }
        return categories;
    }

    @Override
    public List<Category> getFavorites() throws SQLException {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT c.*, tg.name as tax_group_name FROM categories c LEFT JOIN tax_groups tg ON c.tax_group_id = tg.tax_group_id WHERE c.visible = 1 AND c.is_favorite = 1";
        try (Connection connection = DBConnection.getConnection();
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                double defaultIva = rs.getDouble("default_iva");

                Category category = new Category(
                        rs.getInt("category_id"),
                        rs.getString("name"),
                        rs.getBoolean("visible"),
                        rs.getBoolean("is_favorite"),
                        defaultIva);

                Integer parentCategoryId = rs.getInt("parent_category_id");
                if (!rs.wasNull()) {
                    category.setParentCategoryId(parentCategoryId);
                }

                int taxGroupId = rs.getInt("tax_group_id");
                if (!rs.wasNull()) {
                    category.setTaxGroupId(taxGroupId);
                    category.setTaxGroupName(rs.getString("tax_group_name"));
                }
                categories.add(category);
            }
        }
        return categories;
    }

    @Override
    public void save(Category category) throws SQLException {
        String sql = "INSERT INTO categories (name, visible, is_favorite, default_iva, tax_rate, tax_group_id, parent_category_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, category.getName());
            pstmt.setBoolean(2, category.isVisible());
            pstmt.setBoolean(3, category.isFavorite());
            pstmt.setDouble(4, category.getDefaultIva());
            pstmt.setDouble(5, category.getDefaultIva()); // Compatibility
            if (category.getTaxGroupId() != null && category.getTaxGroupId() > 0) {
                pstmt.setInt(6, category.getTaxGroupId());
            } else {
                pstmt.setNull(6, Types.INTEGER);
            }
            if (category.getParentCategoryId() != null) {
                pstmt.setInt(7, category.getParentCategoryId());
            } else {
                pstmt.setNull(7, Types.INTEGER);
            }
            pstmt.executeUpdate();
        }
    }

    @Override
    public void update(Category category) throws SQLException {
        String sql = "UPDATE categories SET name = ?, visible = ?, is_favorite = ?, default_iva = ?, tax_rate = ?, tax_group_id = ?, parent_category_id = ? WHERE category_id = ?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, category.getName());
            pstmt.setBoolean(2, category.isVisible());
            pstmt.setBoolean(3, category.isFavorite());
            pstmt.setDouble(4, category.getDefaultIva());
            pstmt.setDouble(5, category.getDefaultIva()); // Compatibility
            if (category.getTaxGroupId() != null && category.getTaxGroupId() > 0) {
                pstmt.setInt(6, category.getTaxGroupId());
            } else {
                pstmt.setNull(6, Types.INTEGER);
            }
            if (category.getParentCategoryId() != null) {
                pstmt.setInt(7, category.getParentCategoryId());
            } else {
                pstmt.setNull(7, Types.INTEGER);
            }
            pstmt.setInt(8, category.getId());
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

    @Override
    public Category getById(int id) throws SQLException {
        String sql = "SELECT c.*, tg.name as tax_group_name FROM categories c LEFT JOIN tax_groups tg ON c.tax_group_id = tg.tax_group_id WHERE c.category_id = ?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    boolean isFavorite = rs.getBoolean("is_favorite");
                    double defaultIva = rs.getDouble("default_iva");

                    Integer parentCategoryId = rs.getInt("parent_category_id");
                    if (rs.wasNull())
                        parentCategoryId = null;

                    Category category = new Category(
                            rs.getInt("category_id"),
                            rs.getString("name"),
                            rs.getBoolean("visible"),
                            isFavorite,
                            defaultIva);
                    category.setParentCategoryId(parentCategoryId);

                    int taxGroupId = rs.getInt("tax_group_id");
                    if (!rs.wasNull()) {
                        category.setTaxGroupId(taxGroupId);
                        category.setTaxGroupName(rs.getString("tax_group_name"));
                    }
                    return category;
                }
            }
        }
        return null;
    }
}

