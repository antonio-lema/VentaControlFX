package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.PriceListUseCase;
import com.mycompany.ventacontrolfx.domain.model.VisibilityFilter;
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
import com.mycompany.ventacontrolfx.shared.async.AsyncManager;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.DateFilterUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.application.service.PriceUpdateService;

/**
 * Controlador de la pantalla de Gesti\u00f3n de IVA y Subida Masiva de Precios.
 *
 * Soporta 5 tipos de agrupaci\u00f3n para la actualizaci\u00f3n masiva:
 * 1. Por Categor\u00eda (o todas)
 * 2. Top Vendidos (Top N en \u00faltimos N d\u00edas)
 * 3. Sin Movimiento / Slow-movers
 * 4. Rango de Precio
 * 5. Favoritos
 */
public class VatManagementController implements Injectable {
    @FXML
    private TabPane mainTabPane;
    @FXML
    private Tab tabCurrentConfig, tabMassUpdate, tabCatalog, tabHistory;

    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        postInject();
    }

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac TAX GROUP CONFIG (V2.0)
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
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

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac SUBIDA MASIVA: selectores principales
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u2500
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

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac SUBIDA MASIVA: sub-paneles
    // din\u00e1micos
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
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

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac HISTORIAL IVA
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u2500
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

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac QUICK DATE FILTERS (V2.0)
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
    @FXML
    private HBox quickFilterContainer;
    @FXML
    private ToggleButton tglHistHoy, tglHist7d, tglHist1m, tglHistTodo;
    private ToggleGroup histToggleGroup;
    private Integer histFilterDays = null;

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac IVA POR PRODUCTO
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u2500
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

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac MANTENIMIENTO: TAX RATES
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
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

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac MANTENIMIENTO: TAX GROUPS
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u2500
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

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac HISTORIAL ACTUALIZACIONES DE PRECIO
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
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

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac Servicios
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u2500
    private ICategoryRepository categoryRepository;
    private ITaxRepository taxRepository;
    private IProductRepository productRepository;
    private PriceListUseCase priceListUseCase;
    private IPriceUpdateLogRepository priceLogRepository;
    private TaxManagementUseCase taxManagementUseCase;
    private AsyncManager asyncManager;
    private PriceUpdateService priceUpdateService;

    private ObservableList<com.mycompany.ventacontrolfx.domain.model.Product> selectedProducts = FXCollections
            .observableArrayList();

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac Constantes de agrupaci\u00f3n
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
    private static final String GROUP_CATEGORY = "vat.group.category";
    private static final String GROUP_TOP = "vat.group.top";

    private static class InitialData {
        Optional<TaxGroup> defaultGroup;
        List<TaxGroup> taxGroups;
        List<Category> categories;
        List<PriceList> priceLists;
    }

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

    @FXML
    public void initialize() {
        setupTaxHistoryTable();
        setupPriceLogTable();
        setupGroupingSelector();
        setupOperationTypeSelector();
        setupTaxCatalogTables();
        setupVatProductCard();
        setupHistToggleGroup();
    }

    private void setupHistToggleGroup() {
        if (tglHistHoy == null)
            return;
        histToggleGroup = new ToggleGroup();
        tglHistHoy.setToggleGroup(histToggleGroup);
        tglHist7d.setToggleGroup(histToggleGroup);
        tglHist1m.setToggleGroup(histToggleGroup);
        tglHistTodo.setToggleGroup(histToggleGroup);

        histToggleGroup.selectedToggleProperty().addListener((obs, old, nv) -> {
            if (nv == null && old != null) {
                old.setSelected(true);
            }
        });
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
            String query = txtVatProductSearch.getText().trim();
            if (query.isEmpty())
                return;

            asyncManager.runAsyncTask(() -> {
                try {
                    return productRepository.searchPaginated(query, 1, 0, -1, VisibilityFilter.VISIBLE);
                } catch (SQLException ex) {
                    return null;
                }
            }, (res) -> {
                List<com.mycompany.ventacontrolfx.domain.model.Product> foundList = (List<com.mycompany.ventacontrolfx.domain.model.Product>) res;
                if (foundList != null && !foundList.isEmpty()) {
                    com.mycompany.ventacontrolfx.domain.model.Product found = foundList.get(0);
                    if (!vatSelectedProducts.contains(found)) {
                        vatSelectedProducts.add(found);
                        txtVatProductSearch.clear();
                    }
                } else {
                    AlertUtil.showWarning("No encontrado",
                            "No se encontr\u00f3 ning\u00fan producto con ese nombre o SKU.");
                }
            }, null);
        });
    }

    private void postInject() {
        if (container != null) {
            // Permission check for Tabs
            if (!container.getUserSession().hasPermission("admin.precios_masivo")) {
                mainTabPane.getTabs().remove(tabMassUpdate);
            }
            if (!container.getUserSession().hasPermission("admin.iva")) {
                mainTabPane.getTabs().remove(tabCatalog);
                mainTabPane.getTabs().remove(tabCurrentConfig);
            }
            if (!container.getUserSession().hasPermission("admin.precios_historial") && !container.getUserSession().hasPermission("admin.iva")) {
                 mainTabPane.getTabs().remove(tabHistory);
            }

            this.priceListUseCase = container.getPriceListUseCase();
            this.categoryRepository = container.getCategoryUseCase().getRepository();
            this.taxRepository = container.getTaxRepository();
            this.priceLogRepository = container.getPriceLogRepository();
            this.taxManagementUseCase = container.getTaxManagementUseCase();
            this.productRepository = container.getProductRepository();
            this.asyncManager = container.getAsyncManager();
            this.priceUpdateService = container.getPriceUpdateService();

            setupTaxGroupComboBoxes();
            fetchInitialDataAsync(() -> {
                setupHistoryFilter();
                loadTaxCatalogData();
            });
            // Cargar cat\u00e1logo de impuestos inicial
            loadTaxCatalogData();
        }
    }

    private void fetchInitialDataAsync(Runnable onComplete) {
        asyncManager.runAsyncTask(() -> {
            InitialData data = new InitialData();
            try {
                data.defaultGroup = taxRepository.getDefaultTaxGroup();
                data.taxGroups = taxRepository.getAllTaxGroups();
                data.categories = categoryRepository.getAll();
                data.priceLists = priceListUseCase.getAll();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return data;
        }, (data) -> {
            if (data != null) {
                updateUIWithInitialData(data);
                cmbProductTaxGroup.setItems(cmbGlobalTaxGroup.getItems());
                cmbProductTaxGroup.setConverter(cmbGlobalTaxGroup.getConverter());
                setupVatProductExplorer(data.categories);
                if (onComplete != null)
                    onComplete.run();
            }
        }, null);
    }

    private void setupTaxGroupComboBoxes() {
        // Configurar c\u00f3mo se muestran los grupos en los combos
        javafx.util.StringConverter<TaxGroup> converter = new javafx.util.StringConverter<>() {
            @Override
            public String toString(TaxGroup group) {
                return group == null ? "\u2014" : group.getName();
            }

            @Override
            public TaxGroup fromString(String string) {
                return null;
            }
        };
        cmbGlobalTaxGroup.setConverter(converter);
        cmbCategoryTaxGroup.setConverter(converter);
    }

    // \u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090
    // SETUP
    // \u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090

    private void setupVatProductExplorer(List<Category> categories) {
        if (btnVatProductExplorer == null)
            return;
        btnVatProductExplorer.setVisible(true);
        btnVatProductExplorer.setManaged(true);
        btnVatProductExplorer.getItems().clear();

        if (categories == null)
            return;

        for (Category cat : categories) {
            Menu catMenu = new Menu(cat.getName());
            // Placeholder para forzar que el men\u00fa sea expandible
            catMenu.getItems().add(new MenuItem("Cargando..."));

            catMenu.setOnShowing(e -> {
                // Solo cargar si no se ha cargado ya (evitar recargas innecesarias)
                if (catMenu.getItems().size() > 1 || !catMenu.getItems().get(0).getText().equals("Cargando...")) {
                    return;
                }

                asyncManager.runAsyncTask(() -> {
                    try {
                        return productRepository.getByCategory(cat.getId(), -1);
                    } catch (SQLException ex) {
                        return null;
                    }
                }, (res) -> {
                    catMenu.getItems().clear();
                    @SuppressWarnings("unchecked")
                    List<com.mycompany.ventacontrolfx.domain.model.Product> products = (List<com.mycompany.ventacontrolfx.domain.model.Product>) res;

                    // Opci\u00f3n para seleccionar TODOS
                    Label lblSelectAll = new Label("   --- Seleccionar TODOS ---   ");
                    lblSelectAll.getStyleClass().add("menu-item-bold");
                    lblSelectAll.setStyle("-fx-text-fill: -color-primary; -fx-cursor: hand;");

                    CustomMenuItem selectAll = new CustomMenuItem(lblSelectAll, false);
                    selectAll.setHideOnClick(false);

                    selectAll.setOnAction(ev -> {
                        if (products != null) {
                            boolean allCurrentlySelected = products.stream()
                                    .allMatch(p -> vatSelectedProducts.contains(p));

                            if (allCurrentlySelected) {
                                vatSelectedProducts.removeAll(products);
                            } else {
                                for (com.mycompany.ventacontrolfx.domain.model.Product p : products) {
                                    if (!vatSelectedProducts.contains(p)) {
                                        vatSelectedProducts.add(p);
                                    }
                                }
                            }
                            // Forzar actualizaci\u00f3n de los checkboxes visibles en este men\u00fa
                            catMenu.getItems().forEach(item -> {
                                if (item instanceof CustomMenuItem cmi && cmi.getContent() instanceof CheckBox cb) {
                                    // El listener del checkbox ya se encarga de la lista, solo actualizamos UI
                                    Object pObj = cmi.getUserData();
                                    if (pObj instanceof com.mycompany.ventacontrolfx.domain.model.Product p) {
                                        cb.setSelected(vatSelectedProducts.contains(p));
                                    }
                                }
                            });
                        }
                    });
                    catMenu.getItems().add(selectAll);
                    catMenu.getItems().add(new SeparatorMenuItem());

                    if (products != null && !products.isEmpty()) {
                        for (com.mycompany.ventacontrolfx.domain.model.Product p : products) {
                            CheckBox cb = new CheckBox(p.getName());
                            cb.getStyleClass().add("vat-explorer-checkbox");
                            cb.setPrefWidth(250);
                            // Sincronizar estado inicial
                            cb.setSelected(vatSelectedProducts.contains(p));
                            cb.setPrefWidth(250);

                            CustomMenuItem item = new CustomMenuItem(cb, false);
                            item.setUserData(p); // Guardar producto para facilitar actualizaci\u00f3n masiva
                            item.setHideOnClick(false); // Mantener el men\u00fa abierto para selecci\u00f3n
                                                        // m\u00faltiple
                            item.getStyleClass().add("vat-custom-menu-item");

                            cb.selectedProperty().addListener((obs, old, nv) -> {
                                if (nv) {
                                    if (!vatSelectedProducts.contains(p)) {
                                        vatSelectedProducts.add(p);
                                    }
                                } else {
                                    vatSelectedProducts.remove(p);
                                }
                            });
                            catMenu.getItems().add(item);
                        }
                    } else {
                        catMenu.getItems().add(new MenuItem(container.getBundle().getString("vat.group.no_products")));
                    }
                }, null);
            });
            btnVatProductExplorer.getItems().add(catMenu);
        }
    }

    private void setupGroupingSelector() {
        cmbGroupingType.setItems(FXCollections.observableArrayList(
                GROUP_CATEGORY,
                GROUP_ALL,
                GROUP_PRODUCTS,
                GROUP_CLONE,
                GROUP_TOP,
                GROUP_BOTTOM,
                GROUP_SLOW,
                GROUP_RANGE,
                GROUP_FAVORITES));

        cmbGroupingType.setConverter(new javafx.util.StringConverter<String>() {
            @Override
            public String toString(String object) {
                return (object != null && container != null) ? container.getBundle().getString(object) : object;
            }

            @Override
            public String fromString(String string) {
                return null;
            }
        });

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
                    // Bot\u00f3n para eliminar de la selecci\u00f3n
                    Button btnRemove = new Button();
                    btnRemove.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
                    btnRemove.getStyleClass().add("estetica-btn-icon-danger");
                    btnRemove.setOnAction(e -> selectedProducts.remove(item));
                    setGraphic(btnRemove);
                    setContentDisplay(ContentDisplay.RIGHT);
                }
            }
        });

        txtProductSearch.setOnAction(e -> {
            String query = txtProductSearch.getText().trim();
            if (query.isEmpty())
                return;

            asyncManager.runAsyncTask(() -> {
                try {
                    return productRepository.searchPaginated(query, 1, 0, -1, VisibilityFilter.VISIBLE);
                } catch (SQLException ex) {
                    return null;
                }
            }, (res) -> {
                List<com.mycompany.ventacontrolfx.domain.model.Product> foundList = (List<com.mycompany.ventacontrolfx.domain.model.Product>) res;
                if (foundList != null && !foundList.isEmpty()) {
                    com.mycompany.ventacontrolfx.domain.model.Product found = foundList.get(0);
                    if (!selectedProducts.contains(found)) {
                        selectedProducts.add(found);
                        txtProductSearch.clear();
                        updateSelectedCountText();
                    }
                } else {
                    AlertUtil.showWarning("No encontrado",
                            "No se encontr\u00f3 ning\u00fan producto con ese nombre o SKU.");
                }
            }, null);
        });
    }

    private void updateSelectedCountText() {
        lblSelectedCount.setText(selectedProducts.size() + " art\u00edculos seleccionados");
    }

    private void setupOperationTypeSelector() {
        cmbPriceUpdateType.setItems(FXCollections.observableArrayList(
                OP_PERCENTAGE,
                OP_FIXED,
                OP_ROUNDING));

        cmbPriceUpdateType.setConverter(new javafx.util.StringConverter<String>() {
            @Override
            public String toString(String object) {
                return (object != null && container != null) ? container.getBundle().getString(object) : object;
            }

            @Override
            public String fromString(String string) {
                return null;
            }
        });

        cmbPriceUpdateType.getSelectionModel().selectFirst();

        cmbPriceUpdateType.getSelectionModel().selectedItemProperty().addListener((obs, old, nv) -> {
            if (lblPriceValue == null || nv == null)
                return;
            switch (nv) {
                case OP_PERCENTAGE -> {
                    lblPriceValue.setText(container.getBundle().getString("vat.update.type.percentage") + ":");
                    txtPriceValue.setPromptText("Ej: 10  (negativo para bajar)");
                }
                case OP_FIXED -> {
                    lblPriceValue.setText(container.getBundle().getString("vat.update.type.fixed") + ":");
                    txtPriceValue.setPromptText("Ej: 1.00  (negativo para bajar)");
                }
                case OP_ROUNDING -> {
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
                case "percentage" -> container.getBundle().getString("vat.update.type.percentage");
                case "fixed" -> container.getBundle().getString("vat.update.type.fixed");
                case "rounding" -> container.getBundle().getString("vat.update.type.rounding");
                default -> type;
            };
            return new SimpleStringProperty(display);
        });
        colLogScope.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getScope()));
        colLogCategory.setCellValueFactory(cell -> {
            String catName = cell.getValue().getCategoryName();
            return new SimpleStringProperty(catName != null ? catName : "\u2014");
        });
        colLogValue.setCellValueFactory(cell -> {
            String type = cell.getValue().getUpdateType();
            double val = cell.getValue().getValue();
            String display = switch (type) {
                case "percentage" -> String.format("%.2f%%", val);
                case "fixed" -> String.format("%.2f \u20ac", val);
                case "rounding" -> String.format("x.%02.0f", val * 100);
                default -> String.valueOf(val);
            };
            return new SimpleStringProperty(display);
        });
        colLogProducts.setCellValueFactory(
                cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getProductsUpdated())));
        colLogReason.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getReason()));
    }

    private void updateUIWithInitialData(InitialData data) {
        if (data == null)
            return;

        // Cargar Grupo de Impuestos Global actual
        lblCurrentGlobalTaxGroup.setText(
                data.defaultGroup.map(TaxGroup::getName)
                        .orElse(container.getBundle().getString("vat.group.undefined")));

        // Cargar todos los grupos para los combos
        ObservableList<TaxGroup> taxGroupsList = FXCollections.observableArrayList(data.taxGroups);
        cmbGlobalTaxGroup.setItems(taxGroupsList);
        cmbCategoryTaxGroup.setItems(taxGroupsList);

        data.defaultGroup.ifPresent(dg -> cmbGlobalTaxGroup.getSelectionModel().select(dg));

        // Categor\u00edas
        cmbCategory.getItems().clear();
        for (Category cat : data.categories) {
            CheckBox cb = new CheckBox(cat.getName());
            cb.getStyleClass().add("permission-checkbox");
            cb.setMaxWidth(Double.MAX_VALUE);
            cb.setUserData(cat);

            cb.selectedProperty().addListener((obs, old, nv) -> {
                long count = cmbCategory.getItems().stream()
                        .map(m -> ((CustomMenuItem) m).getContent())
                        .filter(n -> n instanceof CheckBox && ((CheckBox) n).isSelected())
                        .count();
                cmbCategory.setText(count == 0 ? container.getBundle().getString("vat.group.choosing")
                        : String.format(container.getBundle().getString("vat.group.selected_count"), count));
            });

            CustomMenuItem item = new CustomMenuItem(cb, false);
            cmbCategory.getItems().add(item);
        }

        Category allOption = new Category(0, container.getBundle().getString("vat.group.all_categories"), true, false,
                0.0);
        ObservableList<Category> priceCategories = FXCollections.observableArrayList();
        priceCategories.add(allOption);
        priceCategories.addAll(data.categories);
        cmbPriceCategory.setItems(priceCategories);
        cmbPriceCategory.getSelectionModel().selectFirst();

        // Cargar listas de precios para el selector de actualizaci\u00f3n masiva
        ObservableList<PriceList> priceListsList = FXCollections.observableArrayList(data.priceLists);
        cmbPriceListUpdate.setItems(priceListsList);
        // Seleccionar la por defecto
        data.priceLists.stream().filter(PriceList::isDefault).findFirst()
                .ifPresent(pl -> cmbPriceListUpdate.getSelectionModel().select(pl));
        if (cmbPriceListUpdate.getSelectionModel().isEmpty() && !data.priceLists.isEmpty()) {
            cmbPriceListUpdate.getSelectionModel().selectFirst();
        }
    }

    private void setupTaxCatalogTables() {
        // --- Tax Rates Table ---
        colRateName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colRateValue.setCellValueFactory(
                cell -> new SimpleStringProperty(String.format("%.2f%%", cell.getValue().getRate())));
        colRateCountry.setCellValueFactory(new PropertyValueFactory<>("country"));
        colRateStatus.setCellValueFactory(
                cell -> new SimpleStringProperty(cell.getValue().isActive()
                        ? container.getBundle().getString("status.active")
                        : container.getBundle().getString("status.inactive")));

        setupTaxRateActionsColumn();

        // --- Tax Groups Table ---
        if (colGroupName != null)
            colGroupName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (colGroupDefault != null)
            colGroupDefault
                    .setCellValueFactory(
                            cell -> new SimpleStringProperty(cell.getValue().isDefault() ? "S\u00cd" : "\u2014"));
        if (colGroupRates != null)
            colGroupRates.setCellValueFactory(cell -> {
                List<TaxRate> rates = cell.getValue().getRates();
                if (rates == null || rates.isEmpty())
                    return new SimpleStringProperty(container.getBundle().getString("vat.group.none"));
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
                System.out.println("LOGGING: Loaded " + rates.size() + " tax rates");
                taxRatesTable.setItems(FXCollections.observableArrayList(rates));

                List<TaxGroup> groups = taxManagementUseCase.getAllTaxGroups();
                System.out.println("LOGGING: Loaded " + groups.size() + " tax groups");
                taxGroupsTable.setItems(FXCollections.observableArrayList(groups));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError(String.format(container.getBundle().getString("vat.group.error.load"), e.getMessage()));
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
        cmbHistoryScope
                .setItems(FXCollections.observableArrayList("Global", "Categor\u00eda (Todo)", "Producto (Todo)"));
        cmbHistoryScope.getSelectionModel().selectFirst();
        cmbHistoryScope.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, nv) -> refreshHistory());
        refreshHistory();
    }

    // \u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090
    // HANDLERS \u2014 IVA
    // \u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090

    @FXML
    void handleUpdateGlobalTaxGroup(ActionEvent event) {
        if (container != null && !container.getUserSession().hasPermission("admin.iva")) {
            AlertUtil.showError(container.getBundle().getString("access.denied"),
                    container.getBundle().getString("vat.group.error.access_denied_cat"));
            return;
        }
        TaxGroup selected = cmbGlobalTaxGroup.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning(container.getBundle().getString("vat.group.error.select_group"));
            return;
        }

        asyncManager.runAsyncTask(() -> {
            taxManagementUseCase.setDefaultTaxGroup(selected.getTaxGroupId());
            return null;
        }, (res) -> {
            showInfo(container.getBundle().getString("vat.group.success.global"));
            lblCurrentGlobalTaxGroup.setText(selected.getName());
            refreshHistory();
        }, (err) -> {
            showError("Error: " + err.getMessage());
        });
    }

    @FXML
    void handleUpdateCategoryTaxGroup(ActionEvent event) {
        if (container != null && !container.getUserSession().hasPermission("admin.iva")) {
            AlertUtil.showError(container.getBundle().getString("access.denied"),
                    container.getBundle().getString("vat.group.error.access_denied_cat"));
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
            showWarning(container.getBundle().getString("vat.group.error.select_cat"));
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
            showInfo(String.format(container.getBundle().getString("vat.group.success.category"), updatedCount));
            refreshHistory();

            // Limpiar selecci\u00f3n
            cmbCategory.getItems().forEach(item -> {
                javafx.scene.Node n = ((CustomMenuItem) item).getContent();
                if (n instanceof CheckBox)
                    ((CheckBox) n).setSelected(false);
            });
            cmbCategory.setText(container.getBundle().getString("vat.group.choosing"));

        } catch (Exception e) {
            showError("Error: " + e.getMessage());
        }
    }

    // \u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090
    // HANDLER \u2014 SUBIDA MASIVA DE PRECIOS
    // \u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090

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
            showWarning("Selecciona el tipo de agrupaci\u00f3n.");
            return;
        }
        if (valueStr == null || valueStr.isBlank()) {
            showWarning("Introduce el valor del ajuste.");
            return;
        }
        if (reason == null || reason.isBlank()) {
            showWarning("El motivo es obligatorio para la auditor\u00eda.");
            return;
        }

        try {
            double value = Double.parseDouble(valueStr.replace(",", "."));

            PriceList targetPriceList = cmbPriceListUpdate.getSelectionModel().getSelectedItem();
            if (targetPriceList == null) {
                showWarning("Selecciona la tarifa de destino.");
                return;
            }
            int priceListId = targetPriceList.getId();

            java.time.LocalDateTime startDate = (dpPriceStartDate.getValue() != null)
                    ? dpPriceStartDate.getValue().atStartOfDay()
                    : java.time.LocalDateTime.now();

            final String finalGrouping = grouping;
            final double finalValue = value;
            final String finalOpType = opType;
            final String finalReason = reason;

            // Extract extra params if needed BEFORE background task
            Object extra = switch (finalGrouping) {
                case GROUP_CATEGORY -> getCategoryIdFromPanel();
                case GROUP_PRODUCTS -> selectedProducts.stream().map(p -> p.getId()).collect(Collectors.toList());
                case GROUP_TOP ->
                    new int[] { parseIntField(txtTopN, "N\u00famero Top"), parseIntField(txtTopDays, "D\u00edas Top") };
                case GROUP_BOTTOM -> new int[] { parseIntField(txtBottomN, "N\u00famero Bottom"),
                        parseIntField(txtBottomDays, "D\u00edas Bottom") };
                case GROUP_SLOW -> parseIntField(txtSlowDays, "D\u00edas Slow");
                case GROUP_RANGE ->
                    new double[] { parseDoubleField(txtMinPrice, "Min"), parseDoubleField(txtMaxPrice, "Max") };
                case GROUP_CLONE -> cmbSourcePriceList.getSelectionModel().getSelectedItem();
                default -> null;
            };

            PriceUpdateService.Request req = new PriceUpdateService.Request();
            req.priceListId = priceListId;
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

            asyncManager.runAsyncTask(() -> {
                return priceUpdateService.execute(req);
            }, (res) -> {
                int count = (int) res;
                AlertUtil.showInfo(container.getBundle().getString("alert.success"),
                        String.format(container.getBundle().getString("vat.price.update.success"),
                                (count == -1 ? "(Clonado)" : count),
                                container.getBundle().getString(finalGrouping)));

                clearPriceFields();
                refreshPriceLog();
            }, (err) -> {
                showError("Error al aplicar actualización: " + err.getMessage());
            });

        } catch (NumberFormatException e) {
            showWarning("El valor introducido no es un n\u00famero v\u00e1lido.");
        } catch (Exception e) {
            showError("Error en la preparaci\u00f3n de la subida: " + e.getMessage());
        }
    }


    /** Devuelve el categoryId seleccionado o null si se eligi\u00f3 "Todas". */
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

    // \u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090
    // HISTORIAL
    // \u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090

    @FXML
    private void handleUpdateProductTaxGroup(ActionEvent event) {
        if (vatSelectedProducts.isEmpty()) {
            AlertUtil.showWarning(container.getBundle().getString("vat.error.no_selection"),
                    container.getBundle().getString("vat.error.select_one"));
            return;
        }

        TaxGroup selectedGroup = cmbProductTaxGroup.getSelectionModel().getSelectedItem();
        if (selectedGroup == null) {
            AlertUtil.showWarning(container.getBundle().getString("vat.error.group_not_selected"),
                    container.getBundle().getString("vat.error.select_new_group"));
            return;
        }

        List<Integer> ids = vatSelectedProducts.stream()
                .map(p -> p.getId())
                .collect(Collectors.toList());

        asyncManager.runAsyncTask(() -> {
            taxManagementUseCase.updateTaxGroupForProducts(ids, selectedGroup.getTaxGroupId(),
                    container.getBundle().getString("vat.log.manual_change"));
            return ids.size();
        }, (res) -> {
            AlertUtil.showInfo(container.getBundle().getString("alert.success"),
                    String.format(container.getBundle().getString("vat.group.success.products"), res));
            vatSelectedProducts.clear();
            refreshHistory();
        }, (err) -> {
            AlertUtil.showError(container.getBundle().getString("vat.error.update_failed"), err.getMessage());
        });
    }

    @FXML
    void handleAddTaxRate(ActionEvent event) {
        TaxRate newRate = new TaxRate();
        newRate.setActive(true);
        newRate.setCountry("ES");
        com.mycompany.ventacontrolfx.util.ModalService.showTransparentModal(
                "/view/dialog/tax_rate_dialog.fxml",
                container.getBundle().getString("vat.dialog.tax_rate.title_new"),
                container,
                (com.mycompany.ventacontrolfx.presentation.controller.dialog.TaxRateDialogController ctrl) -> {
                    ctrl.init(newRate);
                });
        loadTaxCatalogData();
    }

    private void handleEditTaxRate(TaxRate rate) {
        com.mycompany.ventacontrolfx.util.ModalService.showTransparentModal(
                "/view/dialog/tax_rate_dialog.fxml",
                container.getBundle().getString("vat.dialog.tax_rate.title_edit"),
                container,
                (com.mycompany.ventacontrolfx.presentation.controller.dialog.TaxRateDialogController ctrl) -> {
                    ctrl.init(rate);
                });
        loadTaxCatalogData();
    }

    private void handleDeleteTaxRate(TaxRate rate) {
        boolean confirmed = AlertUtil.showConfirmation(container.getBundle().getString("vat.confirm.delete_rate.title"),
                container.getBundle().getString("alert.confirm"),
                String.format(container.getBundle().getString("vat.confirm.delete_rate.msg"), rate.getName()));
        if (confirmed) {
            try {
                taxManagementUseCase.deleteTaxRate(rate.getTaxRateId());
                loadTaxCatalogData();
                showInfo(container.getBundle().getString("vat.dialog.tax_rate.success_delete"));
            } catch (SQLException e) {
                showError(container.getBundle().getString("vat.dialog.tax_rate.error_delete"));
            }
        }
    }

    @FXML
    void handleAddTaxGroup(ActionEvent event) {
        com.mycompany.ventacontrolfx.util.ModalService.showTransparentModal(
                "/view/dialog/tax_group_dialog.fxml",
                container.getBundle().getString("vat.dialog.tax_group.title_new"),
                container,
                (com.mycompany.ventacontrolfx.presentation.controller.dialog.TaxGroupDialogController ctrl) -> {
                    ctrl.init(new TaxGroup());
                });
        loadTaxCatalogData();
        fetchInitialDataAsync(null);
    }

    private void handleEditTaxGroup(TaxGroup group) {
        com.mycompany.ventacontrolfx.util.ModalService.showTransparentModal(
                "/view/dialog/tax_group_dialog.fxml",
                container.getBundle().getString("vat.dialog.tax_group.title_edit"),
                container,
                (com.mycompany.ventacontrolfx.presentation.controller.dialog.TaxGroupDialogController ctrl) -> {
                    ctrl.init(group);
                });
        loadTaxCatalogData();
        fetchInitialDataAsync(null);
    }

    private void handleDeleteTaxGroup(TaxGroup group) {
        boolean confirmed = AlertUtil.showConfirmation(
                container.getBundle().getString("vat.confirm.delete_group.title"),
                container.getBundle().getString("alert.confirm"),
                String.format(container.getBundle().getString("vat.confirm.delete_group.msg"), group.getName()));
        if (confirmed) {
            try {
                taxManagementUseCase.deleteTaxGroup(group.getTaxGroupId());
                loadTaxCatalogData();
                fetchInitialDataAsync(null); // Refresh combos
                showInfo(container.getBundle().getString("vat.dialog.tax_group.success_delete"));
            } catch (SQLException e) {
                showError(container.getBundle().getString("vat.dialog.tax_group.error_delete"));
            }
        }
    }

    @FXML
    public void handleQuickDateFilter(ActionEvent event) {
        ToggleButton btn = (ToggleButton) event.getSource();
        if (btn == tglHistHoy)
            histFilterDays = 1;
        else if (btn == tglHist7d)
            histFilterDays = 7;
        else if (btn == tglHist1m)
            histFilterDays = 30;
        else
            histFilterDays = null;
        handleRefreshHistory();
    }

    @FXML
    public void handleRefreshHistory() {
        // Al refrescar quitamos filtros temporales para ver si hay algo nuevo
        histFilterDays = null;
        // Al usar DateFilterUtils, el bot\u00f3n "Todo" se seleccionar\u00e1 solo si lo
        // forzamos,
        // pero lo m\u00e1s sencillo es reinicializar si queremos limpiar visualmente.
        DateFilterUtils.addQuickFilters(quickFilterContainer, (label) -> {
            if (label.equals(container.getBundle().getString("filter.date.today"))) {
                histFilterDays = 1;
            } else if (label.equals(container.getBundle().getString("filter.date.7d"))) {
                histFilterDays = 7;
            } else if (label.equals(container.getBundle().getString("filter.date.this_month"))) {
                histFilterDays = 30;
            } else {
                histFilterDays = null;
            }
        }, container.getBundle(), this::refreshHistory);

        refreshHistory();
        refreshPriceLog();
    }

    private void refreshHistory() {
        try {
            if (taxManagementUseCase != null) {
                // Determine scope from combo
                TaxRevision.Scope scope = null;
                String scopeStr = cmbHistoryScope != null ? cmbHistoryScope.getSelectionModel().getSelectedItem()
                        : null;
                if (container.getBundle().getString("vat.history.scope.global").equals(scopeStr))
                    scope = TaxRevision.Scope.GLOBAL;
                else if (container.getBundle().getString("vat.history.scope.category").equals(scopeStr))
                    scope = TaxRevision.Scope.CATEGORY;
                else if (container.getBundle().getString("vat.history.scope.product").equals(scopeStr))
                    scope = TaxRevision.Scope.PRODUCT;

                final TaxRevision.Scope finalScope = scope;
                List<TaxRevision> history = taxManagementUseCase.getTaxHistory(finalScope);

                // Apply quick date filter
                if (histFilterDays != null) {
                    LocalDateTime cutoff = LocalDateTime.now().minus(histFilterDays, ChronoUnit.DAYS);
                    history = history.stream()
                            .filter(r -> r.getStartDate().isAfter(cutoff))
                            .collect(Collectors.toList());
                }

                historyTable.setItems(FXCollections.observableArrayList(history));
            }
        } catch (Exception e) {
            showError(String.format(container.getBundle().getString("vat.error.refresh_failed"), e.getMessage()));
        }
        refreshPriceLog();
    }

    private void refreshPriceLog() {
        if (priceLogTable == null)
            return;
        try {
            List<PriceUpdateLog> logs = priceLogRepository.getAll();

            // Apply quick date filter
            if (histFilterDays != null) {
                LocalDateTime cutoff = LocalDateTime.now().minus(histFilterDays, ChronoUnit.DAYS);
                logs = logs.stream()
                        .filter(l -> l.getAppliedAt().isAfter(cutoff))
                        .collect(Collectors.toList());
            }

            priceLogTable.setItems(FXCollections.observableArrayList(logs));
        } catch (Exception ignored) {
            // Tabla puede no existir a\u00fan en BD antigua
        }
    }

    // \u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090
    // HELPERS
    // \u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090\u00e2\u2022\u0090

    private int parseIntField(TextField field, String fieldName) {
        if (field == null || field.getText().isBlank())
            throw new IllegalArgumentException(
                    String.format(container.getBundle().getString("common.field_required"), fieldName));
        try {
            int v = Integer.parseInt(field.getText().trim());
            if (v <= 0)
                throw new NumberFormatException();
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

    private void showInfo(String message) {
        com.mycompany.ventacontrolfx.util.AlertUtil.showInfo(container.getBundle().getString("alert.success"), message);
    }

    private void showWarning(String message) {
        com.mycompany.ventacontrolfx.util.AlertUtil.showWarning(container.getBundle().getString("alert.warning"),
                message);
    }

    private void showError(String message) {
        com.mycompany.ventacontrolfx.util.AlertUtil.showError(container.getBundle().getString("alert.error"), message);
    }

    @FXML
    private void handleClearVatSelection(ActionEvent event) {
        if (vatSelectedProducts != null) {
            vatSelectedProducts.clear();
            com.mycompany.ventacontrolfx.util.AlertUtil
                    .showToast(container.getBundle().getString("vat.selection.cleared"));
        }
    }
}
