package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.Product;
import java.sql.SQLException;
import java.util.List;

public interface IProductRepository {
    List<Product> getAll() throws SQLException;

    List<Product> getAllVisible() throws SQLException;

    List<Product> getFavorites() throws SQLException;

    void save(Product product) throws SQLException;

    void update(Product product) throws SQLException;

    void delete(int id) throws SQLException;

    int count() throws SQLException;

    void updateFavorite(int productId, boolean favorite) throws SQLException;

    void updateVisibility(int productId, boolean visible) throws SQLException;

    void updateVisibilityByCategory(int categoryId, boolean visible) throws SQLException;
}
