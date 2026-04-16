package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.application.usecase.CartUseCase;
import com.mycompany.ventacontrolfx.application.usecase.RestoreSuspendedCartUseCase;
import com.mycompany.ventacontrolfx.infrastructure.navigation.NavigationService;
import com.mycompany.ventacontrolfx.presentation.renderer.CartListRenderer;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.util.ModalService;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import java.util.List;

public class CartController implements Injectable {

    @FXML
    private VBox cartItemsContainer, emptyCartView;
    @FXML
    private Label subtotalLabel, taxLabel, savingsLabel, itemsCountLabel, totalButtonLabel, lblSelectedClient,
            lblCurrentPriceList;
    @FXML
    private HBox hboxSavings;
    @FXML
    private Button btnClearCart, payButton, btnRemoveClient, showAddClientDialog;
    @FXML
    private VBox observationContainer;
    @FXML
    private Label lblGeneralObservation;
    @FXML
    private TextField txtPromoCode;

    private ServiceContainer container;
    private NavigationService navigationService;
    private CartUseCase cartUseCase;
    private RestoreSuspendedCartUseCase restoreSuspendedCartUseCase;
    private CartListRenderer cartRenderer;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.navigationService = container.getNavigationService();
        this.cartUseCase = container.getCartUseCase();
        this.restoreSuspendedCartUseCase = container.getRestoreSuspendedCartUseCase();

        com.mycompany.ventacontrolfx.domain.model.SaleConfig config = container.getICompanyConfigRepository().load();
        this.cartRenderer = new CartListRenderer(
                cartItemsContainer,
                cartUseCase,
                config.getTaxRate(),
                config.isPricesIncludeTax(),
                this.container);

        initBindings();
        initListeners();
        refreshPriceListLabel();
        updateClientUI(cartUseCase.getSelectedClient());
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
                () -> String.format(container.getBundle().getString("cart.summary.subtotal_format"),
                        cartUseCase.getSubtotal()),
                cartUseCase.subtotalProperty()));
        taxLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format(container.getBundle().getString("cart.summary.tax_format"), cartUseCase.getTax()),
                cartUseCase.taxProperty()));

        savingsLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format(container.getBundle().getString("cart.summary.savings_format"),
                        cartUseCase.getTotalSavings()),
                cartUseCase.totalSavingsProperty()));

        hboxSavings.visibleProperty().bind(cartUseCase.totalSavingsProperty().greaterThan(0));
        hboxSavings.managedProperty().bind(hboxSavings.visibleProperty());

        totalButtonLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("%.2f \u20ac", cartUseCase.getGrandTotal()),
                cartUseCase.grandTotalProperty()));
        itemsCountLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format(container.getBundle().getString("cart.summary.items_count_format"),
                        cartUseCase.getItemCount(),
                        cartUseCase.getItemCount() != 1
                                ? container.getBundle().getString("cart.summary.items_suffix_plural")
                                : container.getBundle().getString("cart.summary.items_suffix_singular")),
                cartUseCase.itemCountProperty()));

        // Observation Display Bindings
        observationContainer.visibleProperty().bind(cartUseCase.generalObservationProperty().isNotEmpty());
        observationContainer.managedProperty().bind(observationContainer.visibleProperty());
        lblGeneralObservation.textProperty().bind(cartUseCase.generalObservationProperty());

        // Promo Code Binding
        txtPromoCode.textProperty().bindBidirectional(cartUseCase.appliedPromoCodeProperty());
    }

    private void initListeners() {
        cartUseCase.selectedClientProperty()
                .addListener((obs, oldClient, newClient) -> updateClientUI(newClient));

        cartUseCase.priceListIdProperty()
                .addListener((obs, oldId, newId) -> {
                    if (newId != null) {
                        refreshPriceListLabel();
                        // Esperamos- [x] Investigar el estado de `CartController` para asegurar que la
                        // funcionalidad de borrado existe
                        // - [x] Corregir l\u00f3gica de visibilidad en `CartController.java`
                        // (a\u00f1adidos logs)
                        // - [x] A\u00f1adir `fx:id` en `cart_panel.fxml`
                        // - [x] Restaurar y simplificar estilos en `carrito.css`
                        // - [x] Forzar visibilidad y color en FXML para diagn\u00f3stico
                        // - [x] Verificar la visibilidad del icono de papelera
                        // a que el hilo de fondo de CartUseCase actualice los precios
                        // y luego forzamos un re-render completo de las filas del carrito.
                        // Esto garantiza que la UI siempre muestre el precio correcto
                        // (incluye el caso donde el precio es el mismo entre tarifas).
                        new Thread(() -> {
                            try {
                                Thread.sleep(400);
                            } catch (InterruptedException ignored) {
                            }
                            javafx.application.Platform.runLater(() -> cartRenderer.refreshAllPrices());
                        }, "cart-ui-refresh").start();
                    }
                });
    }

    private void refreshPriceListLabel() {
        try {
            int currentId = cartUseCase.getPriceListId();
            List<com.mycompany.ventacontrolfx.domain.model.PriceList> lists = container.getPriceListUseCase()
                    .getAll();
            lists.stream()
                    .filter(l -> l.getId() == currentId)
                    .findFirst()
                    .ifPresent(l -> lblCurrentPriceList.setText(l.getName()));
        } catch (Exception e) {
            lblCurrentPriceList.setText(container.getBundle().getString("cart.price_list.unknown"));
        }
    }

    private void updateClientUI(Client client) {
        System.out.println("[CartController] updateClientUI: client=" + (client != null ? client.getName() : "null"));
        if (client != null) {
            lblSelectedClient.setText(client.getName());
            lblSelectedClient.setStyle("-fx-text-fill: #1e88e5; -fx-font-weight: bold;");
        } else {
            lblSelectedClient.setText(container.getBundle().getString("cart.client.none"));
            lblSelectedClient.setStyle("");
        }
        btnRemoveClient.setVisible(client != null);
        btnRemoveClient.setManaged(client != null);
        showAddClientDialog.setVisible(client == null);
        showAddClientDialog.setManaged(client == null);
    }

    @FXML
    private void clearCart() {
        if (container.getUserSession().hasPermission("venta.limpiar")) {
            cartUseCase.clear();
        } else {
            AlertUtil.showError(container.getBundle().getString("cart.error.clear_denied.title"),
                    container.getBundle().getString("cart.error.clear_denied.msg"));
        }
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
    private void handleApplyPromo() {
        // Triggered by the button, we just force a refresh
        cartUseCase.updateTotals();
        AlertUtil.showToast(container.getBundle().getString("cart.promo.applied_toast") != null
                ? container.getBundle().getString("cart.promo.applied_toast")
                : "C\u00f3digo procesado");
    }

    @FXML
    private void handleChangePriceList() {
        try {
            List<com.mycompany.ventacontrolfx.domain.model.PriceList> lists = container.getPriceListUseCase()
                    .getAll();
            if (lists.isEmpty()) {
                AlertUtil.showWarning(container.getBundle().getString("price_list.error.load"),
                        container.getBundle().getString("cart.price_list.change.error_load"));
                return;
            }

            int currentId = cartUseCase.getPriceListId();
            com.mycompany.ventacontrolfx.domain.model.PriceList current = lists.stream()
                    .filter(l -> l.getId() == currentId)
                    .findFirst()
                    .orElse(lists.get(0));

            // Contenedor principal
            VBox root = new VBox(20);
            root.getStyleClass().add("modal-container");
            root.setPrefWidth(420);
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(30));

            // Cabecera
            VBox header = new VBox(10);
            header.setAlignment(Pos.CENTER);

            de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView icon = new de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView(
                    de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TAGS);
            icon.setSize("45px");
            icon.setFill(javafx.scene.paint.Color.valueOf("#2196f3")); // Azul primario

            Label title = new Label(container.getBundle().getString("cart.price_list.change.title"));
            title.getStyleClass().add("custom-modal-title");
            Label subtitle = new Label(container.getBundle().getString("cart.price_list.change.subtitle"));
            subtitle.setStyle("-fx-text-fill: #999999; -fx-font-size: 14px;");
            header.getChildren().addAll(icon, title, subtitle);

            // Campo de ComboBox
            VBox content = new VBox(8);
            content.setAlignment(Pos.CENTER_LEFT);
            Label lblHint = new Label(container.getBundle().getString("cart.price_list.change.label"));
            lblHint.setStyle("-fx-font-weight: bold; -fx-text-fill: #555555;");

            javafx.scene.control.ComboBox<com.mycompany.ventacontrolfx.domain.model.PriceList> cmbPriceList = new javafx.scene.control.ComboBox<>(
                    javafx.collections.FXCollections.observableArrayList(lists));
            cmbPriceList.getSelectionModel().select(current);
            cmbPriceList.getStyleClass().add("input-field-modern");
            cmbPriceList.setMaxWidth(Double.MAX_VALUE);
            cmbPriceList.setPrefHeight(45);

            // Cell Factory para el nombre de la tarifa
            cmbPriceList.setCellFactory(
                    lv -> new javafx.scene.control.ListCell<com.mycompany.ventacontrolfx.domain.model.PriceList>() {
                        @Override
                        protected void updateItem(com.mycompany.ventacontrolfx.domain.model.PriceList item,
                                boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                            } else {
                                setText(item.getName());
                            }
                        }
                    });
            cmbPriceList.setButtonCell(
                    new javafx.scene.control.ListCell<com.mycompany.ventacontrolfx.domain.model.PriceList>() {
                        @Override
                        protected void updateItem(com.mycompany.ventacontrolfx.domain.model.PriceList item,
                                boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                            } else {
                                setText(item.getName());
                            }
                        }
                    });

            content.getChildren().addAll(lblHint, cmbPriceList);

            // Botones
            HBox footer = new HBox(15);
            footer.setAlignment(Pos.CENTER);
            Button btnCancel = new Button(container.getBundle().getString("btn.cancel").toUpperCase());
            btnCancel.getStyleClass().add("btn-secondary");
            btnCancel.setPrefHeight(40);
            btnCancel.setPrefWidth(120);

            Button btnConfirm = new Button(container.getBundle().getString("cart.price_list.change.apply"));
            btnConfirm.getStyleClass().add("btn-primary");
            btnConfirm.setPrefHeight(40);
            btnConfirm.setPrefWidth(160);

            footer.getChildren().addAll(btnCancel, btnConfirm);
            root.getChildren().addAll(header, content, footer);

            // Stage Config
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);

            if (cartItemsContainer.getScene() != null) {
                stage.initOwner(cartItemsContainer.getScene().getWindow());
            }

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            if (container != null) {
                container.getThemeManager().applyFullTheme(scene);
            }
            stage.setScene(scene);

            // Animaci\u00f3n sutil
            root.setOpacity(0);
            root.setScaleX(0.9);
            root.setScaleY(0.9);

            stage.setOnShowing(evt -> {
                FadeTransition ft = new FadeTransition(Duration.millis(250), root);
                ft.setToValue(1.0);
                ScaleTransition st = new ScaleTransition(Duration.millis(250), root);
                st.setFromX(0.9);
                st.setFromY(0.9);
                st.setToX(1.0);
                st.setToY(1.0);
                ft.play();
                st.play();
            });

            btnCancel.setOnAction(e -> stage.close());

            btnConfirm.setOnAction(e -> {
                com.mycompany.ventacontrolfx.domain.model.PriceList selected = cmbPriceList.getSelectionModel()
                        .getSelectedItem();
                if (selected != null) {
                    cartUseCase.setPriceListId(selected.getId());
                    stage.close();
                    AlertUtil.showToast(
                            container.getBundle().getString("cart.price_list.change.toast") + selected.getName());
                }
            });

            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("cart.price_list.change.error_load"));
        }
    }

    @FXML
    private void handleSuspendCart() {
        if (!container.getUserSession().hasPermission("venta.aplazar")) {
            AlertUtil.showError(container.getBundle().getString("cart.suspend.denied.title"),
                    container.getBundle().getString("cart.suspend.denied.msg"));
            return;
        }
        if (cartUseCase.getItemCount() == 0) {
            AlertUtil.showWarning(container.getBundle().getString("cart.suspend.empty.title"),
                    container.getBundle().getString("cart.suspend.empty.msg"));
            return;
        }

        // Obtener usuario con seguridad para evitar el "pete"
        com.mycompany.ventacontrolfx.domain.model.User currentUser = container.getUserSession().getCurrentUser();
        if (currentUser == null) {
            AlertUtil.showError(container.getBundle().getString("cart.suspend.session_error.title"),
                    container.getBundle().getString("cart.suspend.session_error.msg"));
            return;
        }

        // Contenedor principal
        VBox root = new VBox(20);
        root.getStyleClass().add("modal-container");
        root.setPrefWidth(420);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));

        // Cabecera
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);

        de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView icon = new de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView(
                de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PAUSE_CIRCLE);
        icon.setSize("45px");
        icon.setFill(javafx.scene.paint.Color.valueOf("#fb8c00")); // Naranja c\u00e1lido (brand warning)

        Label title = new Label(container.getBundle().getString("cart.suspend.title"));
        title.getStyleClass().add("custom-modal-title");
        Label subtitle = new Label(container.getBundle().getString("cart.suspend.subtitle"));
        subtitle.setStyle("-fx-text-fill: #999999; -fx-font-size: 14px;");
        header.getChildren().addAll(icon, title, subtitle);

        // Campo de texto
        VBox content = new VBox(8);
        content.setAlignment(Pos.CENTER_LEFT);
        Label lblHint = new Label(container.getBundle().getString("cart.suspend.alias_label"));
        lblHint.setStyle("-fx-font-weight: bold; -fx-text-fill: #555555;");

        TextField txtAlias = new TextField(container.getBundle().getString("cart.suspend.alias_default")
                + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        txtAlias.getStyleClass().add("input-field-modern");
        txtAlias.setPrefHeight(45);
        content.getChildren().addAll(lblHint, txtAlias);

        // Botones
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER);
        Button btnCancel = new Button(container.getBundle().getString("btn.cancel").toUpperCase());
        btnCancel.getStyleClass().add("btn-secondary");
        btnCancel.setPrefHeight(40);
        btnCancel.setPrefWidth(120);

        Button btnConfirm = new Button(container.getBundle().getString("cart.suspend.apply"));
        btnConfirm.getStyleClass().add("btn-primary");
        btnConfirm.setPrefHeight(40);
        btnConfirm.setPrefWidth(160);

        footer.getChildren().addAll(btnCancel, btnConfirm);
        root.getChildren().addAll(header, content, footer);

        // Stage Config
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);

        // CORRECCI\u00d3N: Establecer due\u00f1o para evitar fallos en algunos sistemas
        if (cartItemsContainer.getScene() != null) {
            stage.initOwner(cartItemsContainer.getScene().getWindow());
        }

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        if (container != null) {
            container.getThemeManager().applyFullTheme(scene);
        }
        stage.setScene(scene);

        // Animaci\u00f3n sutil
        root.setOpacity(0);
        root.setScaleX(0.9);
        root.setScaleY(0.9);
        stage.setOnShowing(evt -> {
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(250),
                    root);
            ft.setToValue(1.0);
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(
                    javafx.util.Duration.millis(250),
                    root);
            st.setFromX(0.9);
            st.setFromY(0.9);
            st.setToX(1.0);
            st.setToY(1.0);
            ft.play();
            st.play();
        });

        btnCancel.setOnAction(e -> stage.close());

        btnConfirm.setOnAction(e -> {
            String alias = txtAlias.getText().trim();
            if (alias.isEmpty()) {
                txtAlias.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2;");
                javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                        javafx.util.Duration.millis(50), txtAlias);
                tt.setByX(5);
                tt.setCycleCount(6);
                tt.setAutoReverse(true);
                tt.play();
                return;
            }
            try {
                container.getSuspendedCartUseCase().suspendCart(
                        alias,
                        new java.util.ArrayList<>(cartUseCase.getCartItems()),
                        cartUseCase.getSelectedClient(),
                        currentUser.getUserId(),
                        cartUseCase.getGrandTotal());
                cartUseCase.clear();
                stage.close();
                AlertUtil.showToast(java.text.MessageFormat.format(
                        container.getBundle().getString("cart.suspend.success_toast"), alias));
            } catch (Exception ex) {
                ex.printStackTrace();
                AlertUtil.showError(container.getBundle().getString("alert.error"),
                        container.getBundle().getString("error.save") + ": " + ex.getMessage());
            }
        });

        stage.showAndWait();
    }

    @FXML
    private void handleShowSuspendedCarts() {
        try {
            ModalService.showTransparentModal("/view/suspended_carts_dialog.fxml",
                    container.getBundle().getString("cart.suspended.title"), container,
                    (SuspendedCartsDialogController controller) -> {
                        if (controller == null)
                            return;
                        controller.setOnCartSelected(
                                (com.mycompany.ventacontrolfx.domain.model.SuspendedCart suspendedCart) -> {
                                    try {
                                        restoreSuspendedCartUseCase.execute(suspendedCart.getId());
                                        AlertUtil.showToast(container.getBundle().getString("history.success.return")
                                                .replace("{0}", suspendedCart.getAlias())); // Reusing key for toast
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        AlertUtil.showError(container.getBundle().getString("alert.error"),
                                                container.getBundle().getString("history.error.load_details") + ": "
                                                        + e.getMessage());
                                    }
                                });
                    });
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("history.error.load") + ": " + e.getMessage());
        }
    }

    @FXML
    private void handlePayButton() {
        if (cartUseCase.getItemCount() == 0)
            return;

        double grandTotal = cartUseCase.getGrandTotal();
        Client selectedClient = cartUseCase.getSelectedClient();

        if (grandTotal > 1000) {
            if (selectedClient == null || selectedClient.getTaxId() == null
                    || selectedClient.getTaxId().trim().isEmpty()) {
                AlertUtil.showError(container.getBundle().getString("payment.error.id_required.title"),
                        container.getBundle().getString("payment.error.id_required.msg"));
                return;
            }
        }

        ModalService.showModal("/view/payment.fxml", container.getBundle().getString("payment.title"),
                Modality.APPLICATION_MODAL, StageStyle.UNDECORATED,
                container, (PaymentController pc) -> {
                    pc.setTotalAmount(cartUseCase.getGrandTotal(), (paid, change, method, cashAmount, cardAmount) -> {
                        try {
                            List<com.mycompany.ventacontrolfx.domain.model.CartItem> items = new java.util.ArrayList<>(
                                    cartUseCase.getCartItems());
                            double total = cartUseCase.getGrandTotal();
                            Client client = cartUseCase.getSelectedClient();
                            Integer clientId = client != null ? client.getId() : null;
                            String observations = cartUseCase.getGeneralObservation();
                            int userId = container.getUserSession().getCurrentUser().getUserId();
                            String promoCode = cartUseCase.getAppliedPromoCode();
                            System.out.println("[CartController] Processing sale: " + items.size() + " items. Promo: "
                                    + promoCode);

                            container.getAsyncManager().runAsyncTask(() -> {
                                // \u00e2\u201d\u20ac\u00e2\u201d\u20ac 1. PROCESAR VENTA EN HILO DE FONDO
                                // \u00e2\u201d\u20ac\u00e2\u201d\u20ac
                                com.mycompany.ventacontrolfx.application.usecase.SaleUseCase.ProcessSaleResult result = container
                                        .getSaleUseCase().processSale(items, total, method, clientId,
                                                userId, 0.0, null, cashAmount, cardAmount, observations, promoCode);
                                int saleId = result.saleId;

                                // \u00e2\u201d\u20ac\u00e2\u201d\u20ac 2. EMISI\u00d3N FISCAL AUTOM\u00c1TICA
                                // EN HILO DE FONDO \u00e2\u201d\u20ac\u00e2\u201d\u20ac
                                try {
                                    if (client != null && client.getTaxId() != null
                                            && !client.getTaxId().trim().isEmpty()) {
                                        String fullAddress = client.getAddress();
                                        if (fullAddress == null)
                                            fullAddress = "";
                                        if (client.getPostalCode() != null && !client.getPostalCode().isEmpty()) {
                                            fullAddress += ", " + client.getPostalCode();
                                        }
                                        if (client.getCity() != null && !client.getCity().isEmpty()) {
                                            fullAddress += " " + client.getCity();
                                        }
                                        if (client.getProvince() != null && !client.getProvince().isEmpty()) {
                                            fullAddress += " (" + client.getProvince() + ")";
                                        }

                                        container.getEmitFiscalDocumentUseCase().emitInvoice(
                                                saleId,
                                                client.getName(),
                                                client.getTaxId(),
                                                fullAddress);
                                    } else {
                                        container.getEmitFiscalDocumentUseCase().emitTicket(saleId);
                                    }
                                } catch (Exception fiscalEx) {
                                    System.err.println("Error en emisi\u00f3n fiscal: " + fiscalEx.getMessage());
                                }
                                return result;

                            }, (com.mycompany.ventacontrolfx.application.usecase.SaleUseCase.ProcessSaleResult result) -> {
                                int saleId = result.saleId;
                                String rewardCode = result.rewardPromoCode;
                                double rAmount = result.rewardAmount;
                                java.time.LocalDateTime rExpiry = result.rewardExpiryDate;
                                // \u00e2\u201d\u20ac\u00e2\u201d\u20ac 3. ACTUALIZAR UI EN HILO PRINCIPAL
                                // \u00e2\u201d\u20ac\u00e2\u201d\u20ac
                                cartUseCase.clear();
                                container.getEventBus().publishDataChange();

                                ModalService.showStandardModal("/view/receipt.fxml",
                                        client != null ? container.getBundle().getString("receipt.title.invoice")
                                                : container.getBundle().getString("receipt.title.simplified"),
                                        container,
                                        (ReceiptController rc) -> {
                                            if (client != null)
                                                rc.setClientInfo(client);
                                            rc.setReceiptData(items, total, paid, change, method, saleId, null, null,
                                                    observations, rewardCode, rAmount, rExpiry);
                                        });

                            }, (Throwable e) -> {
                                if (e.getMessage() != null && e.getMessage().contains("OPERACION_BLOQUEADA")) {
                                    showCashNotOpenAlert(e.getMessage().replace("OPERACION_BLOQUEADA: ", ""));
                                } else {
                                    AlertUtil.showError(container.getBundle().getString("cart.payment.error.process"),
                                            e.getMessage());
                                }
                            });
                        } catch (Exception e) {
                            AlertUtil.showError(container.getBundle().getString("cart.payment.error.unexpected"),
                                    e.getMessage());
                        }
                    });
                });
    }

    /**
     * Muestra un popup visualmente coherente informando que la caja no est\u00e1
     * abierta.
     */
    private void showCashNotOpenAlert(String message) {
        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(35));
        root.setPrefWidth(450);
        root.getStyleClass().add("modal-container");

        // Icono de advertencia premium
        FontAwesomeIconView iconView = new FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_CIRCLE);
        iconView.setSize("60px");
        iconView.setFill(javafx.scene.paint.Color.valueOf("#fb8c00")); // Brand warning orange

        VBox textContent = new VBox(10);
        textContent.setAlignment(Pos.CENTER);

        Label title = new Label(container.getBundle().getString("cart.cash_closed.title"));
        title.getStyleClass().add("modal-title");

        Label content = new Label(message);
        content.getStyleClass().add("modal-subtitle");
        content.setWrapText(true);
        content.setMaxWidth(380);
        content.setStyle("-fx-text-alignment: center;");

        textContent.getChildren().addAll(title, content);

        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER);

        Button btnClose = new Button(container.getBundle().getString("btn.close").toUpperCase());
        btnClose.getStyleClass().add("btn-primary");
        btnClose.setPrefWidth(180);
        btnClose.setPrefHeight(45);

        footer.getChildren().add(btnClose);
        root.getChildren().addAll(iconView, textContent, footer);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);

        if (cartItemsContainer.getScene() != null) {
            stage.initOwner(cartItemsContainer.getScene().getWindow());
        }

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        if (container != null) {
            container.getThemeManager().applyFullTheme(scene);
        }
        stage.setScene(scene);

        btnClose.setOnAction(e -> stage.close());

        // Animaci\u00f3n de entrada
        root.setOpacity(0);
        root.setScaleX(0.9);
        root.setScaleY(0.9);
        stage.setOnShowing(evt -> {
            FadeTransition ft = new FadeTransition(Duration.millis(300), root);
            ft.setToValue(1.0);
            ScaleTransition st = new ScaleTransition(Duration.millis(300), root);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
            ft.play();
        });

        stage.showAndWait();
    }

    @FXML
    private void handleAddManualItem() {
        VBox root = new VBox(20);
        root.getStyleClass().add("modal-container");
        root.setPrefWidth(400);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(25));

        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.PLUS_SQUARE);
        icon.setSize("40");
        icon.setFill(Color.valueOf("#4caf50")); // Green for "add"
        Label title = new Label(container.getBundle().getString("cart.manual_item.title"));
        title.getStyleClass().add("custom-modal-title");
        header.getChildren().addAll(icon, title);

        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER_LEFT);

        Label lblName = new Label(container.getBundle().getString("cart.manual_item.name_label"));
        lblName.getStyleClass().add("input-label-modern");
        TextField txtName = new TextField();
        txtName.setPromptText(container.getBundle().getString("cart.manual_item.name_prompt"));
        txtName.getStyleClass().add("input-field-modern");

        Label lblPrice = new Label(container.getBundle().getString("cart.manual_item.price_label"));
        lblPrice.getStyleClass().add("input-label-modern");
        TextField txtPrice = new TextField();
        txtPrice.setPromptText("0.00");
        txtPrice.getStyleClass().add("input-field-modern");

        // Solo permitir n\u00fameros y punto en el precio
        txtPrice.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                txtPrice.setText(oldVal);
            }
        });

        content.getChildren().addAll(lblName, txtName, lblPrice, txtPrice);

        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER);
        Button btnCancel = new Button(container.getBundle().getString("btn.cancel").toUpperCase());
        btnCancel.getStyleClass().add("btn-secondary");
        Button btnAdd = new Button(container.getBundle().getString("cart.manual_item.apply"));
        btnAdd.getStyleClass().add("btn-primary");
        footer.getChildren().addAll(btnCancel, btnAdd);

        root.getChildren().addAll(header, content, footer);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initOwner(cartItemsContainer.getScene().getWindow());

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        container.getThemeManager().applyFullTheme(scene);
        stage.setScene(scene);

        btnCancel.setOnAction(e -> stage.close());
        btnAdd.setOnAction(e -> {
            String name = txtName.getText().trim();
            String priceStr = txtPrice.getText().trim();

            if (name.isEmpty()) {
                AlertUtil.showWarning(container.getBundle().getString("cart.manual_item.error.incomplete.title"),
                        container.getBundle().getString("cart.manual_item.error.incomplete.msg"));
                return;
            }

            try {
                double price = priceStr.isEmpty() ? 0.0 : Double.parseDouble(priceStr);
                cartUseCase.addCustomItem(name, price);
                stage.close();
            } catch (NumberFormatException ex) {
                AlertUtil.showError(container.getBundle().getString("alert.error"),
                        container.getBundle().getString("cart.manual_item.price_label") + " invalid.");
            }
        });

        stage.show();
    }

    @FXML
    private void handleAddObservation() {
        // Modal sencillo para captura de texto
        VBox root = new VBox(20);
        root.getStyleClass().add("modal-container");
        root.setPrefWidth(400);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(25));

        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);
        FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.COMMENTING);
        icon.setSize("40");
        icon.setFill(Color.valueOf("#2196f3"));
        Label title = new Label(container.getBundle().getString("cart.observation.title"));
        title.getStyleClass().add("custom-modal-title");
        header.getChildren().addAll(icon, title);

        TextArea txtObs = new TextArea(cartUseCase.getGeneralObservation());
        txtObs.setPromptText(container.getBundle().getString("cart.observation.prompt"));
        txtObs.getStyleClass().add("text-area-modern");
        txtObs.setPrefHeight(100);
        txtObs.setWrapText(true);

        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER);
        Button btnCancel = new Button(container.getBundle().getString("btn.cancel").toUpperCase());
        btnCancel.getStyleClass().add("btn-secondary");
        Button btnApply = new Button(container.getBundle().getString("cart.observation.apply"));
        btnApply.getStyleClass().add("btn-primary");
        footer.getChildren().addAll(btnCancel, btnApply);

        root.getChildren().addAll(header, txtObs, footer);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initOwner(cartItemsContainer.getScene().getWindow());

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        container.getThemeManager().applyFullTheme(scene);
        stage.setScene(scene);

        btnCancel.setOnAction(e -> stage.close());
        btnApply.setOnAction(e -> {
            cartUseCase.setGeneralObservation(txtObs.getText().trim());
            stage.close();
        });

        stage.show();
    }

    @FXML
    private void handleClearObservation() {
        cartUseCase.setGeneralObservation("");
    }
}
