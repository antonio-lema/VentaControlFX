package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.SaleConfig;
import com.mycompany.ventacontrolfx.model.User;
import com.mycompany.ventacontrolfx.service.NavigationService;
import com.mycompany.ventacontrolfx.service.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.Injectable;
import com.mycompany.ventacontrolfx.util.ModalService;
import com.mycompany.ventacontrolfx.util.SceneNavigator;
import com.mycompany.ventacontrolfx.util.UserSession;
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
    private HBox logoTextContainer;
    @FXML
    private Label lblAppName;
    @FXML
    private TextField searchField;
    @FXML
    private MenuButton userMenuButton;
    @FXML
    private HBox searchBarContainer;

    private ServiceContainer container;
    private NavigationService navigationService;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        loadLogo();
        setupUserMenu();
        setupSearch();
    }

    public void setNavigationService(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    private void loadLogo() {
        SaleConfig cfg = container.getConfigService().load();
        if (cfg.getLogoPath() != null && !cfg.getLogoPath().isEmpty()) {
            File file = new File(cfg.getLogoPath());
            if (file.exists()) {
                imgLogo.setImage(new Image(file.toURI().toString()));
                imgLogo.setVisible(true);
                imgLogo.setManaged(true);
                logoTextContainer.setVisible(false);
                logoTextContainer.setManaged(false);
                return;
            }
        }
        logoTextContainer.setVisible(true);
        if (lblAppName != null)
            lblAppName.setText(cfg.getAppName());
    }

    private void setupUserMenu() {
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
