package com.mycompany.ventacontrolfx.component;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class SkeletonProductBox extends VBox {

    public SkeletonProductBox() {
        this.getStyleClass().add("skeleton-box");
        this.setPrefWidth(200);
        this.setSpacing(0);

        // Image Placeholder
        Region imageSkeleton = new Region();
        imageSkeleton.getStyleClass().add("skeleton-image");

        // Info Section
        VBox infoSkeleton = new VBox(10);
        infoSkeleton.getStyleClass().add("product-info");
        infoSkeleton.setPadding(new Insets(15, 15, 15, 15));
        
        Region titleSkeleton = new Region();
        titleSkeleton.getStyleClass().addAll("skeleton-text", "skeleton-title");
        
        Region subtitleSkeleton = new Region();
        subtitleSkeleton.getStyleClass().addAll("skeleton-text", "skeleton-subtitle");
        
        infoSkeleton.getChildren().addAll(titleSkeleton, subtitleSkeleton);
        VBox.setVgrow(infoSkeleton, Priority.ALWAYS);

        // Button Placeholder
        Region buttonSkeleton = new Region();
        buttonSkeleton.getStyleClass().add("skeleton-button");

        this.getChildren().addAll(imageSkeleton, infoSkeleton, buttonSkeleton);

        // Pulse Animation
        FadeTransition pulse = new FadeTransition(Duration.millis(800), this);
        pulse.setFromValue(1.0);
        pulse.setToValue(0.5);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();
    }
}
