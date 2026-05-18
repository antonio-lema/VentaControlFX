package com.mycompany.ventacontrolfx.presentation.controller.user;

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
    private TableView<SaleConfig.SpecialDay> tableSpecials;
    @FXML
    private TableColumn<SaleConfig.SpecialDay, java.time.LocalDate> colDate;
    @FXML
    private TableColumn<SaleConfig.SpecialDay, String> colReason;
    @FXML
    private TableColumn<SaleConfig.SpecialDay, Boolean> colStatus;

    private List<SaleConfig.SpecialDay> specialDays;
    private Runnable onSaveCallback;
    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        setupTable();
    }

    private void setupTable() {
        tableSpecials.setPlaceholder(new Label(container != null ? container.getBundle().getString("special.days.no_data") : "No hay d\u00edas especiales introducidos"));
        colDate.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("date"));
        colReason.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("reason"));
        colStatus.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("closed"));

        colDate.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(java.time.LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String month = item.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, container != null ? container.getBundle().getLocale() : java.util.Locale.getDefault());
                    setText(item.getDayOfMonth() + " " + month.toUpperCase() + " " + item.getYear());
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
                }
            }
        });

        colReason.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(item == null || item.trim().isEmpty() ? "Sin descripci\u00f3n" : item);
                    setStyle("-fx-text-fill: #64748b;");
                }
            }
        });

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item ? "CERRADO" : "ABIERTO");
                    badge.setPadding(new javafx.geometry.Insets(4, 10, 4, 10));
                    badge.setMinWidth(80);
                    badge.setAlignment(javafx.geometry.Pos.CENTER);
                    if (item) {
                        badge.setStyle("-fx-background-color: #fef2f2; -fx-text-fill: #ef4444; -fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: 900; -fx-border-color: #fee2e2; -fx-border-radius: 12;");
                    } else {
                        badge.setStyle("-fx-background-color: #ecfdf5; -fx-text-fill: #10b981; -fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: 900; -fx-border-color: #d1fae5; -fx-border-radius: 12;");
                    }
                    setGraphic(badge);
                }
            }
        });
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
        SaleConfig.SpecialDay selected = tableSpecials.getSelectionModel().getSelectedItem();
        if (selected != null) {
            specialDays.remove(selected);
            refreshList();
        }
    }

    private void refreshList() {
        tableSpecials.setItems(javafx.collections.FXCollections.observableArrayList(specialDays));
    }

    @FXML
    private void handleSave() {
        if (onSaveCallback != null)
            onSaveCallback.run();
        close();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    public List<SaleConfig.SpecialDay> getResult() {
        return specialDays;
    }

    private double xOffset = 0, yOffset = 0;

    @FXML
    private void handleMousePressed(javafx.scene.input.MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    private void handleMouseDragged(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) pickerDate.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    private void close() {
        ((Stage) pickerDate.getScene().getWindow()).close();
    }
}

