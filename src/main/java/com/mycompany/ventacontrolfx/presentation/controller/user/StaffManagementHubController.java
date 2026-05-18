package com.mycompany.ventacontrolfx.presentation.controller.user;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.controller.dashboard.OperativeDashboardController;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.PunctualityAuditController;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.WorkSessionController;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for the Unified Staff Management Hub with Custom Navigation.
 */
public class StaffManagementHubController implements Injectable {

    @FXML private StackPane contentArea;
    @FXML private Button btnTabMyShift, btnTabMonitor, btnTabAudit;
    
    // Injected child containers/views (FX:ID from include)
    @FXML private Node shiftManagement, operativeDashboard, punctualityAudit;
    
    // Injected child controllers (FX:ID + "Controller")
    @FXML private WorkSessionController shiftManagementController;
    @FXML private OperativeDashboardController operativeDashboardController;
    @FXML private PunctualityAuditController punctualityAuditController;

    private ServiceContainer container;
    private List<Button> navButtons;
    private List<Node> contents;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.navButtons = Arrays.asList(btnTabMyShift, btnTabMonitor, btnTabAudit);
        this.contents = Arrays.asList(shiftManagement, operativeDashboard, punctualityAudit);
        
        // Propagate injection
        if (shiftManagementController != null) shiftManagementController.inject(container);
        if (operativeDashboardController != null) operativeDashboardController.inject(container);
        if (punctualityAuditController != null) punctualityAuditController.inject(container);
        
        // Ensure first tab is showing
        showMyShift();
    }

    @FXML
    private void showMyShift() {
        switchTab(btnTabMyShift, shiftManagement);
    }

    @FXML
    private void showMonitor() {
        switchTab(btnTabMonitor, operativeDashboard);
        if (operativeDashboardController != null) {
            operativeDashboardController.refreshData();
        }
    }

    @FXML
    private void showAudit() {
        switchTab(btnTabAudit, punctualityAudit);
        if (punctualityAuditController != null) {
            punctualityAuditController.refresh();
        }
    }

    private void switchTab(Button activeBtn, Node activeContent) {
        // Update Buttons Style
        navButtons.forEach(btn -> {
            btn.getStyleClass().remove("hub-nav-button-active");
            if (!btn.getStyleClass().contains("hub-nav-button")) {
                btn.getStyleClass().add("hub-nav-button");
            }
        });
        activeBtn.getStyleClass().add("hub-nav-button-active");

        // Update Content Visibility
        contents.forEach(node -> {
            node.setVisible(false);
            node.setManaged(false);
        });
        activeContent.setVisible(true);
        activeContent.setManaged(true);
    }
}
