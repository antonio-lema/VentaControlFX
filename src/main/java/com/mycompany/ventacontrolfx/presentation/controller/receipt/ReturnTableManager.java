package com.mycompany.ventacontrolfx.presentation.controller.receipt;

import com.mycompany.ventacontrolfx.domain.model.Return;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gestiona el renderizado y comportamiento de la tabla de facturas rectificativas (devoluciones).
 */
public class ReturnTableManager {

    private final ServiceContainer container;
    private final TableView<Return> returnsTable;
    private final TableColumn<Return, String> colId, colUser, colDate, colReason, colActions;
    private final TableColumn<Return, Integer> colSaleId, colClosure;
    private final TableColumn<Return, Double> colAmount;
    private final TableColumn<Return, String> colFiscalStatus;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public ReturnTableManager(
            ServiceContainer container,
            TableView<Return> returnsTable,
            TableColumn<Return, String> colId,
            TableColumn<Return, Integer> colSaleId,
            TableColumn<Return, Integer> colClosure,
            TableColumn<Return, String> colUser,
            TableColumn<Return, String> colDate,
            TableColumn<Return, String> colReason,
            TableColumn<Return, String> colActions,
            TableColumn<Return, Double> colAmount,
            TableColumn<Return, String> colFiscalStatus) {
        this.container = container;
        this.returnsTable = returnsTable;
        this.colId = colId;
        this.colSaleId = colSaleId;
        this.colClosure = colClosure;
        this.colUser = colUser;
        this.colDate = colDate;
        this.colReason = colReason;
        this.colActions = colActions;
        this.colAmount = colAmount;
        this.colFiscalStatus = colFiscalStatus;
    }

    public void init(Runnable onReprint, java.util.function.Consumer<Integer> onViewTicket, java.util.function.Consumer<Return> onDoubleClick) {
        setupTable(onReprint, onViewTicket, onDoubleClick);
    }

    private void setupTable(Runnable onReprint, java.util.function.Consumer<Integer> onViewTicket, java.util.function.Consumer<Return> onDoubleClick) {
        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullReference()));
        colSaleId.setCellValueFactory(new PropertyValueFactory<>("saleId"));
        colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
        colClosure.setCellValueFactory(new PropertyValueFactory<>("closureId"));
        colFiscalStatus.setCellValueFactory(new PropertyValueFactory<>("fiscalStatus"));

        colDate.setCellValueFactory(data -> {
            if (data.getValue() == null || data.getValue().getReturnDatetime() == null) return new SimpleStringProperty("-");
            return new SimpleStringProperty(data.getValue().getReturnDatetime().format(formatter));
        });

        colAmount.setCellValueFactory(new PropertyValueFactory<>("totalRefunded"));
        colAmount.setCellFactory(col -> new TableCell<Return, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().remove("text-danger");
                } else {
                    setText(String.format("%.2f \u20ac", item));
                    getStyleClass().add("text-danger");
                    setStyle("-fx-font-weight: 800;");
                }
            }
        });

        colUser.setCellValueFactory(new PropertyValueFactory<>("userName"));
        colUser.setCellFactory(col -> new TableCell<Return, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item.toUpperCase());
                    badge.getStyleClass().add("badge-info");
                    if ("admin".equalsIgnoreCase(item) || "administrador".equalsIgnoreCase(item)) {
                        badge.setStyle("-fx-background-color: -fx-custom-color-primary-bg; -fx-text-fill: -fx-custom-color-primary;");
                    }
                    setGraphic(badge);
                }
            }
        });

        colReason.setCellFactory(col -> new TableCell<Return, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    Tooltip tooltip = new Tooltip(item);
                    tooltip.setWrapText(true);
                    tooltip.setMaxWidth(300);
                    setTooltip(tooltip);
                }
            }
        });

        colClosure.setCellFactory(col -> new TableCell<Return, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else if (item == null || item == 0) {
                    Label badge = new Label(container.getBundle().getString("returns.table.pending"));
                    badge.getStyleClass().add("badge-warning");
                    setGraphic(badge);
                    setText(null);
                } else {
                    setText("#" + item);
                    setTextFill(Color.web("#64748b"));
                    setGraphic(null);
                }
            }
        });

        colFiscalStatus.setCellFactory(col -> new TableCell<Return, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item.toUpperCase());
                    if ("ACCEPTED".equalsIgnoreCase(item) || "ACEPTADO".equalsIgnoreCase(item)) {
                        badge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-padding: 2 8; -fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: bold;");
                    } else if ("REJECTED".equalsIgnoreCase(item) || "RECHAZADO".equalsIgnoreCase(item)) {
                        badge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-padding: 2 8; -fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: bold;");
                    } else {
                        badge.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 2 8; -fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: bold;");
                    }
                    setGraphic(badge);
                }
            }
        });

        if (container.getAuthService().isAdmin() || container.getAuthService().hasPermission("venta.listar")) {
            colActions.setCellFactory(col -> new TableCell<Return, String>() {
                private final Button btnPrint = new Button();
                private final Button btnView = new Button();
                private final javafx.scene.layout.HBox containerBox = new javafx.scene.layout.HBox(8, btnView, btnPrint);

                {
                    containerBox.setAlignment(javafx.geometry.Pos.CENTER);
                    btnPrint.getStyleClass().add("btn-table-icon");
                    btnPrint.setGraphic(new de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PRINT, "14"));
                    btnPrint.setOnAction(e -> onReprint.run());

                    btnView.getStyleClass().add("btn-table-icon");
                    btnView.setGraphic(new de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.SEARCH, "14"));
                    btnView.setOnAction(e -> onViewTicket.accept(getTableView().getItems().get(getIndex()).getSaleId()));

                    Tooltip.install(btnPrint, new Tooltip("Reimprimir Rectificativa"));
                    Tooltip.install(btnView, new Tooltip("Ver Ticket Original"));
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : containerBox);
                }
            });
        }

        returnsTable.setRowFactory(tv -> {
            TableRow<Return> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onDoubleClick.accept(row.getItem());
                }
            });
            return row;
        });
    }

    public void setData(List<Return> data) {
        returnsTable.setItems(javafx.collections.FXCollections.observableArrayList(data));
    }

    public Return getSelection() {
        return returnsTable.getSelectionModel().getSelectedItem();
    }
}

