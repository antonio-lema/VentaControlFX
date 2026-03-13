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
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ButtonBar;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import com.mycompany.ventacontrolfx.presentation.theme.ThemeManager;

import java.util.List;
import java.sql.SQLException;

public class CartController implements Injectable {

    @FXML
    private VBox cartItemsContainer, emptyCartView;
    @FXML
    private Label subtotalLabel, taxLabel, savingsLabel, itemsCountLabel, totalButtonLabel, lblSelectedClient,
            lblCurrentPriceList;
    @FXML
    private HBox hboxSavings;
    @FXML
    private Button btnClearCart, payButton, btnRemoveClient;

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
                config.isPricesIncludeTax());

        initBindings();
        initListeners();
        refreshPriceListLabel();
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

        savingsLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("🎉 -%.2f €", cartUseCase.getTotalSavings()),
                cartUseCase.totalSavingsProperty()));

        hboxSavings.visibleProperty().bind(cartUseCase.totalSavingsProperty().greaterThan(0));
        hboxSavings.managedProperty().bind(hboxSavings.visibleProperty());

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

        cartUseCase.priceListIdProperty()
                .addListener((obs, oldId, newId) -> {
                    if (newId != null) {
                        refreshPriceListLabel();
                        // Esperamos a que el hilo de fondo de CartUseCase actualice los precios
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
            lblCurrentPriceList.setText("Tarifa Desconocida");
        }
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
        if (container.getUserSession().hasPermission("venta.limpiar")) {
            cartUseCase.clear();
        } else {
            AlertUtil.showError("Acceso Denegado", "No tiene permiso para vaciar el carrito.");
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
    private void handleChangePriceList() {
        try {
            List<com.mycompany.ventacontrolfx.domain.model.PriceList> lists = container.getPriceListUseCase()
                    .getAll();
            if (lists.isEmpty()) {
                AlertUtil.showWarning("Sin Tarifas", "No hay otras tarifas configuradas.");
                return;
            }

            int currentId = cartUseCase.getPriceListId();
            com.mycompany.ventacontrolfx.domain.model.PriceList current = lists.stream()
                    .filter(l -> l.getId() == currentId)
                    .findFirst()
                    .orElse(lists.get(0));

            javafx.scene.control.ChoiceDialog<com.mycompany.ventacontrolfx.domain.model.PriceList> dialog = new javafx.scene.control.ChoiceDialog<>(
                    current, lists);
            dialog.setTitle("Cambiar Tarifa");
            dialog.setHeaderText("Seleccione la tarifa para esta venta");
            dialog.setContentText("Tarifa activa:");

            // Estilizar un poco el diálogo si es posible o usar ModalService si tuviera
            // soporte para esto
            dialog.showAndWait().ifPresent(selected -> {
                cartUseCase.setPriceListId(selected.getId());
                AlertUtil.showToast("Cambiado a: " + selected.getName());
            });

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "No se pudieron cargar las tarifas disponibles.");
        }
    }

    @FXML
    private void handleSuspendCart() {
        if (!container.getUserSession().hasPermission("venta.aplazar")) {
            AlertUtil.showError("Acceso Denegado", "No tiene permiso para aplazar ventas.");
            return;
        }
        if (cartUseCase.getItemCount() == 0) {
            AlertUtil.showWarning("Carrito Vacío", "No hay productos para aplazar.");
            return;
        }

        // Obtener usuario con seguridad para evitar el "pete"
        com.mycompany.ventacontrolfx.domain.model.User currentUser = container.getUserSession().getCurrentUser();
        if (currentUser == null) {
            AlertUtil.showError("Error de Sesión", "No hay un usuario activo. Reinicie la aplicación.");
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
        icon.setFill(javafx.scene.paint.Color.valueOf("#fb8c00")); // Naranja cálido (brand warning)

        Label title = new Label("Aplazar Venta");
        title.getStyleClass().add("custom-modal-title");
        Label subtitle = new Label("Asigne un nombre para identificar esta venta luego");
        subtitle.setStyle("-fx-text-fill: #999999; -fx-font-size: 14px;");
        header.getChildren().addAll(icon, title, subtitle);

        // Campo de texto
        VBox content = new VBox(8);
        content.setAlignment(Pos.CENTER_LEFT);
        Label lblHint = new Label("Nombre o Identificador:");
        lblHint.setStyle("-fx-font-weight: bold; -fx-text-fill: #555555;");

        TextField txtAlias = new TextField(
                "Venta " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        txtAlias.getStyleClass().add("input-field-modern");
        txtAlias.setPrefHeight(45);
        content.getChildren().addAll(lblHint, txtAlias);

        // Botones
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER);
        Button btnCancel = new Button("CANCELAR");
        btnCancel.getStyleClass().add("btn-secondary");
        btnCancel.setPrefHeight(40);
        btnCancel.setPrefWidth(120);

        Button btnConfirm = new Button("APLAZAR AHORA");
        btnConfirm.getStyleClass().add("btn-primary");
        btnConfirm.setPrefHeight(40);
        btnConfirm.setPrefWidth(160);

        footer.getChildren().addAll(btnCancel, btnConfirm);
        root.getChildren().addAll(header, content, footer);

        // Stage Config
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);

        // CORRECCIÓN: Establecer dueño para evitar fallos en algunos sistemas
        if (cartItemsContainer.getScene() != null) {
            stage.initOwner(cartItemsContainer.getScene().getWindow());
        }

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        if (container != null) {
            container.getThemeManager().applyFullTheme(scene);
        }
        stage.setScene(scene);

        // Animación sutil
        root.setOpacity(0);
        root.setScaleX(0.9);
        root.setScaleY(0.9);
        stage.setOnShowing(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent evt) {
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
            }
        });

        btnCancel.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                stage.close();
            }
        });

        btnConfirm.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
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
                    AlertUtil.showToast("Venta '" + alias + "' guardada correctamente.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    AlertUtil.showError("Error", "No se pudo guardar la venta: " + ex.getMessage());
                }
            }
        });

        stage.showAndWait();
    }

    @FXML
    private void handleShowSuspendedCarts() {
        try {
            ModalService.showTransparentModal("/view/suspended_carts_dialog.fxml", "Carritos Aplazados", container,
                    new java.util.function.Consumer<SuspendedCartsDialogController>() {
                        @Override
                        public void accept(SuspendedCartsDialogController controller) {
                            if (controller == null)
                                return;
                            controller.setOnCartSelected(
                                    new java.util.function.Consumer<com.mycompany.ventacontrolfx.domain.model.SuspendedCart>() {
                                        @Override
                                        public void accept(
                                                com.mycompany.ventacontrolfx.domain.model.SuspendedCart suspendedCart) {
                                            try {
                                                restoreSuspendedCartUseCase.execute(suspendedCart.getId());
                                                AlertUtil.showToast(
                                                        "Venta '" + suspendedCart.getAlias() + "' recuperada.");
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                AlertUtil.showError("Error al recuperar",
                                                        "No se pudo recuperar la venta: " + e.getMessage());
                                            }
                                        }
                                    });
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "No se pudo abrir la lista de ventas aplazadas: " + e.getMessage());
        }
    }

    @FXML
    private void handlePayButton() {
        if (cartUseCase.getItemCount() == 0)
            return;

        ModalService.showModal("/view/payment.fxml", "Pago", Modality.APPLICATION_MODAL, StageStyle.UNDECORATED,
                container, new java.util.function.Consumer<PaymentController>() {
                    @Override
                    public void accept(PaymentController pc) {
                        pc.setTotalAmount(cartUseCase.getGrandTotal(), (paid, change, method) -> {
                            try {
                                List<com.mycompany.ventacontrolfx.domain.model.CartItem> items = new java.util.ArrayList<>(
                                        cartUseCase.getCartItems());
                                double total = cartUseCase.getGrandTotal();
                                Client client = cartUseCase.getSelectedClient();
                                Integer clientId = client != null ? client.getId() : null;
                                int userId = container.getUserSession().getCurrentUser().getUserId();

                                int saleId = container.getSaleUseCase().processSale(items, total, method, clientId,
                                        userId);

                                // ── EMISIÓN FISCAL AUTOMÁTICA ──
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
                                    System.err.println("Error en emisión fiscal: " + fiscalEx.getMessage());
                                }

                                cartUseCase.clear();
                                container.getEventBus().publishDataChange();

                                javafx.application.Platform.runLater(() -> {
                                    ModalService.showStandardModal("/view/receipt.fxml",
                                            client != null ? "Factura" : "Factura simplificada", container,
                                            new java.util.function.Consumer<ReceiptController>() {
                                                @Override
                                                public void accept(ReceiptController rc) {
                                                    if (client != null)
                                                        rc.setClientInfo(client);
                                                    rc.setReceiptData(items, total, paid, change, method, saleId, null,
                                                            null);
                                                }
                                            });
                                });
                            } catch (SQLException e) {
                                e.printStackTrace();
                                if (e.getMessage() != null && e.getMessage().contains("OPERACION_BLOQUEADA")) {
                                    showCashNotOpenAlert(e.getMessage().replace("OPERACION_BLOQUEADA: ", ""));
                                } else {
                                    AlertUtil.showError("Error al procesar venta", e.getMessage());
                                }
                            }
                        });
                    }
                });
    }

    /**
     * Muestra un popup visualmente coherente informando que la caja no está
     * abierta.
     */
    private void showCashNotOpenAlert(String message) {
        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(35));
        root.setPrefWidth(450);
        root.getStyleClass().add("modal-container");

        // Icono de advertencia premium
        javafx.scene.image.ImageView iconImg = null; // Reemplazar con FontAwesome si está disponible en este contexto
        FontAwesomeIconView iconView = new FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_CIRCLE);
        iconView.setSize("60px");
        iconView.setFill(javafx.scene.paint.Color.valueOf("#fb8c00")); // Brand warning orange

        VBox textContent = new VBox(10);
        textContent.setAlignment(Pos.CENTER);

        Label title = new Label("CAJA CERRADA");
        title.getStyleClass().add("modal-title");

        Label content = new Label(message);
        content.getStyleClass().add("modal-subtitle");
        content.setWrapText(true);
        content.setMaxWidth(380);
        content.setStyle("-fx-text-alignment: center;");

        textContent.getChildren().addAll(title, content);

        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER);

        Button btnClose = new Button("ENTENDIDO");
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

        // Animación de entrada
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
}
