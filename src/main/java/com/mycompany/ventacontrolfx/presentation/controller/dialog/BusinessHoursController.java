package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.application.usecase.ConfigUseCase;
import com.mycompany.ventacontrolfx.application.usecase.UserUseCase;
import com.mycompany.ventacontrolfx.domain.model.BusinessDay;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
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
import com.mycompany.ventacontrolfx.presentation.controller.user.SpecialDaysController;

public class BusinessHoursController implements Injectable {

    @FXML
    private GridPane gridSchedule;
    @FXML
    private TextField txtGracePeriod;

    @FXML private DatePicker pickerDate;
    @FXML private TextField txtReason;
    @FXML private CheckBox chkClosed;
    @FXML private TableView<SaleConfig.SpecialDay> tableSpecials;
    @FXML private TableColumn<SaleConfig.SpecialDay, LocalDate> colDate;
    @FXML private TableColumn<SaleConfig.SpecialDay, String> colReason;
    @FXML private TableColumn<SaleConfig.SpecialDay, Boolean> colStatus;

    private ServiceContainer container;
    private ConfigUseCase configUseCase;
    private UserUseCase userUseCase;
    private SaleConfig currentConfig;
    private List<User> allUsers = new ArrayList<>();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    // Tracks the container and components for each day (1-7)
    private final Map<Integer, VBox> shiftContainers = new HashMap<>();
    private final CheckBox[] closedChecks = new CheckBox[8];
    private final String[] dayNames = {"", "Lunes", "Martes", "Mi\u00e9rcoles", "Jueves", "Viernes", "S\u00e1bado", "Domingo"};

    private final javafx.collections.ObservableList<SaleConfig.SpecialDay> specialDaysList = javafx.collections.FXCollections.observableArrayList();

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.configUseCase = container.getConfigUseCase();
        this.userUseCase = container.getUserUseCase();
        this.currentConfig = configUseCase.getConfig();
        
        try {
            this.allUsers = userUseCase.getAllUsers();
        } catch (Exception e) {
            e.printStackTrace();
        }

        setupUI();
        setupSpecialDaysTable();
    }

    private void setupSpecialDaysTable() {
        colDate.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("date"));
        colReason.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("reason"));
        colStatus.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("closed"));

        colDate.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String month = item.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, container.getBundle().getLocale());
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

        specialDaysList.setAll(currentConfig.getSpecialDays());
        tableSpecials.setItems(specialDaysList);
        tableSpecials.setPlaceholder(new Label("No hay d\u00edas especiales introducidos"));
    }

    @FXML
    private void handleAddSpecialDay() {
        LocalDate date = pickerDate.getValue();
        if (date == null) return;

        SaleConfig.SpecialDay sd = new SaleConfig.SpecialDay(date, chkClosed.isSelected(), txtReason.getText());
        if (!chkClosed.isSelected()) {
            sd.getShifts().add(new BusinessDay.TimeRange(LocalTime.of(9, 0), LocalTime.of(14, 0)));
        }
        specialDaysList.add(sd);
        txtReason.clear();
        pickerDate.setValue(null);
    }

    @FXML
    private void handleRemoveSpecialDay() {
        SaleConfig.SpecialDay selected = tableSpecials.getSelectionModel().getSelectedItem();
        if (selected != null) {
            specialDaysList.remove(selected);
        }
    }

    private void setupUI() {
        gridSchedule.getChildren().clear();
        List<BusinessDay> schedule = currentConfig.getSchedule();
        String[] dayNames = { "", "Lunes", "Martes", "Mi\u00e9rcoles", "Jueves", "Viernes", "S\u00e1bado", "Domingo" };

        for (int i = 1; i <= 7; i++) {
            final int dayIdx = i;
            BusinessDay dayData = schedule.stream()
                    .filter(d -> d.getDayOfWeek() == dayIdx)
                    .findFirst()
                    .orElse(new BusinessDay(i, false));

            // Row Background for alternating colors and professional look
            Region rowBg = new Region();
            rowBg.setStyle("-fx-background-color: " + (i % 2 == 0 ? "#f8fafc" : "white") + "; " +
                           "-fx-background-radius: 8; -fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0;");
            gridSchedule.add(rowBg, 0, i - 1, 3, 1);

            // Shift container
            VBox vBoxShifts = new VBox(6);
            vBoxShifts.setPadding(new javafx.geometry.Insets(12, 0, 12, 0));
            shiftContainers.put(dayIdx, vBoxShifts);

            closedChecks[i] = new CheckBox("D\u00eda Cerrado");
            closedChecks[i].setSelected(dayData.isClosed());
            closedChecks[i].getStyleClass().add("modern-checkbox");
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
            btnAdd.setTooltip(new Tooltip("A\u00f1adir otro turno"));
            btnAdd.setOnAction(e -> addShiftRow(dayIdx, LocalTime.of(17, 0), LocalTime.of(21, 0), new ArrayList<>()));
            btnAdd.disableProperty().bind(closedChecks[i].selectedProperty());

            Button btnCopy = new Button();
            btnCopy.setGraphic(new FontAwesomeIconView(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.COPY));
            btnCopy.getStyleClass().add("btn-icon-secondary");
            btnCopy.setTooltip(new Tooltip("Copiar a todos los dem\u00e1s d\u00edas"));
            btnCopy.setOnAction(e -> copyShiftsToAll(dayIdx));
            btnCopy.disableProperty().bind(closedChecks[i].selectedProperty());

            // Labels and styling
            Label lblDay = new Label(dayNames[i].toUpperCase());
            lblDay.setStyle("-fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: #1e293b; -fx-padding: 0 0 0 15;");

            HBox actions = new HBox(12, closedChecks[i], btnAdd, btnCopy);
            actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            actions.setPadding(new javafx.geometry.Insets(0, 15, 0, 0));

            gridSchedule.add(lblDay, 0, i - 1);
            gridSchedule.add(vBoxShifts, 1, i - 1);
            gridSchedule.add(actions, 2, i - 1);
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
        VBox vBoxShiftContainer = shiftContainers.get(dayIdx);

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

        btnRemove.setOnAction(e -> vBoxShiftContainer.getChildren().remove(row));

        vBoxShiftContainer.getChildren().add(row);
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
    private void handleSave() {
        List<BusinessDay> newSchedule = new ArrayList<>();
        try {
            for (int i = 1; i <= 7; i++) {
                BusinessDay day = new BusinessDay(i, closedChecks[i].isSelected());
                VBox vBoxShiftContainer = shiftContainers.get(i);

                for (javafx.scene.Node node : vBoxShiftContainer.getChildren()) {
                    if (node instanceof HBox) {
                        HBox row = (HBox) node;
                        TextField tOp = (TextField) row.getChildren().get(1);
                        TextField tCl = (TextField) row.getChildren().get(3);
                        @SuppressWarnings("unchecked")
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
            
            // Sync special days from our unified table
            currentConfig.setSpecialDays(new ArrayList<>(specialDaysList));
            
            if (txtGracePeriod != null) {
                try {
                    currentConfig.setScheduleGracePeriodMins(Integer.parseInt(txtGracePeriod.getText()));
                } catch (Exception e) {}
            }
            
            configUseCase.saveConfig(currentConfig);
            AlertUtil.showInfo("Éxito", "Los horarios y días especiales se han guardados correctamente.");
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "Formato de hora inválido (HH:mm) en algún campo.");
        }
    }

    @FXML
    private void handleCancel() {
        // En modo incrustado, cancelar podría simplemente recargar la vista.
        // Por ahora lo dejamos vacío o podemos recargar los datos.
        inject(container);
    }
}


