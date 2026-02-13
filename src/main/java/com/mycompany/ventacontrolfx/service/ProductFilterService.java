package com.mycompany.ventacontrolfx.service;

import com.mycompany.ventacontrolfx.model.Category;
import com.mycompany.ventacontrolfx.model.Product;
import java.util.List;
import java.util.stream.Collectors;

public class ProductFilterService {

    public enum FilterType {
        ALL,
        CATEGORY,
        FAVORITES,
        SEARCH
    }

    private FilterType currentType = FilterType.ALL;
    private Object currentCriteria = null; // Category or String (query)

    public void setFilterAll() {
        this.currentType = FilterType.ALL;
        this.currentCriteria = null;
    }

    public void setFilterCategory(Category category) {
        if (category == null) {
            setFilterAll();
        } else {
            this.currentType = FilterType.CATEGORY;
            this.currentCriteria = category;
        }
    }

    public void setFilterFavorites() {
        this.currentType = FilterType.FAVORITES;
        this.currentCriteria = null;
    }

    public void setFilterSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            setFilterFavorites();
        } else {
            this.currentType = FilterType.SEARCH;
            this.currentCriteria = query.toLowerCase();
        }
    }

    public List<Product> filter(List<Product> allProducts) {
        if (allProducts == null)
            return List.of();

        switch (currentType) {
            case CATEGORY:
                if (currentCriteria instanceof Category) {
                    int catId = ((Category) currentCriteria).getId();
                    return allProducts.stream()
                            .filter(p -> p.getCategoryId() == catId)
                            .collect(Collectors.toList());
                }
                break;
            case FAVORITES:
                return allProducts.stream()
                        .filter(Product::isFavorite)
                        .collect(Collectors.toList());
            case SEARCH:
                if (currentCriteria instanceof String) {
                    String query = (String) currentCriteria;
                    return allProducts.stream()
                            .filter(p -> matchesSearch(p, query))
                            .collect(Collectors.toList());
                }
                break;
            case ALL:
            default:
                return allProducts;
        }
        return allProducts;
    }

    private boolean matchesSearch(Product p, String query) {
        if (p.getName() != null && p.getName().toLowerCase().contains(query))
            return true;
        if (String.valueOf(p.getId()).contains(query))
            return true;
        if (p.getCategoryName() != null && p.getCategoryName().toLowerCase().contains(query))
            return true;
        return false;
    }

    public FilterType getCurrentType() {
        return currentType;
    }

    public Object getCurrentCriteria() {
        return currentCriteria;
    }
}
