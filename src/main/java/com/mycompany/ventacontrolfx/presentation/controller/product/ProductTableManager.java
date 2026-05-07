package com.mycompany.ventacontrolfx.presentation.controller.product;

import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.application.usecase.ProductUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.shared.async.AsyncManager;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import com.mycompany.ventacontrolfx.presentation.component.ToggleSwitch;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
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
import java.util.WeakHashMap;

/**
 * Gestiona el renderizado y comportamiento de la tabla de productos.
 */
public class ProductTableManager {

    private final ServiceContainer container;
    private final ProductUseCase productUseCase;
    private final AsyncManager asyncManager;
    private final WeakHashMap<String, Image> imageCache = new WeakHashMap<>();
    private double globalIva = 21.0;

    public ProductTableManager(ServiceContainer container, ProductUseCase productUseCase, AsyncManager asyncManager) {
        this.container = container;
        this.productUseCase = productUseCase;
        this.asyncManager = asyncManager;
    }

    public void setup(
            TableColumn<Product, String> colCategoryName, TableColumn<Product, String> colName,
            TableColumn<Product, String> colSku, TableColumn<Product, Integer> colStock,
            TableColumn<Product, Double> colPrice, TableColumn<Product, String> colImage,
            TableColumn<Product, Boolean> colFavorite, TableColumn<Product, Boolean> colVisible,
            TableColumn<Product, Double> colIva, TableColumn<Product, Void> colActions,
            java.util.function.Consumer<Product> onEdit, java.util.function.Consumer<Product> onDelete) {

        colCategoryName.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colCategoryName.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : translateDynamic(item));
            }
        });

        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : translateDynamic(item));
            }
        });

        colSku.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        
        setupPriceColumn(colPrice);
        setupImageColumn(colImage);
        setupToggleColumn(colFavorite, true);
        setupToggleColumn(colVisible, false);
        setupIvaColumn(colIva);
        setupActionColumn(colActions, onEdit, onDelete);
    }

    private void setupPriceColumn(TableColumn<Product, Double> col) {
        col.setCellValueFactory(new PropertyValueFactory<>("price"));
        col.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.2f \u20ac", price));
                    setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #10b981;");
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        });
    }

    private void setupIvaColumn(TableColumn<Product, Double> col) {
        col.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Product p = getTableRow().getItem();
                    setText(String.format("%.1f%%", p.resolveEffectiveIva(globalIva)));
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #1e88e5;");
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private void setupImageColumn(TableColumn<Product, String> col) {
        col.setCellValueFactory(new PropertyValueFactory<>("imagePath"));
        col.setCellFactory(column -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            private final StackPane containerBox = new StackPane(imageView);
            {
                imageView.setFitWidth(36);
                imageView.setFitHeight(36);
                imageView.setPreserveRatio(true);
                containerBox.setAlignment(Pos.CENTER);
            }

            @Override protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);
                imageView.setImage(null);
                if (empty) {
                    setGraphic(null);
                } else if (imagePath == null || imagePath.trim().isEmpty()) {
                    setGraphic(createPlaceholder());
                } else {
                    setGraphic(createPlaceholder());
                    asyncManager.runAsyncTask(() -> resolveFile(imagePath), (File file) -> {
                        if (imagePath.equals(getItem())) {
                            if (file != null) {
                                String uri = file.toURI().toString();
                                Image img = imageCache.computeIfAbsent(uri, k -> new Image(k, 80, 80, true, true));
                                imageView.setImage(img);
                                setGraphic(containerBox);
                            } else {
                                setGraphic(createPlaceholder());
                            }
                        }
                    }, null);
                }
            }
        });
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
                            AlertUtil.showError(container.getBundle().getString("alert.access_denied"), container.getBundle().getString("error.no_permission"));
                            return;
                        }
                        try {
                            boolean newState = !toggle.isSwitchedOn();
                            toggle.setSwitchedOn(newState);
                            if (isFavorite) productUseCase.toggleFavorite(p.getId(), newState);
                            else productUseCase.toggleVisibility(p.getId(), newState);
                        } catch (SQLException ex) {
                            toggle.setSwitchedOn(!toggle.isSwitchedOn());
                            AlertUtil.showError(container.getBundle().getString("alert.error"), container.getBundle().getString("product.error.update_status"));
                        }
                    }
                });
            }
            @Override protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setGraphic(null);
                else { toggle.setState(item); setGraphic(toggle); }
            }
        });
    }

    private void setupActionColumn(TableColumn<Product, Void> col, java.util.function.Consumer<Product> onEdit, java.util.function.Consumer<Product> onDelete) {
        col.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();
            private final HBox pane = new HBox(8, btnEdit, btnDelete);
            {
                pane.setAlignment(Pos.CENTER);
                btnEdit.getStyleClass().add("btn-action-edit");
                btnEdit.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PENCIL, "14"));
                btnEdit.setOnAction(e -> onEdit.accept(getTableRow().getItem()));

                if (container.getUserSession().hasPermission("producto.eliminar")) {
                    btnDelete.getStyleClass().add("btn-action-delete");
                    btnDelete.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH_ALT, "14"));
                    btnDelete.setOnAction(e -> onDelete.accept(getTableRow().getItem()));
                } else {
                    btnDelete.getStyleClass().add("btn-action-lock");
                    btnDelete.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.LOCK, "14"));
                    btnDelete.setOnAction(e -> AlertUtil.showWarning(container.getBundle().getString("alert.locked"), container.getBundle().getString("product.msg.locked_action")));
                }
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic((empty || getTableRow() == null || getTableRow().getItem() == null) ? null : pane);
            }
        });
    }

    private StackPane createPlaceholder() {
        FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.IMAGE, "20");
        icon.setFill(Color.web("#bdc3c7"));
        return new StackPane(icon);
    }

    private File resolveFile(String path) {
        if (path == null || path.isEmpty()) return null;
        File f = new File(path);
        if (f.exists()) return f;
        File defaultDir = new File("data/images/products");
        File f2 = new File(defaultDir, f.getName());
        return f2.exists() ? f2 : null;
    }

    private String translateDynamic(String text) {
        if (text == null || text.isBlank()) return text;
        if (container.getBundle().containsKey(text)) return container.getBundle().getString(text);
        return text;
    }

    public void setGlobalIva(double iva) { this.globalIva = iva; }
}


