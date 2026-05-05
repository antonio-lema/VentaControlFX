package com.mycompany.ventacontrolfx.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * Skeleton for Category buttons in Sell View (160x48 approx)
 */
public class SkeletonCategoryBox extends StackPane {

    public SkeletonCategoryBox() {
        this.getStyleClass().add("skeleton-box");
        this.setPrefSize(160, 48);
        this.setMinSize(160, 48);
        this.setMaxSize(160, 48);
        this.setStyle("-fx-background-radius: 12; -fx-background-color: -fx-bg-surface;");

        HBox content = new HBox(10);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(0, 15, 0, 15));

        // Icon placeholder
        Region icon = new Region();
        icon.setPrefSize(18, 18);
        icon.setMinSize(18, 18);
        icon.getStyleClass().add("skeleton-text");
        icon.setStyle("-fx-background-radius: 4;");

        // Text placeholder
        Region text = new Region();
        text.setPrefSize(80, 12);
        text.getStyleClass().add("skeleton-text");

        content.getChildren().addAll(icon, text);

        // Shimmer
        Rectangle shimmerLine = new Rectangle(40, 100);
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
        Rectangle clip = new Rectangle(160, 48);
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        this.setClip(clip);

        javafx.application.Platform.runLater(() -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                    javafx.util.Duration.seconds(2.0), shimmerLine);
            tt.setFromX(-100);
            tt.setToX(200);
            tt.setCycleCount(javafx.animation.Animation.INDEFINITE);
            tt.play();
        });
    }
}
