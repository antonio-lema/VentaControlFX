package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.Category;
import com.mycompany.ventacontrolfx.service.ProductFilterService.FilterType;
import com.mycompany.ventacontrolfx.application.service.ProductFilterUseCase;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import java.util.List;

public class CategoryMenuRenderer {
    private final FlowPane categoriesFlowPane;
    private final ProductFilterUseCase filterUseCase;
    private final CategorySelectionHandler handler;

    public interface CategorySelectionHandler {
        void onCategorySelected(Category category);

        void onSpecialFilterSelected(FilterType type);
    }

    public CategoryMenuRenderer(FlowPane categoriesFlowPane, ProductFilterUseCase filterUseCase,
            CategorySelectionHandler handler) {
        this.categoriesFlowPane = categoriesFlowPane;
        this.filterUseCase = filterUseCase;
        this.handler = handler;
    }

    public void render(List<Category> favoriteCategories) {
        categoriesFlowPane.getChildren().clear();

        addSpecialButton("Favoritos", FilterType.FAVORITES);
        addSpecialButton("Todos", FilterType.ALL);

        for (Category c : favoriteCategories) {
            if (c.isVisible()) {
                addCategoryButton(c);
            }
        }
        updateStyles();
    }

    private void addSpecialButton(String text, FilterType type) {
        Button btn = new Button(text);
        btn.getStyleClass().add("category-btn");
        btn.setUserData(type);
        btn.setOnAction(e -> handler.onSpecialFilterSelected(type));
        categoriesFlowPane.getChildren().add(btn);
    }

    private void addCategoryButton(Category category) {
        Button btn = new Button(category.getName());
        btn.getStyleClass().add("category-btn");
        btn.setUserData(category);
        btn.setOnAction(e -> handler.onCategorySelected(category));
        categoriesFlowPane.getChildren().add(btn);
    }

    public void updateStyles() {
        FilterType currentType = filterUseCase.getCurrentType();
        Object currentCriteria = filterUseCase.getCurrentCriteria();

        for (Node node : categoriesFlowPane.getChildren()) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                Object data = btn.getUserData();

                boolean active = false;
                if (data instanceof FilterType && data == currentType)
                    active = true;
                else if (data instanceof Category && currentType == FilterType.CATEGORY && data.equals(currentCriteria))
                    active = true;

                if (active)
                    btn.getStyleClass().add("active-category-btn");
                else
                    btn.getStyleClass().remove("active-category-btn");
            }
        }
    }
}
