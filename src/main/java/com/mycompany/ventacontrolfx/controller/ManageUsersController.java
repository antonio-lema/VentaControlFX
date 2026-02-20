package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.User;
import com.mycompany.ventacontrolfx.service.UserService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ManageUsersController {

    @FXML
    private FlowPane userCardsPane;

    @FXML
    private TextField txtSearch;

    private final UserService userService = new UserService();
    private ObservableList<User> userList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        loadUsers();

        // Listener para búsqueda
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
                filterUsers(newValue);
            });
        }
    }

    private void loadUsers() {
        try {
            List<User> users = userService.getAllUsers();
            userList.setAll(users);
            renderUserCards(userList);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudieron cargar los usuarios: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void renderUserCards(List<User> users) {
        userCardsPane.getChildren().clear();
        for (User user : users) {
            userCardsPane.getChildren().add(createUserCard(user));
        }
    }

    private Node createUserCard(User user) {
        VBox card = new VBox(15);
        card.getStyleClass().add("user-card");
        card.setPrefWidth(280);
        card.setMinWidth(280);
        card.setAlignment(Pos.TOP_CENTER);

        // Avatar / Icon
        Label avatar = new Label(user.getFullName().substring(0, 1).toUpperCase());
        avatar.getStyleClass().add("user-card-avatar");
        avatar.setPrefSize(60, 60);
        avatar.setAlignment(Pos.CENTER);

        // Info
        VBox info = new VBox(5);
        info.setAlignment(Pos.CENTER);
        Label name = new Label(user.getFullName());
        name.getStyleClass().add("user-name-label");
        Label username = new Label("@" + user.getUsername());
        username.getStyleClass().add("user-username-label");
        Label email = new Label(user.getEmail());
        email.getStyleClass().add("user-email-label");
        info.getChildren().addAll(name, username, email);

        // Badge
        Label badge = new Label(user.getRole().toUpperCase());
        badge.getStyleClass()
                .add(user.getRole().equalsIgnoreCase("admin") ? "user-card-badge-admin" : "user-card-badge-user");

        // Actions
        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button btnEdit = new Button();
        btnEdit.getStyleClass().add("user-action-btn");
        FontAwesomeIconView editIcon = new FontAwesomeIconView(FontAwesomeIcon.PENCIL);
        editIcon.setSize("16");
        editIcon.setFill(javafx.scene.paint.Color.web("#1e88e5"));
        btnEdit.setGraphic(editIcon);
        btnEdit.setOnAction(e -> handleEditSingleUser(user));

        Button btnDelete = new Button();
        btnDelete.getStyleClass().add("user-action-btn");
        FontAwesomeIconView deleteIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
        deleteIcon.setSize("16");
        deleteIcon.setFill(javafx.scene.paint.Color.web("#e53935"));
        btnDelete.setGraphic(deleteIcon);
        btnDelete.setOnAction(e -> handleDeleteSingleUser(user));

        actions.getChildren().addAll(btnEdit, btnDelete);

        card.getChildren().addAll(avatar, info, badge, actions);
        return card;
    }

    @FXML
    private void handleSearch() {
        if (txtSearch != null) {
            filterUsers(txtSearch.getText());
        }
    }

    private void filterUsers(String query) {
        if (query == null || query.isEmpty()) {
            renderUserCards(userList);
        } else {
            List<User> filteredList = new ArrayList<>();
            for (User u : userList) {
                if (u.getUsername().toLowerCase().contains(query.toLowerCase()) ||
                        u.getFullName().toLowerCase().contains(query.toLowerCase()) ||
                        (u.getEmail() != null && u.getEmail().toLowerCase().contains(query.toLowerCase()))) {
                    filteredList.add(u);
                }
            }
            renderUserCards(filteredList);
        }
    }

    @FXML
    private void handleNewUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/register_user.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);

            Scene scene = new Scene(root);
            scene.setFill(null);
            scene.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Registrar Usuario");
            stage.showAndWait();

            // Recargar al volver
            loadUsers();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo abrir la ventana de registro", Alert.AlertType.ERROR);
        }
    }

    private void handleEditSingleUser(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/register_user.fxml"));
            Parent root = loader.load();

            // Configure controller for edit mode
            RegisterUserController controller = loader.getController();
            controller.setUser(user);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);

            Scene scene = new Scene(root);
            scene.setFill(null);
            scene.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Editar Usuario");
            stage.showAndWait();

            // Reload users after dialog closes
            loadUsers();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo abrir la ventana de edición: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleDeleteSingleUser(User user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Eliminar Usuario");
        alert.setHeaderText("¿Eliminar a " + user.getUsername() + "?");
        alert.setContentText("Esta acción es irreversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean deleted = userService.deleteUser(user.getUserId());
                if (deleted) {
                    loadUsers();
                } else {
                    showAlert("Error", "No se pudo eliminar el usuario", Alert.AlertType.ERROR);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Error al eliminar usuario: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleEditUser() {
    }

    @FXML
    private void handleDeleteUser() {
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
