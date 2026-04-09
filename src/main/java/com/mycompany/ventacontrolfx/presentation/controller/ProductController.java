package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.model.VisibilityFilter;
import com.mycompany.ventacontrolfx.application.usecase.ProductUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.shared.async.AsyncManager;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.ModalService;
import com.mycompany.ventacontrolfx.util.ServerPaginationHelper;
import com.mycompany.ventacontrolfx.component.ToggleSwitch;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.WeakHashMap;

public class ProductController implements Injectable, com.mycompany.ventacontrolfx.util.Searchable,
        com.mycompany.ventacontrolfx.shared.bus.GlobalEventBus.DataChangeListener {

    @FXML
    private TableView<Product> productsTable;
    @FXML
    private TableColumn<Product, String> colCategoryName, colName, colImage, colSku;
    @FXML
    private TableColumn<Product, Integer> colStock;
    @FXML
    private TableColumn<Product, Double> colPrice;
    @FXML
    private TableColumn<Product, Boolean> colFavorite, colVisible;
    @FXML
    private TableColumn<Product, Double> colIva;
    @FXML
    private TableColumn<Product, Void> colActions;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Integer> cmbRowLimit;
    @FXML
    private Button btnAdd;
    @FXML
    private Label lblCount;
    @FXML
    private ToggleButton btnFilterAll, btnFilterVisible, btnFilterDisabled;
    @FXML
    private javafx.scene.layout.VBox mainContainer;

    private ProductUseCase productUseCase;
    private ServiceContainer container;
    private AsyncManager asyncManager;
    @FXML
    private Pagination pagination;

    private ServerPaginationHelper<Product> paginationHelper;
    private String currentSearchQuery = "";
    private ToggleGroup filterGroup;
    private final WeakHashMap<String, Image> imageCache = new WeakHashMap<>();
    private double globalIva = 21.0;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.productUseCase = container.getProductUseCase();
        this.asyncManager = container.getAsyncManager();

        try {
            globalIva = 21.0;
        } catch (Exception e) {
            globalIva = 21.0;
        }

        setupTable();
        setupFilterGroup();
        paginationHelper = new ServerPaginationHelper<>(productsTable, cmbRowLimit, lblCount, pagination,
                container.getBundle().getString("products.entity_plural"),
                this::fetchProductsPage, container.getBundle());
        setupSearch();

        container.getEventBus().subscribe(this);
    }

    private void setupFilterGroup() {
        filterGroup = new ToggleGroup();
        btnFilterAll.setToggleGroup(filterGroup);
        btnFilterVisible.setToggleGroup(filterGroup);
        btnFilterDisabled.setToggleGroup(filterGroup);

        filterGroup.selectedToggleProperty().addListener((obs, old, nv) -> {
            if (nv == null) {
                // Prevenir que se des-eleccione todo
                old.setSelected(true);
                return;
            }
            paginationHelper.refresh();
        });
    }

    @Override
    public void onDataChanged() {
        javafx.application.Platform.runLater(() -> paginationHelper.refresh());
    }

    private void setupTable() {
        colCategoryName.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colCategoryName.setCellFactory(col -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : translateDynamic(item));
            }
        });
        
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setCellFactory(col -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : translateDynamic(item));
            }
        });

        colSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        setupPriceColumn();
        colImage.setCellValueFactory(new PropertyValueFactory<>("imagePath"));

        setupImageColumn();
        setupToggleColumn(colFavorite, true);
        setupToggleColumn(colVisible, false);
        setupIvaColumn();
        setupActionColumn();
    }

    private void setupPriceColumn() {
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colPrice.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(String.format("%.2f â‚¬", price));
                    setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #10b981;");
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        });
    }

    private void setupIvaColumn() {
        colIva.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Product p = getTableRow().getItem();
                    double effectiveIva = p.resolveEffectiveIva(globalIva);
                    setText(String.format("%.1f%%", effectiveIva));
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #1e88e5;");
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private void setupImageColumn() {
        colImage.setCellFactory(column -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            private final StackPane container = new StackPane(imageView);

            {
                imageView.setFitWidth(40);
                imageView.setFitHeight(40);
                imageView.setPreserveRatio(true);
                container.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);

                imageView.setImage(null);
                if (empty) {
                    setGraphic(null);
                } else if (imagePath == null || imagePath.trim().isEmpty()) {
                    FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.IMAGE);
                    icon.setSize("24");
                    icon.setFill(Color.web("#bdc3c7"));
                    setGraphic(new StackPane(icon));
                } else {
                    // Placeholder mientras carga asincrÃ³nicamente
                    FontAwesomeIconView iconLoading = new FontAwesomeIconView(FontAwesomeIcon.IMAGE);
                    iconLoading.setSize("24");
                    iconLoading.setFill(Color.web("#bdc3c7"));
                    setGraphic(new StackPane(iconLoading));

                    // Offload file.exists() I/O from the blocking JavaFX thread
                    asyncManager.runAsyncTask(() -> {
                        return resolveFile(imagePath);
                    }, (File file) -> {
                        // Confirmar que la celda aÃºn muestra el mismo path (protecciÃ³n de reciclaje de
                        // celdas TableView)
                        if (imagePath.equals(getItem())) {
                            if (file != null) {
                                String uri = file.toURI().toString();
                                Image img = imageCache.computeIfAbsent(uri, k -> new Image(k, 80, 80, true, true));
                                imageView.setImage(img);
                                setGraphic(container);
                            } else {
                                FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.IMAGE);
                                icon.setSize("24");
                                icon.setFill(Color.web("#bdc3c7"));
                                setGraphic(new StackPane(icon));
                            }
                        }
                    }, null);
                }
            }
        });
    }

    private File resolveFile(String path) {
        if (path == null || path.isEmpty())
            return null;

        // 1. Try as is (absolute or relative to current working directory)
        File f = new File(path);
        if (f.exists())
            return f;

        // 2. Try relative to "data/images/products" (if only filename)
        File defaultDir = new File("data/images/products");
        File f2 = new File(defaultDir, f.getName());
        if (f2.exists())
            return f2;

        // 3. Try with root prepended (common in some envs)
        File f3 = new File(".", path);
        if (f3.exists())
            return f3;

        return null;
    }

    private void setupToggleColumn(TableColumn<Product, Boolean> col, boolean isFavorite) {
        col.setCellValueFactory(new PropertyValueFactory<>(isFavorite ? "favorite" : "visible"));
        col.setCellFactory(column -> new TableCell<>() {
            private final ToggleSwitch toggle = new ToggleSwitch();
            {
                toggle.setOnMouseClicked(e -> {
                    Product p = getTableRow().getItem();
                    if (p != null) {
                        if (!container.getUserSession().hasPermission("producto.editar")) {
                            AlertUtil.showError(container.getBundle().getString("alert.access_denied"),
                                    container.getBundle().getString("error.no_permission"));
                            return;
                        }
                        try {
                            boolean newState = !toggle.isSwitchedOn();
                            toggle.setSwitchedOn(newState);
                            if (isFavorite)
                                productUseCase.toggleFavorite(p.getId(), newState);
                            else
                                productUseCase.toggleVisibility(p.getId(), newState);
                        } catch (SQLException ex) {
                            toggle.setSwitchedOn(!toggle.isSwitchedOn());
                            AlertUtil.showError(container.getBundle().getString("alert.error"),
                                    container.getBundle().getString("product.error.update_status"));
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null)
                    setGraphic(null);
                else {
                    toggle.setState(item);
                    setGraphic(toggle);
                }
            }
        });
    }

    private void setupActionColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();
            private final HBox pane = new HBox(8, btnEdit, btnDelete);
            {
                pane.setAlignment(Pos.CENTER);
                btnEdit.setGraphic(createIcon(FontAwesomeIcon.PENCIL, "#1e88e5"));
                btnEdit.getStyleClass().add("btn-icon");

                FontAwesomeIconView trashIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
                trashIcon.setSize("16");
                btnDelete.setGraphic(trashIcon);
                btnDelete.getStyleClass().add("btn-trash-small");

                btnEdit.setOnAction(e -> {
                    if (container.getUserSession().hasPermission("producto.editar")) {
                        openProductDialog(getTableRow().getItem());
                    } else {
                        AlertUtil.showError(container.getBundle().getString("alert.access_denied"),
                                container.getBundle().getString("error.no_permission"));
                    }
                });
                btnDelete.setOnAction(e -> {
                    if (container.getUserSession().hasPermission("producto.eliminar")) {
                        handleDeleteProduct(getTableRow().getItem());
                    } else {
                        AlertUtil.showError(container.getBundle().getString("alert.access_denied"),
                                container.getBundle().getString("error.no_permission"));
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    btnEdit.setText(null);
                    btnEdit.setGraphic(createIcon(FontAwesomeIcon.PENCIL, "#1e88e5"));
                    setGraphic(pane);
                }
            }
        });
    }

    private FontAwesomeIconView createIcon(FontAwesomeIcon icon, String color) {
        FontAwesomeIconView view = new FontAwesomeIconView(icon);
        view.setSize("16");
        view.setFill(Color.web(color));
        return view;
    }

    @FXML
    private void handleImportExcel() {
        if (!container.getUserSession().hasPermission("producto.importar") &&
                !container.getUserSession().hasPermission("PRODUCTOS")) {
            AlertUtil.showError(container.getBundle().getString("alert.access_denied"),
                    container.getBundle().getString("error.no_permission"));
            return;
        }

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Seleccionar archivo CSV de productos");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Archivos CSV", "*.csv"));

        File selectedFile = fileChooser.showOpenDialog(btnAdd.getScene().getWindow());
        if (selectedFile != null) {
            asyncManager.runAsyncTask(() -> {
                return container.getProductImportUseCase().importFromCsv(selectedFile);
            }, (Integer count) -> {
                ModalService.showTransparentModal("/view/dialog/import_result_dialog.fxml",
                        container.getBundle().getString("product.import.success_title"),
                        container,
                        (c) -> {
                            if (c instanceof com.mycompany.ventacontrolfx.presentation.controller.dialog.ImportResultDialogController)
                                ((com.mycompany.ventacontrolfx.presentation.controller.dialog.ImportResultDialogController) c)
                                        .initData(count, true, "");
                        });
                paginationHelper.refresh();
            }, (Throwable ex) -> {
                ModalService.showTransparentModal("/view/dialog/import_result_dialog.fxml",
                        container.getBundle().getString("product.import.error_title"),
                        container,
                        (c) -> {
                            if (c instanceof com.mycompany.ventacontrolfx.presentation.controller.dialog.ImportResultDialogController)
                                ((com.mycompany.ventacontrolfx.presentation.controller.dialog.ImportResultDialogController) c)
                                        .initData(0, false, ex.getMessage());
                        });
            });
        }
    }

    private void fetchProductsPage(int offset, int limit) {
        VisibilityFilter filter = getSelectedFilter();
        asyncManager.runAsyncTask(() -> {
            int total = productUseCase.getTotalProductCount(currentSearchQuery, filter);
            List<Product> items = productUseCase.getPaginatedProducts(currentSearchQuery, limit, offset, filter);
            return new Object[] { total, items };
        }, (Object res) -> {
            Object[] data = (Object[]) res;
            int total = (int) data[0];
            @SuppressWarnings("unchecked")
            List<Product> items = (List<Product>) data[1];
            paginationHelper.applyDataTarget(items, total);
        }, null);
    }

    private VisibilityFilter getSelectedFilter() {
        if (filterGroup == null || filterGroup.getSelectedToggle() == null) {
            return VisibilityFilter.VISIBLE;
        }
        if (filterGroup.getSelectedToggle() == btnFilterAll) {
            return VisibilityFilter.ALL;
        } else if (filterGroup.getSelectedToggle() == btnFilterDisabled) {
            return VisibilityFilter.DISABLED;
        }
        return VisibilityFilter.VISIBLE;
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, nv) -> {
            handleSearch(nv);
        });
    }

    @Override
    public void handleSearch(String text) {
        this.currentSearchQuery = (text == null) ? "" : text.trim();
        paginationHelper.refresh();
    }

    @FXML
    private void handleAddProduct() {
        if (!container.getUserSession().hasPermission("producto.crear")) {
            AlertUtil.showError(container.getBundle().getString("alert.access_denied"),
                    container.getBundle().getString("error.no_permission"));
            return;
        }
        openProductDialog(null);
    }

    private void openProductDialog(Product p) {
        ModalService.showStandardModal("/view/add_product.fxml",
                p == null ? container.getBundle().getString("product.dialog.new")
                        : container.getBundle().getString("product.dialog.edit"),
                container, (AddProductController controller) -> {
                    if (p != null)
                        controller.setProduct(p);
                });
        paginationHelper.refresh();
    }

    private void handleDeleteProduct(Product p) {
        if (!container.getUserSession().hasPermission("producto.eliminar")) {
            AlertUtil.showError(container.getBundle().getString("alert.access_denied"),
                    container.getBundle().getString("error.no_permission"));
            return;
        }
        if (AlertUtil.showConfirmation(container.getBundle().getString("alert.delete"),
                container.getBundle().getString("product.confirm.delete") + " " + p.getName() + "?", "")) {
            try {
                productUseCase.deleteProduct(p.getId());
                paginationHelper.refresh();
            } catch (SQLException e) {
                AlertUtil.showError(container.getBundle().getString("alert.error"),
                        container.getBundle().getString("product.error.delete"));
            }
        }
    }

    private String translateDynamic(String text) {
        if (text == null || text.isBlank()) return text;
        if (container != null && container.getBundle() != null && container.getBundle().containsKey(text)) {
            return container.getBundle().getString(text);
        }
        return text;
    }

    @FXML
    private void handleJumpBack10() {
        if (paginationHelper != null) {
            paginationHelper.jumpPages(-10);
        }
    }

    @FXML
    private void handleJumpForward10() {
        if (paginationHelper != null) {
            paginationHelper.jumpPages(10);
        }
    }
}
