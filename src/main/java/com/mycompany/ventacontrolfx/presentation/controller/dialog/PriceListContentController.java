package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.domain.dto.ProductPriceDTO;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.domain.repository.IPriceRepository;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class PriceListContentController implements Injectable {

    @FXML
    private Label lblTitle;
    @FXML
    private TextField txtSearch;
    @FXML
    private TableView<ProductPriceDTO> tablePrices;
    @FXML
    private TableColumn<ProductPriceDTO, Integer> colId;
    @FXML
    private TableColumn<ProductPriceDTO, String> colCategory;
    @FXML
    private TableColumn<ProductPriceDTO, String> colProduct;
    @FXML
    private TableColumn<ProductPriceDTO, String> colBasePrice;
    @FXML
    private TableColumn<ProductPriceDTO, String> colListPrice;
    @FXML
    private TableColumn<ProductPriceDTO, String> colListPvp;
    @FXML
    private TableColumn<ProductPriceDTO, String> colDiff;

    private IPriceRepository priceRepository;
    private PriceList currentList;
    private ObservableList<ProductPriceDTO> allPrices;

    @Override
    public void inject(ServiceContainer container) {
        this.priceRepository = container.getPriceRepository();
    }

    public void initData(PriceList pl) {
        this.currentList = pl;
        lblTitle.setText("Precios: " + pl.getName());
        setupTable();
        loadData();
    }

    private void setupTable() {
        colId.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getProductId()).asObject());
        colCategory.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProductCategory()));
        colProduct.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProductName()));

        colBasePrice.setCellValueFactory(
                cellData -> new SimpleStringProperty(String.format("%.2f €", cellData.getValue().getDefaultPrice())));
        colListPrice.setCellValueFactory(
                cellData -> new SimpleStringProperty(String.format("%.2f €", cellData.getValue().getPrice())));
        colListPvp.setCellValueFactory(
                cellData -> new SimpleStringProperty(String.format("%.2f €", cellData.getValue().getListPvp())));
        colDiff.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getDiffPercentFormatted()));

        // Estilos para la columna de diferencia
        colDiff.setCellFactory(column -> {
            return new javafx.scene.control.TableCell<ProductPriceDTO, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if (item.startsWith("+")) {
                            setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
                        } else if (item.startsWith("-")) {
                            setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold;");
                        }
                    }
                }
            };
        });

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filterTable(newValue));
    }

    private void loadData() {
        try {
            List<ProductPriceDTO> prices = priceRepository.findPricesByList(currentList.getId());
            allPrices = FXCollections.observableArrayList(prices);
            tablePrices.setItems(allPrices);
        } catch (SQLException e) {
            AlertUtil.showError("Error", "No se pudieron cargar los precios de la tarifa: " + e.getMessage());
        }
    }

    private void filterTable(String filterText) {
        if (allPrices == null)
            return;
        if (filterText == null || filterText.isEmpty()) {
            tablePrices.setItems(allPrices);
            return;
        }

        String lowerCaseFilter = filterText.toLowerCase();
        FilteredList<ProductPriceDTO> filteredData = new FilteredList<>(allPrices, p -> {
            if (p.getProductName().toLowerCase().contains(lowerCaseFilter))
                return true;
            if (p.getProductCategory() != null && p.getProductCategory().toLowerCase().contains(lowerCaseFilter))
                return true;
            if (String.valueOf(p.getProductId()).contains(lowerCaseFilter))
                return true;
            return false;
        });
        tablePrices.setItems(filteredData);
    }

    @FXML
    private void handleClose() {
        ((Stage) lblTitle.getScene().getWindow()).close();
    }
}
