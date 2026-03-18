package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.PriceListUseCase;
import com.mycompany.ventacontrolfx.application.usecase.MassivePriceUpdateUseCase;
import com.mycompany.ventacontrolfx.application.usecase.TaxManagementUseCase;
import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.domain.model.TaxRate;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.domain.model.PriceUpdateLog;
import com.mycompany.ventacontrolfx.domain.model.TaxRevision;
import com.mycompany.ventacontrolfx.domain.repository.ICategoryRepository;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import com.mycompany.ventacontrolfx.domain.repository.ITaxRepository;
import com.mycompany.ventacontrolfx.domain.repository.IPriceUpdateLogRepository;
import com.mycompany.ventacontrolfx.domain.model.TaxGroup;
import com.mycompany.ventacontrolfx.domain.exception.BusinessException;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.layout.VBox;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;

/**
 * Controlador de la pantalla de Gestión de IVA y Subida Masiva de Precios.
 *
 * Soporta 5 tipos de agrupación para la actualización masiva:
 * 1. Por Categoría (o todas)
 * 2. Top Vendidos (Top N en últimos N días)
 * 3. Sin Movimiento / Slow-movers
 * 4. Rango de Precio
 * 5. Favoritos
 */
public class VatManagementController implements Injectable {

    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        postInject();
    }

    // ── TAX GROUP CONFIG (V2.0) ───────────────────────────────────────────────
    @FXML
    private Label lblCurrentGlobalTaxGroup;
    @FXML
    private ComboBox<TaxGroup> cmbGlobalTaxGroup;
    @FXML
    private TextField txtGlobalReason;
    @FXML
    private MenuButton cmbCategory;
    @FXML
    private ComboBox<TaxGroup> cmbCategoryTaxGroup;
    @FXML
    private TextField txtCategoryReason;

    // ── SUBIDA MASIVA: selectores principales ─────────────────────────────────
    @FXML
    private ComboBox<PriceList> cmbPriceListUpdate;
    @FXML
    private ComboBox<String> cmbGroupingType;
    @FXML
    private ComboBox<String> cmbPriceUpdateType;
    @FXML
    private Label lblPriceValue;
    @FXML
    private TextField txtPriceValue;
    @FXML
    private TextField txtPriceReason;

    // ── SUBIDA MASIVA: sub-paneles dinámicos ──────────────────────────────────
    @FXML
    private HBox panelClone;
    @FXML
    private ComboBox<PriceList> cmbSourcePriceList;
    @FXML
    private HBox panelCategory;
    @FXML
    private ComboBox<Category> cmbPriceCategory;

    @FXML
    private HBox panelTopSellers;
    @FXML
    private TextField txtTopN;
    @FXML
    private TextField txtTopDays;

    @FXML
    private HBox panelBottomSellers;
    @FXML
    private TextField txtBottomN;
    @FXML
    private TextField txtBottomDays;

    @FXML
    private HBox panelSlowMovers;
    @FXML
    private TextField txtSlowDays;

    @FXML
    private HBox panelPriceRange;
    @FXML
    private TextField txtMinPrice;
    @FXML
    private TextField txtMaxPrice;

    @FXML
    private VBox panelProducts;
    @FXML
    private TextField txtProductSearch;
    @FXML
    private ListView<com.mycompany.ventacontrolfx.domain.model.Product> listSelectedProducts;
    @FXML
    private Label lblSelectedCount;
    @FXML
    private DatePicker dpPriceStartDate;

    @FXML
    private HBox panelFavorites;

    // ── HISTORIAL IVA ─────────────────────────────────────────────────────────
    @FXML
    private ComboBox<String> cmbHistoryScope;
    @FXML
    private TableView<TaxRevision> historyTable;
    @FXML
    private TableColumn<TaxRevision, String> colDate;
    @FXML
    private TableColumn<TaxRevision, String> colEndDate;
    @FXML
    private TableColumn<TaxRevision, String> colScope;
    @FXML
    private TableColumn<TaxRevision, String> colTarget;
    @FXML
    private TableColumn<TaxRevision, String> colRate;
    @FXML
    private TableColumn<TaxRevision, String> colReason;

    // ── IVA POR PRODUCTO ─────────────────────────────────────────────────────
    @FXML
    private TextField txtVatProductSearch;
    @FXML
    private ListView<com.mycompany.ventacontrolfx.domain.model.Product> listVatSelectedProducts;
    @FXML
    private ComboBox<TaxGroup> cmbProductTaxGroup;
    @FXML
    private MenuButton btnVatProductExplorer;

    private ObservableList<com.mycompany.ventacontrolfx.domain.model.Product> vatSelectedProducts = FXCollections
            .observableArrayList();

    // ── MANTENIMIENTO: TAX RATES ──────────────────────────────────────────────
    @FXML
    private TableView<TaxRate> taxRatesTable;
    @FXML
    private TableColumn<TaxRate, String> colRateName;
    @FXML
    private TableColumn<TaxRate, String> colRateValue;
    @FXML
    private TableColumn<TaxRate, String> colRateCountry;
    @FXML
    private TableColumn<TaxRate, String> colRateStatus;
    @FXML
    private TableColumn<TaxRate, Void> colRateActions;

    // ── MANTENIMIENTO: TAX GROUPS ─────────────────────────────────────────────
    @FXML
    private TableView<TaxGroup> taxGroupsTable;
    @FXML
    private TableColumn<TaxGroup, String> colGroupName;
    @FXML
    private TableColumn<TaxGroup, String> colGroupDefault;
    @FXML
    private TableColumn<TaxGroup, String> colGroupRates;
    @FXML
    private TableColumn<TaxGroup, Void> colGroupActions;

    // ── HISTORIAL ACTUALIZACIONES DE PRECIO ───────────────────────────────────
    @FXML
    private TableView<PriceUpdateLog> priceLogTable;
    @FXML
    private TableColumn<PriceUpdateLog, String> colLogDate;
    @FXML
    private TableColumn<PriceUpdateLog, String> colLogType;
    @FXML
    private TableColumn<PriceUpdateLog, String> colLogScope;
    @FXML
    private TableColumn<PriceUpdateLog, String> colLogCategory;
    @FXML
    private TableColumn<PriceUpdateLog, String> colLogValue;
    @FXML
    private TableColumn<PriceUpdateLog, String> colLogProducts;
    @FXML
    private TableColumn<PriceUpdateLog, String> colLogReason;

    // ── Servicios ─────────────────────────────────────────────────────────────
    private MassivePriceUpdateUseCase priceUpdateUseCase;
    private ICategoryRepository categoryRepository;
    private ITaxRepository taxRepository;
    private IProductRepository productRepository;
    private PriceListUseCase priceListUseCase;
    private IPriceUpdateLogRepository priceLogRepository;
    private TaxManagementUseCase taxManagementUseCase;

    private ObservableList<com.mycompany.ventacontrolfx.domain.model.Product> allVisibleProducts = FXCollections
            .observableArrayList();
    private ObservableList<com.mycompany.ventacontrolfx.domain.model.Product> selectedProducts = FXCollections
            .observableArrayList();

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Constantes de agrupación ──────────────────────────────────────────────
    private static final String GROUP_CATEGORY = "Por Categoría";
    private static final String GROUP_TOP = "Más Vendidos (Top N)";
    private static final String GROUP_BOTTOM = "Menos Vendidos (Bottom N)";
    private static final String GROUP_SLOW = "Sin Movimiento (Slow-movers)";
    private static final String GROUP_RANGE = "Por Rango de Precio";
    private static final String GROUP_FAVORITES = "Favoritos ★";
    private static final String GROUP_PRODUCTS = "Productos Específicos";
    private static final String GROUP_ALL = "Todos los Artículos";
    private static final String GROUP_CLONE = "Clonar de otra Tarifa (Sincronizar)";

    @FXML
    public void initialize() {
        setupTaxHistoryTable();
        setupPriceLogTable();
        setupGroupingSelector();
        setupOperationTypeSelector();
        setupTaxCatalogTables();
        setupVatProductCard();
    }

    private void setupVatProductCard() {
        listVatSelectedProducts.setItems(vatSelectedProducts);
        listVatSelectedProducts.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(com.mycompany.ventacontrolfx.domain.model.Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String sku = item.getSku();
                    String displayText = item.getName()
                            + (sku != null && !sku.trim().isEmpty() ? " (" + sku + ")" : "");
                    setText(displayText);
                    Button btnRemove = new Button();
                    btnRemove.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
                    btnRemove.getStyleClass().add("estetica-btn-icon-danger");
                    btnRemove.setOnAction(e -> vatSelectedProducts.remove(item));
                    setGraphic(btnRemove);
                    setContentDisplay(ContentDisplay.RIGHT);
                }
            }
        });

        txtVatProductSearch.setOnAction(e -> {
            String query = txtVatProductSearch.getText().toLowerCase();
            if (query.isEmpty())
                return;

            if (allVisibleProducts.isEmpty()) {
                try {
                    allVisibleProducts.setAll(productRepository.getAllVisible());
                } catch (SQLException ex) {
                    AlertUtil.showError("Error al cargar productos", ex.getMessage());
                }
            }

            Optional<com.mycompany.ventacontrolfx.domain.model.Product> found = allVisibleProducts.stream()
                    .filter(p -> p.getName().toLowerCase().contains(query) || p.getSku().toLowerCase().contains(query))
                    .findFirst();

            if (found.isPresent()) {
                if (!vatSelectedProducts.contains(found.get())) {
                    vatSelectedProducts.add(found.get());
                    txtVatProductSearch.clear();
                }
            } else {
                AlertUtil.showWarning("No encontrado", "No se encontró ningún producto con ese nombre o SKU.");
            }
        });
    }

    private void postInject() {
        if (container != null) {
            this.priceListUseCase = container.getPriceListUseCase();
            this.priceUpdateUseCase = container.getMassivePriceUpdateUseCase();
            this.categoryRepository = container.getCategoryUseCase().getRepository();
            this.taxRepository = container.getTaxRepository();
            this.priceLogRepository = container.getPriceLogRepository();
            this.taxManagementUseCase = container.getTaxManagementUseCase();
            this.productRepository = container.getProductRepository();

            setupTaxGroupComboBoxes();
            loadInitialData();
            setupHistoryFilter();
            cmbProductTaxGroup.setItems(cmbGlobalTaxGroup.getItems());
            cmbProductTaxGroup.setConverter(cmbGlobalTaxGroup.getConverter());
            setupVatProductExplorer();
        }
    }

    private void setupTaxGroupComboBoxes() {
        // Configurar cómo se muestran los grupos en los combos
        javafx.util.StringConverter<TaxGroup> converter = new javafx.util.StringConverter<>() {
            @Override
            public String toString(TaxGroup group) {
                return group == null ? "—" : group.getName();
            }

            @Override
            public TaxGroup fromString(String string) {
                return null;
            }
        };
        cmbGlobalTaxGroup.setConverter(converter);
        cmbCategoryTaxGroup.setConverter(converter);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SETUP
    // ══════════════════════════════════════════════════════════════════════════

    private void setupVatProductExplorer() {
        try {
            List<Category> categories = categoryRepository.getAll();
            if (allVisibleProducts.isEmpty()) {
                allVisibleProducts.setAll(productRepository.getAllVisible());
            }

            btnVatProductExplorer.getItems().clear();

            for (Category cat : categories) {
                Menu catMenu = new Menu(cat.getName());

                List<com.mycompany.ventacontrolfx.domain.model.Product> catProds = allVisibleProducts.stream()
                        .filter(p -> p.getCategoryId() == cat.getId())
                        .collect(Collectors.toList());

                for (com.mycompany.ventacontrolfx.domain.model.Product prod : catProds) {
                    String sku = prod.getSku();
                    String labelText = prod.getName() + (sku != null && !sku.trim().isEmpty() ? " [" + sku + "]" : "");
                    CheckBox cb = new CheckBox(labelText);
                    cb.getStyleClass().add("permission-checkbox");
                    cb.setPadding(new javafx.geometry.Insets(5, 10, 5, 10));
                    cb.setMaxWidth(Double.MAX_VALUE);

                    // Sync state from list to checkbox
                    cb.setSelected(vatSelectedProducts.contains(prod));

                    cb.selectedProperty().addListener((obs, old, nv) -> {
                        if (nv) {
                            if (!vatSelectedProducts.contains(prod)) {
                                vatSelectedProducts.add(prod);
                            }
                        } else {
                            vatSelectedProducts.remove(prod);
                        }
                    });

                    // Sync state from checkbox to list (when list changes externally)
                    vatSelectedProducts.addListener(
                            (javafx.collections.ListChangeListener<com.mycompany.ventacontrolfx.domain.model.Product>) c -> {
                                while (c.next()) {
                                    if (c.wasRemoved() && c.getRemoved().contains(prod)) {
                                        cb.setSelected(false);
                                    } else if (c.wasAdded() && c.getAddedSubList().contains(prod)) {
                                        cb.setSelected(true);
                                    }
                                }
                            });

                    CustomMenuItem item = new CustomMenuItem(cb, false);
                    item.setHideOnClick(false);
                    catMenu.getItems().add(item);
                }

                if (!catMenu.getItems().isEmpty()) {
                    btnVatProductExplorer.getItems().add(catMenu);
                }
            }
        } catch (SQLException e) {
            showError("Error al cargar el explorador: " + e.getMessage());
        }
    }

    private void setupGroupingSelector() {
        cmbGroupingType.setItems(FXCollections.observableArrayList(
                GROUP_CATEGORY,
                GROUP_ALL,
                GROUP_CLONE,
                GROUP_TOP,
                GROUP_BOTTOM,
                GROUP_SLOW,
                GROUP_RANGE,
                GROUP_FAVORITES));
        cmbGroupingType.getSelectionModel().selectFirst();

        cmbGroupingType.getSelectionModel().selectedItemProperty().addListener((obs, old, nv) -> {
            if (nv == null)
                return;
            boolean isCat = GROUP_CATEGORY.equals(nv) || GROUP_ALL.equals(nv);
            boolean isTop = GROUP_TOP.equals(nv);
            boolean isBottom = GROUP_BOTTOM.equals(nv);
            boolean isSlow = GROUP_SLOW.equals(nv);
            boolean isRange = GROUP_RANGE.equals(nv);
            boolean isFav = GROUP_FAVORITES.equals(nv);
            boolean isClone = GROUP_CLONE.equals(nv);
            boolean isProd = GROUP_PRODUCTS.equals(nv);

            setPanel(panelCategory, isCat);
            setPanel(panelTopSellers, isTop);
            setPanel(panelBottomSellers, isBottom);
            setPanel(panelSlowMovers, isSlow);
            setPanel(panelPriceRange, isRange);
            setPanel(panelFavorites, isFav);
            setPanel(panelClone, isClone);
            setPanel(panelProducts, isProd);

            if (isProd) {
                setupProductSearch();
            }
        });
    }

    private void setPanel(javafx.scene.layout.Region panel, boolean visible) {
        if (panel != null) {
            panel.setVisible(visible);
            panel.setManaged(visible);
        }
    }

    private void setupProductSearch() {
        if (allVisibleProducts.isEmpty()) {
            try {
                allVisibleProducts.setAll(productRepository.getAllVisible());
            } catch (SQLException e) {
                AlertUtil.showError("Error al cargar productos", e.getMessage());
            }
        }

        listSelectedProducts.setItems(selectedProducts);
        listSelectedProducts.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(com.mycompany.ventacontrolfx.domain.model.Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String sku = item.getSku();
                    String displayText = item.getName()
                            + (sku != null && !sku.trim().isEmpty() ? " (" + sku + ")" : "");
                    setText(displayText);
                    // Botón para eliminar de la selección
                    Button btnRemove = new Button();
                    btnRemove.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
                    btnRemove.getStyleClass().add("estetica-btn-icon-danger");
                    btnRemove.setOnAction(e -> selectedProducts.remove(item));
                    setGraphic(btnRemove);
                    setContentDisplay(ContentDisplay.RIGHT);
                }
            }
        });

        // Configurar el autocompletado/búsqueda
        txtProductSearch.textProperty().addListener((obs, old, nv) -> {
            if (nv == null || nv.trim().isEmpty())
                return;
            // Podríamos usar un ContextMenu o un Popup para mostrar resultados
        });

        // Por simplicidad en esta fase, usaremos un diálogo de búsqueda al presionar
        // ENTER o un botón
        txtProductSearch.setOnAction(e -> {
            String query = txtProductSearch.getText().toLowerCase();
            if (query.isEmpty())
                return;

            Optional<com.mycompany.ventacontrolfx.domain.model.Product> found = allVisibleProducts.stream()
                    .filter(p -> p.getName().toLowerCase().contains(query) || p.getSku().toLowerCase().contains(query))
                    .findFirst();

            if (found.isPresent()) {
                if (!selectedProducts.contains(found.get())) {
                    selectedProducts.add(found.get());
                    txtProductSearch.clear();
                    updateSelectedCountText();
                }
            } else {
                AlertUtil.showWarning("No encontrado", "No se encontró ningún producto con ese nombre o SKU.");
            }
        });
    }

    private void updateSelectedCountText() {
        lblSelectedCount.setText(selectedProducts.size() + " artículos seleccionados");
    }

    private void setupOperationTypeSelector() {
        cmbPriceUpdateType.setItems(FXCollections.observableArrayList(
                "Porcentaje (%)",
                "Importe Fijo (€)",
                "Redondear a (.99, .50...)"));
        cmbPriceUpdateType.getSelectionModel().selectFirst();

        cmbPriceUpdateType.getSelectionModel().selectedItemProperty().addListener((obs, old, nv) -> {
            if (lblPriceValue == null || nv == null)
                return;
            switch (nv) {
                case "Porcentaje (%)" -> {
                    lblPriceValue.setText("Porcentaje (%):");
                    txtPriceValue.setPromptText("Ej: 10  (negativo para bajar)");
                }
                case "Importe Fijo (€)" -> {
                    lblPriceValue.setText("Importe Fijo (€):");
                    txtPriceValue.setPromptText("Ej: 1.00  (negativo para bajar)");
                }
                case "Redondear a (.99, .50...)" -> {
                    lblPriceValue.setText("Decimal objetivo:");
                    txtPriceValue.setPromptText("Ej: 0.99 o 0.50 o 0.00");
                }
            }
        });
    }

    private void setupTaxHistoryTable() {
        colDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStartDate().format(formatter)));
        colEndDate.setCellValueFactory(cell -> cell.getValue().getEndDate() == null
                ? new SimpleStringProperty("Vigente")
                : new SimpleStringProperty(cell.getValue().getEndDate().format(formatter)));
        colScope.setCellValueFactory(new PropertyValueFactory<>("scope"));
        colTarget.setCellValueFactory(cell -> {
            TaxRevision r = cell.getValue();
            return new SimpleStringProperty(switch (r.getScope()) {
                case GLOBAL -> "Global";
                case CATEGORY -> "Cat. ID: " + r.getCategoryId();
                case PRODUCT -> "Prod. ID: " + r.getProductId();
            });
        });
        colRate.setCellValueFactory(
                cell -> new SimpleStringProperty(String.format("%.2f%%",
                        cell.getValue().getRate())));
        colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
    }

    private void setupPriceLogTable() {
        colLogDate.setCellValueFactory(
                cell -> new SimpleStringProperty(cell.getValue().getAppliedAt().format(formatter)));
        colLogType.setCellValueFactory(cell -> {
            String type = cell.getValue().getUpdateType();
            String display = switch (type) {
                case "percentage" -> "Porcentaje (%)";
                case "fixed" -> "Importe Fijo (€)";
                case "rounding" -> "Redondeo";
                default -> type;
            };
            return new SimpleStringProperty(display);
        });
        colLogScope.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getScope()));
        colLogCategory.setCellValueFactory(cell -> {
            String catName = cell.getValue().getCategoryName();
            return new SimpleStringProperty(catName != null ? catName : "—");
        });
        colLogValue.setCellValueFactory(cell -> {
            String type = cell.getValue().getUpdateType();
            double val = cell.getValue().getValue();
            String display = switch (type) {
                case "percentage" -> String.format("%.2f%%", val);
                case "fixed" -> String.format("%.2f €", val);
                case "rounding" -> String.format("x.%02.0f", val * 100);
                default -> String.valueOf(val);
            };
            return new SimpleStringProperty(display);
        });
        colLogProducts.setCellValueFactory(
                cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getProductsUpdated())));
        colLogReason.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getReason()));
    }

    private void loadInitialData() {
        try {
            // Cargar Grupo de Impuestos Global actual
            Optional<TaxGroup> defaultGroup = taxRepository.getDefaultTaxGroup();
            lblCurrentGlobalTaxGroup.setText(defaultGroup.map(TaxGroup::getName).orElse("No definido"));

            // Cargar todos los grupos para los combos
            List<TaxGroup> taxGroups = taxRepository.getAllTaxGroups();
            cmbGlobalTaxGroup.setItems(FXCollections.observableArrayList(taxGroups));
            cmbCategoryTaxGroup.setItems(FXCollections.observableArrayList(taxGroups));

            defaultGroup.ifPresent(dg -> cmbGlobalTaxGroup.getSelectionModel().select(dg));

            // Categorías
            List<Category> categories = categoryRepository.getAll();
            cmbCategory.getItems().clear();
            for (Category cat : categories) {
                CheckBox cb = new CheckBox(cat.getName());
                cb.getStyleClass().add("permission-checkbox");
                cb.setMaxWidth(Double.MAX_VALUE);
                cb.setUserData(cat);

                cb.selectedProperty().addListener((obs, old, nv) -> {
                    long count = cmbCategory.getItems().stream()
                            .map(m -> ((CustomMenuItem) m).getContent())
                            .filter(n -> n instanceof CheckBox && ((CheckBox) n).isSelected())
                            .count();
                    cmbCategory.setText(count == 0 ? "Elegir..." : count + " seleccionadas");
                });

                CustomMenuItem item = new CustomMenuItem(cb, false);
                cmbCategory.getItems().add(item);
            }

            Category allOption = new Category(0, "Todas las categorías", true, false, 0.0);
            ObservableList<Category> priceCategories = FXCollections.observableArrayList();
            priceCategories.add(allOption);
            priceCategories.addAll(categories);
            cmbPriceCategory.setItems(priceCategories);
            cmbPriceCategory.getSelectionModel().selectFirst();

            // Cargar listas de precios para el selector de actualización masiva
            List<PriceList> priceLists = priceListUseCase.getAll();
            cmbPriceListUpdate.setItems(FXCollections.observableArrayList(priceLists));
            // Seleccionar la por defecto
            priceLists.stream().filter(PriceList::isDefault).findFirst()
                    .ifPresent(pl -> cmbPriceListUpdate.getSelectionModel().select(pl));
            if (cmbPriceListUpdate.getSelectionModel().isEmpty() && !priceLists.isEmpty()) {
                cmbPriceListUpdate.getSelectionModel().selectFirst();
            }

        } catch (SQLException e) {
            showError("No se pudo cargar la información inicial: " + e.getMessage());
        }
        loadTaxCatalogData();
    }

    private void setupTaxCatalogTables() {
        // --- Tax Rates Table ---
        colRateName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colRateValue.setCellValueFactory(
                cell -> new SimpleStringProperty(String.format("%.2f%%", cell.getValue().getRate())));
        colRateCountry.setCellValueFactory(new PropertyValueFactory<>("country"));
        colRateStatus.setCellValueFactory(
                cell -> new SimpleStringProperty(cell.getValue().isActive() ? "Activo" : "Inactivo"));

        setupTaxRateActionsColumn();

        // --- Tax Groups Table ---
        colGroupName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colGroupDefault.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isDefault() ? "SÍ" : "—"));
        colGroupRates.setCellValueFactory(cell -> {
            List<TaxRate> rates = cell.getValue().getRates();
            if (rates == null || rates.isEmpty())
                return new SimpleStringProperty("Sin tasas");
            return new SimpleStringProperty(rates.stream()
                    .map(r -> r.getName() + " (" + r.getRate() + "%)")
                    .collect(Collectors.joining(", ")));
        });

        setupTaxGroupActionsColumn();
    }

    private void loadTaxCatalogData() {
        try {
            if (taxManagementUseCase != null) {
                List<TaxRate> rates = taxManagementUseCase.getAllTaxRates();
                taxRatesTable.setItems(FXCollections.observableArrayList(rates));

                List<TaxGroup> groups = taxManagementUseCase.getAllTaxGroups();
                taxGroupsTable.setItems(FXCollections.observableArrayList(groups));
            }
        } catch (SQLException e) {
            showError("Error al cargar el catálogo de impuestos: " + e.getMessage());
        }
    }

    private void setupTaxRateActionsColumn() {
        colRateActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final HBox container = new HBox(8, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().addAll("btn-sm", "btn-secondary");
                editBtn.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.EDIT));
                editBtn.setOnAction(e -> handleEditTaxRate(getTableView().getItems().get(getIndex())));

                deleteBtn.getStyleClass().addAll("btn-sm", "btn-danger");
                deleteBtn.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
                deleteBtn.setOnAction(e -> handleDeleteTaxRate(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void setupTaxGroupActionsColumn() {
        colGroupActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final HBox container = new HBox(8, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().addAll("btn-sm", "btn-secondary");
                editBtn.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.EDIT));
                editBtn.setOnAction(e -> handleEditTaxGroup(getTableView().getItems().get(getIndex())));

                deleteBtn.getStyleClass().addAll("btn-sm", "btn-danger");
                deleteBtn.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
                deleteBtn.setOnAction(e -> handleDeleteTaxGroup(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void setupHistoryFilter() {
        cmbHistoryScope.setItems(FXCollections.observableArrayList("Global", "Categoría (Todo)", "Producto (Todo)"));
        cmbHistoryScope.getSelectionModel().selectFirst();
        cmbHistoryScope.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, nv) -> refreshHistory());
        refreshHistory();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HANDLERS — IVA
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    void handleUpdateGlobalTaxGroup(ActionEvent event) {
        if (container != null && !container.getUserSession().hasPermission("admin.iva")) {
            AlertUtil.showError("Acceso Denegado", "No tiene permiso para modificar el IVA global.");
            return;
        }
        TaxGroup selected = cmbGlobalTaxGroup.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Selecciona un grupo de impuestos.");
            return;
        }
        try {
            taxManagementUseCase.setDefaultTaxGroup(selected.getTaxGroupId());
            showInfo("Grupo de impuestos global actualizado con éxito. Los nuevos productos heredarán este grupo.");
            lblCurrentGlobalTaxGroup.setText(selected.getName());
            refreshHistory();
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
        }
    }

    @FXML
    void handleUpdateCategoryTaxGroup(ActionEvent event) {
        if (container != null && !container.getUserSession().hasPermission("admin.iva")) {
            AlertUtil.showError("Acceso Denegado", "No tiene permiso para modificar el IVA por categoría.");
            return;
        }
        List<Category> selectedCategories = new ArrayList<>();
        for (javafx.scene.control.MenuItem item : cmbCategory.getItems()) {
            if (item instanceof CustomMenuItem) {
                javafx.scene.Node content = ((CustomMenuItem) item).getContent();
                if (content instanceof CheckBox && ((CheckBox) content).isSelected()) {
                    selectedCategories.add((Category) content.getUserData());
                }
            }
        }

        TaxGroup taxGroup = cmbCategoryTaxGroup.getSelectionModel().getSelectedItem();

        if (selectedCategories.isEmpty()) {
            showWarning("Selecciona al menos una categoría.");
            return;
        }
        if (taxGroup == null) {
            showWarning("Selecciona un grupo de impuestos.");
            return;
        }

        try {
            int updatedCount = 0;
            for (Category cat : selectedCategories) {
                cat.setTaxGroupId(taxGroup.getTaxGroupId());
                container.getCategoryUseCase().update(cat);
                taxManagementUseCase.logCategoryTaxChange(cat.getId(), taxGroup.getTaxGroupId(),
                        txtCategoryReason.getText());
                updatedCount++;
            }

            container.getTaxRepository().syncMirroredValues();
            showInfo("Grupo de impuestos aplicado con éxito a " + updatedCount + " categorías.");
            refreshHistory();

            // Limpiar selección
            cmbCategory.getItems().forEach(item -> {
                javafx.scene.Node n = ((CustomMenuItem) item).getContent();
                if (n instanceof CheckBox)
                    ((CheckBox) n).setSelected(false);
            });
            cmbCategory.setText("Elegir...");

        } catch (Exception e) {
            showError("Error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HANDLER — SUBIDA MASIVA DE PRECIOS
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    void handleMassivePriceUpdate(ActionEvent event) {
        if (container != null && !container.getUserSession().hasPermission("admin.precios_masivo")) {
            AlertUtil.showError("Acceso Denegado",
                    "No tiene permiso para realizar actualizaciones masivas de precios.");
            return;
        }
        String grouping = cmbGroupingType.getSelectionModel().getSelectedItem();
        String opType = cmbPriceUpdateType.getSelectionModel().getSelectedItem();
        String valueStr = txtPriceValue.getText();
        String reason = txtPriceReason.getText();

        if (grouping == null) {
            showWarning("Selecciona el tipo de agrupación.");
            return;
        }
        if (valueStr == null || valueStr.isBlank()) {
            showWarning("Introduce el valor del ajuste.");
            return;
        }
        if (reason == null || reason.isBlank()) {
            showWarning("El motivo es obligatorio para la auditoría.");
            return;
        }

        try {
            double value = Double.parseDouble(valueStr.replace(",", "."));
            int updatedCount;

            PriceList targetPriceList = cmbPriceListUpdate.getSelectionModel().getSelectedItem();
            if (targetPriceList == null) {
                showWarning("Selecciona la tarifa de destino.");
                return;
            }
            int priceListId = targetPriceList.getId();

            java.time.LocalDateTime startDate = (dpPriceStartDate.getValue() != null)
                    ? dpPriceStartDate.getValue().atStartOfDay()
                    : java.time.LocalDateTime.now();

            // ── Resolver operación (porcentaje / fijo / redondeo) ──────────────
            if (GROUP_CLONE.equals(grouping)) {
                PriceList source = cmbSourcePriceList.getSelectionModel().getSelectedItem();
                if (source == null) {
                    showWarning("Selecciona la tarifa de origen.");
                    return;
                }
                priceUpdateUseCase.cloneAndAdjustPrices(source.getId(), priceListId, value, reason, startDate);
                updatedCount = -1;
            } else if ("Porcentaje (%)".equals(opType)) {
                updatedCount = applyByGroupingPercentage(priceListId, grouping, value, reason, startDate);
            } else if ("Importe Fijo (€)".equals(opType)) {
                updatedCount = applyByGroupingFixed(priceListId, grouping, value, reason, startDate);
            } else if ("Redondear a (.99, .50...)".equals(opType)) {
                updatedCount = applyByGroupingRounding(priceListId, grouping, value, reason, startDate);
            } else {
                showWarning("Tipo de operación no reconocido.");
                return;
            }

            AlertUtil.showInfo("Éxito",
                    "Se han actualizado " + (updatedCount == -1 ? "(Clonado)" : updatedCount) + " productos.\n" +
                            "Agrupación: " + grouping + " | Operación registrada en el historial.");

            clearPriceFields();
            refreshPriceLog();

        } catch (NumberFormatException e) {
            showWarning("El valor introducido no es un número válido.");
        } catch (Exception e) {
            showError("Error al aplicar actualización: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Aplica un porcentaje según la agrupación seleccionada. */
    private int applyByGroupingPercentage(int priceListId, String grouping, double pct, String reason,
            java.time.LocalDateTime startDate)
            throws Exception {
        return switch (grouping) {
            case GROUP_ALL -> priceUpdateUseCase.applyPercentageIncreaseToAll(priceListId, pct, reason, startDate);
            case GROUP_CATEGORY -> {
                Integer catId = getCategoryIdFromPanel();
                yield catId != null
                        ? priceUpdateUseCase.applyPercentageIncreaseToCategory(priceListId, catId, pct,
                                reason, startDate)
                        : priceUpdateUseCase.applyPercentageIncreaseToAll(priceListId, pct, reason, startDate);
            }
            case GROUP_PRODUCTS -> {
                List<Integer> ids = selectedProducts.stream().map(p -> p.getId()).collect(Collectors.toList());
                if (ids.isEmpty())
                    throw new BusinessException("No se han seleccionado productos.");
                yield priceUpdateUseCase.applyPercentageIncreaseToProducts(priceListId, ids, pct, reason, startDate);
            }
            case GROUP_TOP -> {
                int topN = parseIntField(txtTopN, "Número de artículos Top");
                int topDays = parseIntField(txtTopDays, "Días de análisis");
                yield priceUpdateUseCase.applyToTopSellers(priceListId, topN, topDays, pct, reason, true, startDate);
            }
            case GROUP_BOTTOM -> {
                int bottomN = parseIntField(txtBottomN, "Número de artículos");
                int bottomDays = parseIntField(txtBottomDays, "Días de análisis");
                yield priceUpdateUseCase.applyToBottomSellers(priceListId, bottomN, bottomDays, pct, reason,
                        true, startDate);
            }
            case GROUP_SLOW -> {
                int slowDays = parseIntField(txtSlowDays, "Días sin venta");
                yield priceUpdateUseCase.applyToSlowMovers(priceListId, slowDays, pct, reason, true, startDate);
            }
            case GROUP_RANGE -> {
                double min = parseDoubleField(txtMinPrice, "Precio mínimo");
                double max = parseDoubleField(txtMaxPrice, "Precio máximo");
                yield priceUpdateUseCase.applyToPriceRange(priceListId, min, max, pct, reason, true, startDate);
            }
            case GROUP_FAVORITES -> priceUpdateUseCase.applyToFavorites(priceListId, pct, reason, true, startDate);
            default -> throw new IllegalStateException("Agrupación no reconocida: " + grouping);
        };
    }

    /** Aplica importe fijo según la agrupación seleccionada. */
    private int applyByGroupingFixed(int priceListId, String grouping, double amount, String reason,
            java.time.LocalDateTime startDate) throws Exception {
        return switch (grouping) {
            case GROUP_ALL ->
                priceUpdateUseCase.applyFixedAmountIncreaseToAll(priceListId, amount, reason, startDate);
            case GROUP_CATEGORY -> {
                Integer catId = getCategoryIdFromPanel();
                yield catId != null
                        ? priceUpdateUseCase.applyFixedAmountIncreaseToCategory(priceListId, catId, amount,
                                reason, startDate)
                        : priceUpdateUseCase.applyFixedAmountIncreaseToAll(priceListId, amount, reason, startDate);
            }
            case GROUP_PRODUCTS -> {
                List<Integer> ids = selectedProducts.stream().map(p -> p.getId()).collect(Collectors.toList());
                if (ids.isEmpty())
                    throw new BusinessException("No se han seleccionado productos.");
                yield priceUpdateUseCase.applyFixedAmountIncreaseToProducts(priceListId, ids, amount, reason,
                        startDate);
            }
            case GROUP_TOP -> {
                int topN = parseIntField(txtTopN, "Número de artículos Top");
                int topDays = parseIntField(txtTopDays, "Días de análisis");
                yield priceUpdateUseCase.applyToTopSellers(priceListId, topN, topDays, amount, reason, false,
                        startDate);
            }
            case GROUP_BOTTOM -> {
                int bottomN = parseIntField(txtBottomN, "Número de artículos");
                int bottomDays = parseIntField(txtBottomDays, "Días de análisis");
                yield priceUpdateUseCase.applyToBottomSellers(priceListId, bottomN, bottomDays, amount,
                        reason, false, startDate);
            }
            case GROUP_SLOW -> {
                int slowDays = parseIntField(txtSlowDays, "Días sin venta");
                yield priceUpdateUseCase.applyToSlowMovers(priceListId, slowDays, amount, reason, false, startDate);
            }
            case GROUP_RANGE -> {
                double min = parseDoubleField(txtMinPrice, "Precio mínimo");
                double max = parseDoubleField(txtMaxPrice, "Precio máximo");
                yield priceUpdateUseCase.applyToPriceRange(priceListId, min, max, amount, reason, false, startDate);
            }
            case GROUP_FAVORITES -> priceUpdateUseCase.applyToFavorites(priceListId, amount, reason, false, startDate);
            default -> throw new IllegalStateException("Agrupación no reconocida: " + grouping);
        };
    }

    /** Aplica redondeo según la agrupación seleccionada. */
    private int applyByGroupingRounding(int priceListId, String grouping, double target, String reason,
            java.time.LocalDateTime startDate)
            throws Exception {
        return switch (grouping) {
            case GROUP_ALL -> priceUpdateUseCase.applyRoundingToAll(priceListId, target, reason, startDate);
            case GROUP_CATEGORY -> {
                Integer catId = getCategoryIdFromPanel();
                yield catId != null
                        ? priceUpdateUseCase.applyRoundingToCategory(priceListId, catId, target, reason, startDate)
                        : priceUpdateUseCase.applyRoundingToAll(priceListId, target, reason, startDate);
            }
            case GROUP_PRODUCTS -> {
                List<Integer> ids = selectedProducts.stream().map(p -> p.getId()).collect(Collectors.toList());
                if (ids.isEmpty())
                    throw new BusinessException("No se han seleccionado productos.");
                yield priceUpdateUseCase.applyRoundingToProducts(priceListId, ids, target, reason, startDate);
            }
            case GROUP_TOP -> {
                int topN = parseIntField(txtTopN, "Número de artículos Top");
                int topDays = parseIntField(txtTopDays, "Días de análisis");
                yield priceUpdateUseCase.applyRoundingToTopSellers(priceListId, topN, topDays, target, reason,
                        startDate);
            }
            case GROUP_BOTTOM -> {
                int bottomN = parseIntField(txtBottomN, "Número de artículos");
                int bottomDays = parseIntField(txtBottomDays, "Días de análisis");
                yield priceUpdateUseCase.applyRoundingToBottomSellers(priceListId, bottomN, bottomDays, target,
                        reason, startDate);
            }
            case GROUP_SLOW -> {
                int slowDays = parseIntField(txtSlowDays, "Días sin venta");
                yield priceUpdateUseCase.applyRoundingToSlowMovers(priceListId, slowDays, target, reason, startDate);
            }
            case GROUP_RANGE -> {
                double min = parseDoubleField(txtMinPrice, "Precio mínimo");
                double max = parseDoubleField(txtMaxPrice, "Precio máximo");
                yield priceUpdateUseCase.applyRoundingToPriceRange(priceListId, min, max, target, reason, startDate);
            }
            case GROUP_FAVORITES -> priceUpdateUseCase.applyRoundingToFavorites(priceListId, target, reason, startDate);
            default -> throw new UnsupportedOperationException(
                    "Operación de redondeo no soportada para agrupación: " + grouping);
        };
    }

    /** Devuelve el categoryId seleccionado o null si se eligió "Todas". */
    private Integer getCategoryIdFromPanel() {
        Category selected = cmbPriceCategory != null
                ? cmbPriceCategory.getSelectionModel().getSelectedItem()
                : null;
        return (selected != null && selected.getId() > 0) ? selected.getId() : null;
    }

    private void clearPriceFields() {
        txtPriceValue.clear();
        txtPriceReason.clear();
        if (txtTopN != null)
            txtTopN.clear();
        if (txtTopDays != null)
            txtTopDays.clear();
        if (txtBottomN != null)
            txtBottomN.clear();
        if (txtBottomDays != null)
            txtBottomDays.clear();
        if (txtSlowDays != null)
            txtSlowDays.clear();
        if (txtMinPrice != null)
            txtMinPrice.clear();
        if (txtMaxPrice != null)
            txtMaxPrice.clear();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HISTORIAL
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void handleUpdateProductTaxGroup(ActionEvent event) {
        if (vatSelectedProducts.isEmpty()) {
            AlertUtil.showWarning("Sin selección", "Por favor, seleccione al menos un producto.");
            return;
        }

        TaxGroup selectedGroup = cmbProductTaxGroup.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            AlertUtil.showWarning("Grupo no seleccionado", "Por favor, seleccione el nuevo grupo de IVA.");
            return;
        }

        try {
            List<Integer> ids = vatSelectedProducts.stream()
                    .map(p -> p.getId())
                    .collect(Collectors.toList());
            taxManagementUseCase.updateTaxGroupForProducts(ids, selectedGroup.getTaxGroupId(),
                    "Cambio manual por producto");

            AlertUtil.showInfo("Éxito", "IVA actualizado para " + ids.size() + " productos.");
            vatSelectedProducts.clear();
            refreshHistory();
        } catch (SQLException e) {
            AlertUtil.showError("Error al actualizar IVA", e.getMessage());
        }
    }

    @FXML
    void handleAddTaxRate(ActionEvent event) {
        TaxRate newRate = new TaxRate();
        newRate.setActive(true);
        newRate.setCountry("ES");
        com.mycompany.ventacontrolfx.util.ModalService.showTransparentModal(
                "/view/dialog/tax_rate_dialog.fxml",
                "Nueva Tasa Impositiva",
                container,
                (com.mycompany.ventacontrolfx.presentation.controller.dialog.TaxRateDialogController ctrl) -> {
                    ctrl.init(newRate);
                });
        loadTaxCatalogData();
    }

    private void handleEditTaxRate(TaxRate rate) {
        com.mycompany.ventacontrolfx.util.ModalService.showTransparentModal(
                "/view/dialog/tax_rate_dialog.fxml",
                "Editar Tasa Impositiva",
                container,
                (com.mycompany.ventacontrolfx.presentation.controller.dialog.TaxRateDialogController ctrl) -> {
                    ctrl.init(rate);
                });
        loadTaxCatalogData();
    }

    private void handleDeleteTaxRate(TaxRate rate) {
        boolean confirmed = AlertUtil.showConfirmation("Eliminar Tasa", "Confirmar eliminación",
                "¿Está seguro de que desea eliminar la tasa '" + rate.getName()
                        + "'?\nEsta acción no se puede deshacer.");
        if (confirmed) {
            try {
                taxManagementUseCase.deleteTaxRate(rate.getTaxRateId());
                loadTaxCatalogData();
                showInfo("Tasa eliminada correctamente.");
            } catch (SQLException e) {
                showError("No se pudo eliminar la tasa. Es posible que esté en uso.");
            }
        }
    }

    @FXML
    void handleAddTaxGroup(ActionEvent event) {
        com.mycompany.ventacontrolfx.util.ModalService.showTransparentModal(
                "/view/dialog/tax_group_dialog.fxml",
                "Nuevo Grupo de Impuestos",
                container,
                (com.mycompany.ventacontrolfx.presentation.controller.dialog.TaxGroupDialogController ctrl) -> {
                    ctrl.init(new TaxGroup());
                });
        loadTaxCatalogData();
        loadInitialData();
    }

    private void handleEditTaxGroup(TaxGroup group) {
        com.mycompany.ventacontrolfx.util.ModalService.showTransparentModal(
                "/view/dialog/tax_group_dialog.fxml",
                "Editar Grupo de Impuestos",
                container,
                (com.mycompany.ventacontrolfx.presentation.controller.dialog.TaxGroupDialogController ctrl) -> {
                    ctrl.init(group);
                });
        loadTaxCatalogData();
        loadInitialData();
    }

    private void handleDeleteTaxGroup(TaxGroup group) {
        boolean confirmed = AlertUtil.showConfirmation("Eliminar Grupo", "Confirmar eliminación",
                "¿Está seguro de que desea eliminar el grupo '" + group.getName()
                        + "'?\nEsta acción no se puede deshacer.");
        if (confirmed) {
            try {
                taxManagementUseCase.deleteTaxGroup(group.getTaxGroupId());
                loadTaxCatalogData();
                loadInitialData(); // Refresh combos
                showInfo("Grupo eliminado correctamente.");
            } catch (SQLException e) {
                showError("No se pudo eliminar el grupo. Es posible que esté en uso en productos o categorías.");
            }
        }
    }

    @FXML
    void handleRefreshHistory(ActionEvent event) {
        refreshHistory();
    }

    private void refreshHistory() {
        try {
            if (taxManagementUseCase != null) {
                // Determine scope from combo
                TaxRevision.Scope scope = null;
                String scopeStr = cmbHistoryScope.getSelectionModel().getSelectedItem();
                if ("Global".equals(scopeStr))
                    scope = TaxRevision.Scope.GLOBAL;
                else if ("Categoría (Todo)".equals(scopeStr))
                    scope = TaxRevision.Scope.CATEGORY;
                else if ("Producto (Todo)".equals(scopeStr))
                    scope = TaxRevision.Scope.PRODUCT;

                final TaxRevision.Scope finalScope = scope;
                List<TaxRevision> history = taxManagementUseCase.getTaxHistory(finalScope);
                historyTable.setItems(FXCollections.observableArrayList(history));
            }
        } catch (Exception e) {
            showError("Error al refrescar historial: " + e.getMessage());
        }
        refreshPriceLog();
    }

    private void refreshPriceLog() {
        if (priceLogTable == null)
            return;
        try {
            List<PriceUpdateLog> logs = priceLogRepository.getAll();
            priceLogTable.setItems(FXCollections.observableArrayList(logs));
        } catch (Exception ignored) {
            // Tabla puede no existir aún en BD antigua
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private int parseIntField(TextField field, String fieldName) {
        if (field == null || field.getText().isBlank())
            throw new IllegalArgumentException("El campo '" + fieldName + "' es obligatorio.");
        try {
            int v = Integer.parseInt(field.getText().trim());
            if (v <= 0)
                throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El campo '" + fieldName + "' debe ser un número entero positivo.");
        }
    }

    private double parseDoubleField(TextField field, String fieldName) {
        if (field == null || field.getText().isBlank())
            throw new IllegalArgumentException("El campo '" + fieldName + "' es obligatorio.");
        try {
            return Double.parseDouble(field.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El campo '" + fieldName + "' debe ser un número válido.");
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Atención");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
