package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.model.Product;
import java.sql.SQLException;
import java.util.List;

/**
 * Domain Repository Interface for Products.
 * Pure business contract, detached from SQL/JDBC details.
 */
public interface IProductRepository {
    List<Product> findVisible() throws SQLException;

    List<Product> findAll() throws SQLException;

    int countTotal() throws SQLException;

    void save(Product product) throws SQLException;

    void update(Product product) throws SQLException;

    void delete(int id) throws SQLException;
}
