package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import com.mycompany.ventacontrolfx.shared.bus.GlobalEventBus;
import java.sql.SQLException;
import java.util.List;

public class ProductUseCase {
    private final IProductRepository repository;
    private final com.mycompany.ventacontrolfx.util.AuthorizationService authService;
    private final GlobalEventBus eventBus;

    public ProductUseCase(IProductRepository repository,
            com.mycompany.ventacontrolfx.util.AuthorizationService authService,
            GlobalEventBus eventBus) {
        this.repository = repository;
        this.authService = authService;
        this.eventBus = eventBus;
    }

    public List<Product> getAllProducts() throws SQLException {
        return repository.getAll();
    }

    public List<Product> getAllProducts(int priceListId) throws SQLException {
        return repository.getAll(priceListId);
    }

    public List<Product> getVisibleProducts() throws SQLException {
        return repository.getAllVisible();
    }

    public List<Product> getVisibleProducts(int priceListId) throws SQLException {
        return repository.getAllVisible(priceListId);
    }

    public List<Product> getFavorites() throws SQLException {
        return repository.getFavorites();
    }

    public List<Product> getFavorites(int priceListId) throws SQLException {
        return repository.getFavorites(priceListId);
    }

    public void saveProduct(Product product) throws SQLException {
        authService.checkPermission("PRODUCTOS");
        if (product.getId() == 0) {
            repository.save(product);
        } else {
            repository.update(product);
        }
        if (eventBus != null)
            eventBus.publishDataChange();
    }

    public void deleteProduct(int id) throws SQLException {
        authService.checkPermission("PRODUCTOS");
        repository.delete(id);
        if (eventBus != null)
            eventBus.publishDataChange();
    }

    public void toggleFavorite(int productId, boolean favorite) throws SQLException {
        authService.checkPermission("PRODUCTOS");
        repository.updateFavorite(productId, favorite);
        if (eventBus != null)
            eventBus.publishDataChange();
    }

    public void toggleVisibility(int productId, boolean visible) throws SQLException {
        authService.checkPermission("PRODUCTOS");
        repository.updateVisibility(productId, visible);
        if (eventBus != null)
            eventBus.publishDataChange();
    }

    public List<Product> getPaginatedProducts(String query, int limit, int offset) throws SQLException {
        return repository.searchPaginated(query, limit, offset, -1, false);
    }

    public int getTotalProductCount(String query) throws SQLException {
        return repository.countSearch(query, false);
    }
}
