package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.domain.dto.PriceHistoryEventDTO;
import com.mycompany.ventacontrolfx.domain.dto.PriceUpdateLogDTO;
import com.mycompany.ventacontrolfx.domain.dto.ProductPriceDTO;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.domain.repository.IPriceRepository;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.shared.async.AsyncManager;
import com.mycompany.ventacontrolfx.util.ServerPaginationHelper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import com.mycompany.ventacontrolfx.util.DateFilterUtils;
import javafx.beans.property.SimpleStringProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    // History Tab Components
    @FXML
    private TabPane tabPane;
    @FXML
    private ListView<PriceHistoryEventDTO> listHistoryTimeline;

    // Filter UI
    @FXML
    private HBox paneFilterInfo;
    @FXML
    private Label lblFilterInfo;

    @FXML
    private HBox quickFilterContainer;
    @FXML
    private ToggleButton tglFilterHoy, tglFilter7d, tglFilter1m, tglFilterTodo;

    private IPriceRepository priceRepository;
    private AsyncManager asyncManager;
    private ServiceContainer container;
    private PriceList currentList;
    private ServerPaginationHelper<ProductPriceDTO> paginationHelper;
    private String currentSearch = "";
    private LocalDateTime currentFilterDate;
    private Integer historyPeriodDays = null;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.priceRepository = container.getPriceRepository();
        this.asyncManager = container.getAsyncManager();
    }

    public void initData(PriceList pl) {
        this.currentList = pl;
        lblTitle.setText("Precios: " + pl.getName());
        setupTables();
        paginationHelper = new ServerPaginationHelper<>(tablePrices, null, lblCount, pagination, "productos",
                this::fetchPricesPage);

        // Load initial data for tabs
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null && newTab.getText().contains("Historial")) {
                loadHistory();
            }
        });

        DateFilterUtils.addQuickFilters(quickFilterContainer, (label) -> {
            switch (label) {
                case "Hoy":
                    historyPeriodDays = 1;
                    break;
                case "7D":
                    historyPeriodDays = 7;
                    break;
                case "Este Mes":
                    historyPeriodDays = 30;
                    break;
                default:
                    historyPeriodDays = null;
                    break;
            }
        }, this::loadHistory);
    }

    private void setupTables() {
        // Table: Current Prices
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
        colDiff.setCellValueFactory(cellData -> {
            ProductPriceDTO dto = cellData.getValue();
            if (currentFilterDate != null) {
                return new SimpleStringProperty(dto.getDiffWithCurrentFormatted());
            } else {
                return new SimpleStringProperty(dto.getDiffPercentFormatted());
            }
        });

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

        // Timeline Setup
        listHistoryTimeline.setCellFactory(lv -> new HistoryTimelineCell());
        listHistoryTimeline.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                PriceHistoryEventDTO selected = listHistoryTimeline.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    applyHistoryFilter(selected);
                }
            }
        });
    }

    private void applyHistoryFilter(PriceHistoryEventDTO event) {
        if (event.getType() == PriceHistoryEventDTO.EventType.BULK_UPDATE) {
            this.currentFilterDate = event.getTimestamp();
            this.currentSearch = "";
            txtSearch.setText("");
            lblFilterInfo.setText("Filtro: Actualización Masiva ("
                    + event.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) + ")");
            colDiff.setText("▲ vs Hoy");
        } else {
            this.currentFilterDate = null;
            this.currentSearch = event.getTargetName();
            txtSearch.setText(event.getTargetName());
            lblFilterInfo.setText("Mostrando cambios en: " + event.getTargetName());
        }

        paneFilterInfo.setVisible(true);
        paneFilterInfo.setManaged(true);
        tabPane.getSelectionModel().select(0);
        paginationHelper.refresh();
    }

    private void fetchPricesPage(int offset, int limit) {
        asyncManager.runAsyncTask(() -> {
            int total = priceRepository.countPricesByList(currentList.getId(), currentSearch, currentFilterDate);
            List<ProductPriceDTO> items = priceRepository.findPricesByListPaginated(currentList.getId(), currentSearch,
                    currentFilterDate, limit, offset);
            return new Object[] { total, items };
        }, (res) -> {
            Object[] data = (Object[]) res;
            int total = (int) data[0];
            @SuppressWarnings("unchecked")
            List<ProductPriceDTO> items = (List<ProductPriceDTO>) data[1];
            paginationHelper.applyDataTarget(items, total);
        }, null);
    }

    private void loadHistory() {
        asyncManager.runAsyncTask(() -> {
            List<PriceUpdateLogDTO> bulkLogs = priceRepository.findBulkUpdateLog(currentList.getId());
            List<ProductPriceDTO> priceHistory = priceRepository.findAllPriceHistory(currentList.getId());

            List<PriceHistoryEventDTO> events = new ArrayList<>();

            // Map bulk logs
            for (PriceUpdateLogDTO log : bulkLogs) {
                events.add(new PriceHistoryEventDTO(
                        PriceHistoryEventDTO.EventType.BULK_UPDATE,
                        log.getAppliedAt(),
                        "Actualización Masiva: " + log.getUpdateType(),
                        "Modificación de " + log.getValue() + " aplicada a " + log.getScope(),
                        log.getReason(),
                        "Afectó a " + log.getProductsUpdated() + " productos",
                        null));
            }

            // Map individual history
            for (ProductPriceDTO hist : priceHistory) {
                if (hist.getEndDate() != null) {
                    events.add(new PriceHistoryEventDTO(
                            PriceHistoryEventDTO.EventType.MANUAL_CHANGE,
                            hist.getEndDate(),
                            "Cambio Manual: " + hist.getProductName(),
                            "Precio anterior: " + String.format("%.2f €", hist.getPrice()),
                            hist.getReason(),
                            "Sustituido por nueva vigencia",
                            hist.getProductName()));
                }
            }

            // Sort by date desc
            events.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

            // Apply quick date filter if any
            if (historyPeriodDays != null) {
                LocalDateTime cutoff = LocalDateTime.now().minus(historyPeriodDays, ChronoUnit.DAYS);
                return events.stream()
                        .filter(e -> e.getTimestamp().isAfter(cutoff))
                        .collect(Collectors.toList());
            }

            return events;
        }, (res) -> {
            List<PriceHistoryEventDTO> events = (List<PriceHistoryEventDTO>) res;
            listHistoryTimeline.setItems(FXCollections.observableArrayList(events));
        }, null);
    }

    @FXML
    private void handleBack() {
        container.getNavigationService().navigateTo("/view/price_lists.fxml");
    }

    @FXML
    private void handleRefresh() {
        this.currentFilterDate = null;
        this.currentSearch = "";
        txtSearch.setText("");
        paneFilterInfo.setVisible(false);
        paneFilterInfo.setManaged(false);
        colDiff.setText("▲ %");
        paginationHelper.refresh();
        if (tabPane.getSelectionModel().getSelectedIndex() == 1) {
            loadHistory();
        }
    }

    @FXML
    private void handleClose() {
        handleBack();
    }

    @FXML
    private void handleDateFilter(javafx.event.ActionEvent event) {
        ToggleButton source = (ToggleButton) event.getSource();
        if (!source.isSelected()) {
            source.setSelected(true);
            return;
        }

        // Unselect others
        tglFilterHoy.setSelected(source == tglFilterHoy);
        tglFilter7d.setSelected(source == tglFilter7d);
        tglFilter1m.setSelected(source == tglFilter1m);
        tglFilterTodo.setSelected(source == tglFilterTodo);

        if (source == tglFilterHoy)
            historyPeriodDays = 1;
        else if (source == tglFilter7d)
            historyPeriodDays = 7;
        else if (source == tglFilter1m)
            historyPeriodDays = 30;
        else
            historyPeriodDays = null;

        loadHistory();
    }
}
