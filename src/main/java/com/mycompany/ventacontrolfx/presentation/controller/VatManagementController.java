package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.PriceListUseCase;
import com.mycompany.ventacontrolfx.application.usecase.ScheduleVatChangeUseCase;
import com.mycompany.ventacontrolfx.application.usecase.MassivePriceUpdateUseCase;
import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.domain.model.PriceUpdateLog;
import com.mycompany.ventacontrolfx.domain.model.TaxRevision;
import com.mycompany.ventacontrolfx.domain.repository.ICategoryRepository;
import com.mycompany.ventacontrolfx.domain.repository.IPriceRepository;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import com.mycompany.ventacontrolfx.domain.repository.ITaxRepository;
import com.mycompany.ventacontrolfx.domain.repository.IPriceUpdateLogRepository;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

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

    // ── IVA CONFIG ────────────────────────────────────────────────────────────
    @FXML
    private Label lblCurrentGlobalVat;
    @FXML
    private TextField txtGlobalRate;
    @FXML
    private TextField txtGlobalReason;
    @FXML
    private ComboBox<Category> cmbCategory;
    @FXML
    private TextField txtCategoryRate;
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
    }

    private void postInject() {
        if (container != null) {
            this.priceListUseCase = container.getPriceListUseCase();
            this.vatUseCase = container.getVatUseCase();
            this.priceUpdateUseCase = container.getMassivePriceUpdateUseCase();
            this.categoryRepository = container.getCategoryUseCase().getRepository(); // Assuming getter exists or
                                                                                      // getting from container
            this.priceLogRepository = container.getPriceLogRepository();

            loadInitialData();
            setupHistoryFilter();
        }
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
                cell -> new SimpleStringProperty(String.format("%.2f%%", cell.getValue().getRate())));
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
            Optional<TaxRevision> global = vatUseCase.getCurrentGlobalRate();
            global.ifPresent(r -> lblCurrentGlobalVat.setText(String.format("%.2f%%", r.getRate())));

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
    void handleUpdateGlobalVat(ActionEvent event) {
        if (container != null && !container.getUserSession().hasPermission("admin.iva")) {
            AlertUtil.showError("Acceso Denegado", "No tiene permiso para modificar el IVA global.");
            return;
        }
        try {
            double rate = Double.parseDouble(txtGlobalRate.getText());
            String reason = txtGlobalReason.getText();
            vatUseCase.applyGlobalRateChange(rate, null, reason);
            showInfo("Tasa global actualizada con éxito.");
            txtGlobalRate.clear();
            txtGlobalReason.clear();
            loadInitialData();
            refreshHistory();
        } catch (NumberFormatException e) {
            showWarning("Por favor ingresa una tasa válida.");
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
        }
    }

    @FXML
    void handleUpdateCategoryVat(ActionEvent event) {
        if (container != null && !container.getUserSession().hasPermission("admin.iva")) {
            AlertUtil.showError("Acceso Denegado", "No tiene permiso para modificar el IVA por categoría.");
            return;
        }
        Category selected = cmbCategory.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Selecciona una categoría.");
            return;
        }
        try {
            double rate = Double.parseDouble(txtCategoryRate.getText());
            String reason = txtCategoryReason.getText();
            vatUseCase.applyCategoryRateChange(selected.getId(), rate, null, reason);
            showInfo("IVA de categoría actualizado con éxito.");
            txtCategoryRate.clear();
            txtCategoryReason.clear();
            refreshHistory();
        } catch (NumberFormatException e) {
            showWarning("Por favor ingresa una tasa válida.");
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
    void handleRefreshHistory(ActionEvent event) {
        refreshHistory();
    }

    private void refreshHistory() {
        try {
            List<TaxRevision> history = vatUseCase.getGlobalHistory();
            historyTable.setItems(FXCollections.observableArrayList(history));
        } catch (SQLException e) {
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
