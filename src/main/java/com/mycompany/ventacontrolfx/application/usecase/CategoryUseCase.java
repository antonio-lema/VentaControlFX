package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.domain.repository.ICategoryRepository;
import java.sql.SQLException;
import java.util.List;

public class CategoryUseCase {
    private final ICategoryRepository repository;

    public CategoryUseCase(ICategoryRepository repository) {
        this.repository = repository;
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
        repository.save(category);
    }

    public void updateCategory(Category category) throws SQLException {
        repository.update(category);
    }

    public void deleteCategory(int id) throws SQLException {
        repository.delete(id);
    }

    public int getCount() throws SQLException {
        return repository.count();
    }
}
