package com.mycompany.ventacontrolfx.util;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class RippleEffect {

    public static void applyTo(Button button) {
        // Force button adjustments to allow full ripple coverage and left alignment
        // We override padding to 0 so the Graphic (holding the ripple) fills the entire
        // button.
        String currentStyle = button.getStyle() != null ? button.getStyle() : "";
        button.setStyle(currentStyle + "; -fx-padding: 0;");
        button.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Create a Pane to hold the ripples (overlay)
        Pane rippleContainer = new Pane();
        rippleContainer.setMouseTransparent(true); // Ensure ripple doesn't block interactions

        // Clip the ripple container to the button's bounds
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(button.widthProperty());
        clip.heightProperty().bind(button.heightProperty());
        clip.setArcWidth(5); // Match CSS radius
        clip.setArcHeight(5);
        rippleContainer.setClip(clip);

        // Prepare the content
        Node originalGraphic = button.getGraphic();
        String originalText = button.getText();

        StackPane contentPane = new StackPane();
        contentPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        contentPane.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        // Restore padding here (moved from Button to inner Content)
        contentPane.setPadding(new javafx.geometry.Insets(12, 15, 12, 15));

        // If button has text, wrap it in a Label
        if (originalText != null && !originalText.isEmpty()) {
            Label textLabel = new Label(originalText);
            textLabel.getStyleClass().add("label");
            // textLabel.setStyle(button.getStyle()); // Avoid copying the padding:0 style
            // back

            contentPane.getChildren().add(textLabel);
            button.setText(""); // clear text from button proper
        }

        if (originalGraphic != null) {
            contentPane.getChildren().add(originalGraphic);
        }

        // Combine Content + Ripple Overlay
        StackPane root = new StackPane(contentPane, rippleContainer);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.setAlignment(javafx.geometry.Pos.CENTER_LEFT); // Ensure root aligns left too

        // Mouse listener on the button (or root) to spawn ripples
        // We attach to button to capture clicks anywhere on the button surface
        button.setOnMousePressed(e -> {
            double x = e.getX();
            double y = e.getY();

            // Spawn ripple
            createRipple(rippleContainer, x, y, button.getWidth());
        });

        button.setGraphic(root);
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    private static void createRipple(Pane container, double x, double y, double boundsWidth) {
        Circle circle = new Circle(x, y, 0);
        circle.setFill(Color.web("#ffffff", 0.3)); // White, semi-transparent
        circle.setManaged(false); // Important: Prevent Pane from resizing to fit the circle

        container.getChildren().add(circle);

        // Animation
        double maxRadius = boundsWidth * 2.5; // Make it much larger to ensure coverage before fadeout

        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(
                new KeyFrame(Duration.millis(1000),
                        new KeyValue(circle.radiusProperty(), maxRadius),
                        new KeyValue(circle.opacityProperty(), 0)));

        timeline.setOnFinished(e -> container.getChildren().remove(circle));
        timeline.play();
    }
}
