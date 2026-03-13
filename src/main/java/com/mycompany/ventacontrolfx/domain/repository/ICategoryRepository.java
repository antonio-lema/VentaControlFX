package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.Category;
import java.sql.SQLException;
import java.util.List;

public interface ICategoryRepository {
    List<Category> getAll() throws SQLException;

    List<Category> getAllVisible() throws SQLException;

    List<Category> getFavorites() throws SQLException;

    void save(Category category) throws SQLException;

    void update(Category category) throws SQLException;

    void delete(int id) throws SQLException;

    int count() throws SQLException;

    Category getById(int id) throws SQLException;
}
