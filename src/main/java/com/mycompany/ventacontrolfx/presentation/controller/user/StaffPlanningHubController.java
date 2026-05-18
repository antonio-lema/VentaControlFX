package com.mycompany.ventacontrolfx.presentation.controller.user;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.BusinessHoursController;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for the Unified Staff Planning Hub with Custom Navigation.
 */
public class StaffPlanningHubController implements Injectable {

    @FXML private StackPane contentArea;
    @FXML private Button btnTabCalendar, btnTabBusinessHours;
    
    @FXML private Node staffCalendar, businessHours;
    
    @FXML private StaffCalendarController staffCalendarController;
    @FXML private BusinessHoursController businessHoursController;

    private ServiceContainer container;
    private List<Button> navButtons;
    private List<Node> contents;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.navButtons = Arrays.asList(btnTabCalendar, btnTabBusinessHours);
        this.contents = Arrays.asList(staffCalendar, businessHours);
        
        // Propagate injection
        if (staffCalendarController != null) staffCalendarController.inject(container);
        if (businessHoursController != null) businessHoursController.inject(container);
        
        showCalendar();
    }

    @FXML
    private void showCalendar() {
        switchTab(btnTabCalendar, staffCalendar);
    }

    @FXML
    private void showBusinessHours() {
        switchTab(btnTabBusinessHours, businessHours);
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

        // Refresh data when switching back to a tab
        if (activeContent == staffCalendar && staffCalendarController != null) {
            staffCalendarController.refresh();
        }
    }
}
