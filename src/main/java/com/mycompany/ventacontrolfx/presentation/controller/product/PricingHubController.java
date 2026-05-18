package com.mycompany.ventacontrolfx.presentation.controller.product;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.PriceListController;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.PromotionsController;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for the Unified Pricing and Promotions Hub.
 */
public class PricingHubController implements Injectable {

    @FXML private StackPane contentArea;
    @FXML private Button btnTabLists, btnTabPromos;
    
    @FXML private Node priceLists, promotions;
    
    @FXML private PriceListController priceListsController;
    @FXML private PromotionsController promotionsController;

    private ServiceContainer container;
    private List<Button> navButtons;
    private List<Node> contents;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.navButtons = Arrays.asList(btnTabLists, btnTabPromos);
        this.contents = Arrays.asList(priceLists, promotions);
        
        // Propagate injection
        if (priceListsController != null) priceListsController.inject(container);
        if (promotionsController != null) promotionsController.inject(container);
        
        showPriceLists();
    }

    @FXML
    private void showPriceLists() {
        switchTab(btnTabLists, priceLists);
    }

    @FXML
    private void showPromotions() {
        switchTab(btnTabPromos, promotions);
    }

    private void switchTab(Button activeBtn, Node activeContent) {
        navButtons.forEach(btn -> {
            btn.getStyleClass().remove("hub-nav-button-active");
            if (!btn.getStyleClass().contains("hub-nav-button")) {
                btn.getStyleClass().add("hub-nav-button");
            }
        });
        activeBtn.getStyleClass().add("hub-nav-button-active");

        contents.forEach(node -> {
            node.setVisible(false);
            node.setManaged(false);
        });
        activeContent.setVisible(true);
        activeContent.setManaged(true);
    }
}
