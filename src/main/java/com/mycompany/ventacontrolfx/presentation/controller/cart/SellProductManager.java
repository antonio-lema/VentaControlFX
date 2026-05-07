package com.mycompany.ventacontrolfx.presentation.controller.cart;

import com.mycompany.ventacontrolfx.application.usecase.ProductFilterUseCase;
import com.mycompany.ventacontrolfx.application.usecase.ProductFilterUseCase.FilterType;
import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.model.Promotion;
import com.mycompany.ventacontrolfx.domain.model.PromotionScope;
import com.mycompany.ventacontrolfx.domain.model.VisibilityFilter;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.renderer.ProductGridRenderer;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestiona la carga paginada, filtrado y renderizado de productos en la pantalla de venta.
 */
public class SellProductManager {

    private final ServiceContainer container;
    private final ProductFilterUseCase filterUseCase;
    private final ProductGridRenderer productRenderer;
    private final VBox loadingOverlay;
    private final Label filterLabel;
    private final ScrollPane productsScrollPane;

    private int currentOffset = 0;
    private static final int PAGE_SIZE = 60;
    private int totalProductCount = 0;
    private List<Promotion> activePromotions = new ArrayList<>();
    private int selectedPriceListId = -1;

    public SellProductManager(
            ServiceContainer container,
            ProductFilterUseCase filterUseCase,
            ProductGridRenderer productRenderer,
            VBox loadingOverlay,
            Label filterLabel,
            ScrollPane productsScrollPane) {
        this.container = container;
        this.filterUseCase = filterUseCase;
        this.productRenderer = productRenderer;
        this.loadingOverlay = loadingOverlay;
        this.filterLabel = filterLabel;
        this.productsScrollPane = productsScrollPane;
    }

    public void init(int initialPriceListId) {
        this.selectedPriceListId = initialPriceListId;
        loadActivePromotions();
    }

    private void loadActivePromotions() {
        container.getAsyncManager().runAsyncTask(() -> {
            try { return container.getPromotionUseCase().getActivePromotions(); }
            catch (Exception e) { return new ArrayList<Promotion>(); }
        }, promos -> this.activePromotions = promos, null);
    }

    public void refresh(int priceListId) {
        this.selectedPriceListId = priceListId;
        this.currentOffset = 0;
        productsScrollPane.setVvalue(0);
        productRenderer.showSkeleton(12);
        loadingOverlay.setVisible(true);
        loadingOverlay.setStyle("-fx-background-color: transparent;");

        container.getAsyncManager().runAsyncTask(() -> {
            FilterType type = filterUseCase.getCurrentType();
            Object criteria = filterUseCase.getCurrentCriteria();
            int count = 0;
            List<Product> products;

            if (type == FilterType.CATEGORY && criteria != null) {
                int catId = ((Category) criteria).getId();
                count = container.getProductRepository().countByCategory(catId, VisibilityFilter.VISIBLE);
                products = container.getProductRepository().getByCategoryPaginated(catId, PAGE_SIZE, 0, selectedPriceListId);
            } else if (type == FilterType.FAVORITES) {
                count = container.getProductRepository().countFavorites();
                products = container.getProductRepository().getFavoritesPaginated(PAGE_SIZE, 0, selectedPriceListId);
            } else if (type == FilterType.PROMOTIONS) {
                List<Integer> pIds = getAffectedProductIds();
                List<Integer> cIds = getAffectedCategoryIds();
                boolean hasGlobal = hasGlobalPromotion();
                count = container.getProductRepository().countPromoted(pIds, cIds, hasGlobal);
                products = container.getProductRepository().getPromotedPaginated(pIds, cIds, hasGlobal, PAGE_SIZE, 0, selectedPriceListId);
            } else if (type == FilterType.SEARCH && criteria != null) {
                String query = (String) criteria;
                count = container.getProductRepository().countSearch(query, VisibilityFilter.VISIBLE);
                products = container.getProductRepository().searchPaginated(query, PAGE_SIZE, 0, selectedPriceListId, VisibilityFilter.VISIBLE);
            } else {
                count = container.getProductRepository().countSearch("", VisibilityFilter.VISIBLE);
                products = container.getProductRepository().searchPaginated("", PAGE_SIZE, 0, selectedPriceListId, VisibilityFilter.VISIBLE);
            }
            return new Object[]{products, count};
        }, result -> {
            Object[] data = (Object[]) result;
            List<Product> products = (List<Product>) data[0];
            this.totalProductCount = (Integer) data[1];
            filterUseCase.updateSourceData(products);
            productRenderer.render(products);
            updateFilterLabel();
            loadingOverlay.setVisible(false);
        }, null);
    }

    public void loadNextPage() {
        if (loadingOverlay.isVisible() || productRenderer.getVisibleCount() >= totalProductCount) return;
        
        this.currentOffset += PAGE_SIZE;
        loadingOverlay.setVisible(true);

        container.getAsyncManager().runAsyncTask(() -> {
            FilterType type = filterUseCase.getCurrentType();
            Object criteria = filterUseCase.getCurrentCriteria();
            if (type == FilterType.CATEGORY && criteria != null) 
                return container.getProductRepository().getByCategoryPaginated(((Category) criteria).getId(), PAGE_SIZE, currentOffset, selectedPriceListId);
            if (type == FilterType.SEARCH && criteria != null)
                return container.getProductRepository().searchPaginated((String) criteria, PAGE_SIZE, currentOffset, selectedPriceListId, VisibilityFilter.VISIBLE);
            if (type == FilterType.FAVORITES)
                return container.getProductRepository().getFavoritesPaginated(PAGE_SIZE, currentOffset, selectedPriceListId);
            if (type == FilterType.PROMOTIONS)
                return container.getProductRepository().getPromotedPaginated(getAffectedProductIds(), getAffectedCategoryIds(), hasGlobalPromotion(), PAGE_SIZE, currentOffset, selectedPriceListId);
            
            return container.getProductRepository().searchPaginated("", PAGE_SIZE, currentOffset, selectedPriceListId, VisibilityFilter.VISIBLE);
        }, products -> {
            productRenderer.append(products);
            loadingOverlay.setVisible(false);
        }, null);
    }

    private void updateFilterLabel() {
        FilterType type = filterUseCase.getCurrentType();
        Object criteria = filterUseCase.getCurrentCriteria();
        String lbl = container.getBundle().getString("sell.all_products");
        if (type == FilterType.FAVORITES) lbl = container.getBundle().getString("sell.category.favorites");
        else if (type == FilterType.PROMOTIONS) lbl = container.getBundle().getString("sell.category.promotions");
        else if (type == FilterType.CATEGORY) lbl = translateDynamic(((Category) criteria).getName());
        filterLabel.setText(lbl + " (" + totalProductCount + ")");
    }

    public String getDiscountDescription(Product p) {
        if (activePromotions == null) return null;
        for (Promotion promo : activePromotions) {
            if (promo.getScope() == PromotionScope.GLOBAL && (promo.getCode() == null || promo.getCode().trim().isEmpty())) return formatPromo(promo);
            if (promo.getScope() == PromotionScope.PRODUCT && promo.getAffectedIds().contains(p.getId())) return formatPromo(promo);
            if (promo.getScope() == PromotionScope.CATEGORY && promo.getAffectedIds().contains(p.getCategoryId())) return formatPromo(promo);
        }
        return null;
    }

    private String formatPromo(Promotion p) {
        String abbr = container.getBundle().getString("sell.promo.discount_abbr");
        switch (p.getType()) {
            case PERCENTAGE: return String.format("%.0f%% " + abbr, p.getValue());
            case FIXED_DISCOUNT: return String.format("%.2f\u20ac " + abbr, p.getValue());
            case VOLUME_DISCOUNT: return (p.getBuyQty() + p.getFreeQty()) + "x" + p.getBuyQty();
            default: return p.getName();
        }
    }

    private List<Integer> getAffectedProductIds() {
        return activePromotions.stream().filter(p -> p.getScope() == PromotionScope.PRODUCT).flatMap(p -> p.getAffectedIds().stream()).collect(Collectors.toList());
    }

    private List<Integer> getAffectedCategoryIds() {
        return activePromotions.stream().filter(p -> p.getScope() == PromotionScope.CATEGORY).flatMap(p -> p.getAffectedIds().stream()).collect(Collectors.toList());
    }

    private boolean hasGlobalPromotion() {
        return activePromotions.stream().anyMatch(p -> p.getScope() == PromotionScope.GLOBAL);
    }

    private String translateDynamic(String text) {
        if (text == null || text.isBlank()) return text;
        return container.getBundle().containsKey(text) ? container.getBundle().getString(text) : text;
    }

    public List<Promotion> getActivePromotions() { return activePromotions; }
}

