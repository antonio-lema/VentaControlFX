package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.application.usecase.UserUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.ModalService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ManageUsersController implements Injectable {

    @FXML
    private FlowPane userCardsPane;
    @FXML
    private TextField txtSearch;
    @FXML
    private Label lblCount;

    private UserUseCase userUseCase;
    private ServiceContainer container;
    private ObservableList<User> userList = FXCollections.observableArrayList();

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.userUseCase = container.getUserUseCase();
        loadUsers();

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filterUsers(newValue));
        }
    }

    private void loadUsers() {
        try {
            List<User> users = userUseCase.listUsers();
            userList.setAll(users);
            renderUserCards(userList);
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "No se pudieron cargar los usuarios: " + e.getMessage());
        }
    }

    private void renderUserCards(List<User> users) {
        userCardsPane.getChildren().clear();
        if (lblCount != null)
            lblCount.setText(users.size() + " usuarios encontrados");
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

        String initial = (user.getFullName() != null && !user.getFullName().isEmpty())
                ? user.getFullName().substring(0, 1).toUpperCase()
                : "?";
        Label avatar = new Label(initial);
        avatar.getStyleClass().add("user-card-avatar");
        avatar.setPrefSize(60, 60);
        avatar.setAlignment(Pos.CENTER);

        VBox info = new VBox(5);
        info.setAlignment(Pos.CENTER);
        Label name = new Label(user.getFullName());
        name.getStyleClass().add("user-name-label");
        Label username = new Label("@" + user.getUsername());
        username.getStyleClass().add("user-username-label");
        Label email = new Label(user.getEmail());
        email.getStyleClass().add("user-email-label");
        info.getChildren().addAll(name, username, email);

        if (user.getCompanyName() != null && !user.getCompanyName().isEmpty()) {
            Label company = new Label(user.getCompanyName());
            company.setStyle("-fx-text-fill: -color-primary; -fx-font-size: 13px; -fx-font-weight: bold;");
            info.getChildren().add(company);
        }

        Label badge = new Label(user.getRole().toUpperCase());
        badge.getStyleClass()
                .add(user.getRole().equalsIgnoreCase("admin") ? "user-card-badge-admin" : "user-card-badge-user");

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button btnEdit = new Button();
        btnEdit.getStyleClass().add("user-action-btn");
        FontAwesomeIconView editIcon = new FontAwesomeIconView(FontAwesomeIcon.PENCIL);
        editIcon.setSize("18");
        editIcon.getStyleClass().add("sidebar-icon"); // Reutilizar clase si aplica o dejar a CSS
        btnEdit.setGraphic(editIcon);
        btnEdit.setOnAction(e -> handleEditSingleUser(user));

        Button btnDelete = new Button();
        btnDelete.getStyleClass().add("user-action-btn");
        FontAwesomeIconView deleteIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
        deleteIcon.setSize("18");
        btnDelete.setGraphic(deleteIcon);
        btnDelete.setOnAction(e -> handleDeleteSingleUser(user));

        actions.getChildren().addAll(btnEdit, btnDelete);
        card.getChildren().addAll(avatar, info, badge, actions);
        return card;
    }

    private void filterUsers(String query) {
        if (query == null || query.isEmpty()) {
            renderUserCards(userList);
        } else {
            String q = query.toLowerCase();
            List<User> filtered = userList.stream().filter(u -> u.getUsername().toLowerCase().contains(q) ||
                    u.getFullName().toLowerCase().contains(q) ||
                    (u.getEmail() != null && u.getEmail().toLowerCase().contains(q)))
                    .collect(java.util.stream.Collectors.toList());
            renderUserCards(filtered);
        }
    }

    @FXML
    private void handleNewUser() {
        ModalService.showTransparentModal("/view/register_user.fxml", "Registrar Usuario", container, null);
        loadUsers();
    }

    private void handleEditSingleUser(User user) {
        ModalService.showTransparentModal("/view/register_user.fxml", "Editar Usuario", container,
                (RegisterUserController controller) -> {
                    controller.setUser(user);
                });
        loadUsers();
    }

    private void handleDeleteSingleUser(User user) {
        if (AlertUtil.showConfirmation("Eliminar Usuario", "¿Eliminar a " + user.getUsername() + "?",
                "Esta acción es irreversible.")) {
            try {
                if (userUseCase.deleteUser(user.getUserId())) {
                    loadUsers();
                } else {
                    AlertUtil.showError("Error", "No se pudo eliminar el usuario");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                AlertUtil.showError("Error", "Error al eliminar usuario: " + e.getMessage());
            }
        }
    }
}
