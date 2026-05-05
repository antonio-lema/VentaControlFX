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
        this.setPrefSize(200, 80);
        this.setMinSize(180, 70);
        this.setMaxSize(240, 90);
        this.setStyle("-fx-background-radius: 16; -fx-background-color: -fx-bg-surface;");

        HBox content = new HBox(15);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(15));

        // Icon placeholder (Circle)
        Region icon = new Region();
        icon.setPrefSize(40, 40);
        icon.setMinSize(40, 40);
        icon.getStyleClass().add("skeleton-text");
        icon.setStyle("-fx-background-radius: 50;");

        VBox textBox = new VBox(8);
        textBox.setAlignment(Pos.CENTER_LEFT);

        Region title = new Region();
        title.setPrefSize(80, 10);
        title.getStyleClass().add("skeleton-text");

        Region value = new Region();
        value.setPrefSize(110, 16);
        value.getStyleClass().add("skeleton-text");

        textBox.getChildren().addAll(title, value);
        content.getChildren().addAll(icon, textBox);

        // Shimmer
        Rectangle shimmerLine = new Rectangle(50, 150);
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
