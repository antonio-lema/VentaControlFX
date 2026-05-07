package com.mycompany.ventacontrolfx.presentation.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * Skeleton for History Table rows - Perfectly aligned with sales.fxml columns
 */
public class SkeletonHistoryRow extends StackPane {

    public SkeletonHistoryRow() {
        this.getStyleClass().add("skeleton-box");
        this.setPrefHeight(50);
        this.setMinHeight(50);
        this.setMaxHeight(50);
        this.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");

        HBox content = new HBox(0); // 0 spacing to align with TableView cells
        content.setAlignment(Pos.CENTER_LEFT);

        // Column 1: Ticket ID (100px)
        Region col1 = createBar(80, 24);
        StackPane cell1 = new StackPane(col1);
        cell1.setMinWidth(100); cell1.setPrefWidth(100); cell1.setMaxWidth(100);
        cell1.setAlignment(Pos.CENTER_LEFT);

        // Column 2: Attended by (180px)
        Region col2 = createBar(160, 24);
        StackPane cell2 = new StackPane(col2);
        cell2.setMinWidth(180); cell2.setPrefWidth(180); cell2.setMaxWidth(180);
        cell2.setAlignment(Pos.CENTER_LEFT);

        // Column 3: Date (150px)
        Region col3 = createBar(130, 24);
        StackPane cell3 = new StackPane(col3);
        cell3.setMinWidth(150); cell3.setPrefWidth(150); cell3.setMaxWidth(150);
        cell3.setAlignment(Pos.CENTER_LEFT);

        // Column 4: Total (120px)
        Region col4 = createBar(100, 24);
        StackPane cell4 = new StackPane(col4);
        cell4.setMinWidth(120); cell4.setPrefWidth(120); cell4.setMaxWidth(120);
        cell4.setAlignment(Pos.CENTER_LEFT);

        // Column 5: Method (150px)
        Region col5 = createBar(130, 24);
        StackPane cell5 = new StackPane(col5);
        cell5.setMinWidth(150); cell5.setPrefWidth(150); cell5.setMaxWidth(150);
        cell5.setAlignment(Pos.CENTER_LEFT);

        // Column 6: Fiscal Status (120px)
        Region col6 = createBar(100, 24);
        StackPane cell6 = new StackPane(col6);
        cell6.setMinWidth(120); cell6.setPrefWidth(120); cell6.setMaxWidth(120);
        cell6.setAlignment(Pos.CENTER_LEFT);

        // Add a filler region that grows (like constrained resize policy)
        Region filler = new Region();
        HBox.setHgrow(filler, Priority.ALWAYS);

        content.getChildren().addAll(cell1, cell2, cell3, cell4, cell5, cell6, filler);

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


