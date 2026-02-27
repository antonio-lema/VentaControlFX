package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.application.usecase.ProductUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.shared.async.AsyncManager;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.ModalService;
import com.mycompany.ventacontrolfx.control.ToggleSwitch;
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
import javafx.scene.paint.Color;
import javafx.stage.Modality;

import java.io.File;
import java.sql.SQLException;
import java.util.WeakHashMap;

public class ProductController implements Injectable, com.mycompany.ventacontrolfx.util.Searchable {

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
    private Label lblCount;

    private ProductUseCase productUseCase;
    private ServiceContainer container;
    private AsyncManager asyncManager;
    private final WeakHashMap<String, Image> imageCache = new WeakHashMap<>();

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.productUseCase = container.getProductUseCase();
        this.asyncManager = container.getAsyncManager();

        setupTable();
        loadProducts();
        setupSearch();
    }

    private void setupTable() {
        colCategoryName.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        setupImageColumn();
        setupToggleColumn(colFavorite, true);
        setupToggleColumn(colVisible, false);
        setupActionColumn();
    }

    private void setupImageColumn() {
        colImage.setCellFactory(column -> new TableCell<>() {
            private final ImageView imageView = new ImageView();

            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);
                if (empty || imagePath == null || imagePath.isEmpty()) {
                    setGraphic(null);
                } else {
                    File file = resolveFile(imagePath);
                    if (file != null && file.exists()) {
                        String uri = file.toURI().toString();
                        Image img = imageCache.computeIfAbsent(uri, k -> new Image(k, 40, 40, true, true));
                        imageView.setImage(img);
                        setGraphic(imageView);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private File resolveFile(String path) {
        // 1. Try as is (absolute or relative to project root)
        File f = new File(path);
        if (f.exists())
            return f;

        // 2. If it's just a filename, try in default directory
        File defaultDir = new File("data/images/products");
        File f2 = new File(defaultDir, f.getName());
        if (f2.exists())
            return f2;

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
                btnDelete.setGraphic(createIcon(FontAwesomeIcon.TRASH, "#e53935"));
                btnEdit.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                btnDelete.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                btnEdit.setOnAction(e -> openProductDialog(getTableRow().getItem()));
                btnDelete.setOnAction(e -> handleDeleteProduct(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private FontAwesomeIconView createIcon(FontAwesomeIcon icon, String color) {
        FontAwesomeIconView view = new FontAwesomeIconView(icon);
        view.setSize("16");
        view.setFill(Color.web(color));
        return view;
    }

    private void loadProducts() {
        asyncManager.runAsyncTask(() -> productUseCase.getAllProducts(), products -> {
            productsTable.setItems(FXCollections.observableArrayList(products));
            updateCount(products.size());
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
            var filtered = allProducts.stream()
                    .filter(p -> p.getName().toLowerCase().contains(query) ||
                            (p.getCategoryName() != null && p.getCategoryName().toLowerCase().contains(query)))
                    .toList();
            productsTable.setItems(FXCollections.observableArrayList(filtered));
            updateCount(filtered.size());
        }, null);
    }

    @FXML
    private void handleAddProduct() {
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
