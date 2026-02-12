/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.ventacontrolfx.service;

/**
 *
 * @author PracticasSoftware1
 */
import com.mycompany.ventacontrolfx.dao.DBConnection;
import com.mycompany.ventacontrolfx.dao.ProductDAO;
import com.mycompany.ventacontrolfx.model.Product;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ProductService {

    public List<Product> getAllProducts() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            ProductDAO productDAO = new ProductDAO(conn);
            return productDAO.getAllProducts();
        }
    }

    public void addProduct(Product product) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            ProductDAO productDAO = new ProductDAO(conn);
            productDAO.addProduct(product);
        }
    }

    public void updateProduct(Product product) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            ProductDAO productDAO = new ProductDAO(conn);
            productDAO.updateProduct(product);
        }
    }

    public void deleteProduct(int productId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            ProductDAO productDAO = new ProductDAO(conn);
            productDAO.deleteProduct(productId);
        }
    }
}
