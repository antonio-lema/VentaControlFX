package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.PriceListUseCase;
import com.mycompany.ventacontrolfx.application.usecase.ScheduleVatChangeUseCase;
import com.mycompany.ventacontrolfx.application.usecase.MassivePriceUpdateUseCase;
import com.mycompany.ventacontrolfx.application.usecase.TaxManagementUseCase;
import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.domain.model.TaxRate;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.domain.model.PriceUpdateLog;
import com.mycompany.ventacontrolfx.domain.model.TaxRevision;
import com.mycompany.ventacontrolfx.domain.repository.ICategoryRepository;
import com.mycompany.ventacontrolfx.domain.repository.IPriceRepository;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import com.mycompany.ventacontrolfx.domain.repository.ITaxRepository;
import com.mycompany.ventacontrolfx.domain.repository.IPriceUpdateLogRepository;
import com.mycompany.ventacontrolfx.domain.model.TaxGroup;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
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
    private ComboBox<Category> cmbCategory;
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
    private ScheduleVatChangeUseCase vatUseCase;
    private MassivePriceUpdateUseCase priceUpdateUseCase;
    private ICategoryRepository categoryRepository;
    private ITaxRepository taxRepository;
    private IPriceRepository priceRepository;
    private IProductRepository productRepository;
    private PriceListUseCase priceListUseCase;
    private IPriceUpdateLogRepository priceLogRepository;
    private TaxManagementUseCase taxManagementUseCase;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Constantes de agrupación ──────────────────────────────────────────────
    private static final String GROUP_CATEGORY = "Por Categoría";
    private static final String GROUP_TOP = "Más Vendidos (Top N)";
    private static final String GROUP_BOTTOM = "Menos Vendidos (Bottom N)";
    private static final String GROUP_SLOW = "Sin Movimiento (Slow-movers)";
    private static final String GROUP_RANGE = "Por Rango de Precio";
    private static final String GROUP_FAVORITES = "Favoritos ★";
    private static final String GROUP_ALL = "Todos los Artículos";
    private static final String GROUP_CLONE = "Clonar de otra Tarifa (Sincronizar)";

    @FXML
    public void initialize() {
        setupTaxHistoryTable();
        setupPriceLogTable();
        setupGroupingSelector();
        setupOperationTypeSelector();
        setupTaxCatalogTables();
    }

    private void postInject() {
        if (container != null) {
            this.priceListUseCase = container.getPriceListUseCase();
            this.vatUseCase = container.getVatUseCase();
            this.priceUpdateUseCase = container.getMassivePriceUpdateUseCase();
            this.categoryRepository = container.getCategoryUseCase().getRepository();
            this.taxRepository = container.getTaxRepository();
            this.priceLogRepository = container.getPriceLogRepository();
            this.taxManagementUseCase = container.getTaxManagementUseCase();

            setupTaxGroupComboBoxes();
            loadInitialData();
            setupHistoryFilter();
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

            setPanel(panelCategory, isCat);
            setPanel(panelTopSellers, isTop);
            setPanel(panelBottomSellers, isBottom);
            setPanel(panelSlowMovers, isSlow);
            setPanel(panelPriceRange, isRange);
            setPanel(panelFavorites, isFav);
            setPanel(panelClone, isClone);
        });
    }

    private void setPanel(HBox panel, boolean visible) {
        if (panel != null) {
            panel.setVisible(visible);
            panel.setManaged(visible);
        }
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
        // Obsoleto en V2: Se reimplementará en la fase de UI.
        /*
         * colDate.setCellValueFactory(cell -> new
         * SimpleStringProperty(cell.getValue().getStartDate().format(formatter)));
         * colEndDate.setCellValueFactory(cell -> cell.getValue().getEndDate() == null
         * ? new SimpleStringProperty("Vigente")
         * : new SimpleStringProperty(cell.getValue().getEndDate().format(formatter)));
         * colScope.setCellValueFactory(new PropertyValueFactory<>("scope"));
         * colTarget.setCellValueFactory(cell -> {
         * TaxRevision r = cell.getValue();
         * return new SimpleStringProperty(switch (r.getScope()) {
         * case GLOBAL -> "Global";
         * case CATEGORY -> "Cat. ID: " + r.getCategoryId();
         * case PRODUCT -> "Prod. ID: " + r.getProductId();
         * });
         * });
         * colRate.setCellValueFactory(
         * cell -> new SimpleStringProperty(String.format("%.2f%%",
         * cell.getValue().getRate())));
         * colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
         */
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
            cmbCategory.setItems(FXCollections.observableArrayList(categories));

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
            taxRepository.setDefaultTaxGroup(selected.getTaxGroupId());
            showInfo("Grupo de impuestos global actualizado con éxito. Los nuevos productos heredarán este grupo.");
            lblCurrentGlobalTaxGroup.setText(selected.getName());
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
        Category category = cmbCategory.getSelectionModel().getSelectedItem();
        TaxGroup taxGroup = cmbCategoryTaxGroup.getSelectionModel().getSelectedItem();

        if (category == null) {
            showWarning("Selecciona una categoría.");
            return;
        }
        if (taxGroup == null) {
            showWarning("Selecciona un grupo de impuestos.");
            return;
        }

        try {
            category.setTaxGroupId(taxGroup.getTaxGroupId());
            container.getCategoryUseCase().update(category);

            showInfo("Grupo de impuestos para la categoría '" + category.getName() + "' actualizado con éxito.");
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
            String updateTypeKey;

            PriceList targetPriceList = cmbPriceListUpdate.getSelectionModel().getSelectedItem();
            if (targetPriceList == null) {
                showWarning("Selecciona la tarifa de destino.");
                return;
            }
            int priceListId = targetPriceList.getId();

            // ── Resolver operación (porcentaje / fijo / redondeo) ──────────────
            if (GROUP_CLONE.equals(grouping)) {
                updateTypeKey = "clonacion";
                PriceList source = cmbSourcePriceList.getSelectionModel().getSelectedItem();
                if (source == null) {
                    showWarning("Selecciona la tarifa de origen.");
                    return;
                }
                priceUpdateUseCase.cloneAndAdjustPrices(source.getId(), priceListId, value, reason);
                updatedCount = -1; // No lo sabemos exactamente sin otra query, pondremos -1 o un valor simbólico
            } else if ("Porcentaje (%)".equals(opType)) {
                updateTypeKey = "percentage";
                updatedCount = applyByGroupingPercentage(priceListId, grouping, value, reason);
            } else if ("Importe Fijo (€)".equals(opType)) {
                updateTypeKey = "fixed";
                updatedCount = applyByGroupingFixed(priceListId, grouping, value, reason);
            } else if ("Redondear a (.99, .50...)".equals(opType)) {
                updateTypeKey = "rounding";
                updatedCount = applyByGroupingRounding(priceListId, grouping, value, reason);
            } else {
                showWarning("Tipo de operación no reconocido.");
                return;
            }

            // ── Guardar en historial ───────────────────────────────────────────
            // ELIMINADO: Ahora lo hace automáticamente el Use Case delegando en el
            // Repositorio de Auditoría
            // saveLog(updateTypeKey, grouping, value, updatedCount, reason);

            AlertUtil.showInfo("Éxito",
                    "Se han actualizado " + updatedCount + " productos.\n" +
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
    private int applyByGroupingPercentage(int priceListId, String grouping, double pct, String reason)
            throws Exception {
        return switch (grouping) {
            case GROUP_ALL -> priceUpdateUseCase.applyPercentageIncreaseToAll(priceListId, pct, reason);
            case GROUP_CATEGORY -> {
                Integer catId = getCategoryIdFromPanel();
                yield catId != null
                        ? priceUpdateUseCase.applyPercentageIncreaseToCategory(priceListId, catId, pct,
                                reason)
                        : priceUpdateUseCase.applyPercentageIncreaseToAll(priceListId, pct, reason);
            }
            case GROUP_TOP -> {
                int topN = parseIntField(txtTopN, "Número de artículos Top");
                int topDays = parseIntField(txtTopDays, "Días de análisis");
                yield priceUpdateUseCase.applyToTopSellers(priceListId, topN, topDays, pct, reason, true);
            }
            case GROUP_BOTTOM -> {
                int bottomN = parseIntField(txtBottomN, "Número de artículos");
                int bottomDays = parseIntField(txtBottomDays, "Días de análisis");
                yield priceUpdateUseCase.applyToBottomSellers(priceListId, bottomN, bottomDays, pct, reason,
                        true);
            }
            case GROUP_SLOW -> {
                int slowDays = parseIntField(txtSlowDays, "Días sin venta");
                yield priceUpdateUseCase.applyToSlowMovers(priceListId, slowDays, pct, reason, true);
            }
            case GROUP_RANGE -> {
                double min = parseDoubleField(txtMinPrice, "Precio mínimo");
                double max = parseDoubleField(txtMaxPrice, "Precio máximo");
                yield priceUpdateUseCase.applyToPriceRange(priceListId, min, max, pct, reason, true);
            }
            case GROUP_FAVORITES -> priceUpdateUseCase.applyToFavorites(priceListId, pct, reason, true);
            default -> throw new IllegalStateException("Agrupación no reconocida: " + grouping);
        };
    }

    /** Aplica importe fijo según la agrupación seleccionada. */
    private int applyByGroupingFixed(int priceListId, String grouping, double amount, String reason) throws Exception {
        return switch (grouping) {
            case GROUP_ALL ->
                priceUpdateUseCase.applyFixedAmountIncreaseToAll(priceListId, amount, reason);
            case GROUP_CATEGORY -> {
                Integer catId = getCategoryIdFromPanel();
                yield catId != null
                        ? priceUpdateUseCase.applyFixedAmountIncreaseToCategory(priceListId, catId, amount,
                                reason)
                        : priceUpdateUseCase.applyFixedAmountIncreaseToAll(priceListId, amount, reason);
            }
            case GROUP_TOP -> {
                int topN = parseIntField(txtTopN, "Número de artículos Top");
                int topDays = parseIntField(txtTopDays, "Días de análisis");
                // Para importe fijo sobre top sellers: aplicamos porcentaje proporcional aprox,
                // pero lo más correcto es usar el propio método con multiplier = amount como
                // incremento absoluto.
                // La implementación del use case aplica el valor directamente como porcentaje
                // (mal diseño previo).
                // Aqui usamos el método applyFixedAmountIncreaseToAll limitado a top sellers
                // via SQL:
                yield priceUpdateUseCase.applyToTopSellers(priceListId, topN, topDays, amount, reason, false);
            }
            case GROUP_BOTTOM -> {
                int bottomN = parseIntField(txtBottomN, "Número de artículos");
                int bottomDays = parseIntField(txtBottomDays, "Días de análisis");
                yield priceUpdateUseCase.applyToBottomSellers(priceListId, bottomN, bottomDays, amount,
                        reason, false);
            }
            case GROUP_SLOW -> {
                int slowDays = parseIntField(txtSlowDays, "Días sin venta");
                yield priceUpdateUseCase.applyToSlowMovers(priceListId, slowDays, amount, reason, false);
            }
            case GROUP_RANGE -> {
                double min = parseDoubleField(txtMinPrice, "Precio mínimo");
                double max = parseDoubleField(txtMaxPrice, "Precio máximo");
                yield priceUpdateUseCase.applyToPriceRange(priceListId, min, max, amount, reason, false);
            }
            case GROUP_FAVORITES -> priceUpdateUseCase.applyToFavorites(priceListId, amount, reason, false);
            default -> throw new IllegalStateException("Agrupación no reconocida: " + grouping);
        };
    }

    /** Aplica redondeo según la agrupación seleccionada. */
    private int applyByGroupingRounding(int priceListId, String grouping, double target, String reason)
            throws Exception {
        return switch (grouping) {
            case GROUP_ALL -> priceUpdateUseCase.applyRoundingToAll(priceListId, target, reason);
            case GROUP_CATEGORY -> {
                Integer catId = getCategoryIdFromPanel();
                yield catId != null
                        ? priceUpdateUseCase.applyRoundingToCategory(priceListId, catId, target, reason)
                        : priceUpdateUseCase.applyRoundingToAll(priceListId, target, reason);
            }
            case GROUP_TOP -> {
                int topN = parseIntField(txtTopN, "Número de artículos Top");
                int topDays = parseIntField(txtTopDays, "Días de análisis");
                yield priceUpdateUseCase.applyRoundingToTopSellers(priceListId, topN, topDays, target, reason);
            }
            case GROUP_BOTTOM -> {
                int bottomN = parseIntField(txtBottomN, "Número de artículos");
                int bottomDays = parseIntField(txtBottomDays, "Días de análisis");
                yield priceUpdateUseCase.applyRoundingToBottomSellers(priceListId, bottomN, bottomDays, target,
                        reason);
            }
            case GROUP_SLOW -> {
                int slowDays = parseIntField(txtSlowDays, "Días sin venta");
                yield priceUpdateUseCase.applyRoundingToSlowMovers(priceListId, slowDays, target, reason);
            }
            case GROUP_RANGE -> {
                double min = parseDoubleField(txtMinPrice, "Precio mínimo");
                double max = parseDoubleField(txtMaxPrice, "Precio máximo");
                yield priceUpdateUseCase.applyRoundingToPriceRange(priceListId, min, max, target, reason);
            }
            case GROUP_FAVORITES -> priceUpdateUseCase.applyRoundingToFavorites(priceListId, target, reason);
            default -> throw new UnsupportedOperationException(
                    "Redondeo no soportado para agrupación: " + grouping);
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
    void handleAddTaxRate(ActionEvent event) {
        TaxRate newRate = new TaxRate();
        newRate.setActive(true);
        newRate.setCountry("ES");
        showTaxRateDialog(newRate).ifPresent(rate -> {
            try {
                taxManagementUseCase.saveTaxRate(rate);
                loadTaxCatalogData();
                showInfo("Tasa impositiva guardada con éxito.");
            } catch (SQLException e) {
                showError("Error al guardar tasa: " + e.getMessage());
            }
        });
    }

    private void handleEditTaxRate(TaxRate rate) {
        showTaxRateDialog(rate).ifPresent(updated -> {
            try {
                taxManagementUseCase.updateTaxRate(updated);
                loadTaxCatalogData();
                showInfo("Tasa impositiva actualizada.");
            } catch (SQLException e) {
                showError("Error al actualizar tasa: " + e.getMessage());
            }
        });
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
        TaxGroup newGroup = new TaxGroup();
        newGroup.setRates(new ArrayList<>());
        showTaxGroupDialog(newGroup).ifPresent(group -> {
            try {
                taxManagementUseCase.saveTaxGroup(group);
                loadTaxCatalogData();
                loadInitialData(); // Refresh combos
                showInfo("Grupo de impuestos creado con éxito.");
            } catch (SQLException e) {
                showError("Error al crear grupo: " + e.getMessage());
            }
        });
    }

    private void handleEditTaxGroup(TaxGroup group) {
        showTaxGroupDialog(group).ifPresent(updated -> {
            try {
                taxManagementUseCase.updateTaxGroup(updated);
                loadTaxCatalogData();
                loadInitialData(); // Refresh combos
                showInfo("Grupo de impuestos actualizado.");
            } catch (SQLException e) {
                showError("Error al actualizar grupo: " + e.getMessage());
            }
        });
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

    private Optional<TaxRate> showTaxRateDialog(TaxRate rate) {
        Dialog<TaxRate> dialog = new Dialog<>();
        dialog.setTitle(rate.getTaxRateId() == 0 ? "Nueva Tasa Impositiva" : "Editar Tasa Impositiva");
        dialog.setHeaderText("Configure los detalles de la tasa impositiva.");

        ButtonType saveButtonType = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField name = new TextField(rate.getName());
        name.setPromptText("Nombre (ej: IVA 21%)");
        TextField value = new TextField(String.valueOf(rate.getRate()));
        value.setPromptText("Tasa (ej: 21.0)");
        TextField country = new TextField(rate.getCountry());
        country.setPromptText("País (ej: ES)");
        CheckBox active = new CheckBox("Activa");
        active.setSelected(rate.isActive());

        DatePicker startDate = new DatePicker();
        if (rate.getValidFrom() != null) {
            startDate.setValue(rate.getValidFrom().toLocalDate());
        } else {
            startDate.setValue(java.time.LocalDate.now());
        }

        grid.add(new Label("Nombre:"), 0, 0);
        grid.add(name, 1, 0);
        grid.add(new Label("Tasa (%):"), 0, 1);
        grid.add(value, 1, 1);
        grid.add(new Label("País (ISO):"), 0, 2);
        grid.add(country, 1, 2);
        grid.add(new Label("Fecha Inicio:"), 0, 3);
        grid.add(startDate, 1, 3);
        grid.add(active, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    rate.setName(name.getText());
                    rate.setRate(Double.parseDouble(value.getText().replace(",", ".")));
                    rate.setCountry(country.getText());
                    rate.setActive(active.isSelected());

                    if (startDate.getValue() != null) {
                        rate.setValidFrom(startDate.getValue().atStartOfDay());
                    }
                    return rate;
                } catch (NumberFormatException e) {
                    AlertUtil.showWarning("Valor Inválido", "Por favor, introduce una tasa numérica válida.");
                    return null;
                }
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private Optional<TaxGroup> showTaxGroupDialog(TaxGroup group) {
        Dialog<TaxGroup> dialog = new Dialog<>();
        dialog.setTitle(group.getTaxGroupId() == 0 ? "Nuevo Grupo de Impuestos" : "Editar Grupo de Impuestos");
        dialog.setHeaderText("Configure el grupo y seleccione las tasas que lo componen.");

        ButtonType saveButtonType = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        VBox content = new VBox(15);
        content.setPadding(new javafx.geometry.Insets(20));

        TextField name = new TextField(group.getName());
        name.setPromptText("Nombre del grupo (ej: IVA General)");
        CheckBox isDefault = new CheckBox("Grupo por defecto para nuevos productos");
        isDefault.setSelected(group.isDefault());

        Label lblRates = new Label("Seleccionar tasas incluidas:");
        lblRates.setStyle("-fx-font-weight: bold;");

        ListView<TaxRate> ratesListView = new ListView<>();
        try {
            List<TaxRate> allRates = taxManagementUseCase.getActiveTaxRates();
            ratesListView.setItems(FXCollections.observableArrayList(allRates));
            ratesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            // Pre-seleccionar las tasas actuales
            if (group.getRates() != null) {
                for (TaxRate r : group.getRates()) {
                    allRates.stream()
                            .filter(ar -> ar.getTaxRateId() == r.getTaxRateId())
                            .findFirst()
                            .ifPresent(ar -> ratesListView.getSelectionModel().select(ar));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        ratesListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TaxRate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.getName() + " (" + item.getRate() + "%)");
            }
        });

        content.getChildren().addAll(new Label("Nombre:"), name, isDefault, lblRates, ratesListView);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(400);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                group.setName(name.getText());
                group.setDefault(isDefault.isSelected());
                group.setRates(new ArrayList<>(ratesListView.getSelectionModel().getSelectedItems()));
                return group;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    @FXML
    void handleRefreshHistory(ActionEvent event) {
        refreshHistory();
    }

    private void refreshHistory() {
        try {
            // List<TaxRevision> history = vatUseCase.getGlobalHistory();
            // historyTable.setItems(FXCollections.observableArrayList(history));
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
