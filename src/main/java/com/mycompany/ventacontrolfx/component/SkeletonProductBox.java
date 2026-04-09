package com.mycompany.ventacontrolfx.component;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class SkeletonProductBox extends VBox {

    public SkeletonProductBox() {
        this.getStyleClass().add("skeleton-box");
        this.setPrefWidth(200);
        this.setSpacing(0);

        // 1. Image Placeholder Section
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(150);
        imageContainer.setMinHeight(150);
        imageContainer.setMaxHeight(150);
        imageContainer.getStyleClass().add("skeleton-image-container");

        Region imageFiller = new Region();
        imageFiller.getStyleClass().add("skeleton-image-filler");
        StackPane.setMargin(imageFiller, new Insets(8));

        // Small price badge placeholder
        Region priceBadge = new Region();
        priceBadge.getStyleClass().add("skeleton-price-badge");
        StackPane.setAlignment(priceBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(priceBadge, new Insets(10, 10, 0, 0));

        imageContainer.getChildren().addAll(imageFiller, priceBadge);

        // 2. Info Section
        VBox infoSkeleton = new VBox(8);
        infoSkeleton.getStyleClass().add("product-info");
        infoSkeleton.setPadding(new Insets(15));
        
        Region titleSkeleton = new Region();
        titleSkeleton.getStyleClass().addAll("skeleton-text", "skeleton-title");
        
        Region categorySkeleton = new Region();
        categorySkeleton.getStyleClass().addAll("skeleton-text", "skeleton-subtitle");

        Region stockSkeleton = new Region();
        stockSkeleton.getStyleClass().addAll("skeleton-text", "skeleton-stock");
        
        infoSkeleton.getChildren().addAll(titleSkeleton, categorySkeleton, stockSkeleton);
        VBox.setVgrow(infoSkeleton, Priority.ALWAYS);

        // 3. Button Placeholder
        Region buttonSkeleton = new Region();
        buttonSkeleton.getStyleClass().add("skeleton-button");

        this.getChildren().addAll(imageContainer, infoSkeleton, buttonSkeleton);

        // Pulse Animation (slightly faster for smoother feel)
        FadeTransition pulse = new FadeTransition(Duration.millis(600), this);
        pulse.setFromValue(1.0);
        pulse.setToValue(0.6);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();
    }
}
