package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.util.function.Consumer;

public class ClosureReminderController implements Injectable {

    private Consumer<String> onAction;
    private Stage stage;

    public void setOnAction(Consumer<String> onAction) {
        this.onAction = onAction;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Override
    public void inject(ServiceContainer container) {
        // No heavy injection needed
    }

    @FXML
    private void handleCloseNow() {
        if (onAction != null)
            onAction.accept("CLOSE_NOW");
        close();
    }

    @FXML
    private void handleSnooze5() {
        if (onAction != null)
            onAction.accept("SNOOZE_5");
        close();
    }

    @FXML
    private void handleSnooze15() {
        if (onAction != null)
            onAction.accept("SNOOZE_15");
        close();
    }

    @FXML
    private void handleSnooze60() {
        if (onAction != null)
            onAction.accept("SNOOZE_60");
        close();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        if (stage != null)
            stage.close();
    }
}
