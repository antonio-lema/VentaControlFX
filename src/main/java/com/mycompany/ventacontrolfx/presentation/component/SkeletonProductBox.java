package com.mycompany.ventacontrolfx.presentation.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class SkeletonProductBox extends StackPane {

    public SkeletonProductBox() {
        this.getStyleClass().add("skeleton-box");
        this.setPrefWidth(200); 
        this.setMaxWidth(200);
        this.setMinWidth(200);
        
        VBox content = new VBox();
        content.setSpacing(0);
        content.setAlignment(Pos.TOP_LEFT);

        // 1. Image Placeholder Section
        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("skeleton-image-container");
        imageContainer.setPrefHeight(150);
        imageContainer.setMinHeight(150);
        imageContainer.setMaxHeight(150);
        imageContainer.setAlignment(Pos.CENTER);

        // Clip estático como en ProductBox real
        Rectangle clipImage = new Rectangle(200, 180);
        clipImage.setArcWidth(28);
        clipImage.setArcHeight(28);
        imageContainer.setClip(clipImage);

        // Display container for the margin, just like real ProductBox uses for its image
        StackPane imageDisplayContainer = new StackPane();
        StackPane.setMargin(imageDisplayContainer, new Insets(8));
        
        Region imageFiller = new Region();
        imageFiller.getStyleClass().add("skeleton-image-filler");
        imageFiller.setPrefSize(184, 134);
        imageFiller.setMaxSize(184, 134);
        
        imageDisplayContainer.getChildren().add(imageFiller);

        Region priceBadge = new Region();
        priceBadge.getStyleClass().add("skeleton-price-badge");
        StackPane.setAlignment(priceBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(priceBadge, new Insets(10, 10, 0, 0));

        imageContainer.getChildren().addAll(imageDisplayContainer, priceBadge);

        // 2. Info Section
        VBox infoSkeleton = new VBox(8);
        infoSkeleton.getStyleClass().add("product-info"); // Reusing exactly the same padding class
        infoSkeleton.setAlignment(Pos.TOP_LEFT);

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
        buttonSkeleton.setMinHeight(48);
        buttonSkeleton.setMaxHeight(48);
        buttonSkeleton.setMaxWidth(Double.MAX_VALUE);

        content.getChildren().addAll(imageContainer, infoSkeleton, buttonSkeleton);

        // --- SHIMMER EFFECT ---
        Rectangle shimmerLine = new Rectangle(60, 300);
        shimmerLine.setFill(
                new javafx.scene.paint.LinearGradient(0, 0, 1, 0, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.TRANSPARENT),
                        new javafx.scene.paint.Stop(0.5, javafx.scene.paint.Color.web("#ffffff", 0.5)),
                        new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.TRANSPARENT)));
        shimmerLine.setRotate(15);
        shimmerLine.setMouseTransparent(true);
        shimmerLine.setCache(true);
        shimmerLine.setManaged(false); 
        shimmerLine.setCacheHint(javafx.scene.CacheHint.SPEED);

        this.getChildren().addAll(content, shimmerLine);

        // Card clip
        Rectangle clip = new Rectangle(200, 300);
        clip.widthProperty().bind(this.widthProperty());
        clip.heightProperty().bind(this.heightProperty());
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        this.setClip(clip);

        // Animations
        javafx.application.Platform.runLater(() -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                    javafx.util.Duration.seconds(3.0), shimmerLine);
            tt.setFromX(-250);
            tt.setToX(250);
            tt.setCycleCount(javafx.animation.Animation.INDEFINITE);
            tt.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);

            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(2.0),
                    content);
            ft.setFromValue(0.7);
            ft.setToValue(1.0);
            ft.setAutoReverse(true);
            ft.setCycleCount(javafx.animation.Animation.INDEFINITE);

            tt.play();
            ft.play();
        });
    }
}
