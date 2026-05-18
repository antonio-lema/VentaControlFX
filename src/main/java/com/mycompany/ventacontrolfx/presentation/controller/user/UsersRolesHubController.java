package com.mycompany.ventacontrolfx.presentation.controller.user;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.ManageUsersController;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.ManageRolesController;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for the Unified Users and Roles Security Hub.
 */
public class UsersRolesHubController implements Injectable {

    @FXML private StackPane contentArea;
    @FXML private Button btnTabUsers, btnTabRoles;
    
    @FXML private Node manageUsers, manageRoles;
    
    @FXML private ManageUsersController manageUsersController;
    @FXML private ManageRolesController manageRolesController;

    private ServiceContainer container;
    private List<Button> navButtons;
    private List<Node> contents;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.navButtons = Arrays.asList(btnTabUsers, btnTabRoles);
        this.contents = Arrays.asList(manageUsers, manageRoles);
        
        // Propagate injection
        if (manageUsersController != null) {
            manageUsersController.inject(container);
        }
        if (manageRolesController != null) {
            manageRolesController.inject(container);
        }
        
        showUsers();
    }

    @FXML
    private void showUsers() {
        switchTab(btnTabUsers, manageUsers);
    }

    @FXML
    private void showRoles() {
        switchTab(btnTabRoles, manageRoles);
    }

    private void switchTab(Button activeBtn, Node activeContent) {
        navButtons.forEach(btn -> {
            btn.getStyleClass().remove("hub-nav-button-active");
            if (!btn.getStyleClass().contains("hub-nav-button")) {
                btn.getStyleClass().add("hub-nav-button");
            }
        });
        activeBtn.getStyleClass().add("hub-nav-button-active");

        contents.forEach(node -> {
            node.setVisible(false);
            node.setManaged(false);
        });
        activeContent.setVisible(true);
        activeContent.setManaged(true);
        
        // Refresh active views if needed
        if (activeContent == manageUsers && manageUsersController != null) {
            // ManageUsersController updates on visible change or refresh if needed, but its initialize handles it.
        } else if (activeContent == manageRoles && manageRolesController != null) {
            // ManageRolesController updates
        }
    }
    
    /**
     * Programmatically switches to the Roles tab.
     */
    public void selectRolesTab() {
        showRoles();
    }

    /**
     * Programmatically switches to the Users tab.
     */
    public void selectUsersTab() {
        showUsers();
    }
}
