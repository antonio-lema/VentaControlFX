package com.mycompany.ventacontrolfx.presentation.controller.cart;

import com.mycompany.ventacontrolfx.application.usecase.CategoryUseCase;
import com.mycompany.ventacontrolfx.application.usecase.ProductFilterUseCase;
import com.mycompany.ventacontrolfx.presentation.component.SkeletonCategoryBox;
import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona el menú de categorías y filtros especiales en la pantalla de venta.
 */
public class SellCategoryManager {

    private final ServiceContainer container;
    private final CategoryUseCase categoryUseCase;
    private final ProductFilterUseCase filterUseCase;
    private final FlowPane categoriesMegaFlowPane;
    private final Label lblSelectedCategory;
    private final VBox categoryMenuContainer;

    public SellCategoryManager(
            ServiceContainer container,
            CategoryUseCase categoryUseCase,
            ProductFilterUseCase filterUseCase,
            FlowPane categoriesMegaFlowPane,
            Label lblSelectedCategory,
            VBox categoryMenuContainer) {
        this.container = container;
        this.categoryUseCase = categoryUseCase;
        this.filterUseCase = filterUseCase;
        this.categoriesMegaFlowPane = categoriesMegaFlowPane;
        this.lblSelectedCategory = lblSelectedCategory;
        this.categoryMenuContainer = categoryMenuContainer;
    }

    public void setup(java.util.function.Consumer<Integer> onCategoryChanged, Runnable onRefresh) {
        if (categoriesMegaFlowPane == null) return;

        // Mostrar esqueletos
        categoriesMegaFlowPane.getChildren().clear();
        for (int i = 0; i < 6; i++) categoriesMegaFlowPane.getChildren().add(new SkeletonCategoryBox());

        container.getAsyncManager().runAsyncTask(() -> {
            List<Category> cats = categoryUseCase.getAll();
            List<Category> result = new ArrayList<>();
            result.add(createSpecialCategory("sell.all_categories", -1, ""));
            result.add(createSpecialCategory("sell.category.favorites", -2, "\u2605 "));
            result.add(createSpecialCategory("sell.category.promotions", -3, "% "));
            result.addAll(cats);
            return result;
        }, categories -> {
            renderMenu(categories, onCategoryChanged, onRefresh);
            updateLabel(container.getCartUseCase().getSelectedCategoryId());
        }, null);
    }

    private Category createSpecialCategory(String bundleKey, int id, String prefix) {
        Category cat = new Category();
        cat.setName(prefix + container.getBundle().getString(bundleKey));
        cat.setId(id);
        return cat;
    }

    private void renderMenu(List<Category> categories, java.util.function.Consumer<Integer> onCategoryChanged, Runnable onRefresh) {
        categoriesMegaFlowPane.getChildren().clear();
        for (Category cat : categories) {
            Button btn = new Button(translateDynamic(cat.getName()));
            btn.getStyleClass().add("category-mega-button");
            btn.setPrefWidth(160);
            btn.setPrefHeight(48);
            btn.setOnAction(e -> {
                applyFilter(cat, onRefresh);
                onCategoryChanged.accept(cat.getId());
                toggleMenu();
            });
            categoriesMegaFlowPane.getChildren().add(btn);
        }
    }

    public void applyFilter(Category cat, Runnable onRefresh) {
        if (cat.getId() == -1) filterUseCase.applyAll();
        else if (cat.getId() == -2) filterUseCase.applyFavorites();
        else if (cat.getId() == -3) {
            try {
                filterUseCase.applyPromotions(container.getPromotionUseCase().getActivePromotions());
            } catch (SQLException e) { e.printStackTrace(); }
        }
        else filterUseCase.applyCategory(cat);
        onRefresh.run();
    }

    public void applyFilterById(int catId, Runnable onRefresh) {
        if (catId == -1) { filterUseCase.applyAll(); onRefresh.run(); }
        else if (catId == -2) { filterUseCase.applyFavorites(); onRefresh.run(); }
        else if (catId == -3) { 
            try {
                filterUseCase.applyPromotions(container.getPromotionUseCase().getActivePromotions()); 
                onRefresh.run();
            } catch (SQLException e) { e.printStackTrace(); }
        }
        else {
            container.getAsyncManager().runAsyncTask(() -> categoryUseCase.getAll(), cats -> {
                cats.stream().filter(c -> c.getId() == catId).findFirst().ifPresent(c -> {
                    filterUseCase.applyCategory(c);
                    onRefresh.run();
                });
            }, null);
        }
    }

    public void updateLabel(int catId) {
        if (catId == -1) lblSelectedCategory.setText(container.getBundle().getString("sell.all_categories"));
        else if (catId == -2) lblSelectedCategory.setText("\u2605 " + container.getBundle().getString("sell.category.favorites"));
        else if (catId == -3) lblSelectedCategory.setText("% " + container.getBundle().getString("sell.category.promotions"));
        else {
            container.getAsyncManager().runAsyncTask(() -> categoryUseCase.getAll(), cats -> {
                cats.stream().filter(c -> c.getId() == catId).findFirst()
                        .ifPresent(c -> lblSelectedCategory.setText(translateDynamic(c.getName())));
            }, null);
        }
    }

    public void toggleMenu() {
        boolean isVisible = categoryMenuContainer.isVisible();
        categoryMenuContainer.setVisible(!isVisible);
        categoryMenuContainer.setManaged(!isVisible);
    }

    private String translateDynamic(String text) {
        if (text == null || text.isBlank()) return text;
        return container.getBundle().containsKey(text) ? container.getBundle().getString(text) : text;
    }
}


