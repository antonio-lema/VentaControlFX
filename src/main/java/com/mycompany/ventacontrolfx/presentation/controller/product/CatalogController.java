package com.mycompany.ventacontrolfx.presentation.controller.product;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for the Unified Catalog Hub with Custom Navigation.
 */
public class CatalogController implements Injectable {

    @FXML private StackPane contentArea;
    @FXML private Button btnTabProducts, btnTabCategories;
    
    // Injected child views (FX:ID from include)
    @FXML private Node productsView, categoriesView;
    
    // Injected child controllers (FX:ID + "Controller")
    @FXML private ProductController productsViewController;
    @FXML private CategoryController categoriesViewController;

    private ServiceContainer container;
    private List<Button> navButtons;
    private List<Node> contents;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.navButtons = Arrays.asList(btnTabProducts, btnTabCategories);
        this.contents = Arrays.asList(productsView, categoriesView);
        
        // Propagate injection
        if (productsViewController != null) productsViewController.inject(container);
        if (categoriesViewController != null) categoriesViewController.inject(container);
        
        showProducts();
    }

    @FXML
    private void showProducts() {
        switchTab(btnTabProducts, productsView);
    }

    @FXML
    private void showCategories() {
        switchTab(btnTabCategories, categoriesView);
    }

    private void switchTab(Button activeBtn, Node activeContent) {
        // Update Buttons Style
        navButtons.forEach(btn -> {
            btn.getStyleClass().remove("hub-nav-button-active");
            if (!btn.getStyleClass().contains("hub-nav-button")) {
                btn.getStyleClass().add("hub-nav-button");
            }
        });
        activeBtn.getStyleClass().add("hub-nav-button-active");

        // Update Content Visibility
        contents.forEach(node -> {
            node.setVisible(false);
            node.setManaged(false);
        });
        activeContent.setVisible(true);
        activeContent.setManaged(true);
    }
}
