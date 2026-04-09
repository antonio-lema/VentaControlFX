package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.model.VisibilityFilter;
import java.sql.SQLException;
import java.util.List;

public interface IProductRepository {
        List<Product> getAll() throws SQLException;

        List<Product> getAll(int priceListId) throws SQLException;

        List<Product> getAllVisible() throws SQLException;

        List<Product> getAllVisible(int priceListId) throws SQLException;

        List<Product> searchPaginated(String query, int limit, int offset, int priceListId, VisibilityFilter visibility)
                        throws SQLException;

        int countSearch(String query, VisibilityFilter visibility) throws SQLException;

        List<Product> getFavorites() throws SQLException;

        List<Product> getFavorites(int priceListId) throws SQLException;

        java.util.List<Product> getFavoritesPaginated(int limit, int offset, int priceListId) throws SQLException;

        int countFavorites() throws SQLException;

        Product getById(int id) throws SQLException;

        void save(Product product) throws SQLException;

        void saveAll(List<Product> products) throws SQLException;

        void update(Product product) throws SQLException;

        void delete(int id) throws SQLException;

        int count() throws SQLException;

        void updateFavorite(int productId, boolean favorite) throws SQLException;

        void updateVisibility(int productId, boolean visible) throws SQLException;

        void updateVisibilityByCategory(int categoryId, boolean visible) throws SQLException;

        void updateTaxRateByCategory(int categoryId, double taxRate) throws SQLException;

        void updateTaxRateToAll(double taxRate) throws SQLException;

        void updateTaxGroupByCategory(int categoryId, int taxGroupId) throws SQLException;

        void updateTaxGroupToAll(int taxGroupId) throws SQLException;

        void updateTaxGroupForProducts(java.util.List<Integer> productIds, int taxGroupId) throws SQLException;

        int updateStock(int productId, int quantityDelta, java.sql.Connection conn) throws SQLException;

        java.util.List<Product> getLowStock() throws SQLException;

        java.util.List<Product> getByCategory(int categoryId, int priceListId) throws SQLException;

        java.util.List<Product> getByCategoryPaginated(int categoryId, int limit, int offset, int priceListId)
                        throws SQLException;

        int countByCategory(int categoryId, VisibilityFilter visibility) throws SQLException;

        Product findBySku(String sku) throws SQLException;

        java.util.List<Product> getBasicMetadata() throws SQLException;
}
