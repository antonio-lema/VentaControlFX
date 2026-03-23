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
import com.mycompany.ventacontrolfx.shared.async.AsyncManager;
import com.mycompany.ventacontrolfx.util.ServerPaginationHelper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
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
    @FXML
    private Pagination pagination;
    @FXML
    private Label lblCount;

    private IPriceRepository priceRepository;
    private AsyncManager asyncManager;
    private PriceList currentList;
    private ServerPaginationHelper<ProductPriceDTO> paginationHelper;
    private String currentSearch = "";

    @Override
    public void inject(ServiceContainer container) {
        this.priceRepository = container.getPriceRepository();
        this.asyncManager = container.getAsyncManager();
    }

    public void initData(PriceList pl) {
        this.currentList = pl;
        lblTitle.setText("Precios: " + pl.getName());
        setupTable();
        paginationHelper = new ServerPaginationHelper<>(tablePrices, null, lblCount, pagination, "productos",
                this::fetchPricesPage);
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("productId"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("productCategory"));
        colProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));

        colBasePrice.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null)
                    setText(null);
                else
                    setText(String.format("%.2f €", getTableRow().getItem().getDefaultPrice()));
            }
        });
        colListPrice.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null)
                    setText(null);
                else
                    setText(String.format("%.2f €", getTableRow().getItem().getPrice()));
            }
        });
        colListPvp.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null)
                    setText(null);
                else
                    setText(String.format("%.2f €", getTableRow().getItem().getListPvp()));
            }
        });
        colDiff.setCellValueFactory(new PropertyValueFactory<>("diffPercentFormatted"));

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

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            this.currentSearch = newValue;
            paginationHelper.refresh();
        });
    }

    private void fetchPricesPage(int offset, int limit) {
        asyncManager.runAsyncTask(() -> {
            int total = priceRepository.countPricesByList(currentList.getId(), currentSearch);
            List<ProductPriceDTO> items = priceRepository.findPricesByListPaginated(currentList.getId(), currentSearch,
                    limit, offset);
            return new Object[] { total, items };
        }, (res) -> {
            Object[] data = (Object[]) res;
            int total = (int) data[0];
            @SuppressWarnings("unchecked")
            List<ProductPriceDTO> items = (List<ProductPriceDTO>) data[1];
            paginationHelper.applyDataTarget(items, total);
        }, null);
    }

    @FXML
    private void handleClose() {
        ((Stage) lblTitle.getScene().getWindow()).close();
    }
}
