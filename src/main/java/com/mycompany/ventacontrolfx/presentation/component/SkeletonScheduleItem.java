package com.mycompany.ventacontrolfx.presentation.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/**
 * Skeleton for Schedule Items in Dashboard Sidebar
 */
public class SkeletonScheduleItem extends StackPane {

    public SkeletonScheduleItem() {
        this.getStyleClass().add("skeleton-box");
        this.setPrefHeight(60);
        this.setMinHeight(60);
        this.setStyle("-fx-background-color: -fx-bg-surface; -fx-background-radius: 12; -fx-padding: 10;");

        VBox content = new VBox(8);
        content.setAlignment(Pos.CENTER_LEFT);

        // Name Placeholder
        Region name = new Region();
        name.setPrefSize(180, 12);
        name.getStyleClass().add("skeleton-text");

        // Time Range Placeholder
        Region time = new Region();
        time.setPrefSize(100, 10);
        time.getStyleClass().add("skeleton-text");

        content.getChildren().addAll(name, time);

        // Shimmer
        Rectangle shimmerLine = new Rectangle(40, 100);
        shimmerLine.setFill(
                new javafx.scene.paint.LinearGradient(0, 0, 1, 0, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.TRANSPARENT),
                        new javafx.scene.paint.Stop(0.5, javafx.scene.paint.Color.web("#ffffff", 0.3)),
                        new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.TRANSPARENT)));
        shimmerLine.setRotate(15);
        shimmerLine.setManaged(false);
        shimmerLine.setMouseTransparent(true);

        this.getChildren().addAll(content, shimmerLine);

        // Clip
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(this.widthProperty());
        clip.heightProperty().bind(this.heightProperty());
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        this.setClip(clip);

        javafx.application.Platform.runLater(() -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                    javafx.util.Duration.seconds(2.0), shimmerLine);
            tt.setFromX(-100);
            tt.setToX(300);
            tt.setCycleCount(javafx.animation.Animation.INDEFINITE);
            tt.play();
        });
    }
}


