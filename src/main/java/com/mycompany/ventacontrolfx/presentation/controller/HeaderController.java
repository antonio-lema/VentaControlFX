package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.application.usecase.ConfigUseCase;
import com.mycompany.ventacontrolfx.infrastructure.navigation.NavigationService;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.ModalService;
import com.mycompany.ventacontrolfx.util.SceneNavigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.File;

public class HeaderController implements Injectable {

    @FXML
    private ImageView imgLogo;
    @FXML
    private de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView defaultLogoIcon;
    @FXML
    private Label lblAppName;
    @FXML
    private Label lblHeaderUsername;
    @FXML
    private TextField searchField;
    @FXML
    private MenuButton userMenuButton;
    @FXML
    private HBox searchBarContainer;

    private ServiceContainer container;
    private NavigationService navigationService;
    private ConfigUseCase configUseCase;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.configUseCase = container.getConfigUseCase();
        this.navigationService = container.getNavigationService();
        loadLogo();
        setupUserMenu();
        setupSearch();
    }

    private void loadLogo() {
        SaleConfig cfg = configUseCase.getConfig();
        String path = cfg.getLogoPath();

        // Fallback to app icon if logo is not set
        if (path == null || path.isEmpty()) {
            path = cfg.getAppIconPath();
        }

        if (path != null && !path.isEmpty()) {
            File file = new File(path);
            if (file.exists()) {
                imgLogo.setImage(new Image(file.toURI().toString()));
                imgLogo.setVisible(true);
                imgLogo.setManaged(true);
                if (defaultLogoIcon != null) {
                    defaultLogoIcon.setVisible(false);
                    defaultLogoIcon.setManaged(false);
                }
                return;
            }
        }
        if (lblAppName != null)
            lblAppName.setText(cfg.getAppName());
    }

    private void setupUserMenu() {
        // Mostrar nombre del usuario en sesión
        if (lblHeaderUsername != null && container.getUserSession() != null) {
            var currentUser = container.getUserSession().getCurrentUser();
            if (currentUser != null) {
                String name = currentUser.getFullName() != null && !currentUser.getFullName().isBlank()
                        ? currentUser.getFullName()
                        : currentUser.getUsername();
                lblHeaderUsername.setText(name != null ? name : "Usuario");
            }
        }
        userMenuButton.setOnMouseEntered(e -> {
            if (!userMenuButton.isShowing())
                userMenuButton.show();
        });
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (navigationService != null) {
                navigationService.search(newVal);
            }
        });

        // Evitar que el campo de búsqueda robe el foco al arrancar
        Platform.runLater(() -> {
            if (searchBarContainer != null) {
                searchBarContainer.requestFocus();
            }
        });
    }

    public void setSearchBarVisible(boolean visible) {
        if (searchBarContainer != null) {
            searchBarContainer.setVisible(visible);
            searchBarContainer.setManaged(visible);
        }
    }

    @FXML
    private void handleCashClosure() {
        ModalService.showStandardModal("/view/cash_closure.fxml", "Cierre de Caja", container, null);
    }

    @FXML
    private void handleLogout() {
        if (AlertUtil.showConfirmation("Cerrar Sesión", "¿Estás seguro?", "Se cerrará la sesión actual.")) {
            container.getUserSession().logout();
            Stage stage = (Stage) userMenuButton.getScene().getWindow();
            SceneNavigator.loadScene(stage, "/view/login.fxml", "Login", 900, 600, false, container);
        }
    }
}
