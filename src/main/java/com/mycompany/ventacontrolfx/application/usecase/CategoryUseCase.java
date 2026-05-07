package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.domain.repository.ICategoryRepository;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import java.sql.SQLException;
import java.util.List;

public class CategoryUseCase {
    private final ICategoryRepository repository;
    private final IProductRepository productRepository;
    private final com.mycompany.ventacontrolfx.infrastructure.security.AuthorizationService authService;

    public CategoryUseCase(ICategoryRepository repository,
            IProductRepository productRepository,
            com.mycompany.ventacontrolfx.infrastructure.security.AuthorizationService authService) {
        this.repository = repository;
        this.productRepository = productRepository;
        this.authService = authService;
    }

    public ICategoryRepository getRepository() {
        return repository;
    }

    public List<Category> getAll() throws SQLException {
        return repository.getAll();
    }

    public List<Category> getVisible() throws SQLException {
        return repository.getAllVisible();
    }

    public List<Category> getFavorites() throws SQLException {
        return repository.getFavorites();
    }

    public void addCategory(Category category) throws SQLException {
        authService.checkPermission("PRODUCTOS");
        repository.save(category);
    }

    public void updateCategory(Category category) throws SQLException {
        authService.checkPermission("PRODUCTOS");
        repository.update(category);

        // Propagar cambios a productos de esta categor\u00eda
        productRepository.updateTaxRateByCategory(category.getId(), category.getDefaultIva());
        if (category.getTaxGroupId() != null) {
            productRepository.updateTaxGroupByCategory(category.getId(), category.getTaxGroupId());
        }
    }

    public void update(Category category) throws SQLException {
        updateCategory(category);
    }

    public void deleteCategory(int id) throws SQLException {
        authService.checkPermission("PRODUCTOS");
        repository.delete(id);
    }

    public int getCount() throws SQLException {
        return repository.count();
    }
}


