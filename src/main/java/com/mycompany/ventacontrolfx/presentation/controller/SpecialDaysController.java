package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.BusinessDay;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SpecialDaysController implements Injectable {

    @FXML
    private DatePicker pickerDate;
    @FXML
    private TextField txtReason;
    @FXML
    private CheckBox chkClosed;
    @FXML
    private ListView<SaleConfig.SpecialDay> listSpecials;

    private List<SaleConfig.SpecialDay> specialDays;
    private Runnable onSaveCallback;
    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
    }

    public void initData(List<SaleConfig.SpecialDay> existing, Runnable onSave) {
        this.specialDays = new ArrayList<>(existing);
        this.onSaveCallback = onSave;
        refreshList();
    }

    @FXML
    private void handleAdd() {
        LocalDate date = pickerDate.getValue();
        if (date == null)
            return;

        SaleConfig.SpecialDay sd = new SaleConfig.SpecialDay(date, chkClosed.isSelected(), txtReason.getText());
        if (!chkClosed.isSelected()) {
            // Default shift for special open days
            sd.getShifts().add(new BusinessDay.TimeRange(LocalTime.of(9, 0), LocalTime.of(14, 0)));
        }
        specialDays.add(sd);
        txtReason.clear();
        pickerDate.setValue(null);
        refreshList();
    }

    @FXML
    private void handleRemove() {
        SaleConfig.SpecialDay selected = listSpecials.getSelectionModel().getSelectedItem();
        if (selected != null) {
            specialDays.remove(selected);
            refreshList();
        }
    }

    private void refreshList() {
        listSpecials.getItems().setAll(specialDays);
        listSpecials.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SaleConfig.SpecialDay item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(15);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    row.setPadding(new javafx.geometry.Insets(10));
                    row.setStyle(
                            "-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0;");

                    javafx.scene.layout.VBox vDate = new javafx.scene.layout.VBox(2);
                    vDate.setAlignment(javafx.geometry.Pos.CENTER);
                    vDate.setPrefWidth(60);
                    vDate.setPadding(new javafx.geometry.Insets(0, 10, 0, 0));
                    vDate.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 0 1 0 0;");

                    Label lblDay = new Label(String.valueOf(item.getDate().getDayOfMonth()));
                    lblDay.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #1e293b;");
                    Label lblMonth = new Label(item.getDate().getMonth().name().substring(0, 3));
                    lblMonth.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-text-transform: uppercase;");
                    vDate.getChildren().addAll(lblDay, lblMonth);

                    javafx.scene.layout.VBox vInfo = new javafx.scene.layout.VBox(3);
                    Label lblReason = new Label(item.getReason());
                    lblReason.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
                    Label lblSub = new Label(container != null 
                            ? String.format(container.getBundle().getString("special.days.label.year"), item.getDate().getYear())
                            : "Año " + item.getDate().getYear());
                    lblSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
                    vInfo.getChildren().addAll(lblReason, lblSub);

                    javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                    javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                    Label badge = new Label(item.isClosed() 
                            ? (container != null ? container.getBundle().getString("special.status.closed") : "CERRADO")
                            : (container != null ? container.getBundle().getString("special.status.open") : "ABIERTO"));
                    badge.setPadding(new javafx.geometry.Insets(4, 10, 4, 10));
                    badge.setMinWidth(75);
                    badge.setAlignment(javafx.geometry.Pos.CENTER);
                    if (item.isClosed()) {
                        badge.setStyle(
                                "-fx-background-color: #fef2f2; -fx-text-fill: #ef4444; -fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: bold; -fx-border-color: #fee2e2; -fx-border-radius: 12;");
                    } else {
                        badge.setStyle(
                                "-fx-background-color: #ecfdf5; -fx-text-fill: #10b981; -fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: bold; -fx-border-color: #d1fae5; -fx-border-radius: 12;");
                    }

                    row.getChildren().addAll(vDate, vInfo, spacer, badge);
                    setGraphic(row);
                    setText(null);
                }
            }
        });
    }

    @FXML
    private void handleSave() {
        if (onSaveCallback != null)
            onSaveCallback.run();
        close();
    }

    public List<SaleConfig.SpecialDay> getResult() {
        return specialDays;
    }

    private void close() {
        ((Stage) pickerDate.getScene().getWindow()).close();
    }
}
