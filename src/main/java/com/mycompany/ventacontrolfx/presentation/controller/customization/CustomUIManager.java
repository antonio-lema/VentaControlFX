package com.mycompany.ventacontrolfx.presentation.controller.customization;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.util.function.Function;

/**
 * Gestor de la interfaz de usuario para el panel de personalización.
 */
public class CustomUIManager {

    public void bindSliderLabel(Slider slider, Label label, Function<Double, String> formatter) {
        if (slider == null || label == null) return;
        label.setText(formatter.apply(slider.getValue()));
        slider.valueProperty().addListener((obs, o, n) -> label.setText(formatter.apply(n.doubleValue())));
    }

    public void trySetColor(ColorPicker cp, String hex) {
        if (cp != null && hex != null) {
            try { cp.setValue(Color.valueOf(hex)); } catch (Exception ignored) {}
        }
    }

    public void trySetSlider(Slider s, String val) {
        if (s != null && val != null) {
            try { s.setValue(Double.parseDouble(val)); } catch (Exception ignored) {}
        }
    }

    public void showThemePreviewPopup(String primary, String bg, String text, ColorPicker anchor) {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.NONE);

        VBox root = new VBox(18);
        root.setPadding(new Insets(30));
        root.setPrefWidth(300);
        root.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 24; -fx-border-color: " + primary + "44; -fx-border-width: 2; -fx-border-radius: 24; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 25, 0, 0, 10);");
        root.setAlignment(Pos.CENTER);

        Label title = new Label("Paleta Aplicada");
        title.setStyle("-fx-text-fill: " + primary + "; -fx-font-weight: 900; -fx-font-size: 20px;");

        Label sub = new Label("La interfaz se ha actualizado con tus nuevos colores premium.");
        sub.setStyle("-fx-text-fill: " + text + "; -fx-font-size: 13px; -fx-opacity: 0.8;");
        sub.setWrapText(true); sub.setTextAlignment(TextAlignment.CENTER);

        Button btn = new Button("GENIAL");
        btn.setCursor(Cursor.HAND);
        btn.setStyle("-fx-background-color: " + primary + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 10 35; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 4);");
        btn.setOnAction(e -> stage.close());

        root.getChildren().addAll(title, sub, btn);
        Scene scene = new Scene(root); scene.setFill(null);
        stage.setScene(scene);

        if (anchor != null && anchor.getScene() != null) {
            stage.setX(anchor.getScene().getWindow().getX() + 80);
            stage.setY(anchor.getScene().getWindow().getY() + 150);
        }

        stage.show();
        Timeline fadeout = new Timeline(new KeyFrame(Duration.seconds(4), e -> stage.close()));
        fadeout.play();
    }
}

