package com.mycompany.ventacontrolfx.service;

import com.mycompany.ventacontrolfx.dao.CategoryDAO;
import com.mycompany.ventacontrolfx.model.Category;
import com.mycompany.ventacontrolfx.dao.DBConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class CategoryService {

    public List<Category> getAllCategories() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            CategoryDAO categoryDAO = new CategoryDAO(conn);
            return categoryDAO.getAllCategories();
        }
    }

    public void addCategory(Category category) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            CategoryDAO categoryDAO = new CategoryDAO(conn);
            categoryDAO.addCategory(category);
        }
    }

    public void updateCategory(Category category) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            CategoryDAO categoryDAO = new CategoryDAO(conn);
            categoryDAO.updateCategory(category);
        }
    }

    public void deleteCategory(int categoryId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            CategoryDAO categoryDAO = new CategoryDAO(conn);
            categoryDAO.deleteCategory(categoryId);
        }
    }
}
