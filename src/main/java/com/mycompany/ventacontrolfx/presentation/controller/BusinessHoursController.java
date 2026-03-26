package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.ConfigUseCase;
import com.mycompany.ventacontrolfx.application.usecase.UserUseCase;
import com.mycompany.ventacontrolfx.domain.model.BusinessDay;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BusinessHoursController implements Injectable {

    @FXML
    private GridPane gridSchedule;
    @FXML
    private TextField txtGracePeriod;

    private ConfigUseCase configUseCase;
    private UserUseCase userUseCase;
    private SaleConfig currentConfig;
    private List<User> allUsers = new ArrayList<>();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    // Tracks the container and components for each day (1-7)
    private final Map<Integer, VBox> shiftContainers = new HashMap<>();
    private final CheckBox[] closedChecks = new CheckBox[8];

    @Override
    public void inject(ServiceContainer container) {
        this.configUseCase = container.getConfigUseCase();
        this.userUseCase = container.getUserUseCase();
        try {
            this.allUsers = userUseCase.getAllUsers();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.currentConfig = configUseCase.getConfig();
        setupUI();
    }

    private void setupUI() {
        gridSchedule.getChildren().clear();
        List<BusinessDay> schedule = currentConfig.getSchedule();
        String[] dayNames = { "", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo" };

        for (int i = 1; i <= 7; i++) {
            final int dayIdx = i;
            BusinessDay dayData = schedule.stream()
                    .filter(d -> d.getDayOfWeek() == dayIdx)
                    .findFirst()
                    .orElse(new BusinessDay(i, false));

            // Shift container (FlowPane is better for multiple shifts)
            VBox vBoxShifts = new VBox(6);
            vBoxShifts.setPadding(new javafx.geometry.Insets(5, 0, 5, 0));
            shiftContainers.put(dayIdx, vBoxShifts);

            closedChecks[i] = new CheckBox("Día Cerrado");
            closedChecks[i].setSelected(dayData.isClosed());
            vBoxShifts.disableProperty().bind(closedChecks[i].selectedProperty());

            // Load shifts
            if (dayData.getShifts().isEmpty() && !dayData.isClosed()) {
                addShiftRow(dayIdx, LocalTime.of(9, 0), LocalTime.of(14, 0), new ArrayList<>());
            } else {
                for (BusinessDay.TimeRange range : dayData.getShifts()) {
                    addShiftRow(dayIdx, range.getOpen(), range.getClose(), range.getAssignedUserIds());
                }
            }

            // Modern Buttons
            Button btnAdd = new Button();
            btnAdd.setGraphic(new FontAwesomeIconView(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLUS_CIRCLE));
            btnAdd.getStyleClass().add("btn-icon-primary");
            btnAdd.setTooltip(new Tooltip("Añadir otro turno"));
            btnAdd.setOnAction(e -> addShiftRow(dayIdx, LocalTime.of(17, 0), LocalTime.of(21, 0), new ArrayList<>()));
            btnAdd.disableProperty().bind(closedChecks[i].selectedProperty());

            Button btnCopy = new Button();
            btnCopy.setGraphic(new FontAwesomeIconView(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.COPY));
            btnCopy.getStyleClass().add("btn-icon-secondary");
            btnCopy.setTooltip(new Tooltip("Copiar a todos los demás días"));
            btnCopy.setOnAction(e -> copyShiftsToAll(dayIdx));
            btnCopy.disableProperty().bind(closedChecks[i].selectedProperty());

            // Labels and styling
            Label lblDay = new Label(dayNames[i].toUpperCase());
            lblDay.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");

            HBox dayRow = new HBox(15, lblDay);
            dayRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            dayRow.setPadding(new javafx.geometry.Insets(10, 15, 10, 15));
            dayRow.setStyle(
                    "-fx-background-color: " + (i % 2 == 0 ? "#f8fafc" : "white") + "; -fx-background-radius: 8;");

            gridSchedule.add(lblDay, 0, i - 1);
            gridSchedule.add(vBoxShifts, 1, i - 1);
            gridSchedule.add(new HBox(10, closedChecks[i], btnAdd, btnCopy), 2, i - 1);
        }

        if (txtGracePeriod != null) {
            txtGracePeriod.setText(String.valueOf(currentConfig.getScheduleGracePeriodMins()));
        }
    }

    private void copyShiftsToAll(int sourceDay) {
        VBox sourceContainer = shiftContainers.get(sourceDay);
        List<BusinessDay.TimeRange> sourceShifts = new ArrayList<>();
        for (javafx.scene.Node node : sourceContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox row = (HBox) node;
                TextField tOpen = (TextField) row.getChildren().get(1);
                TextField tClose = (TextField) row.getChildren().get(3);
                List<Integer> assignedUserIds = (List<Integer>) row.getUserData();
                sourceShifts.add(
                        new BusinessDay.TimeRange(LocalTime.parse(tOpen.getText()), LocalTime.parse(tClose.getText()),
                                assignedUserIds));
            }
        }

        for (int i = 1; i <= 7; i++) {
            if (i == sourceDay)
                continue;
            VBox targetContainer = shiftContainers.get(i);
            targetContainer.getChildren().clear();
            for (BusinessDay.TimeRange r : sourceShifts) {
                addShiftRow(i, r.getOpen(), r.getClose(), r.getAssignedUserIds());
            }
            closedChecks[i].setSelected(false);
        }
    }

    private void addShiftRow(int dayIdx, LocalTime open, LocalTime close, List<Integer> assignedIds) {
        VBox container = shiftContainers.get(dayIdx);

        TextField txtOpen = new TextField(open.format(timeFormatter));
        txtOpen.setPrefWidth(70);
        txtOpen.setAlignment(javafx.geometry.Pos.CENTER);
        txtOpen.getStyleClass().add("time-picker-field");

        TextField txtClose = new TextField(close.format(timeFormatter));
        txtClose.setPrefWidth(70);
        txtClose.setAlignment(javafx.geometry.Pos.CENTER);
        txtClose.getStyleClass().add("time-picker-field");

        // Staff Assignment Menu
        MenuButton btnStaff = new MenuButton();
        btnStaff.setGraphic(new FontAwesomeIconView(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.USERS));
        btnStaff.getStyleClass().add("btn-icon-secondary");
        btnStaff.setTooltip(new Tooltip("Asignar personal a este turno"));

        List<Integer> selectedIds = new ArrayList<>(assignedIds);
        refreshStaffMenu(btnStaff, selectedIds);

        Button btnRemove = new Button();
        btnRemove.setGraphic(new FontAwesomeIconView(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MINUS_CIRCLE));
        btnRemove.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-padding: 0;");
        btnRemove.setTooltip(new Tooltip("Eliminar turno"));

        HBox row = new HBox(8, new Label("De"), txtOpen, new Label("a"), txtClose, btnStaff, btnRemove);
        row.setUserData(selectedIds); // Store assigned IDs in the row for saving
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 2 10; -fx-background-color: #f1f5f9; -fx-background-radius: 6;");

        btnRemove.setOnAction(e -> container.getChildren().remove(row));

        container.getChildren().add(row);
    }

    private void refreshStaffMenu(MenuButton menu, List<Integer> selected) {
        menu.getItems().clear();
        for (User u : allUsers) {
            CheckBox cb = new CheckBox(u.getFullName());
            cb.setSelected(selected.contains(u.getUserId()));
            cb.selectedProperty().addListener((obs, old, val) -> {
                if (val && !selected.contains(u.getUserId()))
                    selected.add(u.getUserId());
                else if (!val)
                    selected.remove((Integer) u.getUserId());
                updateStaffMenuIcon(menu, selected);
            });
            CustomMenuItem item = new CustomMenuItem(cb);
            item.setHideOnClick(false);
            menu.getItems().add(item);
        }
        updateStaffMenuIcon(menu, selected);
    }

    private void updateStaffMenuIcon(MenuButton menu, List<Integer> selected) {
        if (selected.isEmpty()) {
            menu.setStyle("-fx-background-color: transparent;");
        } else {
            menu.setStyle("-fx-background-color: #e0e7ff; -fx-background-radius: 4;");
        }
    }

    @FXML
    private void handleManageSpecialDays() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/special_days.fxml"));
            VBox root = loader.load();
            SpecialDaysController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Festivos y Excepciones");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new javafx.scene.Scene(root));

            controller.initData(currentConfig.getSpecialDays(), () -> {
                currentConfig.setSpecialDays(controller.getResult());
                // This save is already triggered when the special days controller confirms
                // changes.
                // If we want to ensure a save happens even if the modal is just closed without
                // changes,
                // we could move configUseCase.saveConfig(currentConfig) after
                // stage.showAndWait().
                // For now, keeping it here as it's tied to the result being set.
            });

            stage.showAndWait();
            // Call saveConfig immediately after the special days modal closes,
            // to ensure any changes (or even just opening/closing) are persisted.
            configUseCase.saveConfig(currentConfig);
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "No se pudo abrir el gestor de festivos.");
        }
    }

    @FXML
    private void handleSave() {
        List<BusinessDay> newSchedule = new ArrayList<>();
        try {
            for (int i = 1; i <= 7; i++) {
                BusinessDay day = new BusinessDay(i, closedChecks[i].isSelected());
                VBox container = shiftContainers.get(i);

                for (javafx.scene.Node node : container.getChildren()) {
                    if (node instanceof HBox) {
                        HBox row = (HBox) node;
                        TextField tOp = (TextField) row.getChildren().get(1);
                        TextField tCl = (TextField) row.getChildren().get(3);
                        List<Integer> userIds = (List<Integer>) row.getUserData();

                        day.getShifts().add(new BusinessDay.TimeRange(
                                LocalTime.parse(tOp.getText(), timeFormatter),
                                LocalTime.parse(tCl.getText(), timeFormatter),
                                userIds));
                    }
                }
                newSchedule.add(day);
            }
            currentConfig.setSchedule(newSchedule);
            if (txtGracePeriod != null) {
                currentConfig.setScheduleGracePeriodMins(Integer.parseInt(txtGracePeriod.getText()));
            }
            configUseCase.saveConfig(currentConfig);
            closeWindow();
        } catch (Exception e) {
            AlertUtil.showError("Error", "Formato de hora inválido (HH:mm) en algún campo.");
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) gridSchedule.getScene().getWindow();
        stage.close();
    }
}
