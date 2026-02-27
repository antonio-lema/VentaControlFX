package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.application.usecase.CartUseCase;
import com.mycompany.ventacontrolfx.infrastructure.navigation.NavigationService;
import com.mycompany.ventacontrolfx.presentation.renderer.CartListRenderer;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.util.ModalService;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

import java.util.List;
import java.sql.SQLException;

public class CartController implements Injectable {

    @FXML
    private VBox cartItemsContainer, emptyCartView;
    @FXML
    private Label subtotalLabel, taxLabel, itemsCountLabel, totalButtonLabel, lblSelectedClient;
    @FXML
    private Button btnClearCart, payButton, btnRemoveClient;

    private ServiceContainer container;
    private NavigationService navigationService;
    private CartUseCase cartUseCase;
    private CartListRenderer cartRenderer;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.navigationService = container.getNavigationService();
        this.cartUseCase = container.getCartUseCase();
        this.cartRenderer = new CartListRenderer(cartItemsContainer, cartUseCase);
        initBindings();
        initListeners();
    }

    private void initBindings() {
        cartItemsContainer.visibleProperty().bind(cartUseCase.itemCountProperty().greaterThan(0));
        cartItemsContainer.managedProperty().bind(cartItemsContainer.visibleProperty());
        emptyCartView.visibleProperty().bind(cartUseCase.itemCountProperty().isEqualTo(0));
        emptyCartView.managedProperty().bind(emptyCartView.visibleProperty());

        payButton.disableProperty().bind(cartUseCase.itemCountProperty().isEqualTo(0));
        payButton.visibleProperty().bind(cartUseCase.itemCountProperty().greaterThan(0));
        payButton.managedProperty().bind(payButton.visibleProperty());

        btnClearCart.visibleProperty().bind(cartUseCase.itemCountProperty().greaterThan(0));
        btnClearCart.managedProperty().bind(btnClearCart.visibleProperty());

        subtotalLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("💰 %.2f €", cartUseCase.getSubtotal()),
                cartUseCase.subtotalProperty()));
        taxLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("📑 %.2f €", cartUseCase.getTax()),
                cartUseCase.taxProperty()));
        totalButtonLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("%.2f €", cartUseCase.getGrandTotal()),
                cartUseCase.grandTotalProperty()));
        itemsCountLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("📦 Subtotal (%d item%s)", cartUseCase.getItemCount(),
                        cartUseCase.getItemCount() != 1 ? "s" : ""),
                cartUseCase.itemCountProperty()));
    }

    private void initListeners() {
        cartUseCase.selectedClientProperty()
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
        cartUseCase.clear();
    }

    @FXML
    private void handleRemoveSelectedClient() {
        cartUseCase.setSelectedClient(null);
    }

    @FXML
    private void showAddClientDialog() {
        navigationService.navigateTo("/view/clients.fxml");
    }

    @FXML
    private void handlePayButton() {
        if (cartUseCase.getItemCount() == 0)
            return;

        ModalService.showModal("/view/payment.fxml", "Pago", Modality.APPLICATION_MODAL, StageStyle.UNDECORATED,
                container, (PaymentController pc) -> {
                    pc.setTotalAmount(cartUseCase.getGrandTotal(), (paid, change, method) -> {
                        try {
                            List<com.mycompany.ventacontrolfx.domain.model.CartItem> items = new java.util.ArrayList<>(
                                    cartUseCase.getCartItems());
                            double total = cartUseCase.getGrandTotal();
                            Client client = cartUseCase.getSelectedClient();
                            Integer clientId = client != null ? client.getId() : null;
                            int userId = container.getUserSession().getCurrentUser().getUserId();

                            int saleId = container.getSaleUseCase().processSale(items, total, method, clientId, userId);

                            cartUseCase.clear();
                            container.getEventBus().publishDataChange();

                            javafx.application.Platform.runLater(() -> {
                                ModalService.showStandardModal("/view/receipt.fxml",
                                        client != null ? "Factura" : "Factura simplificada", container,
                                        (ReceiptController rc) -> {
                                            if (client != null)
                                                rc.setClientInfo(client);
                                            rc.setReceiptData(items, total, paid, change, method, saleId, null, null);
                                        });
                            });
                        } catch (SQLException e) {
                            e.printStackTrace();
                            AlertUtil.showError("Error al procesar venta", e.getMessage());
                        }
                    });
                });

    }
}
