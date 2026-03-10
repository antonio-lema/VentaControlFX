package com.mycompany.ventacontrolfx.util;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import java.util.concurrent.atomic.AtomicBoolean;

public class AlertUtil {

    private static final String APP_PRIMARY = "#1e88e5";
    private static final String APP_DANGER = "#e53935";
    private static final String APP_WARNING = "#fb8c00";
    private static final String APP_SUCCESS = "#4caf50";

    public enum CustomAlertType {
        ERROR, INFO, WARNING, CONFIRMATION
    }

    public static void showError(String header, String content) {
        showCustomAlert(CustomAlertType.ERROR, header, content);
    }

    public static void showInfo(String header, String content) {
        showCustomAlert(CustomAlertType.INFO, header, content);
    }

    public static void showWarning(String header, String content) {
        showCustomAlert(CustomAlertType.WARNING, header, content);
    }

    /**
     * Shows a non-blocking toast notification at the top of the application.
     */
    public static void showToast(String message) {
        Stage toastStage = new Stage();
        toastStage.initStyle(StageStyle.TRANSPARENT);
        toastStage.setAlwaysOnTop(true);

        HBox root = new HBox(15);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(12, 25, 12, 20));
        root.setStyle(
                "-fx-background-color: #333333; -fx-background-radius: 30; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 4);");

        FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.CHECK_CIRCLE);
        icon.setFill(Color.web(APP_SUCCESS));
        icon.setSize("20px");

        Label label = new Label(message);
        label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        root.getChildren().addAll(icon, label);

        Scene scene = new Scene(root);
        scene.setFill(null);
        toastStage.setScene(scene);

        // Dynamic position: Top center of the screen
        try {
            javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
            double screenWidth = screen.getVisualBounds().getWidth();
            toastStage.setX((screenWidth - root.prefWidth(-1)) / 2 + screen.getVisualBounds().getMinX());
            toastStage.setY(screen.getVisualBounds().getMinY() + 40);
        } catch (Exception e) {
            toastStage.setX(500);
            toastStage.setY(50);
        }

        toastStage.show();

        // Animation
        root.setOpacity(0);
        root.setTranslateY(-20);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
        fadeIn.setToValue(1.0);
        TranslateTransition moveIn = new TranslateTransition(Duration.millis(300), root);
        moveIn.setToY(0);

        ParallelTransition show = new ParallelTransition(fadeIn, moveIn);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
        fadeOut.setDelay(Duration.seconds(2.5));
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                toastStage.close();
            }
        });

        show.play();
        fadeOut.play();
    }

    public static String showInput(String title, String header, String defaultValue) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("Introduce el valor:");

        // Style it a bit to match the app if possible, or just return result
        java.util.Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    public static boolean showConfirmation(String title, String header, String content) {
        AtomicBoolean response = new AtomicBoolean(false);
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        VBox root = createBaseContainer(stage, CustomAlertType.CONFIRMATION, header, content);

        HBox buttons = new HBox(15);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(10, 0, 0, 0));

        Button btnCancel = new Button("CANCELAR");
        btnCancel.setStyle(
                "-fx-background-color: #f5f5f5; -fx-text-fill: #666666; -fx-background-radius: 20; -fx-font-weight: bold; -fx-padding: 10 25; -fx-cursor: hand;");
        btnCancel.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                stage.close();
            }
        });

        Button btnOk = new Button("ACEPTAR");
        btnOk.setStyle("-fx-background-color: " + APP_PRIMARY
                + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-font-weight: bold; -fx-padding: 10 25; -fx-cursor: hand;");
        btnOk.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                response.set(true);
                stage.close();
            }
        });

        buttons.getChildren().addAll(btnCancel, btnOk);
        root.getChildren().add(buttons);

        showWithAnimation(stage, root);
        return response.get();
    }

    private static void showCustomAlert(CustomAlertType type, String header, String content) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        VBox root = createBaseContainer(stage, type, header, content);

        Button btnOk = new Button("ENTENDIDO");
        String btnColor = APP_PRIMARY;
        if (type == CustomAlertType.ERROR)
            btnColor = APP_DANGER;
        if (type == CustomAlertType.WARNING)
            btnColor = APP_WARNING;

        btnOk.setStyle("-fx-background-color: " + btnColor
                + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-font-weight: bold; -fx-padding: 10 35; -fx-cursor: hand;");
        btnOk.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                stage.close();
            }
        });

        root.getChildren().add(btnOk);

        showWithAnimation(stage, root);
    }

    private static VBox createBaseContainer(Stage stage, CustomAlertType type, String header, String content) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle(
                "-fx-background-color: white; -fx-background-radius: 20; -fx-border-color: #eeeeee; -fx-border-width: 1; -fx-border-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 30, 0, 0, 10);");

        FontAwesomeIcon icon;
        String color;
        switch (type) {
            case ERROR:
                icon = FontAwesomeIcon.TIMES_CIRCLE;
                color = APP_DANGER;
                break;
            case WARNING:
                icon = FontAwesomeIcon.EXCLAMATION_TRIANGLE;
                color = APP_WARNING;
                break;
            case CONFIRMATION:
                icon = FontAwesomeIcon.QUESTION_CIRCLE;
                color = APP_PRIMARY;
                break;
            default:
                icon = FontAwesomeIcon.INFO_CIRCLE;
                color = APP_PRIMARY;
        }

        FontAwesomeIconView iconView = new FontAwesomeIconView(icon);
        iconView.setSize("50px");
        iconView.setFill(Color.web(color));

        Label lblHeader = new Label(header);
        lblHeader.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #333333;");
        lblHeader.setWrapText(true);
        lblHeader.setAlignment(Pos.CENTER);

        Label lblContent = new Label(content);
        lblContent.setStyle("-fx-font-size: 14px; -fx-text-fill: #777777; -fx-text-alignment: center;");
        lblContent.setWrapText(true);
        lblContent.setMaxWidth(350);
        lblContent.setAlignment(Pos.CENTER);

        root.getChildren().addAll(iconView, lblHeader, lblContent);
        return root;
    }

    private static void showWithAnimation(Stage stage, VBox root) {
        Scene scene = new Scene(root);
        scene.setFill(null);
        stage.setScene(scene);

        root.setOpacity(0);
        root.setScaleX(0.85);
        root.setScaleY(0.85);

        stage.setOnShowing(new EventHandler<javafx.stage.WindowEvent>() {
            @Override
            public void handle(javafx.stage.WindowEvent e) {
                FadeTransition ft = new FadeTransition(Duration.millis(200), root);
                ft.setToValue(1.0);
                ScaleTransition st = new ScaleTransition(Duration.millis(250), root);
                st.setToX(1.0);
                st.setToY(1.0);
                st.setInterpolator(Interpolator.EASE_OUT);
                new ParallelTransition(ft, st).play();
            }
        });

        stage.showAndWait();
    }
}
