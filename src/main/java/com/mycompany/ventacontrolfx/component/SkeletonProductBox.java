package com.mycompany.ventacontrolfx.component;

import javafx.animation.Animation;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class SkeletonProductBox extends StackPane {

    public SkeletonProductBox() {
        this.getStyleClass().add("skeleton-box");
        this.setPrefWidth(200);
        this.setMaxWidth(200);
        this.setPrefHeight(320); // Aproximado al ProductBox original

        VBox content = new VBox();
        content.setSpacing(0);
        content.setAlignment(Pos.TOP_LEFT);

        // 1. Image Placeholder Section
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(150);
        imageContainer.setMinHeight(150);
        imageContainer.getStyleClass().add("skeleton-image-container");

        Region imageFiller = new Region();
        imageFiller.getStyleClass().add("skeleton-image-filler");
        StackPane.setMargin(imageFiller, new Insets(8));

        Region priceBadge = new Region();
        priceBadge.getStyleClass().add("skeleton-price-badge");
        StackPane.setAlignment(priceBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(priceBadge, new Insets(10, 10, 0, 0));

        imageContainer.getChildren().addAll(imageFiller, priceBadge);

        // 2. Info Section
        VBox infoSkeleton = new VBox(10);
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
        buttonSkeleton.setMinHeight(40);

        content.getChildren().addAll(imageContainer, infoSkeleton, buttonSkeleton);

        // --- EFECTO DE BRILLO (SHIMMER) ---
        Region shimmer = new Region();
        shimmer.setStyle("-fx-background-color: linear-gradient(to right, " +
                         "transparent 0%, " +
                         "rgba(255, 255, 255, 0.05) 25%, " +
                         "rgba(255, 250, 200, 0.6) 50%, " +
                         "rgba(255, 255, 255, 0.05) 75%, " +
                         "transparent 100%);");
        shimmer.setMouseTransparent(true);
        shimmer.setPrefWidth(1000); // Muy ancho para cubrir la rotaci\u00f3n
        shimmer.setPrefHeight(1000); 
        shimmer.setRotate(20);      // Inclinaci\u00f3n m\u00e1s pronunciada

        this.getChildren().addAll(content, shimmer);

        // Clip para que el brillo no se salga de los bordes redondeados de la caja
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(this.widthProperty());
        clip.heightProperty().bind(this.heightProperty());
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        this.setClip(clip);

        // Animaci\u00f3n de traslaci\u00f3n infinita (aumentamos el rango para asegurar que pase por todo)
        TranslateTransition tt = new TranslateTransition(Duration.seconds(1.8), shimmer);
        tt.setFromX(-600);
        tt.setToX(600);
        tt.setCycleCount(Animation.INDEFINITE);
        tt.play();
    }
}
