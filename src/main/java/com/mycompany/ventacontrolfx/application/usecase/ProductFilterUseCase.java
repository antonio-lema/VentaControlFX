package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.domain.model.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProductFilterUseCase {

    public enum FilterType {
        ALL, FAVORITES, CATEGORY, SEARCH, PROMOTIONS
    }

    private List<Product> sourceData = new ArrayList<>();
    private FilterType currentType = FilterType.ALL;
    private Object currentCriteria = null;

    public void updateSourceData(List<Product> products) {
        this.sourceData = products;
    }

    public List<Product> applyAll() {
        currentType = FilterType.ALL;
        currentCriteria = null;
        return sourceData;
    }

    public List<Product> applyFavorites() {
        currentType = FilterType.FAVORITES;
        currentCriteria = null;
        return sourceData.stream().filter(Product::isFavorite).collect(Collectors.toList());
    }

    public List<Product> applyCategory(Category category) {
        currentType = FilterType.CATEGORY;
        currentCriteria = category;
        return sourceData.stream().filter(p -> p.getCategoryId() == category.getId()).collect(Collectors.toList());
    }

    public List<Product> applySearch(String query) {
        currentType = FilterType.SEARCH;
        currentCriteria = query;
        if (query == null || query.isBlank())
            return applyAll();
        return sourceData.stream()
                .filter(p -> p.getName().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public List<Product> applyPromotions(List<com.mycompany.ventacontrolfx.domain.model.Promotion> promos) {
        currentType = FilterType.PROMOTIONS;
        currentCriteria = promos;
        if (promos == null)
            return sourceData;

        boolean hasGlobal = promos.stream()
                .anyMatch(p -> p.getScope() == com.mycompany.ventacontrolfx.domain.model.PromotionScope.GLOBAL);
        if (hasGlobal)
            return sourceData;

        List<Integer> affectedProductIds = promos.stream()
                .filter(p -> p.getScope() == com.mycompany.ventacontrolfx.domain.model.PromotionScope.PRODUCT)
                .flatMap(p -> p.getAffectedIds().stream())
                .collect(Collectors.toList());

        List<Integer> affectedCategoryIds = promos.stream()
                .filter(p -> p.getScope() == com.mycompany.ventacontrolfx.domain.model.PromotionScope.CATEGORY)
                .flatMap(p -> p.getAffectedIds().stream())
                .collect(Collectors.toList());

        return sourceData.stream()
                .filter(p -> affectedProductIds.contains(p.getId()) || affectedCategoryIds.contains(p.getCategoryId()))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public List<Product> applyCurrentFilter() {
        switch (currentType) {
            case FAVORITES:
                return applyFavorites();
            case CATEGORY:
                return applyCategory((Category) currentCriteria);
            case SEARCH:
                return applySearch((String) currentCriteria);
            case PROMOTIONS:
                return applyPromotions((List<com.mycompany.ventacontrolfx.domain.model.Promotion>) currentCriteria);
            case ALL:
            default:
                return applyAll();
        }
    }

    public FilterType getCurrentType() {
        return currentType;
    }

    public Object getCurrentCriteria() {
        return currentCriteria;
    }
}

