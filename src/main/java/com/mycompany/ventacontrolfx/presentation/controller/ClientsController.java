package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.application.usecase.ClientUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.ModalService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.function.Consumer;
import java.sql.SQLException;
import java.util.List;

public class ClientsController implements Injectable, com.mycompany.ventacontrolfx.util.Searchable {

    @FXML
    private TextField searchField;
    @FXML
    private FlowPane clientsListContainer;
    @FXML
    private Label lblCount;

    private ClientUseCase clientUseCase;
    private ServiceContainer container;
    private Runnable closeAction;
    private java.util.function.Consumer<Client> onClientSelected;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.clientUseCase = container.getClientUseCase();
        loadClients();
        setupSearch();
    }

    public void init(Runnable closeAction) {
        this.closeAction = closeAction;
    }

    public void setOnClientSelected(java.util.function.Consumer<Client> listener) {
        this.onClientSelected = listener;
    }

    private void loadClients() {
        try {
            List<Client> clients = clientUseCase.getAllClients();
            displayClients(clients);
        } catch (SQLException e) {
            AlertUtil.showError("Error", "No se pudieron cargar los clientes.");
        }
    }

    @Override
    public void handleSearch(String text) {
        if (searchField != null) {
            searchField.setText(text);
        }
        try {
            displayClients(clientUseCase.searchClients(text));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void displayClients(List<Client> clients) {
        clientsListContainer.getChildren().clear();
        if (lblCount != null)
            lblCount.setText(clients.size() + " clientes");
        if (clients.isEmpty()) {
            clientsListContainer.getChildren().add(new Label("No se encontraron clientes"));
            return;
        }
        for (Client client : clients) {
            clientsListContainer.getChildren().add(createClientCard(client));
        }
    }

    private Node createClientCard(Client client) {
        VBox card = new VBox(12);
        card.getStyleClass().add("client-card");
        card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 4); " +
                "-fx-border-color: #f0f0f0; -fx-border-width: 1; -fx-border-radius: 15;");
        card.setPrefWidth(280);

        // Header with Icon and Name
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane iconContainer = new StackPane();
        iconContainer.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 10; -fx-padding: 10;");
        FontAwesomeIconView userIcon = new FontAwesomeIconView(FontAwesomeIcon.BUILDING);
        userIcon.setFill(javafx.scene.paint.Color.valueOf("#1e88e5"));
        userIcon.setSize("20");
        iconContainer.getChildren().add(userIcon);

        VBox nameBox = new VBox(2);
        Label name = new Label(client.getName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 15; -fx-text-fill: #2c3e50;");
        name.setWrapText(true);

        Label cif = new Label("CIF: " + client.getTaxId());
        cif.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12;");
        nameBox.getChildren().addAll(name, cif);

        header.getChildren().addAll(iconContainer, nameBox);

        // Separator
        Separator sep = new Separator();
        sep.setStyle("-fx-opacity: 0.5;");

        // Actions
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnSelect = new Button("Seleccionar");
        btnSelect.setStyle("-fx-background-color: #1e88e5; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 8; -fx-padding: 8 15; -fx-cursor: hand;");
        btnSelect.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.CHECK));
        ((FontAwesomeIconView) btnSelect.getGraphic()).setFill(javafx.scene.paint.Color.WHITE);
        btnSelect.setOnAction(e -> {
            if (onClientSelected != null) {
                onClientSelected.accept(client);
            } else {
                container.getCartUseCase().setSelectedClient(client);
            }
            handleClose();
        });

        Button btnEdit = new Button();
        btnEdit.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PENCIL));
        btnEdit.setStyle(
                "-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 8; -fx-cursor: hand; -fx-border-color: #eee; -fx-border-radius: 8;");
        btnEdit.setOnAction(e -> openClientForm(client));

        Button btnDelete = new Button();
        btnDelete.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
        btnDelete.setStyle(
                "-fx-background-color: #fff5f5; -fx-background-radius: 8; -fx-padding: 8; -fx-cursor: hand; -fx-border-color: #ffe0e0; -fx-border-radius: 8;");
        ((FontAwesomeIconView) btnDelete.getGraphic()).setFill(javafx.scene.paint.Color.valueOf("#e53935"));
        btnDelete.setOnAction(e -> handleDelete(client));

        actions.getChildren().addAll(btnSelect, btnEdit, btnDelete);
        card.getChildren().addAll(header, sep, actions);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle()
                + "-fx-border-color: #1e88e5; -fx-effect: dropshadow(three-pass-box, rgba(30,136,229,0.2), 15, 0, 0, 6);"));
        card.setOnMouseExited(
                e -> card.setStyle(card.getStyle().replace("-fx-border-color: #1e88e5;", "-fx-border-color: #f0f0f0;")
                        .replace("-fx-effect: dropshadow(three-pass-box, rgba(30,136,229,0.2), 15, 0, 0, 6);",
                                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 4);")));

        return card;
    }

    @FXML
    private void handleAddClient() {
        openClientForm(null);
    }

    private void openClientForm(Client client) {
        ModalService.showTransparentModal("/view/client_form.fxml", client == null ? "Nuevo Cliente" : "Editar Cliente",
                container, (ClientFormController ctrl) -> {
                    ctrl.init(client);
                });
        loadClients();
    }

    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, old, nv) -> handleSearch(nv));
        }
    }

    private void handleDelete(Client client) {
        if (AlertUtil.showConfirmation("Eliminar", "¿Seguro que desea eliminar a " + client.getName() + "?", "")) {
            try {
                clientUseCase.deleteClient(client.getId());
                loadClients();
            } catch (SQLException e) {
                AlertUtil.showError("Error", "No se pudo eliminar el cliente.");
            }
        }
    }

    @FXML
    private void handleClose() {
        if (closeAction != null) {
            closeAction.run();
        } else if (onClientSelected != null) {
            // Se usa como modal picker
            if (searchField != null && searchField.getScene() != null && searchField.getScene().getWindow() != null) {
                ((Stage) searchField.getScene().getWindow()).close();
            }
        } else if (container != null && container.getNavigationService() != null) {
            // Se usa mediante navegación, volvemos a la vista de venta
            container.getNavigationService().navigateTo("/view/sell_view.fxml");
        }
    }
}
