package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.domain.dto.PriceHistoryEventDTO;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.time.format.DateTimeFormatter;

public class HistoryTimelineCell extends ListCell<PriceHistoryEventDTO> {
    private final HBox container = new HBox(15);
    private final VBox iconContainer = new VBox();
    private final VBox contentContainer = new VBox(5);
    private final Label lblTitle = new Label();
    private final Label lblTime = new Label();
    private final Label lblDescription = new Label();
    private final Label lblDetails = new Label();
    private final Label lblReason = new Label();
    private final FontAwesomeIconView icon = new FontAwesomeIconView();

    public HistoryTimelineCell() {
        setupLayout();
    }

    private void setupLayout() {
        container.setPadding(new Insets(15));
        container.setAlignment(Pos.TOP_LEFT);
        container.setStyle("-fx-background-color: -fx-bg-surface; -fx-background-radius: 8; -fx-margin: 5 0;");

        iconContainer.setAlignment(Pos.CENTER);
        iconContainer.setMinWidth(40);
        iconContainer.setMinHeight(40);
        iconContainer.setMaxWidth(40);
        iconContainer.setMaxHeight(40);
        iconContainer.setStyle("-fx-background-radius: 20;");
        iconContainer.getChildren().add(icon);

        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: -fx-text-main;");
        lblTime.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-text-muted;");
        lblDescription.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-text-main;");
        lblDetails.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-accent-dark; -fx-font-weight: bold;");
        lblReason.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-text-muted; -fx-font-style: italic;");

        VBox header = new VBox(2);
        header.getChildren().addAll(lblTitle, lblTime);

        contentContainer.getChildren().addAll(header, lblDescription, lblDetails, lblReason);
        HBox.setHgrow(contentContainer, Priority.ALWAYS);

        container.getChildren().addAll(iconContainer, contentContainer);
    }

    @Override
    protected void updateItem(PriceHistoryEventDTO item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            lblTitle.setText(item.getTitle());
            lblTime.setText(item.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            lblDescription.setText(item.getDescription());
            lblDetails.setText(item.getDetails());
            lblReason.setText(item.getReason() != null ? "Motivo: " + item.getReason() : "");

            if (item.getType() == PriceHistoryEventDTO.EventType.BULK_UPDATE) {
                icon.setGlyphName("CHART_LINE");
                iconContainer.setStyle("-fx-background-color: #e0f2fe; -fx-background-radius: 20;");
                icon.setFill(javafx.scene.paint.Color.valueOf("#0369a1"));
            } else {
                icon.setGlyphName("USER_EDIT");
                iconContainer.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 20;");
                icon.setFill(javafx.scene.paint.Color.valueOf("#15803d"));
            }

            setGraphic(container);
        }
    }
}

