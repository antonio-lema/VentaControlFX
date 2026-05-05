package com.mycompany.ventacontrolfx.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/**
 * Skeleton for Summary/KPI Cards (Dashboard/History)
 */
public class SkeletonStatCard extends StackPane {

    public SkeletonStatCard() {
        this.getStyleClass().add("skeleton-box");
        // No fixed size or background, let it fit the parent label
        this.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(0);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(5, 0, 5, 0));

        // Value placeholder - Much thicker and wider to match the kpi-value-saas font size
        Region value = new Region();
        value.setMinWidth(160);
        value.setPrefWidth(200);
        value.setMaxWidth(240);
        value.setMinHeight(34);
        value.setPrefHeight(34);
        value.setMaxHeight(34);
        value.getStyleClass().add("skeleton-text");
        value.setStyle("-fx-background-radius: 8;");

        content.getChildren().add(value);

        // Shimmer - Larger and slower for a premium feel
        Rectangle shimmerLine = new Rectangle(80, 150);
        shimmerLine.setFill(
                new javafx.scene.paint.LinearGradient(0, 0, 1, 0, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                        new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.TRANSPARENT),
                        new javafx.scene.paint.Stop(0.5, javafx.scene.paint.Color.web("#ffffff", 0.4)),
                        new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.TRANSPARENT)));
        shimmerLine.setRotate(15);
        shimmerLine.setManaged(false);
        shimmerLine.setMouseTransparent(true);

        this.getChildren().addAll(content, shimmerLine);

        // Clip
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(this.widthProperty());
        clip.heightProperty().bind(this.heightProperty());
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        this.setClip(clip);

        javafx.application.Platform.runLater(() -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                    javafx.util.Duration.seconds(2.5), shimmerLine);
            tt.setFromX(-150);
            tt.setToX(250);
            tt.setCycleCount(javafx.animation.Animation.INDEFINITE);
            tt.play();
        });
    }
}
