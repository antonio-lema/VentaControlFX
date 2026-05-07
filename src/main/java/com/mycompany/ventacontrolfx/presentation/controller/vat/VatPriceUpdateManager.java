package com.mycompany.ventacontrolfx.presentation.controller.vat;

import com.mycompany.ventacontrolfx.application.service.PriceUpdateService;
import com.mycompany.ventacontrolfx.domain.exception.BusinessException;
import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.model.VisibilityFilter;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.shared.async.AsyncManager;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestiona la pestaña de Subida Masiva de Precios del VatManagementController.
 */
public class VatPriceUpdateManager {

    private final ServiceContainer container;
    private final PriceUpdateService priceUpdateService;
    private final AsyncManager asyncManager;
    private final IProductRepository productRepository;

    // UI References
    private final ComboBox<String> cmbGroupingType;
    private final ComboBox<String> cmbPriceUpdateType;
    private final TextField txtPriceValue;
    private final TextField txtPriceReason;
    private final DatePicker dpPriceStartDate;
    private final ComboBox<PriceList> cmbPriceListUpdate;
    private final Label lblPriceValue;

    // Dynamic Panels
    private final HBox panelCategory;
    private final ComboBox<Category> cmbPriceCategory;
    private final HBox panelTopSellers;
    private final TextField txtTopN, txtTopDays;
    private final HBox panelBottomSellers;
    private final TextField txtBottomN, txtBottomDays;
    private final HBox panelSlowMovers;
    private final TextField txtSlowDays;
    private final HBox panelPriceRange;
    private final TextField txtMinPrice, txtMaxPrice;
    private final VBox panelProducts;
    private final TextField txtProductSearch;
    private final ListView<Product> listSelectedProducts;
    private final Label lblSelectedCount;
    private final HBox panelFavorites;
    private final HBox panelClone;
    private final ComboBox<PriceList> cmbSourcePriceList;

    private final ObservableList<Product> selectedProducts = FXCollections.observableArrayList();

    // Constants (Sync with VatManagementController)
    private static final String GROUP_CATEGORY = "vat.group.category";
    private static final String GROUP_TOP = "vat.group.top";
    private static final String GROUP_BOTTOM = "vat.group.bottom";
    private static final String GROUP_SLOW = "vat.group.slow";
    private static final String GROUP_RANGE = "vat.group.range";
    private static final String GROUP_FAVORITES = "vat.group.favorites";
    private static final String GROUP_PRODUCTS = "vat.group.products";
    private static final String GROUP_ALL = "vat.group.all";
    private static final String GROUP_CLONE = "vat.group.clone";

    private static final String OP_PERCENTAGE = "vat.update.type.percentage";
    private static final String OP_FIXED = "vat.update.type.fixed";
    private static final String OP_ROUNDING = "vat.update.type.rounding_full";

    public VatPriceUpdateManager(ServiceContainer container, PriceUpdateService priceUpdateService, AsyncManager asyncManager, IProductRepository productRepository, ComboBox<String> cmbGroupingType, ComboBox<String> cmbPriceUpdateType, TextField txtPriceValue, TextField txtPriceReason, DatePicker dpPriceStartDate, ComboBox<PriceList> cmbPriceListUpdate, Label lblPriceValue, HBox panelCategory, ComboBox<Category> cmbPriceCategory, HBox panelTopSellers, TextField txtTopN, TextField txtTopDays, HBox panelBottomSellers, TextField txtBottomN, TextField txtBottomDays, HBox panelSlowMovers, TextField txtSlowDays, HBox panelPriceRange, TextField txtMinPrice, TextField txtMaxPrice, VBox panelProducts, TextField txtProductSearch, ListView<Product> listSelectedProducts, Label lblSelectedCount, HBox panelFavorites, HBox panelClone, ComboBox<PriceList> cmbSourcePriceList) {
        this.container = container;
        this.priceUpdateService = priceUpdateService;
        this.asyncManager = asyncManager;
        this.productRepository = productRepository;
        this.cmbGroupingType = cmbGroupingType;
        this.cmbPriceUpdateType = cmbPriceUpdateType;
        this.txtPriceValue = txtPriceValue;
        this.txtPriceReason = txtPriceReason;
        this.dpPriceStartDate = dpPriceStartDate;
        this.cmbPriceListUpdate = cmbPriceListUpdate;
        this.lblPriceValue = lblPriceValue;
        this.panelCategory = panelCategory;
        this.cmbPriceCategory = cmbPriceCategory;
        this.panelTopSellers = panelTopSellers;
        this.txtTopN = txtTopN;
        this.txtTopDays = txtTopDays;
        this.panelBottomSellers = panelBottomSellers;
        this.txtBottomN = txtBottomN;
        this.txtBottomDays = txtBottomDays;
        this.panelSlowMovers = panelSlowMovers;
        this.txtSlowDays = txtSlowDays;
        this.panelPriceRange = panelPriceRange;
        this.txtMinPrice = txtMinPrice;
        this.txtMaxPrice = txtMaxPrice;
        this.panelProducts = panelProducts;
        this.txtProductSearch = txtProductSearch;
        this.listSelectedProducts = listSelectedProducts;
        this.lblSelectedCount = lblSelectedCount;
        this.panelFavorites = panelFavorites;
        this.panelClone = panelClone;
        this.cmbSourcePriceList = cmbSourcePriceList;
    }

    public void init() {
        setupGroupingSelector();
        setupOperationTypeSelector();
    }

    private void setupGroupingSelector() {
        cmbGroupingType.setItems(FXCollections.observableArrayList(
                GROUP_CATEGORY, GROUP_ALL, GROUP_PRODUCTS, GROUP_CLONE,
                GROUP_TOP, GROUP_BOTTOM, GROUP_SLOW, GROUP_RANGE, GROUP_FAVORITES));

        cmbGroupingType.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(String object) {
                return (object != null && container != null) ? container.getBundle().getString(object) : object;
            }
            @Override public String fromString(String string) { return null; }
        });

        cmbGroupingType.getSelectionModel().selectFirst();
        cmbGroupingType.getSelectionModel().selectedItemProperty().addListener((obs, old, nv) -> {
            if (nv == null) return;
            setPanel(panelCategory, GROUP_CATEGORY.equals(nv) || GROUP_ALL.equals(nv));
            setPanel(panelTopSellers, GROUP_TOP.equals(nv));
            setPanel(panelBottomSellers, GROUP_BOTTOM.equals(nv));
            setPanel(panelSlowMovers, GROUP_SLOW.equals(nv));
            setPanel(panelPriceRange, GROUP_RANGE.equals(nv));
            setPanel(panelFavorites, GROUP_FAVORITES.equals(nv));
            setPanel(panelClone, GROUP_CLONE.equals(nv));
            setPanel(panelProducts, GROUP_PRODUCTS.equals(nv));
            if (GROUP_PRODUCTS.equals(nv)) setupProductSearch();
        });
    }

    private void setPanel(Region panel, boolean visible) {
        if (panel != null) {
            panel.setVisible(visible);
            panel.setManaged(visible);
        }
    }

    private void setupOperationTypeSelector() {
        cmbPriceUpdateType.setItems(FXCollections.observableArrayList(OP_PERCENTAGE, OP_FIXED, OP_ROUNDING));
        cmbPriceUpdateType.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(String object) {
                return (object != null && container != null) ? container.getBundle().getString(object) : object;
            }
            @Override public String fromString(String string) { return null; }
        });
        cmbPriceUpdateType.getSelectionModel().selectFirst();
        cmbPriceUpdateType.getSelectionModel().selectedItemProperty().addListener((obs, old, nv) -> {
            if (lblPriceValue == null || nv == null) return;
            switch (nv) {
                case OP_PERCENTAGE -> {
                    lblPriceValue.setText(container.getBundle().getString("vat.update.type.percentage") + ":");
                    txtPriceValue.setPromptText("Ej: 10 (negativo para bajar)");
                }
                case OP_FIXED -> {
                    lblPriceValue.setText(container.getBundle().getString("vat.update.type.fixed") + ":");
                    txtPriceValue.setPromptText("Ej: 1.00 (negativo para bajar)");
                }
                case OP_ROUNDING -> {
                    lblPriceValue.setText("Decimal objetivo:");
                    txtPriceValue.setPromptText("Ej: 0.99 o 0.50 o 0.00");
                }
            }
        });
    }

    private void setupProductSearch() {
        listSelectedProducts.setItems(selectedProducts);
        listSelectedProducts.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); }
                else {
                    String sku = item.getSku();
                    setText(item.getName() + (sku != null && !sku.trim().isEmpty() ? " (" + sku + ")" : ""));
                    Button btnRemove = new Button();
                    btnRemove.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
                    btnRemove.getStyleClass().add("estetica-btn-icon-danger");
                    btnRemove.setOnAction(e -> { selectedProducts.remove(item); updateSelectedCountText(); });
                    setGraphic(btnRemove);
                    setContentDisplay(ContentDisplay.RIGHT);
                }
            }
        });

        txtProductSearch.setOnAction(e -> {
            String query = txtProductSearch.getText().trim();
            if (query.isEmpty()) return;
            asyncManager.runAsyncTask(() -> {
                try { return productRepository.searchPaginated(query, 1, 0, -1, VisibilityFilter.VISIBLE); }
                catch (SQLException ex) { return null; }
            }, (res) -> {
                List<Product> foundList = (List<Product>) res;
                if (foundList != null && !foundList.isEmpty()) {
                    Product found = foundList.get(0);
                    if (!selectedProducts.contains(found)) {
                        selectedProducts.add(found);
                        txtProductSearch.clear();
                        updateSelectedCountText();
                    }
                } else {
                    AlertUtil.showWarning("No encontrado", "No se encontró ningún producto con ese nombre o SKU.");
                }
            }, null);
        });
    }

    private void updateSelectedCountText() {
        if (lblSelectedCount != null) lblSelectedCount.setText(selectedProducts.size() + " artículos seleccionados");
    }

    public void handleMassivePriceUpdate(Runnable onFinished) {
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
            AlertUtil.showWarning(container.getBundle().getString("alert.warning"), "Selecciona el tipo de agrupación.");
            return;
        }
        if (valueStr == null || valueStr.isBlank()) {
            AlertUtil.showWarning(container.getBundle().getString("alert.warning"), "Introduce el valor del ajuste.");
            return;
        }
        if (reason == null || reason.isBlank()) {
            AlertUtil.showWarning(container.getBundle().getString("alert.warning"), "El motivo es obligatorio para la auditoría.");
            return;
        }

        try {
            double value = Double.parseDouble(valueStr.replace(",", "."));
            PriceList targetPriceList = cmbPriceListUpdate.getSelectionModel().getSelectedItem();
            if (targetPriceList == null) { 
                AlertUtil.showWarning(container.getBundle().getString("alert.warning"), "Selecciona la tarifa de destino."); 
                return; 
            }

            LocalDateTime startDate = (dpPriceStartDate.getValue() != null) ? dpPriceStartDate.getValue().atStartOfDay() : LocalDateTime.now();

            Object extra = switch (grouping) {
                case GROUP_CATEGORY -> getCategoryIdFromPanel();
                case GROUP_PRODUCTS -> selectedProducts.stream().map(Product::getId).collect(Collectors.toList());
                case GROUP_TOP -> new int[] { parseIntField(txtTopN, "Número Top"), parseIntField(txtTopDays, "Días Top") };
                case GROUP_BOTTOM -> new int[] { parseIntField(txtBottomN, "Número Bottom"), parseIntField(txtBottomDays, "Días Bottom") };
                case GROUP_SLOW -> parseIntField(txtSlowDays, "Días Slow");
                case GROUP_RANGE -> new double[] { parseDoubleField(txtMinPrice, "Min"), parseDoubleField(txtMaxPrice, "Max") };
                case GROUP_CLONE -> cmbSourcePriceList.getSelectionModel().getSelectedItem();
                default -> null;
            };

            PriceUpdateService.Request req = new PriceUpdateService.Request();
            req.priceListId = targetPriceList.getId();
            req.value = value;
            req.reason = reason;
            req.startDate = startDate;
            req.operation = switch (opType) {
                case OP_PERCENTAGE -> PriceUpdateService.Operation.PERCENTAGE;
                case OP_FIXED -> PriceUpdateService.Operation.FIXED;
                case OP_ROUNDING -> PriceUpdateService.Operation.ROUNDING;
                default -> throw new BusinessException("Tipo de operación no reconocido.");
            };
            req.grouping = switch (grouping) {
                case GROUP_ALL -> PriceUpdateService.Grouping.ALL;
                case GROUP_CATEGORY -> PriceUpdateService.Grouping.CATEGORY;
                case GROUP_PRODUCTS -> PriceUpdateService.Grouping.PRODUCTS;
                case GROUP_CLONE -> PriceUpdateService.Grouping.CLONE;
                case GROUP_TOP -> PriceUpdateService.Grouping.TOP;
                case GROUP_BOTTOM -> PriceUpdateService.Grouping.BOTTOM;
                case GROUP_SLOW -> PriceUpdateService.Grouping.SLOW;
                case GROUP_RANGE -> PriceUpdateService.Grouping.RANGE;
                case GROUP_FAVORITES -> PriceUpdateService.Grouping.FAVORITES;
                default -> throw new BusinessException("Agrupación no reconocida.");
            };
            req.extra = extra;

            asyncManager.runAsyncTask(() -> priceUpdateService.execute(req), (res) -> {
                int count = (int) res;
                AlertUtil.showInfo(container.getBundle().getString("alert.success"),
                        String.format(container.getBundle().getString("vat.price.update.success"), (count == -1 ? "(Clonado)" : count), container.getBundle().getString(grouping)));
                clearFields();
                if (onFinished != null) onFinished.run();
            }, (err) -> AlertUtil.showError(container.getBundle().getString("alert.error"), "Error al aplicar actualización: " + err.getMessage()));

        } catch (NumberFormatException e) {
             AlertUtil.showWarning(container.getBundle().getString("alert.warning"), "El valor introducido no es un número válido.");
        } catch (Exception e) { 
            AlertUtil.showError(container.getBundle().getString("alert.error"), "Error en la preparación de la subida: " + e.getMessage()); 
        }
    }

    private Integer getCategoryIdFromPanel() {
        Category selected = cmbPriceCategory != null ? cmbPriceCategory.getSelectionModel().getSelectedItem() : null;
        return (selected != null && selected.getId() > 0) ? selected.getId() : null;
    }

    private void clearFields() {
        txtPriceValue.clear();
        txtPriceReason.clear();
        if (txtTopN != null) txtTopN.clear();
        if (txtTopDays != null) txtTopDays.clear();
        if (txtBottomN != null) txtBottomN.clear();
        if (txtBottomDays != null) txtBottomDays.clear();
        if (txtSlowDays != null) txtSlowDays.clear();
        if (txtMinPrice != null) txtMinPrice.clear();
        if (txtMaxPrice != null) txtMaxPrice.clear();
    }

    private int parseIntField(TextField field, String fieldName) {
        if (field == null || field.getText().isBlank())
            throw new IllegalArgumentException(
                    String.format(container.getBundle().getString("common.field_required"), fieldName));
        try {
            int v = Integer.parseInt(field.getText().trim());
            if (v <= 0) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format(container.getBundle().getString("common.field_positive_int"), fieldName));
        }
    }

    private double parseDoubleField(TextField field, String fieldName) {
        if (field == null || field.getText().isBlank())
            throw new IllegalArgumentException(
                    String.format(container.getBundle().getString("common.field_required"), fieldName));
        try {
            return Double.parseDouble(field.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    String.format(container.getBundle().getString("common.field_invalid_num"), fieldName));
        }
    }
}

