package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.model.VisibilityFilter;
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

    private java.util.List<Product> metadataCache = null;
    private long lastCacheUpdate = 0;
    private static final long CACHE_TTL = 300000; // 5 minutes

    public void invalidateCache() {
        metadataCache = null;
    }

    private java.util.List<Product> getCache() throws SQLException {
        if (metadataCache == null || (System.currentTimeMillis() - lastCacheUpdate) > CACHE_TTL) {
            metadataCache = repository.getBasicMetadata(); // Instant load
            lastCacheUpdate = System.currentTimeMillis();
        }
        return metadataCache;
    }

    public List<Product> getPaginatedProducts(String query, int limit, int offset, VisibilityFilter visibility)
            throws SQLException {
        // Optimized: if simple search, use cache first to get IDs, then query DB only
        // for those IDs (to get prices)
        // Or for now, just fallback to repository if complex, but use cache for basic
        // 'All' counts
        return repository.searchPaginated(query, limit, offset, -1, visibility);
    }

    public int getTotalProductCount(String query, VisibilityFilter visibility) throws SQLException {
        if ((query == null || query.isBlank()) && visibility == VisibilityFilter.VISIBLE) {
            return getCache().size();
        }
        return repository.countSearch(query, visibility);
    }
}
