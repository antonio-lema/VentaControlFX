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
        this.setPrefHeight(55);
        this.setMinHeight(55);
        this.setMaxHeight(55);
        this.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");

        HBox content = new HBox(0);
        content.setAlignment(Pos.CENTER_LEFT);

        // colStaffStatus: 100px
        Region col1 = createBar(40, 24);
        col1.setStyle("-fx-background-radius: 50;");
        StackPane cell1 = new StackPane(col1);
        cell1.setMinWidth(100); cell1.setPrefWidth(100); cell1.setMaxWidth(100);

        // colStaffName: 200px
        Region col2 = createBar(185, 24);
        StackPane cell2 = new StackPane(col2);
        cell2.setMinWidth(200); cell2.setPrefWidth(200); cell2.setMaxWidth(200);
        cell2.setAlignment(Pos.CENTER_LEFT);

        // colStaffRole: 120px
        Region col3 = createBar(110, 24);
        StackPane cell3 = new StackPane(col3);
        cell3.setMinWidth(120); cell3.setPrefWidth(120); cell3.setMaxWidth(120);
        cell3.setAlignment(Pos.CENTER_LEFT);

        // colStaffShift: 130px
        Region col4 = createBar(120, 24);
        StackPane cell4 = new StackPane(col4);
        cell4.setMinWidth(130); cell4.setPrefWidth(130); cell4.setMaxWidth(130);
        cell4.setAlignment(Pos.CENTER_LEFT);

        // colStaffStart: 100px
        Region col5 = createBar(90, 24);
        StackPane cell5 = new StackPane(col5);
        cell5.setMinWidth(100); cell5.setPrefWidth(100); cell5.setMaxWidth(100);
        cell5.setAlignment(Pos.CENTER_LEFT);

        // colStaffDuration: 100px
        Region col6 = createBar(90, 24);
        StackPane cell6 = new StackPane(col6);
        cell6.setMinWidth(100); cell6.setPrefWidth(100); cell6.setMaxWidth(100);
        cell6.setAlignment(Pos.CENTER_LEFT);

        // colStaffProgress: 150px
        Region col7 = createBar(140, 14);
        col7.setStyle("-fx-background-radius: 8;");
        StackPane cell7 = new StackPane(col7);
        cell7.setMinWidth(150); cell7.setPrefWidth(150); cell7.setMaxWidth(150);
        cell7.setAlignment(Pos.CENTER_LEFT);

        Region filler = new Region();
        HBox.setHgrow(filler, Priority.ALWAYS);

        content.getChildren().addAll(cell1, cell2, cell3, cell4, cell5, cell6, cell7, filler);

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

    private Region createBar(double w, double h) {
        Region r = new Region();
        r.setMinWidth(w); r.setMaxWidth(w); r.setPrefWidth(w);
        r.setMinHeight(h); r.setMaxHeight(h); r.setPrefHeight(h);
        r.getStyleClass().add("skeleton-text");
        return r;
    }
}
