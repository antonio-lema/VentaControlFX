package com.mycompany.ventacontrolfx.application.service;

import com.mycompany.ventacontrolfx.model.Category;
import com.mycompany.ventacontrolfx.model.Product;
import com.mycompany.ventacontrolfx.service.ProductFilterService;
import com.mycompany.ventacontrolfx.service.ProductFilterService.FilterType;
import java.util.List;

/**
 * Encapsulates filtering logic for products.
 * Decouples SellViewController from ProductFilterService details.
 */
public class ProductFilterUseCase {
    private final ProductFilterService filterService;
    private List<Product> cachedProducts;

    public ProductFilterUseCase(ProductFilterService filterService) {
        this.filterService = filterService;
    }

    public void updateSourceData(List<Product> products) {
        this.cachedProducts = products;
    }

    public List<Product> applySearch(String text) {
        filterService.setFilterSearch(text);
        return getFiltered();
    }

    public List<Product> applyCategory(Category category) {
        if (category == null)
            filterService.setFilterAll();
        else
            filterService.setFilterCategory(category);
        return getFiltered();
    }

    public List<Product> applyFavorites() {
        filterService.setFilterFavorites();
        return getFiltered();
    }

    public List<Product> applyAll() {
        filterService.setFilterAll();
        return getFiltered();
    }

    public FilterType getCurrentType() {
        return filterService.getCurrentType();
    }

    public Object getCurrentCriteria() {
        return filterService.getCurrentCriteria();
    }

    private List<Product> getFiltered() {
        if (cachedProducts == null)
            return List.of();
        return filterService.filter(cachedProducts);
    }
}
