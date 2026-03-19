package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.application.usecase.ProductUseCase;
import com.mycompany.ventacontrolfx.application.usecase.ScheduleVatChangeUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.shared.async.AsyncManager;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.ModalService;
import com.mycompany.ventacontrolfx.util.PaginationHelper;
import com.mycompany.ventacontrolfx.component.ToggleSwitch;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.WeakHashMap;

public class ProductController implements Injectable, com.mycompany.ventacontrolfx.util.Searchable,
        com.mycompany.ventacontrolfx.shared.bus.GlobalEventBus.DataChangeListener {

    @FXML
    private TableView<Product> productsTable;
    @FXML
    private TableColumn<Product, String> colCategoryName, colName, colImage;
    @FXML
    private TableColumn<Product, Double> colPrice;
    @FXML
    private TableColumn<Product, Boolean> colFavorite, colVisible;
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
    private javafx.scene.layout.VBox mainContainer;

    private ProductUseCase productUseCase;
    private ServiceContainer container;
    private AsyncManager asyncManager;
    private PaginationHelper<Product> paginationHelper;
    private final WeakHashMap<String, Image> imageCache = new WeakHashMap<>();
    private ScheduleVatChangeUseCase vatUseCase;
    private double globalIva = 21.0;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.productUseCase = container.getProductUseCase();
        this.asyncManager = container.getAsyncManager();
        this.vatUseCase = container.getVatUseCase();

        try {
            // globalIva = vatUseCase.getCurrentGlobalRate()
            // .map(com.mycompany.ventacontrolfx.domain.model.TaxRevision::getRate)
            // .orElse(21.0);
            globalIva = 21.0;
        } catch (Exception e) {
            globalIva = 21.0;
        }

        setupTable();
        paginationHelper = new PaginationHelper<>(productsTable, cmbRowLimit, lblCount, "productos");
        loadProducts();
        setupSearch();

        container.getEventBus().subscribe(this);
    }

    @Override
    public void onDataChanged() {
        javafx.application.Platform.runLater(this::loadProducts);
    }

    private void setupTable() {
        colCategoryName.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colImage.setCellValueFactory(new PropertyValueFactory<>("imagePath"));

        setupImageColumn();
        setupToggleColumn(colFavorite, true);
        setupToggleColumn(colVisible, false);
        setupActionColumn();
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
                if (empty) {
                    setGraphic(null);
                } else if (imagePath == null || imagePath.isEmpty()) {
                    imageView.setImage(null);
                    FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.IMAGE);
                    icon.setSize("24");
                    icon.setFill(Color.web("#bdc3c7"));
                    setGraphic(new StackPane(icon));
                } else {
                    File file = resolveFile(imagePath);
                    if (file != null && file.exists()) {
                        String uri = file.toURI().toString();
                        Image img = imageCache.computeIfAbsent(uri, k -> new Image(k, 80, 80, true, true));
                        imageView.setImage(img);
                        setGraphic(container);
                    } else {
                        imageView.setImage(null);
                        FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.IMAGE);
                        icon.setSize("24");
                        icon.setFill(Color.web("#bdc3c7"));
                        setGraphic(new StackPane(icon));
                    }
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
                            AlertUtil.showError("Acceso Denegado", "No tiene permiso para editar productos.");
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
                            AlertUtil.showError("Error", "No se pudo actualizar el estado.");
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
                FontAwesomeIconView trashIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
                trashIcon.setSize("16");
                btnDelete.setGraphic(trashIcon);
                btnDelete.getStyleClass().add("btn-trash-small");
                btnEdit.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                btnEdit.setOnAction(e -> {
                    if (container.getUserSession().hasPermission("producto.editar")) {
                        openProductDialog(getTableRow().getItem());
                    } else {
                        AlertUtil.showError("Acceso Denegado", "No tiene permiso para editar productos.");
                    }
                });
                btnDelete.setOnAction(e -> {
                    if (container.getUserSession().hasPermission("producto.eliminar")) {
                        handleDeleteProduct(getTableRow().getItem());
                    } else {
                        AlertUtil.showError("Acceso Denegado", "No tiene permiso para eliminar productos.");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Product p = getTableRow().getItem();
                    double effectiveIva = p.resolveEffectiveIva(globalIva);
                    btnEdit.setText(String.format("%.1f%%", effectiveIva));
                    btnEdit.setGraphic(createIcon(FontAwesomeIcon.PENCIL, "#1e88e5"));
                    btnEdit.setContentDisplay(ContentDisplay.LEFT);
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
            AlertUtil.showError("Acceso Denegado", "No tiene permiso para importar productos.");
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
                        "Resultado de Importación",
                        container,
                        (com.mycompany.ventacontrolfx.presentation.controller.dialog.ImportResultDialogController c) -> {
                            c.initData(count, true, "");
                        });
                loadProducts();
            }, (Throwable ex) -> {
                ModalService.showTransparentModal("/view/dialog/import_result_dialog.fxml",
                        "Error de Importación",
                        container,
                        (com.mycompany.ventacontrolfx.presentation.controller.dialog.ImportResultDialogController c) -> {
                            c.initData(0, false, ex.getMessage());
                        });
            });
        }
    }

    private void loadProducts() {
        asyncManager.runAsyncTask(() -> productUseCase.getAllProducts(), products -> {
            paginationHelper.setData(products);
        }, null);
    }

    private void updateCount(int count) {
        if (lblCount != null)
            lblCount.setText(count + " productos encontrados");
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, old, nv) -> handleSearch(nv));
    }

    @Override
    public void handleSearch(String text) {
        if (text == null || text.trim().isEmpty()) {
            loadProducts();
            return;
        }
        String query = text.toLowerCase().trim();
        asyncManager.runAsyncTask(() -> productUseCase.getAllProducts(), allProducts -> {
            List<Product> filtered = allProducts.stream()
                    .filter(p -> p.getName().toLowerCase().contains(query) ||
                            (p.getCategoryName() != null && p.getCategoryName().toLowerCase().contains(query)))
                    .toList();
            paginationHelper.setData(filtered);
        }, null);
    }

    @FXML
    private void handleAddProduct() {
        if (!container.getUserSession().hasPermission("producto.crear")) {
            AlertUtil.showError("Acceso Denegado", "No tiene permiso para crear nuevos productos.");
            return;
        }
        openProductDialog(null);
    }

    private void openProductDialog(Product p) {
        ModalService.showStandardModal("/view/add_product.fxml", p == null ? "Nuevo Producto" : "Editar Producto",
                container, (AddProductController controller) -> {
                    if (p != null)
                        controller.setProduct(p);
                });
        loadProducts();
    }

    private void handleDeleteProduct(Product p) {
        if (!container.getUserSession().hasPermission("producto.eliminar")) {
            AlertUtil.showError("Acceso Denegado", "No tiene permiso para eliminar productos.");
            return;
        }
        if (AlertUtil.showConfirmation("Eliminar", "¿Seguro que desea eliminar " + p.getName() + "?", "")) {
            try {
                productUseCase.deleteProduct(p.getId());
                loadProducts();
            } catch (SQLException e) {
                AlertUtil.showError("Error", "No se pudo eliminar el producto.");
            }
        }
    }
}
