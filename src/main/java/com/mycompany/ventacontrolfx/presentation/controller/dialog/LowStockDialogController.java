package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.shared.async.AsyncManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;

import java.util.List;

public class LowStockDialogController implements Injectable {

    @FXML
    private TableView<Product> tableLowStock;
    @FXML
    private TableColumn<Product, String> colName;
    @FXML
    private TableColumn<Product, Integer> colCurrentStock;
    @FXML
    private TableColumn<Product, Integer> colMinStock;

    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        setupTable();
        loadData();
        setupDragging();
    }

    private void setupDragging() {
        // Esperamos a que la escena esté lista para evitar el error que has visto
        tableLowStock.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                VBox root = (VBox) newScene.getRoot();
                final double[] xOffset = new double[1];
                final double[] yOffset = new double[1];

                root.setOnMousePressed(event -> {
                    xOffset[0] = event.getSceneX();
                    yOffset[0] = event.getSceneY();
                });

                root.setOnMouseDragged(event -> {
                    Stage stage = (Stage) root.getScene().getWindow();
                    stage.setX(event.getScreenX() - xOffset[0]);
                    stage.setY(event.getScreenY() - yOffset[0]);
                });
            }
        });
    }

    private void setupTable() {
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colCurrentStock
                .setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getStockQuantity()).asObject());
        colMinStock.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getMinStock()).asObject());
    }

    private void loadData() {
        AsyncManager.execute(container.getDashboardUseCase().getLowStockProductsTask(),
                (List<Product> products) -> {
                    if (products != null) {
                        tableLowStock.setItems(FXCollections.observableArrayList(products));
                    }
                });
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) tableLowStock.getScene().getWindow();
        stage.close();
    }
}

