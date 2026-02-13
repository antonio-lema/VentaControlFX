package com.mycompany.ventacontrolfx.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;

import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class NavigationService {

    private final ScrollPane mainContent;
    private final VBox loadingOverlay;
    private final VBox cartPanel;
    private final HBox favoriteCategoriesBox;
    private final HBox searchBarContainer;
    private final HBox filterDisplayContainer;
    private final TilePane productsPane;
    private final VBox productsSubmenu;
    private final Label lblProductsArrow;

    // Sidebar buttons
    private final Button btnSell;
    private final Button btnProducts;
    private final Button btnProductsList;
    private final Button btnCategories;

    private final List<Button> sidebarButtons;

    // Cache views to avoid reloading? (Optional, based on original code always
    // reloading)
    // Original code reloaded each time. We'll stick to that for now or improve.

    public NavigationService(
            ScrollPane mainContent,
            VBox loadingOverlay,
            VBox cartPanel,
            HBox favoriteCategoriesBox,
            HBox searchBarContainer,
            HBox filterDisplayContainer,
            TilePane productsPane,
            VBox productsSubmenu,
            Label lblProductsArrow,
            Button btnSell,
            Button btnProducts,
            Button btnProductsList,
            Button btnCategories) {
        this.mainContent = mainContent;
        this.loadingOverlay = loadingOverlay;
        this.cartPanel = cartPanel;
        this.favoriteCategoriesBox = favoriteCategoriesBox;
        this.searchBarContainer = searchBarContainer;
        this.filterDisplayContainer = filterDisplayContainer;
        this.productsPane = productsPane;
        this.productsSubmenu = productsSubmenu;
        this.lblProductsArrow = lblProductsArrow;

        this.btnSell = btnSell;
        this.btnProducts = btnProducts;
        this.btnProductsList = btnProductsList;
        this.btnCategories = btnCategories;

        this.sidebarButtons = Arrays.asList(btnSell, btnProducts, btnProductsList, btnCategories);
    }

    public void showSellView(Runnable dataLoadAction) {
        simulateLoading(() -> {
            mainContent.setContent(productsPane);
            setSalesComponentsVisible(true);
            setActiveButton(btnSell);
            if (dataLoadAction != null)
                dataLoadAction.run();
        });
    }

    public void showProductsView(Runnable dataLoadAction) {
        simulateLoading(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/mycompany/ventacontrolfx/view/products.fxml"));
                // Note: Original path was "/view/products.fxml". Adjusting to possible absolute
                // path if needed.
                // Assuming standard Maven structure: src/main/resources/view/products.fxml ->
                // "/view/products.fxml"
                // Checking MainController usage: "/view/products.fxml" (Step 3068 line 187).
                // I should use exactly what MainController used.

                Parent view = FXMLLoader.load(getClass().getResource("/view/products.fxml"));
                mainContent.setContent(view);

                setSalesComponentsVisible(false);
                setActiveButton(btnProductsList);

                if (dataLoadAction != null)
                    dataLoadAction.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void showCategoriesView(Runnable dataLoadAction) {
        simulateLoading(() -> {
            try {
                Parent view = FXMLLoader.load(getClass().getResource("/view/categories.fxml"));
                mainContent.setContent(view);

                setSalesComponentsVisible(false);
                setActiveButton(btnCategories);

                if (dataLoadAction != null)
                    dataLoadAction.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void toggleProductsMenu() {
        boolean isVisible = productsSubmenu.isVisible();
        productsSubmenu.setVisible(!isVisible);
        productsSubmenu.setManaged(!isVisible);

        if (!isVisible) {
            lblProductsArrow.setText("▼");
        } else {
            lblProductsArrow.setText("<");
        }

        setActiveButton(btnProducts);
    }

    private void setSalesComponentsVisible(boolean visible) {
        if (cartPanel != null) {
            cartPanel.setVisible(visible);
            cartPanel.setManaged(visible);
        }
        if (favoriteCategoriesBox != null) {
            favoriteCategoriesBox.setVisible(visible);
            favoriteCategoriesBox.setManaged(visible);
        }
        if (searchBarContainer != null) {
            searchBarContainer.setVisible(visible);
            searchBarContainer.setManaged(visible);
        }
        if (filterDisplayContainer != null) {
            filterDisplayContainer.setVisible(visible);
            filterDisplayContainer.setManaged(visible);
        }
    }

    private void simulateLoading(Runnable action) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(true);
            PauseTransition pause = new PauseTransition(Duration.millis(300));
            pause.setOnFinished(e -> {
                action.run();
                loadingOverlay.setVisible(false);
            });
            pause.play();
        } else {
            action.run();
        }
    }

    public void setActiveButton(Button activeButton) {
        for (Button btn : sidebarButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("active-sidebar-button");
            }
        }

        if (activeButton != null) {
            if (!activeButton.getStyleClass().contains("active-sidebar-button")) {
                activeButton.getStyleClass().add("active-sidebar-button");
            }
        }
    }
}
