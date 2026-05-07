package com.mycompany.ventacontrolfx.presentation.controller.user;

import com.mycompany.ventacontrolfx.application.usecase.ConfigUseCase;
import com.mycompany.ventacontrolfx.application.usecase.UserUseCase;
import com.mycompany.ventacontrolfx.domain.model.BusinessDay;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.domain.model.StaffVacation;
import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.infrastructure.persistence.JdbcStaffRepository;
import com.mycompany.ventacontrolfx.shared.util.SpanishHolidays;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class StaffCalendarController implements Injectable {

    @FXML
    private Label lblMonthYear;
    @FXML
    private GridPane gridCalendar;

    private ConfigUseCase configUseCase;
    private UserUseCase userUseCase;
    private JdbcStaffRepository staffRepo;

    private LocalDate viewingDate = LocalDate.now().withDayOfMonth(1);
    private List<User> allUsers = new ArrayList<>();
    private List<StaffVacation> allVacations = new ArrayList<>();
    private SaleConfig currentConfig;
    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.configUseCase = container.getConfigUseCase();
        this.userUseCase = container.getUserUseCase();
        this.staffRepo = new JdbcStaffRepository();
        this.currentConfig = configUseCase.getConfig();
        this.container = container;

        try {
            this.allUsers = userUseCase.getAllUsers();
            this.allVacations = staffRepo.getAllVacations();
        } catch (Exception e) {
            e.printStackTrace();
        }

        renderCalendar();
    }

    @FXML
    private void handlePrevMonth() {
        viewingDate = viewingDate.minusMonths(1);
        renderCalendar();
    }

    @FXML
    private void handleNextMonth() {
        viewingDate = viewingDate.plusMonths(1);
        renderCalendar();
    }

    private void renderCalendar() {
        gridCalendar.getChildren().clear();
        gridCalendar.getRowConstraints().clear();
        gridCalendar.getColumnConstraints().clear();

        lblMonthYear.setText(
                viewingDate.getMonth().getDisplayName(TextStyle.FULL, container.getBundle().getLocale()).toUpperCase() + " "
                        + viewingDate.getYear());

        // Define Column Constraints to fill width
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7.0);
            cc.setHgrow(Priority.ALWAYS);
            gridCalendar.getColumnConstraints().add(cc);
        }

        LocalDate firstOfMonth = viewingDate.withDayOfMonth(1);
        int dayOfWeekStart = firstOfMonth.getDayOfWeek().getValue(); // 1=Mon, 7=Sun
        int daysInMonth = firstOfMonth.lengthOfMonth();

        int row = 0;
        int col = dayOfWeekStart - 1;

        // Ensure rows grow
        for (int r = 0; r < 6; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setMinHeight(120);
            gridCalendar.getRowConstraints().add(rc);
        }

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = viewingDate.withDayOfMonth(day);
            VBox dayCell = createDayCell(date);
            gridCalendar.add(dayCell, col, row);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createDayCell(LocalDate date) {
        VBox v = new VBox(8);
        v.setPadding(new Insets(12));
        v.getStyleClass().add("calendar-cell-modern");

        // Modern styling
        String baseStyle = "-fx-border-color: #f1f5f9; -fx-border-width: 0.5;";
        if (date.isEqual(LocalDate.now())) {
            baseStyle += "-fx-background-color: #fffbeb; -fx-border-color: #fcd34d; -fx-border-width: 1;";
        } else {
            baseStyle += "-fx-background-color: white;";
        }
        v.setStyle(baseStyle);
        final String finalBaseStyle = baseStyle;

        HBox header = new HBox(5);
        header.setAlignment(Pos.TOP_LEFT);

        Label lblNum = new Label(String.valueOf(date.getDayOfMonth()));
        lblNum.setStyle("-fx-font-weight: 800; -fx-font-size: 16px; -fx-text-fill: #334155;");
        header.getChildren().add(lblNum);

        // Spacer to push content to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().add(spacer);

        // Check National Holiday
        if (SpanishHolidays.isHoliday(date)) {
            Label lblHoliday = new Label(SpanishHolidays.getHolidayName(date).toUpperCase());
            lblHoliday.setStyle(
                    "-fx-font-size: 9px; -fx-text-fill: white; -fx-font-weight: 900; -fx-background-color: #ef4444; -fx-padding: 3 6; -fx-background-radius: 4;");
            header.getChildren().add(lblHoliday);
            v.setStyle(v.getStyle() + "-fx-background-color: #fff1f2;");
        }

        v.getChildren().add(header);

        VBox content = new VBox(5);
        content.setAlignment(Pos.CENTER_LEFT);

        // Special Days (Closures/Adjustments)
        Optional<SaleConfig.SpecialDay> special = currentConfig.getSpecialDays().stream()
                .filter(sd -> sd.getDate().equals(date)).findFirst();
        if (special.isPresent()) {
            HBox badge = createBadge(special.get().getReason(), special.get().isClosed() ? "#64748b" : "#6366f1",
                    FontAwesomeIcon.CALENDAR_TIMES_ALT);
            content.getChildren().add(badge);
            if (special.get().isClosed()) {
                v.setOpacity(0.7);
                v.setStyle(v.getStyle() + "-fx-background-color: #f8fafc;");
            }
        }

        // Assigned Staff (Pill format)
        int dayOfWeek = date.getDayOfWeek().getValue();
        BusinessDay bd = currentConfig.getSchedule().stream().filter(s -> s.getDayOfWeek() == dayOfWeek).findFirst()
                .orElse(null);
        if (bd != null && !bd.isClosed() && (!special.isPresent() || !special.get().isClosed())) {
            Set<Integer> assignedIdSet = new HashSet<>();
            bd.getShifts().forEach(s -> assignedIdSet.addAll(s.getAssignedUserIds()));

            if (!assignedIdSet.isEmpty()) {
                HBox staffBadge = createBadge(assignedIdSet.size() + " " + container.getBundle().getString("calendar.label.employees"), "#059669", FontAwesomeIcon.USERS);
                content.getChildren().add(staffBadge);
            }
        }

        // Vacations
        List<StaffVacation> activeVacations = allVacations.stream()
                .filter(vac -> !date.isBefore(vac.getStartDate()) && !date.isAfter(vac.getEndDate()))
                .collect(Collectors.toList());

        for (StaffVacation vac : activeVacations) {
            String userName = allUsers.stream().filter(u -> u.getUserId() == vac.getUserId())
                    .map(User::getFullName).findFirst().orElse(String.format(container.getBundle().getString("dashboard.staff.user"), vac.getUserId()));
            HBox vacBadge = createBadge(userName.split(" ")[0] + " " + container.getBundle().getString("calendar.label.vacation_suffix"), "#0284c7", FontAwesomeIcon.UMBRELLA);
            content.getChildren().add(vacBadge);
        }

        v.getChildren().add(content);

        // Hover effects in code (since we don't have a complex CSS file handy)
        v.setOnMouseEntered(e -> v.setStyle(v.getStyle()
                + "-fx-background-color: #f8fafc; -fx-translate-y: -2; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);"));
        v.setOnMouseExited(e -> {
            v.setStyle(finalBaseStyle);
            if (SpanishHolidays.isHoliday(date))
                v.setStyle(v.getStyle() + "-fx-background-color: #fff1f2;");
            if (date.isEqual(LocalDate.now()))
                v.setStyle(v.getStyle() + "-fx-background-color: #fffbeb;");
            if (special.isPresent() && special.get().isClosed())
                v.setStyle(v.getStyle() + "-fx-background-color: #f8fafc;");
        });

        return v;
    }

    private HBox createBadge(String text, String color, FontAwesomeIcon icon) {
        HBox badge = new HBox(4);
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.setPadding(new Insets(3, 6, 3, 6));
        badge.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 12; -fx-border-color: " + color
                + "44; -fx-border-radius: 12;");

        FontAwesomeIconView iconView = new FontAwesomeIconView(icon);
        iconView.setSize("10");
        iconView.setFill(javafx.scene.paint.Color.web(color));

        Label lbl = new Label(text.toUpperCase());
        lbl.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: " + color + ";");

        badge.getChildren().addAll(iconView, lbl);
        return badge;
    }

    @FXML
    private void handleClose() {
        ((Stage) lblMonthYear.getScene().getWindow()).close();
    }
}


