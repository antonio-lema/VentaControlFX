package com.mycompany.ventacontrolfx.presentation.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class SkeletonClientBox extends StackPane {

    public SkeletonClientBox() {
        this.getStyleClass().add("skeleton-box");
        this.setPrefWidth(280);
        this.setMaxWidth(280);
        this.setMinWidth(280);
        this.setPrefHeight(200);
        this.setMaxHeight(200);
        this.setMinHeight(200);
        
        this.setStyle("-fx-background-color: -fx-bg-surface; -fx-padding: 20; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_LEFT);

        // 1. Header (Avatar + Name)
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Region avatarSkeleton = new Region();
        avatarSkeleton.setPrefSize(40, 40);
        avatarSkeleton.setMinSize(40, 40);
        avatarSkeleton.getStyleClass().add("skeleton-text");
        avatarSkeleton.setStyle("-fx-background-radius: 50;"); // Circle

        VBox nameBox = new VBox(6);
        Region titleSkeleton = new Region();
        titleSkeleton.setPrefSize(220, 16);
        titleSkeleton.getStyleClass().add("skeleton-text");

        Region subtitleSkeleton = new Region();
        subtitleSkeleton.setPrefSize(140, 10);
        subtitleSkeleton.getStyleClass().add("skeleton-text");
        
        nameBox.getChildren().addAll(titleSkeleton, subtitleSkeleton);
        header.getChildren().addAll(avatarSkeleton, nameBox);

        // 2. Info Lines
        VBox infoLines = new VBox(10);
        for (int i = 0; i < 3; i++) {
            HBox line = new HBox(10);
            line.setAlignment(Pos.CENTER_LEFT);
            Region iconPlaceholder = new Region();
            iconPlaceholder.setPrefSize(14, 14);
            iconPlaceholder.getStyleClass().add("skeleton-text");
            
            Region textPlaceholder = new Region();
            textPlaceholder.setPrefSize(200 + (i * 20), 12);
            textPlaceholder.getStyleClass().add("skeleton-text");
            
            line.getChildren().addAll(iconPlaceholder, textPlaceholder);
            infoLines.getChildren().add(line);
        }

        // 3. Footer (City + Buttons)
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        
        Region citySkeleton = new Region();
        citySkeleton.setPrefSize(80, 12);
        citySkeleton.getStyleClass().add("skeleton-text");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        HBox buttons = new HBox(8);
        Region b1 = new Region(); b1.setPrefSize(32, 32); b1.getStyleClass().add("skeleton-text"); b1.setStyle("-fx-background-radius: 50;");
        Region b2 = new Region(); b2.setPrefSize(32, 32); b2.getStyleClass().add("skeleton-text"); b2.setStyle("-fx-background-radius: 50;");
        buttons.getChildren().addAll(b1, b2);
        
        footer.getChildren().addAll(citySkeleton, spacer, buttons);

        content.getChildren().addAll(header, infoLines, footer);

        // --- SHIMMER EFFECT ---
        Rectangle shimmerLine = new Rectangle(60, 400);
        shimmerLine.setFill(
                new javafx.scene.paint.LinearGradient(0, 0, 1, 0, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.TRANSPARENT),
                        new javafx.scene.paint.Stop(0.5, javafx.scene.paint.Color.web("#ffffff", 0.3)),
                        new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.TRANSPARENT)));
        shimmerLine.setRotate(15);
        shimmerLine.setMouseTransparent(true);
        shimmerLine.setManaged(false);
        shimmerLine.setCache(true);
        shimmerLine.setCacheHint(javafx.scene.CacheHint.SPEED);

        this.getChildren().addAll(content, shimmerLine);

        // Clip
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(this.widthProperty());
        clip.heightProperty().bind(this.heightProperty());
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        this.setClip(clip);

        // Animations
        javafx.application.Platform.runLater(() -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                    javafx.util.Duration.seconds(3.0), shimmerLine);
            tt.setFromX(-350);
            tt.setToX(350);
            tt.setCycleCount(javafx.animation.Animation.INDEFINITE);
            tt.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);

            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(2.0),
                    content);
            ft.setFromValue(0.6);
            ft.setToValue(1.0);
            ft.setAutoReverse(true);
            ft.setCycleCount(javafx.animation.Animation.INDEFINITE);

            tt.play();
            ft.play();
        });
    }
}


