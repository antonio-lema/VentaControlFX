package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.Client;
import com.mycompany.ventacontrolfx.service.CartService;
import com.mycompany.ventacontrolfx.service.ClientService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import java.sql.SQLException;
import java.util.List;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;

public class ClientsController implements com.mycompany.ventacontrolfx.util.Injectable {

    @FXML
    private TextField searchField;
    @FXML
    private FlowPane clientsListContainer;
    @FXML
    private Button btnToggleForm;
    @FXML
    private Label lblCount;

    private ClientService clientService;
    private CartService cartService;
    private Runnable closeAction;
    private com.mycompany.ventacontrolfx.service.ServiceContainer container;

    @Override
    public void inject(com.mycompany.ventacontrolfx.service.ServiceContainer container) {
        this.container = container;
        this.clientService = container.getClientService();
        this.cartService = container.getCartService();
        loadClients();
        setupSearch();
    }

    public void init(CartService cartService, Runnable closeAction) {
        this.cartService = cartService;
        this.closeAction = closeAction;
    }

    @FXML
    private void handleToggleForm() {
        openClientForm(null);
    }

    private void openClientForm(Client client) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/client_form.fxml"));
            Parent root = loader.load();

            ClientFormController controller = loader.getController();
            controller.inject(container); // CRÍTICO: inyectar antes de init() para que clientService no sea null
            controller.init(client);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());
            scene.setFill(null);
            stage.setScene(scene);

            stage.showAndWait();

            // Reload if saved
            if (controller.isSaveClicked()) {
                loadClients();
            }

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "No se pudo abrir el formulario");
        }
    }

    private void loadClients() {
        try {
            List<Client> clients = clientService.getAllClients();
            displayClients(clients);

            // Note: StatusBar update is now handled via GlobalEventBus if applicable
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "No se pudieron cargar las empresas");
        }
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                List<Client> filtered = clientService.searchClients(newVal);
                displayClients(filtered);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void displayClients(List<Client> clients) {
        clientsListContainer.getChildren().clear();
        if (lblCount != null) {
            lblCount.setText(clients.size() + " empresas encontradas");
        }

        if (clients.isEmpty()) {
            Label placeholder = new Label("No se encontraron empresas");
            placeholder.setStyle("-fx-text-fill: #a0aec0; -fx-padding: 20;");
            clientsListContainer.getChildren().add(placeholder);
            return;
        }

        for (Client client : clients) {
            clientsListContainer.getChildren().add(createClientCard(client));
        }
    }

    private Node createClientCard(Client client) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPrefWidth(260); // Card width
        card.setMinWidth(260);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 15; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");

        // Check if currently selected in cart
        if (cartService != null && cartService.getSelectedClient() != null &&
                cartService.getSelectedClient().getId() == client.getId()) {
            card.setStyle(card.getStyle() + "-fx-border-color: #1e88e5; -fx-border-width: 2; -fx-border-radius: 15;");
        }

        // Header: Avatar + Name
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label(client.getName().length() > 0 ? client.getName().substring(0, 1).toUpperCase() : "?");
        avatar.setStyle(
                "-fx-background-color: #e3f2fd; -fx-text-fill: #1e88e5; -fx-font-weight: bold; -fx-font-size: 18; -fx-background-radius: 50; -fx-alignment: center;");
        avatar.setPrefSize(40, 40);

        Label name = new Label(client.getName());
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 15;");
        name.setWrapText(true);

        header.getChildren().addAll(avatar, name);

        // Details
        VBox details = new VBox(5);
        details.setPadding(new Insets(10, 0, 10, 0));

        Label cifLabel = new Label("CIF: " + client.getTaxId());
        cifLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12;");

        Label phoneLabel = new Label("Tel: " + client.getPhone());
        phoneLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12;");

        Label emailLabel = new Label(client.getEmail());
        emailLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12;");

        details.getChildren().addAll(cifLabel, phoneLabel, emailLabel);
        VBox.setVgrow(details, Priority.ALWAYS);

        // Actions
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);

        Button btnSelect = new Button("Seleccionar");
        btnSelect.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnSelect, Priority.ALWAYS);
        btnSelect.setStyle(
                "-fx-background-color: #1e88e5; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold;");
        btnSelect.setOnAction(e -> {
            if (cartService != null) {
                cartService.setSelectedClient(client);
                handleClose();
                if (container != null && container.getNavigationService() != null) {
                    container.getNavigationService().navigateTo("/view/sell_view.fxml");
                }
            }
        });

        Button btnEdit = new Button();
        btnEdit.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PENCIL));
        btnEdit.setStyle("-fx-background-color: #f7fafc; -fx-cursor: hand; -fx-background-radius: 8;");
        btnEdit.setOnAction(e -> openClientForm(client));

        Button btnDelete = new Button();
        btnDelete.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
        btnDelete.setStyle("-fx-background-color: #fff5f5; -fx-cursor: hand; -fx-background-radius: 8;");
        ((FontAwesomeIconView) btnDelete.getGraphic()).setFill(javafx.scene.paint.Color.web("#e53935"));
        btnDelete.setOnAction(e -> handleDelete(client));

        actions.getChildren().addAll(btnSelect, btnEdit, btnDelete);

        card.getChildren().addAll(header, details, actions);

        // Hover effect mimics existing
        card.setOnMouseEntered(e -> card
                .setStyle(card.getStyle() + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 5);"));
        card.setOnMouseExited(e -> card.setStyle(
                card.getStyle().replace("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 5);",
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);")));

        return card;
    }

    private void handleDelete(Client client) {
        boolean confirmed = AlertUtil.showConfirmation("Eliminar Empresa",
                "¿Estás seguro de eliminar a " + client.getName() + "?", "Esta acción no se puede deshacer.");
        if (confirmed) {
            try {
                clientService.deleteClient(client.getId());
                loadClients();
            } catch (SQLException e) {
                e.printStackTrace();
                AlertUtil.showError("Error", "No se pudo eliminar la empresa");
            }
        }
    }

    @FXML
    private void handleClose() {
        if (closeAction != null) {
            closeAction.run();
        } else {
            // Fallback for independent windows
            if (searchField.getScene() != null && searchField.getScene().getWindow() != null) {
                Stage stage = (Stage) searchField.getScene().getWindow();
                // Check if it's a modal dialog (not the main application stage)
                if (stage.getModality() != javafx.stage.Modality.NONE) {
                    stage.close();
                } else {
                    // It's running in the main view. Just notify.
                    AlertUtil.showInfo("Selección Exitosa",
                            "La empresa seleccionada se ha aplicado al carrito.\nRegresa a 'TPV' (Ventas) para continuar.");
                }
            }
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        switch (type) {
            case ERROR:
                AlertUtil.showError(title, content);
                break;
            case WARNING:
                AlertUtil.showWarning(title, content);
                break;
            case CONFIRMATION:
                AlertUtil.showConfirmation(title, content, "");
                break;
            default:
                AlertUtil.showInfo(title, content);
                break;
        }
    }
}
