package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.PromotionUseCase;
import com.mycompany.ventacontrolfx.domain.model.Promotion;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class PromotionsController implements Injectable {

    @FXML
    private TableView<Promotion> promotionsTable;
    @FXML
    private TableColumn<Promotion, String> colName;
    @FXML
    private TableColumn<Promotion, String> colType;
    @FXML
    private TableColumn<Promotion, Double> colValue;
    @FXML
    private TableColumn<Promotion, String> colScope;
    @FXML
    private TableColumn<Promotion, String> colDates;
    @FXML
    private TableColumn<Promotion, String> colStatus;
    @FXML
    private TableColumn<Promotion, Void> colActions;
    @FXML
    private TextField searchField;

    private PromotionUseCase promotionUseCase;
    private final ObservableList<Promotion> masterData = FXCollections.observableArrayList();
    private final ObservableList<Promotion> filteredData = FXCollections.observableArrayList();
    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.promotionUseCase = container.getPromotionUseCase();
        setupTable();
        loadData();
        setupSearch();
    }

    private void setupTable() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getType().getDisplayName()));
        colValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        colScope.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getScope().getDisplayName()));

        // Custom cell factories for formatting
        colDates.setCellValueFactory(cellData -> {
            Promotion p = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(p.getStartDate() + " - " + p.getEndDate());
        });

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Promotion p = getTableRow().getItem();
                    boolean active = p.isActive();
                    Label statusLabel = new Label(active 
                            ? container.getBundle().getString("promotion.status.active") 
                            : container.getBundle().getString("promotion.status.inactive"));
                    statusLabel.getStyleClass().add(active ? "badge-success" : "badge-danger");
                    setGraphic(statusLabel);
                }
            }
        });

        setupActionsColumn();
        promotionsTable.setItems(filteredData);
    }

    private void setupActionsColumn() {
        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();
            private final HBox container = new HBox(8, btnEdit, btnDelete);

            {
                btnEdit.getStyleClass().addAll("btn-icon", "btn-edit-small");
                btnEdit.setGraphic(new FontAwesomeIconView(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.EDIT));
                btnEdit.setOnAction(e -> handleEdit(getTableRow().getItem()));

                btnDelete.getStyleClass().addAll("btn-icon", "btn-delete-small");
                btnDelete.setGraphic(new FontAwesomeIconView(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TRASH));
                btnDelete.setOnAction(e -> handleDelete(getTableRow().getItem()));

                container.setAlignment(javafx.geometry.Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void loadData() {
        try {
            List<Promotion> promotions = promotionUseCase.getAllPromotions();
            masterData.setAll(promotions);
            applyFilter();
        } catch (SQLException e) {
            e.printStackTrace();
            showError(container.getBundle().getString("promotion.error.load"), e.getMessage());
        }
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
    }

    private void applyFilter() {
        String filter = searchField.getText().toLowerCase();
        if (filter == null || filter.isEmpty()) {
            filteredData.setAll(masterData);
        } else {
            List<Promotion> filtered = masterData.stream()
                    .filter(p -> p.getName().toLowerCase().contains(filter))
                    .collect(Collectors.toList());
            filteredData.setAll(filtered);
        }
    }

    @FXML
    private void handleNewPromotion() {
        showPromotionForm(null);
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    private void handleEdit(Promotion promotion) {
        showPromotionForm(promotion);
    }

    private void handleDelete(Promotion promotion) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(container.getBundle().getString("promotion.confirm.delete.title"));
        alert.setHeaderText(container.getBundle().getString("promotion.confirm.delete.header"));
        alert.setContentText(promotion.getName());

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    promotionUseCase.deletePromotion(promotion.getId());
                    loadData();
                } catch (SQLException e) {
                    showError(container.getBundle().getString("promotion.error.delete"), e.getMessage());
                }
            }
        });
    }

    private void showPromotionForm(Promotion promotion) {
        try {
            PromotionFormController controller = com.mycompany.ventacontrolfx.util.ModalService.showTransparentModal(
                "/view/promotion_form.fxml", 
                promotion == null ? container.getBundle().getString("promotion.btn.new") : container.getBundle().getString("promotion.btn.edit"), 
                container, 
                c -> c.setPromotion(promotion)
            );

            if (controller != null && controller.isSaved()) {
                Promotion p = controller.getPromotion();
                promotionUseCase.savePromotion(p);
                loadData();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError(container.getBundle().getString("promotion.error.form"), e.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(container.getBundle().getString("alert.error"));
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
