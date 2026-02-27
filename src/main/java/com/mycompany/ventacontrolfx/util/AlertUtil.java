package com.mycompany.ventacontrolfx.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.scene.Node;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import java.util.Optional;

/**
 * Modern utility class for showing JavaFX alerts consistently with the app's
 * theme.
 */
public class AlertUtil {

    public static void showError(String header, String content) {
        showAlert(AlertType.ERROR, "Error", header, content);
    }

    public static void showInfo(String header, String content) {
        showAlert(AlertType.INFORMATION, "Información", header, content);
    }

    public static void showWarning(String header, String content) {
        showAlert(AlertType.WARNING, "Advertencia", header, content);
    }

    public static boolean showConfirmation(String title, String header, String content) {
        Alert alert = createBaseAlert(AlertType.CONFIRMATION, title, header, content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static void showAlert(AlertType type, String title, String header, String content) {
        Alert alert = createBaseAlert(type, title, header, content);
        alert.showAndWait();
    }

    private static Alert createBaseAlert(AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(null);

        // Style the DialogPane
        DialogPane dialogPane = alert.getDialogPane();
        ThemeManager.applyStyles(dialogPane.getStylesheets());
        dialogPane.getStyleClass().add("modern-alert");
        dialogPane.setPrefWidth(450);

        // Remove OS frame for true modernization
        Stage stage = (Stage) dialogPane.getScene().getWindow();
        try {
            // Check if already initialized to avoid error
            if (stage.getStyle() != StageStyle.TRANSPARENT) {
                stage.initStyle(StageStyle.TRANSPARENT);
            }
        } catch (Exception e) {
        }
        dialogPane.getScene().setFill(Color.TRANSPARENT);

        // Determine colors and icons
        FontAwesomeIcon icon;
        String color;
        switch (type) {
            case ERROR:
                icon = FontAwesomeIcon.TIMES_CIRCLE;
                color = "#FF4B2B";
                break;
            case WARNING:
                icon = FontAwesomeIcon.EXCLAMATION_TRIANGLE;
                color = "#FFB75E";
                break;
            case INFORMATION:
                icon = FontAwesomeIcon.CHECK_CIRCLE;
                color = "#43E97B";
                break;
            case CONFIRMATION:
                icon = FontAwesomeIcon.QUESTION_CIRCLE;
                color = "#4FACFE";
                break;
            default:
                icon = FontAwesomeIcon.INFO_CIRCLE;
                color = "#4FACFE";
        }

        // 1. Icon in a large colored circle
        FontAwesomeIconView iconView = new FontAwesomeIconView(icon);
        iconView.setSize("50px");
        iconView.setFill(Color.WHITE);

        StackPane iconCircle = new StackPane(iconView);
        iconCircle.setPrefSize(90, 90);
        iconCircle.setMaxSize(90, 90);
        iconCircle.setStyle("-fx-background-color: " + color
                + "; -fx-background-radius: 50; -fx-effect: dropshadow(three-pass-box, " + color
                + "88, 20, 0, 0, 10);");

        // 2. Title and Message
        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label(header);
        titleLabel
                .setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2D3436; -fx-padding: 15 0 5 0;");

        javafx.scene.control.Label msgLabel = new javafx.scene.control.Label(content);
        msgLabel.setWrapText(true);
        msgLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #636E72; -fx-text-alignment: center;");
        msgLabel.setMaxWidth(380);

        // 3. Assemble Custom Layout
        VBox customContent = new VBox(15, iconCircle, titleLabel, msgLabel);
        customContent.setAlignment(Pos.CENTER);
        customContent.setPadding(new Insets(30, 25, 20, 25));

        dialogPane.setContent(customContent);
        dialogPane.setGraphic(null);
        dialogPane.setHeader(null);

        // 4. Entrance Animation (Scale + Fade)
        dialogPane.setOpacity(0);
        dialogPane.setScaleX(0.8);
        dialogPane.setScaleY(0.8);

        stage.setOnShowing(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(300), dialogPane);
            st.setToX(1.0);
            st.setToY(1.0);

            FadeTransition ft = new FadeTransition(Duration.millis(300), dialogPane);
            ft.setToValue(1.0);

            st.play();
            ft.play();
        });

        return alert;
    }
}
