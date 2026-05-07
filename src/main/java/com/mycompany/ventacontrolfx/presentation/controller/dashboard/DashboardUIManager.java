package com.mycompany.ventacontrolfx.presentation.controller.dashboard;

import com.mycompany.ventacontrolfx.presentation.component.SkeletonScheduleItem;
import com.mycompany.ventacontrolfx.presentation.component.SkeletonStaffRow;
import com.mycompany.ventacontrolfx.presentation.component.SkeletonStatCard;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Gestor de la interfaz de usuario del Dashboard.
 * Maneja estados de carga (skeletons) y actualización de KPIs.
 */
public class DashboardUIManager {

    private final ServiceContainer container;
    private final Label lblStaffCount, lblCashStatus, lblCashAmount, lblOpenTime, lblCompliance;
    private final VBox skeletonStaffContainer, vboxTodaySchedule;
    private final TableView<?> tableStaff;

    public DashboardUIManager(ServiceContainer container, Label lblStaffCount, Label lblCashStatus, Label lblCashAmount, 
                             Label lblOpenTime, Label lblCompliance, VBox skeletonStaffContainer, VBox vboxTodaySchedule, TableView<?> tableStaff) {
        this.container = container;
        this.lblStaffCount = lblStaffCount;
        this.lblCashStatus = lblCashStatus;
        this.lblCashAmount = lblCashAmount;
        this.lblOpenTime = lblOpenTime;
        this.lblCompliance = lblCompliance;
        this.skeletonStaffContainer = skeletonStaffContainer;
        this.vboxTodaySchedule = vboxTodaySchedule;
        this.tableStaff = tableStaff;
    }

    public void showSkeletons(boolean show) {
        Platform.runLater(() -> {
            if (show) {
                lblStaffCount.setGraphic(new SkeletonStatCard()); lblStaffCount.setText("");
                lblCashAmount.setGraphic(new SkeletonStatCard()); lblCashAmount.setText("");
                lblOpenTime.setGraphic(new SkeletonStatCard()); lblOpenTime.setText("");
                lblCompliance.setGraphic(new SkeletonStatCard()); lblCompliance.setText("");
                
                renderStaffSkeletons();
                renderScheduleSkeletons();
            } else {
                lblStaffCount.setGraphic(null);
                lblCashAmount.setGraphic(null);
                lblOpenTime.setGraphic(null);
                lblCompliance.setGraphic(null);
                if (skeletonStaffContainer != null) { skeletonStaffContainer.setVisible(false); skeletonStaffContainer.setManaged(false); }
            }
        });
    }

    private void renderStaffSkeletons() {
        if (skeletonStaffContainer == null) return;
        skeletonStaffContainer.getChildren().clear();
        for (int i = 0; i < 6; i++) {
            skeletonStaffContainer.getChildren().add(new SkeletonStaffRow());
            Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
            skeletonStaffContainer.getChildren().add(spacer);
        }
        skeletonStaffContainer.prefHeightProperty().bind(tableStaff.heightProperty());
        skeletonStaffContainer.prefWidthProperty().bind(tableStaff.widthProperty());
        skeletonStaffContainer.setStyle("-fx-background-color: white; -fx-padding: 38 20 20 20;");
        skeletonStaffContainer.setVisible(true); skeletonStaffContainer.setManaged(true);
    }

    private void renderScheduleSkeletons() {
        if (vboxTodaySchedule == null) return;
        vboxTodaySchedule.getChildren().clear();
        for (int i = 0; i < 5; i++) { vboxTodaySchedule.getChildren().add(new SkeletonScheduleItem()); }
    }

    public void updateKPIs(int activeStaff, boolean isCashOpen, double cashAmount, String openTime) {
        Platform.runLater(() -> {
            lblStaffCount.setText(String.valueOf(activeStaff));
            lblCashStatus.setText(container.getBundle().getString("dashboard.cash." + (isCashOpen ? "open" : "closed")));
            lblCashStatus.setStyle(isCashOpen ? "-fx-text-fill: #16a34a;" : "-fx-text-fill: #ef4444;");
            lblCashAmount.setText(String.format("%.2f \u20ac", cashAmount));
            lblOpenTime.setText(openTime != null ? openTime : "--:--");
        });
    }
}


