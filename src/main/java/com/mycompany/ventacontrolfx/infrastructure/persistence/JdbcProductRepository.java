package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.dao.DBConnection;
import com.mycompany.ventacontrolfx.dao.ProductDAO;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import com.mycompany.ventacontrolfx.model.Product;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * JDBC Implementation of the Product Repository.
 */
public class JdbcProductRepository implements IProductRepository {

    @Override
    public List<Product> findAll() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return new ProductDAO(conn).getAllProducts();
        }
    }

    @Override
    public List<Product> findVisible() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return new ProductDAO(conn).getAllVisibleProducts();
        }
    }

    @Override
    public void save(Product product) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            new ProductDAO(conn).addProduct(product);
        }
    }

    @Override
    public void update(Product product) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            new ProductDAO(conn).updateProduct(product);
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            new ProductDAO(conn).deleteProduct(id);
        }
    }

    @Override
    public int countTotal() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return new ProductDAO(conn).getCount();
        }
    }
}
