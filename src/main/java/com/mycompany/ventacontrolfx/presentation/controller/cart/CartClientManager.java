package com.mycompany.ventacontrolfx.presentation.controller.cart;

import com.mycompany.ventacontrolfx.application.usecase.CartUseCase;
import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import java.util.List;

/**
 * Gestor de la información del cliente y la tarifa aplicada en el carrito.
 */
public class CartClientManager {

    private final ServiceContainer container;
    private final CartUseCase cartUseCase;
    private final Label lblClient, lblPriceList;
    private final Button btnRemoveClient, btnAddClient;

    public CartClientManager(ServiceContainer container, CartUseCase cartUseCase, Label lblClient, Label lblPriceList, Button btnRemoveClient, Button btnAddClient) {
        this.container = container;
        this.cartUseCase = cartUseCase;
        this.lblClient = lblClient;
        this.lblPriceList = lblPriceList;
        this.btnRemoveClient = btnRemoveClient;
        this.btnAddClient = btnAddClient;
    }

    public void update(Client client) {
        if (client != null) {
            lblClient.setText(client.getName());
            lblClient.setStyle("-fx-text-fill: #1e88e5; -fx-font-weight: bold;");
        } else {
            lblClient.setText(container.getBundle().getString("cart.client.none"));
            lblClient.setStyle("");
        }
        btnRemoveClient.setVisible(client != null);
        btnRemoveClient.setManaged(client != null);
        btnAddClient.setVisible(client == null);
        btnAddClient.setManaged(client == null);
    }

    public void refreshPriceListLabel() {
        try {
            int currentId = cartUseCase.getPriceListId();
            List<PriceList> lists = container.getPriceListUseCase().getAll();
            lists.stream()
                    .filter(l -> l.getId() == currentId)
                    .findFirst()
                    .ifPresentOrElse(
                        l -> lblPriceList.setText(l.getName()),
                        () -> lblPriceList.setText(container.getBundle().getString("cart.price_list.unknown"))
                    );
        } catch (Exception e) {
            lblPriceList.setText(container.getBundle().getString("cart.price_list.unknown"));
        }
    }
}

