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
import javafx.collections.FXCollections;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;

public class ClientsController implements Injectable, com.mycompany.ventacontrolfx.util.Searchable {

    @FXML
    private FlowPane clientsListContainer;
    @FXML
    private TextField searchField;
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
            java.util.List<Client> clients = clientUseCase.getAllClients();
            displayClients(clients);
        } catch (SQLException e) {
            AlertUtil.showError("Error", "No se pudieron cargar los clientes.");
        }
    }

    @Override
    public void handleSearch(String text) {
        try {
            displayClients(clientUseCase.searchClients(text));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void displayClients(java.util.List<Client> clients) {
        if (clientsListContainer == null)
            return;
        clientsListContainer.getChildren().clear();
        for (Client client : clients) {
            clientsListContainer.getChildren().add(createClientCard(client));
        }
        if (lblCount != null)
            lblCount.setText(clients.size() + " registros encontrados");
    }

    private Node createClientCard(Client client) {
        VBox card = new VBox(12);
        card.getStyleClass().add("client-card");
        card.setPrefWidth(280);
        card.setMinWidth(280);
        card.setStyle("-fx-background-color: -fx-bg-surface; -fx-padding: 20; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-cursor: hand;");

        // Cabecera: Icono y Nombre
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIconView icon = new FontAwesomeIconView(
                client.isIsCompany() ? FontAwesomeIcon.BUILDING : FontAwesomeIcon.USER);
        icon.setSize("24");
        icon.setFill(Color.valueOf(client.isIsCompany() ? "#2563eb" : "#64748b"));

        VBox nameBox = new VBox(2);
        Label nameLabel = new Label(client.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: -fx-text-custom-main;");
        nameLabel.setWrapText(true);

        Label typeLabel = new Label(client.isIsCompany() ? "Empresa" : "Particular");
        typeLabel.setStyle(
                "-fx-font-size: 11; -fx-text-fill: #94a3b8; -fx-text-transform: uppercase; -fx-letter-spacing: 1;");

        nameBox.getChildren().addAll(nameLabel, typeLabel);
        header.getChildren().addAll(icon, nameBox);

        // Info: CIF, Email, Teléfono
        VBox info = new VBox(8);
        info.getChildren().addAll(
                createDetailItem(FontAwesomeIcon.ID_CARD, client.getTaxId()),
                createDetailItem(FontAwesomeIcon.TAGS, getPriceListName(client.getPriceListId())),
                createDetailItem(FontAwesomeIcon.ENVELOPE, client.getEmail()),
                createDetailItem(FontAwesomeIcon.PHONE, client.getPhone()));

        // Footer: Ubicación y Botones
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);

        Label cityLabel = new Label(client.getCity());
        cityLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox actionButtons = new HBox(8);
        Button btnEdit = new Button();
        btnEdit.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PENCIL));
        btnEdit.setStyle(
                "-fx-background-color: #eff6ff; -fx-text-fill: #1e40af; -fx-padding: 5;");
        btnEdit.setOnAction(e -> {
            e.consume();
            if (container.getUserSession().hasPermission("cliente.editar")) {
                openClientForm(client);
            } else {
                AlertUtil.showError("Acceso Denegado", "No tiene permiso para editar clientes.");
            }
        });

        Button btnDelete = new Button();
        FontAwesomeIconView trashIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
        trashIcon.setSize("14");
        btnDelete.setGraphic(trashIcon);
        btnDelete.getStyleClass().add("btn-trash-small");
        btnDelete.setOnAction(e -> {
            e.consume();
            handleDelete(client);
        });

        actionButtons.getChildren().addAll(btnEdit, btnDelete);
        footer.getChildren().addAll(cityLabel, spacer, actionButtons);

        card.getChildren().addAll(header, info, footer);

        // Al hacer click en la card (fuera de botones) se selecciona
        card.setOnMouseClicked(e -> handleSelect(client));

        return card;
    }

    private HBox createDetailItem(FontAwesomeIcon glyph, String text) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        FontAwesomeIconView icon = new FontAwesomeIconView(glyph);
        icon.setSize("14");
        icon.setFill(Color.valueOf("#94a3b8"));
        Label label = new Label(text == null || text.isEmpty() ? "---" : text);
        label.setStyle("-fx-font-size: 13; -fx-text-fill: -fx-text-custom-light;");
        item.getChildren().addAll(icon, label);
        return item;
    }

    private void handleSelect(Client client) {
        if (client == null)
            return;
        if (onClientSelected != null) {
            onClientSelected.accept(client);
        } else {
            container.getCartUseCase().setSelectedClient(client);
        }
        handleClose();
    }

    private String getPriceListName(int priceListId) {
        if (priceListId <= 0)
            return "Tarifa Estándar";
        try {
            List<com.mycompany.ventacontrolfx.domain.model.PriceList> lists = container.getPriceListUseCase()
                    .getAll();
            return lists.stream()
                    .filter(l -> l.getId() == priceListId)
                    .findFirst()
                    .map(com.mycompany.ventacontrolfx.domain.model.PriceList::getName)
                    .orElse("Tarifa Estándar");
        } catch (Exception e) {
            return "Tarifa Estándar";
        }
    }

    @FXML
    private void handleAddClient() {
        if (!container.getUserSession().hasPermission("cliente.crear")) {
            AlertUtil.showError("Acceso Denegado", "No tiene permiso para registrar clientes.");
            return;
        }
        openClientForm(null);
    }

    private void openClientForm(Client client) {
        ModalService.showTransparentModal("/view/client_form.fxml",
                client == null ? "Nuevo Registro" : "Editar Registro",
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
        if (client == null)
            return;
        if (!container.getUserSession().hasPermission("cliente.eliminar")) {
            AlertUtil.showError("Acceso Denegado", "No tiene permiso para eliminar clientes.");
            return;
        }
        if (AlertUtil.showConfirmation("Eliminar", "¿Seguro que desea eliminar a " + client.getName() + "?",
                "Esta acción no se puede deshacer.")) {
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
            if (clientsListContainer != null && clientsListContainer.getScene() != null
                    && clientsListContainer.getScene().getWindow() != null) {
                ((Stage) clientsListContainer.getScene().getWindow()).close();
            }
        } else if (container != null && container.getNavigationService() != null) {
            container.getNavigationService().navigateTo("/view/sell_view.fxml");
        }
    }
}
