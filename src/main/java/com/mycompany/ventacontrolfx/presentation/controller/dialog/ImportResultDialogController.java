package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

public class ImportResultDialogController {

    @FXML
    private Label lblTitle;
    @FXML
    private Label lblSubtitle;
    @FXML
    private Label lblCount;
    @FXML
    private Label lblMessage;
    @FXML
    private StackPane iconContainer;
    @FXML
    private FontAwesomeIconView iconView;

    private boolean confirmed = false;

    public void initData(int count, boolean success, String errorMessage) {
        lblCount.setText(String.valueOf(count));

        if (!success) {
            lblTitle.setText("Error en la Importaci\u00c3\u00b3n");
            lblSubtitle.setText("No se pudieron procesar los productos");
            lblMessage.setText(errorMessage);
            lblCount.setStyle("-fx-text-fill: -fx-custom-color-danger;");
            iconContainer.setStyle(
                    "-fx-background-color: -fx-custom-color-danger-bg; -fx-padding: 20; -fx-background-radius: 50%;");
            iconView.setGlyphName("EXCLAMATION_TRIANGLE");
            iconView.setFill(javafx.scene.paint.Color.valueOf("#ef4444")); // Danger color
        }
    }

    @FXML
    private void handleClose() {
        confirmed = true;
        closeStage();
    }

    private void closeStage() {
        ((Stage) lblTitle.getScene().getWindow()).close();
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
