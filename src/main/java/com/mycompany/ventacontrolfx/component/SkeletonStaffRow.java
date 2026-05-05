package com.mycompany.ventacontrolfx.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * Skeleton for Staff Table rows in Dashboard
 */
public class SkeletonStaffRow extends StackPane {

    public SkeletonStaffRow() {
        this.getStyleClass().add("skeleton-box");
        this.setPrefHeight(45);
        this.setMinHeight(45);
        this.setMaxHeight(45);
        this.setStyle("-fx-background-color: -fx-bg-surface; -fx-border-color: -fx-color-border-subtle; -fx-border-width: 0 0 1 0;");

        HBox content = new HBox(15);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(0, 15, 0, 15));

        // Column 1: Status Icon (Circle)
        Region statusCol = new Region();
        statusCol.setPrefSize(12, 12);
        statusCol.getStyleClass().add("skeleton-text");
        statusCol.setStyle("-fx-background-radius: 50;");

        // Column 2: Name Placeholder
        Region nameCol = new Region();
        nameCol.setPrefSize(150, 12);
        nameCol.getStyleClass().add("skeleton-text");

        // Column 3: Role Placeholder
        Region roleCol = new Region();
        roleCol.setPrefSize(80, 10);
        roleCol.getStyleClass().add("skeleton-text");

        // Column 4: Time/Shift Placeholder
        Region timeCol = new Region();
        timeCol.setPrefSize(100, 10);
        timeCol.getStyleClass().add("skeleton-text");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Column 5: Progress Bar Placeholder
        Region progressCol = new Region();
        progressCol.setPrefSize(120, 8);
        progressCol.getStyleClass().add("skeleton-text");
        progressCol.setStyle("-fx-background-radius: 4;");

        content.getChildren().addAll(statusCol, nameCol, roleCol, timeCol, spacer, progressCol);

        // Shimmer
        Rectangle shimmerLine = new Rectangle(50, 100);
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
                    javafx.util.Duration.seconds(2.2), shimmerLine);
            tt.setFromX(-200);
            tt.setToX(1200);
            tt.setCycleCount(javafx.animation.Animation.INDEFINITE);
            tt.play();
        });
    }
}
