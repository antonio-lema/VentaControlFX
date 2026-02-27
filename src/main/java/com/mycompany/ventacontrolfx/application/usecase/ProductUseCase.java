package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import java.sql.SQLException;
import java.util.List;

public class ProductUseCase {
    private final IProductRepository repository;

    public ProductUseCase(IProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> getAllProducts() throws SQLException {
        return repository.getAll();
    }

    public List<Product> getVisibleProducts() throws SQLException {
        return repository.getAllVisible();
    }

    public List<Product> getFavorites() throws SQLException {
        return repository.getFavorites();
    }

    public void saveProduct(Product product) throws SQLException {
        if (product.getId() == 0) {
            repository.save(product);
        } else {
            repository.update(product);
        }
    }

    public void deleteProduct(int id) throws SQLException {
        repository.delete(id);
    }

    public void toggleFavorite(int productId, boolean favorite) throws SQLException {
        repository.updateFavorite(productId, favorite);
    }

    public void toggleVisibility(int productId, boolean visible) throws SQLException {
        repository.updateVisibility(productId, visible);
    }
}
