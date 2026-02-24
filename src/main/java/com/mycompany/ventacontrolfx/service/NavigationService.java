package com.mycompany.ventacontrolfx.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;

import javafx.scene.layout.FlowPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class NavigationService {

    private final ScrollPane mainContent;
    private final VBox loadingOverlay;
    private final VBox cartPanel;
    private final Node favoriteCategoriesBox;
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

    private final Button btnHistory;
    private final Button btnUsers;
    private final Button btnClients; // Added btnClients
    private final Button btnConfig; // Added btnConfig
    private final Button btnClosures; // Added btnClosures

    private final List<Button> sidebarButtons;
    private final CartService cartService; // Added CartService

    // Summary Cards
    private final HBox cardCountProducts;
    private final HBox cardCountCategories;
    private final HBox cardCountHistory;
    private final HBox cardCountClosures;
    private final HBox cardCountClients;
    private final HBox cardCountUsers;
    private final List<HBox> summaryCards;

    public NavigationService(
            ScrollPane mainContent,
            VBox loadingOverlay,
            VBox cartPanel,
            Node favoriteCategoriesBox,
            HBox searchBarContainer,
            HBox filterDisplayContainer,
            TilePane productsPane,
            VBox productsSubmenu,
            Label lblProductsArrow,
            Button btnSell,
            Button btnProducts,
            Button btnProductsList,
            Button btnCategories,
            Button btnHistory,
            Button btnUsers,
            Button btnClients, // Added btnClients
            Button btnConfig, // Added btnConfig
            Button btnClosures, // Added btnClosures
            CartService cartService,
            HBox cardCountProducts,
            HBox cardCountCategories,
            HBox cardCountHistory,
            HBox cardCountClosures,
            HBox cardCountClients,
            HBox cardCountUsers) { // Added CartService
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
        this.btnHistory = btnHistory;
        this.btnUsers = btnUsers;
        this.btnClients = btnClients;
        this.btnConfig = btnConfig;
        this.btnClosures = btnClosures;
        this.cartService = cartService;

        this.sidebarButtons = Arrays.asList(btnSell, btnProducts, btnProductsList, btnCategories, btnHistory, btnUsers,
                btnClients, btnConfig, btnClosures);

        this.cardCountProducts = cardCountProducts;
        this.cardCountCategories = cardCountCategories;
        this.cardCountHistory = cardCountHistory;
        this.cardCountClosures = cardCountClosures;
        this.cardCountClients = cardCountClients;
        this.cardCountUsers = cardCountUsers;

        this.summaryCards = Arrays.asList(cardCountProducts, cardCountCategories, cardCountHistory,
                cardCountClosures, cardCountClients, cardCountUsers);
    }

    public void showSellView(Runnable dataLoadAction) {
        simulateLoading(() -> {
            mainContent.setContent(productsPane);
            setSalesComponentsVisible(true);
            showOnlyCard(cardCountProducts); // For selling, show products count
            setActiveButton(btnSell);
            if (dataLoadAction != null)
                dataLoadAction.run();
        });
    }

    public void showProductsView(Runnable dataLoadAction) {
        simulateLoading(() -> {
            try {
                Parent view = FXMLLoader.load(getClass().getResource("/view/products.fxml"));
                mainContent.setContent(view);

                setSalesComponentsVisible(false);
                showOnlyCard(cardCountProducts);
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
                showOnlyCard(cardCountCategories);
                setActiveButton(btnCategories);

                if (dataLoadAction != null)
                    dataLoadAction.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void showHistoryView(Runnable dataLoadAction) {
        simulateLoading(() -> {
            try {
                Parent view = FXMLLoader.load(getClass().getResource("/view/sales.fxml"));
                mainContent.setContent(view);

                setSalesComponentsVisible(false);
                showOnlyCard(cardCountHistory);
                setActiveButton(btnHistory);

                if (dataLoadAction != null)
                    dataLoadAction.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void showClosureHistoryView(Runnable dataLoadAction) {
        simulateLoading(() -> {
            try {
                Parent view = FXMLLoader.load(getClass().getResource("/view/closure_history.fxml"));
                mainContent.setContent(view);

                setSalesComponentsVisible(false);
                showOnlyCard(cardCountClosures);
                setActiveButton(btnClosures);

                if (dataLoadAction != null)
                    dataLoadAction.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void showUsersView(Runnable dataLoadAction) {
        simulateLoading(() -> {
            try {
                Parent view = FXMLLoader.load(getClass().getResource("/view/manage_users.fxml"));
                mainContent.setContent(view);

                setSalesComponentsVisible(false); // Hide optional sales components
                showOnlyCard(cardCountUsers);
                setActiveButton(btnUsers);

                if (dataLoadAction != null)
                    dataLoadAction.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void showConfigView(Runnable dataLoadAction) {
        simulateLoading(() -> {
            try {
                Parent view = FXMLLoader.load(getClass().getResource("/view/sale_config.fxml"));
                mainContent.setContent(view);

                setSalesComponentsVisible(false);
                showOnlyCard(null); // No card for config
                setActiveButton(btnConfig);

                if (dataLoadAction != null)
                    dataLoadAction.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void showClientsView(Runnable dataLoadAction) {
        simulateLoading(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/clients.fxml"));
                Parent view = loader.load();

                // Initialize controller
                com.mycompany.ventacontrolfx.controller.ClientsController controller = loader.getController();
                if (controller != null) {
                    controller.init(cartService, () -> showSellView(null));
                }

                mainContent.setContent(view);

                setSalesComponentsVisible(false); // Hide optional sales components
                showOnlyCard(cardCountClients);
                setActiveButton(btnClients);

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

    private void showOnlyCard(HBox cardToShow) {
        if (summaryCards == null)
            return;
        for (HBox card : summaryCards) {
            if (card != null) {
                boolean isThisCard = (card == cardToShow);
                card.setVisible(isThisCard);
                card.setManaged(isThisCard);
            }
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
