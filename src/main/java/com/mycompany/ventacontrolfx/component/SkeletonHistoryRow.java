package com.mycompany.ventacontrolfx.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * Skeleton for History Table rows (Horizontal)
 */
public class SkeletonHistoryRow extends StackPane {

    public SkeletonHistoryRow() {
        this.getStyleClass().add("skeleton-box");
        this.setPrefHeight(45);
        this.setMinHeight(45);
        this.setMaxHeight(45);
        this.setStyle("-fx-background-color: -fx-bg-surface; -fx-border-color: -fx-color-border-subtle; -fx-border-width: 0 0 1 0;");

        HBox content = new HBox(15);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(0, 15, 0, 15));

        // Column 1: ID Placeholder
        Region idCol = new Region();
        idCol.setPrefSize(40, 12);
        idCol.getStyleClass().add("skeleton-text");

        // Column 2: Date Placeholder
        Region dateCol = new Region();
        dateCol.setPrefSize(140, 12);
        dateCol.getStyleClass().add("skeleton-text");

        // Column 3: Payment Method Placeholder
        Region methodCol = new Region();
        methodCol.setPrefSize(100, 12);
        methodCol.getStyleClass().add("skeleton-text");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Column 4: Total Placeholder (Right aligned)
        Region totalCol = new Region();
        totalCol.setPrefSize(80, 16);
        totalCol.getStyleClass().add("skeleton-text");

        content.getChildren().addAll(idCol, dateCol, methodCol, spacer, totalCol);

        // Shimmer
        Rectangle shimmerLine = new Rectangle(60, 100);
        shimmerLine.setFill(
                new javafx.scene.paint.LinearGradient(0, 0, 1, 0, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.TRANSPARENT),
                        new javafx.scene.paint.Stop(0.5, javafx.scene.paint.Color.web("#ffffff", 0.3)),
                        new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.TRANSPARENT)));
        shimmerLine.setRotate(15);
        shimmerLine.setManaged(false);
        shimmerLine.setMouseTransparent(true);

        this.getChildren().addAll(content, shimmerLine);

        javafx.application.Platform.runLater(() -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                    javafx.util.Duration.seconds(2.0), shimmerLine);
            tt.setFromX(-200);
            tt.setToX(1000); // Longer for horizontal rows
            tt.setCycleCount(javafx.animation.Animation.INDEFINITE);
            tt.play();
        });
    }
}
