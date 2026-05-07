package com.mycompany.ventacontrolfx.presentation.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class SkeletonRoleBox extends StackPane {

    public SkeletonRoleBox() {
        this.getStyleClass().add("skeleton-box");
        this.setPrefWidth(280);
        this.setMaxWidth(280);
        this.setMinWidth(280);
        this.setPrefHeight(180);
        this.setMaxHeight(180);
        this.setMinHeight(180);
        
        this.setStyle("-fx-background-color: -fx-bg-surface; -fx-padding: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");

        VBox content = new VBox(15);
        content.setPadding(new Insets(15));
        content.setAlignment(Pos.TOP_CENTER);

        // Avatar (Circular)
        Region avatar = new Region();
        avatar.setPrefSize(60, 60);
        avatar.getStyleClass().add("skeleton-text");
        avatar.setStyle("-fx-background-radius: 50; -fx-background-color: #f39c12; -fx-opacity: 0.3;");

        // Info (Center aligned)
        VBox info = new VBox(8);
        info.setAlignment(Pos.CENTER);
        Region name = new Region(); name.setPrefSize(220, 14); name.getStyleClass().add("skeleton-text");
        Region desc = new Region(); desc.setPrefSize(240, 10); desc.getStyleClass().add("skeleton-text");
        Region perms = new Region(); perms.setPrefSize(180, 12); perms.getStyleClass().add("skeleton-text");
        info.getChildren().addAll(name, desc, perms);

        // Actions
        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER);
        Region b1 = new Region(); b1.setPrefSize(32, 32); b1.getStyleClass().add("skeleton-text"); b1.setStyle("-fx-background-radius: 50;");
        Region b2 = new Region(); b2.setPrefSize(32, 32); b2.getStyleClass().add("skeleton-text"); b2.setStyle("-fx-background-radius: 50;");
        actions.getChildren().addAll(b1, b2);

        content.getChildren().addAll(avatar, info, actions);

        // Shimmer
        Rectangle shimmer = new Rectangle(60, 400);
        shimmer.setFill(new javafx.scene.paint.LinearGradient(0, 0, 1, 0, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.TRANSPARENT),
                new javafx.scene.paint.Stop(0.5, javafx.scene.paint.Color.web("#ffffff", 0.3)),
                new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.TRANSPARENT)));
        shimmer.setRotate(20);
        shimmer.setManaged(false);
        shimmer.setMouseTransparent(true);

        this.getChildren().addAll(content, shimmer);

        // Animations
        javafx.application.Platform.runLater(() -> {
            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.seconds(2.5), shimmer);
            tt.setFromX(-350); tt.setToX(350); tt.setCycleCount(-1); tt.play();

            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(1.5), content);
            ft.setFromValue(0.6); ft.setToValue(1.0); ft.setAutoReverse(true); ft.setCycleCount(-1); ft.play();
        });

        // Clip
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(this.widthProperty());
        clip.heightProperty().bind(this.heightProperty());
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        this.setClip(clip);
    }
}


