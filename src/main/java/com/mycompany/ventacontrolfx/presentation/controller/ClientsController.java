package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.application.usecase.ClientUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.component.SkeletonClientBox;
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
        if (clientsListContainer == null) return;
        
        // Mostrar esqueletos
        clientsListContainer.getChildren().clear();
        for (int i = 0; i < 8; i++) {
            clientsListContainer.getChildren().add(new SkeletonClientBox());
        }
        
        container.getAsyncManager().runAsyncTask(() -> {
            try {
                return clientUseCase.getAllClients();
            } catch (SQLException e) {
                return null;
            }
        }, (clients) -> {
            if (clients != null) {
                displayClients((java.util.List<Client>) clients);
            } else {
                AlertUtil.showError(container.getBundle().getString("alert.error"),
                        container.getBundle().getString("client.error.load"));
                clientsListContainer.getChildren().clear();
            }
        }, null);
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
            lblCount.setText(clients.size() + " " + container.getBundle().getString("clients.count_suffix"));
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

        Label typeLabel = new Label(client.isIsCompany() ? container.getBundle().getString("client.type.company")
                : container.getBundle().getString("client.type.individual"));
        typeLabel.setStyle(
                "-fx-font-size: 11; -fx-text-fill: #94a3b8; -fx-text-transform: uppercase; -fx-letter-spacing: 1;");

        nameBox.getChildren().addAll(nameLabel, typeLabel);
        header.getChildren().addAll(icon, nameBox);

        // Info: CIF, Email, Tel\u00e9fono
        VBox info = new VBox(8);
        info.getChildren().addAll(
                createDetailItem(FontAwesomeIcon.ID_CARD, client.getTaxId()),
                createDetailItem(FontAwesomeIcon.TAGS, getPriceListName(client.getPriceListId())),
                createDetailItem(FontAwesomeIcon.ENVELOPE, client.getEmail()),
                createDetailItem(FontAwesomeIcon.PHONE, client.getPhone()));

        // Footer: Ubicaci\u00f3n y Botones
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);

        Label cityLabel = new Label(client.getCity());
        cityLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #64748b;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox actionButtons = new HBox(8);

        // Botón Editar Circular
        Button btnEdit = new Button();
        btnEdit.getStyleClass().add("btn-action-edit");
        FontAwesomeIconView editIcon = new FontAwesomeIconView(FontAwesomeIcon.PENCIL);
        editIcon.setSize("16");
        btnEdit.setGraphic(editIcon);
        btnEdit.setOnAction(e -> {
            e.consume();
            if (container.getUserSession().hasPermission("cliente.editar")) {
                openClientForm(client);
            } else {
                AlertUtil.showError(container.getBundle().getString("alert.access_denied"),
                        container.getBundle().getString("error.no_permission"));
            }
        });

        // Botón Eliminar / Desbloquear según permisos
        Button btnAction = new Button();
        boolean canDelete = container.getUserSession().hasPermission("cliente.eliminar");
        
        if (canDelete) {
            btnAction.getStyleClass().add("btn-action-delete");
            FontAwesomeIconView trashIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH_ALT);
            trashIcon.setSize("16");
            btnAction.setGraphic(trashIcon);
            btnAction.setTooltip(new Tooltip(container.getBundle().getString("client.btn.delete")));
            btnAction.setOnAction(e -> {
                e.consume();
                handleDelete(client);
            });
        } else {
            btnAction.getStyleClass().add("btn-action-lock");
            btnAction.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-background-radius: 50;");
            FontAwesomeIconView lockIcon = new FontAwesomeIconView(FontAwesomeIcon.LOCK);
            lockIcon.setSize("16");
            lockIcon.setFill(Color.WHITE);
            btnAction.setGraphic(lockIcon);
            btnAction.setTooltip(new Tooltip(container.getBundle().getString("client.btn.unlock")));
            btnAction.setOnAction(e -> {
                e.consume();
                AlertUtil.showWarning(container.getBundle().getString("alert.locked"),
                        container.getBundle().getString("client.msg.locked_action"));
            });
        }

        actionButtons.getChildren().addAll(btnEdit, btnAction);
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
            return container.getBundle().getString("client.price_list.standard");
        try {
            List<com.mycompany.ventacontrolfx.domain.model.PriceList> lists = container.getPriceListUseCase()
                    .getAll();
            return lists.stream()
                    .filter(l -> l.getId() == priceListId)
                    .findFirst()
                    .map(com.mycompany.ventacontrolfx.domain.model.PriceList::getName)
                    .orElse(container.getBundle().getString("client.price_list.standard"));
        } catch (Exception e) {
            return container.getBundle().getString("client.price_list.standard");
        }
    }

    @FXML
    private void handleAddClient() {
        if (!container.getUserSession().hasPermission("cliente.crear")) {
            AlertUtil.showError(container.getBundle().getString("alert.access_denied"),
                    container.getBundle().getString("error.no_permission"));
            return;
        }
        openClientForm(null);
    }

    private void openClientForm(Client client) {
        ModalService.showTransparentModal("/view/client_form.fxml",
                client == null ? container.getBundle().getString("client.dialog.new")
                        : container.getBundle().getString("client.dialog.edit"),
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
            AlertUtil.showError(container.getBundle().getString("alert.access_denied"),
                    container.getBundle().getString("error.no_permission"));
            return;
        }

        String title = container.getBundle().getString("alert.confirm");
        String header = container.getBundle().getString("client.confirm.delete") + " " + client.getName() + "?";

        if (AlertUtil.showConfirmation(title, header, "")) {
            try {
                clientUseCase.deleteClient(client.getId());
                loadClients();
            } catch (SQLException e) {
                AlertUtil.showError(container.getBundle().getString("alert.error"),
                        container.getBundle().getString("client.error.delete"));
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
