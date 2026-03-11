package com.mycompany.ventacontrolfx.util;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class RippleEffect {

    public static void applyTo(Button button) {
        // Force button adjustments to allow full ripple coverage
        String currentStyle = button.getStyle() != null ? button.getStyle() : "";
        button.setStyle(currentStyle + "; -fx-padding: 0;");

        // Preserve original settings
        ContentDisplay originalDisplay = button.getContentDisplay();
        Node originalGraphic = button.getGraphic();
        String originalText = button.getText();

        // Create a Pane to hold the ripples (overlay)
        Pane rippleContainer = new Pane();
        rippleContainer.setMouseTransparent(true);

        // Clip the ripple container to the button's bounds
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(button.widthProperty());
        clip.heightProperty().bind(button.heightProperty());
        clip.setArcWidth(20); // More rounded for premium look
        clip.setArcHeight(20);
        rippleContainer.setClip(clip);

        // Create content container based on ContentDisplay
        Pane contentPane;
        if (originalDisplay == ContentDisplay.TOP) {
            VBox vbox = new VBox(10); // Match original spacing
            vbox.setAlignment(javafx.geometry.Pos.CENTER);
            contentPane = vbox;
            contentPane.setPadding(new javafx.geometry.Insets(12, 10, 12, 10));
        } else {
            HBox hbox = new HBox(12);
            hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            contentPane = hbox;
            contentPane.setPadding(new javafx.geometry.Insets(12, 15, 12, 15));
        }

        contentPane.setMouseTransparent(true);
        contentPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Add Graphic first
        if (originalGraphic != null) {
            contentPane.getChildren().add(originalGraphic);
        }

        // Add Text as Label
        if (originalText != null && !originalText.isEmpty()) {
            Label textLabel = new Label(originalText);
            textLabel.getStyleClass().add("label");
            textLabel.setStyle("-fx-text-fill: inherit; -fx-font-weight: inherit; -fx-font-size: inherit;");
            contentPane.getChildren().add(textLabel);
            button.setText("");
        }

        // Combine Content + Ripple Overlay
        StackPane root = new StackPane(contentPane, rippleContainer);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.setAlignment(javafx.geometry.Pos.CENTER);

        button.setOnMousePressed(e -> {
            createRipple(rippleContainer, e.getX(), e.getY(), button.getWidth());
        });

        button.setGraphic(root);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    private static void createRipple(Pane container, double x, double y, double boundsWidth) {
        Circle circle = new Circle(x, y, 0);
        circle.setFill(Color.web("#ffffff", 0.28)); // White, semi-transparent
        circle.setManaged(false); // Important: Prevent Pane from resizing to fit the circle

        container.getChildren().add(circle);

        // Animation — snappy modern ripple (380ms)
        double maxRadius = boundsWidth * 1.5;

        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(
                new KeyFrame(Duration.millis(380),
                        new KeyValue(circle.radiusProperty(), maxRadius),
                        new KeyValue(circle.opacityProperty(), 0)));

        timeline.setOnFinished(e -> container.getChildren().remove(circle));
        timeline.play();
    }
}
