package com.mycompany.ventacontrolfx.presentation.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class SkeletonUserBox extends StackPane {

    public SkeletonUserBox() {
        this.getStyleClass().add("skeleton-box");
        this.setPrefWidth(280);
        this.setMaxWidth(280);
        this.setMinWidth(280);
        this.setPrefHeight(180);
        this.setMaxHeight(180);
        this.setMinHeight(180);
        
        this.setStyle("-fx-background-color: -fx-bg-surface; -fx-padding: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setAlignment(Pos.TOP_LEFT);

        // Header: Avatar circle + Name + Role
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Region avatar = new Region();
        avatar.setPrefSize(44, 44);
        avatar.getStyleClass().add("skeleton-text");
        avatar.setStyle("-fx-background-radius: 50;");

        VBox nameBox = new VBox(5);
        Region name = new Region(); name.setPrefSize(220, 14); name.getStyleClass().add("skeleton-text");
        Region role = new Region(); role.setPrefSize(120, 10); role.getStyleClass().add("skeleton-text");
        nameBox.getChildren().addAll(name, role);
        header.getChildren().addAll(avatar, nameBox);

        // Details lines
        VBox details = new VBox(8);
        for(int i=0; i<2; i++) {
            HBox line = new HBox(8);
            line.setAlignment(Pos.CENTER_LEFT);
            Region icon = new Region(); icon.setPrefSize(12, 12); icon.getStyleClass().add("skeleton-text");
            Region text = new Region(); text.setPrefSize(120 + (i*30), 10); text.getStyleClass().add("skeleton-text");
            line.getChildren().addAll(icon, text);
            details.getChildren().add(line);
        }

        // Actions (Buttons)
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Region b1 = new Region(); b1.setPrefSize(28, 28); b1.getStyleClass().add("skeleton-text"); b1.setStyle("-fx-background-radius: 50;");
        Region b2 = new Region(); b2.setPrefSize(28, 28); b2.getStyleClass().add("skeleton-text"); b2.setStyle("-fx-background-radius: 50;");
        actions.getChildren().addAll(b1, b2);

        content.getChildren().addAll(header, details, actions);

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
            tt.setFromX(-300); tt.setToX(350); tt.setCycleCount(-1); tt.play();

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


