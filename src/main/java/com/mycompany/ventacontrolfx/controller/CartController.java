package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.Client;
import com.mycompany.ventacontrolfx.service.CartService;
import com.mycompany.ventacontrolfx.service.NavigationService;
import com.mycompany.ventacontrolfx.service.ServiceContainer;
import com.mycompany.ventacontrolfx.util.Injectable;
import com.mycompany.ventacontrolfx.util.ModalService;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

public class CartController implements Injectable {
    @FXML
    private VBox cartItemsContainer, emptyCartView;
    @FXML
    private Label subtotalLabel, taxLabel, itemsCountLabel, totalButtonLabel, lblSelectedClient;
    @FXML
    private Button btnClearCart, payButton, btnRemoveClient;

    private ServiceContainer container;
    private NavigationService navigationService;
    private CartListRenderer cartRenderer;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.cartRenderer = new CartListRenderer(cartItemsContainer, container.getCartService());
        initBindings();
        initListeners();
    }

    public void init(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    private void initBindings() {
        CartService cart = container.getCartService();
        cartItemsContainer.visibleProperty().bind(cart.itemCountProperty().greaterThan(0));
        cartItemsContainer.managedProperty().bind(cartItemsContainer.visibleProperty());
        emptyCartView.visibleProperty().bind(cart.itemCountProperty().isEqualTo(0));
        emptyCartView.managedProperty().bind(emptyCartView.visibleProperty());

        payButton.disableProperty().bind(cart.itemCountProperty().isEqualTo(0));
        payButton.visibleProperty().bind(cart.itemCountProperty().greaterThan(0));
        payButton.managedProperty().bind(payButton.visibleProperty());

        btnClearCart.visibleProperty().bind(cart.itemCountProperty().greaterThan(0));
        btnClearCart.managedProperty().bind(btnClearCart.visibleProperty());

        subtotalLabel.textProperty().bind(cart.subtotalProperty().asString("%.2f €"));
        taxLabel.textProperty().bind(cart.taxProperty().asString("%.2f €"));
        totalButtonLabel.textProperty().bind(Bindings.format("Total: %.2f €", cart.grandTotalProperty()));
        itemsCountLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("Subtotal (%d item%s)", cart.getItemCount(), cart.getItemCount() != 1 ? "s" : ""),
                cart.itemCountProperty()));
    }

    private void initListeners() {
        container.getCartService().selectedClientProperty()
                .addListener((obs, oldClient, newClient) -> updateClientUI(newClient));
    }

    private void updateClientUI(Client client) {
        if (client != null) {
            lblSelectedClient.setText(client.getName());
            lblSelectedClient.setStyle("-fx-text-fill: #1e88e5; -fx-font-weight: bold;");
        } else {
            lblSelectedClient.setText("Añadir empresa");
            lblSelectedClient.setStyle("");
        }
        btnRemoveClient.setVisible(client != null);
        btnRemoveClient.setManaged(client != null);
    }

    @FXML
    private void clearCart() {
        container.getCartService().clear();
    }

    @FXML
    private void handleRemoveSelectedClient() {
        container.getCartService().setSelectedClient(null);
    }

    @FXML
    private void showAddClientDialog() {
        navigationService.navigateTo("/view/clients.fxml");
    }

    @FXML
    private void handlePayButton() {
        if (container.getCartService().getItemCount() == 0)
            return;
        ModalService.showModal("/view/payment.fxml", "Pago", Modality.APPLICATION_MODAL, StageStyle.UNDECORATED,
                container, (PaymentController controller) -> {
                    controller.setTotalAmount(container.getCartService().getGrandTotal(), (paid, change, method) -> {
                        container.getSaleAppService().processSale(paid, change, method,
                                (saleId, items, total, p, c, m, client) -> {
                                    ModalService.showStandardModal("/view/receipt.fxml",
                                            client != null ? "Factura" : "Factura simplificada", container,
                                            (ReceiptController rc) -> {
                                                if (client != null)
                                                    rc.setClientInfo(client);
                                                rc.setReceiptData(items, total, p, c, m, saleId, null, null);
                                            });
                                });
                    });
                });
    }
}
