package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Price;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.application.usecase.ProductUseCase;
import com.mycompany.ventacontrolfx.application.usecase.CategoryUseCase;
import com.mycompany.ventacontrolfx.application.usecase.PriceListUseCase;
import com.mycompany.ventacontrolfx.application.usecase.PriceUseCase;
import com.mycompany.ventacontrolfx.application.dto.PriceInfoDTO;
import com.mycompany.ventacontrolfx.domain.model.TaxGroup;
import com.mycompany.ventacontrolfx.domain.service.TaxEngineService;
import com.mycompany.ventacontrolfx.domain.exception.BusinessException;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AddProductController implements Injectable {

    @FXML
    private TextField txtName, txtIva, txtStockQuantity, txtMinStock;
    @FXML
    private VBox vboxPrices;
    @FXML
    private HBox hboxStockDetails;
    @FXML
    private ComboBox<Category> cmbCategory;
    @FXML
    private ComboBox<TaxGroup> cmbTaxGroup;
    @FXML
    private CheckBox chkFavorite, chkManageStock;
    @FXML
    private ImageView ivProductImage;
    @FXML
    private Label lblTitle;

    private ServiceContainer container;
    private ProductUseCase productUseCase;
    private CategoryUseCase categoryUseCase;
    private PriceListUseCase priceListUseCase;
    private PriceUseCase priceUseCase;
    private TaxEngineService taxEngineService;
    private Product productToEdit;
    private File selectedImageFile;

    private Map<Integer, TextField> priceFields = new HashMap<>();
    private Map<Integer, Label> diffLabels = new HashMap<>();
    private PriceList defaultPriceList;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.productUseCase = container.getProductUseCase();
        this.categoryUseCase = container.getCategoryUseCase();
        this.priceListUseCase = container.getPriceListUseCase();
        this.priceUseCase = container.getPriceUseCase();
        this.taxEngineService = container.getTaxEngineService();
        setupCategoryComboBox();
        setupTaxGroupComboBox();
        loadCategories();
        loadPriceLists();
        loadTaxGroups();
        setupStockControls();
    }

    private void setupStockControls() {
        hboxStockDetails.disableProperty().bind(chkManageStock.selectedProperty().not());
        txtStockQuantity.setText("0");
        txtMinStock.setText("0");
    }

    private void loadPriceLists() {
        try {
            List<PriceList> lists = priceListUseCase.getAll();
            vboxPrices.getChildren().clear();
            priceFields.clear();
            diffLabels.clear();

            for (PriceList pl : lists) {
                if (pl.isDefault()) {
                    defaultPriceList = pl;
                }
                HBox row = new HBox();
                row.getStyleClass().add("modern-input-container");
                row.setAlignment(Pos.CENTER_LEFT);

                FontAwesomeIconView icon = new FontAwesomeIconView(
                        de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MONEY);
                icon.setSize("18");
                icon.getStyleClass().add("sidebar-icon");
                HBox.setMargin(icon, new Insets(0, 10, 0, 0));

                TextField txtDynamicPrice = new TextField();
                txtDynamicPrice.setPromptText("0.00");
                txtDynamicPrice.getStyleClass().add("modern-input-field");
                HBox.setHgrow(txtDynamicPrice, javafx.scene.layout.Priority.ALWAYS);

                // Label para identificar la tarifa claramente
                String baseSuffix = container.getBundle().getString("product.price_list.base_suffix");
                Label lblListName = new Label(pl.getName() + (pl.isDefault() ? " (" + baseSuffix + "):" : ":"));
                lblListName.setMinWidth(140);
                lblListName.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b; -fx-font-size: 13px;");
                HBox.setMargin(lblListName, new Insets(0, 10, 0, 0));

                Label lblDiff = new Label("");
                lblDiff.setMinWidth(65);
                lblDiff.setAlignment(Pos.CENTER_RIGHT);
                lblDiff.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                HBox.setMargin(lblDiff, new Insets(0, 0, 0, 10));

                row.getChildren().addAll(icon, lblListName, txtDynamicPrice, lblDiff);
                vboxPrices.getChildren().add(row);

                priceFields.put(pl.getId(), txtDynamicPrice);
                if (!pl.isDefault()) {
                    diffLabels.put(pl.getId(), lblDiff);
                }
            }

            // Set up listeners AFTER all fields exist
            if (defaultPriceList != null && priceFields.containsKey(defaultPriceList.getId())) {
                TextField defaultField = priceFields.get(defaultPriceList.getId());

                // When default price changes, update all diffs
                defaultField.textProperty().addListener((obs, oldV, newV) -> updateAllDiffs());

                // When any specific price changes, update its diff
                for (Map.Entry<Integer, TextField> entry : priceFields.entrySet()) {
                    if (entry.getKey() != defaultPriceList.getId()) {
                        entry.getValue().textProperty().addListener((obs, oldV, newV) -> updateDiff(entry.getKey()));
                    }
                }
            }

        } catch (SQLException e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("product.error.load_price_lists"));
        }
    }

    private void updateAllDiffs() {
        for (Integer id : diffLabels.keySet()) {
            updateDiff(id);
        }
    }

    private void updateDiff(int priceListId) {
        if (defaultPriceList == null || !priceFields.containsKey(defaultPriceList.getId()))
            return;
        if (!priceFields.containsKey(priceListId) || !diffLabels.containsKey(priceListId))
            return;

        try {
            double defaultPrice = Double
                    .parseDouble(priceFields.get(defaultPriceList.getId()).getText().replace(",", "."));
            double currentPrice = Double.parseDouble(priceFields.get(priceListId).getText().replace(",", "."));

            if (defaultPrice > 0) {
                double diffPercent = ((currentPrice - defaultPrice) / defaultPrice) * 100.0;
                Label lbl = diffLabels.get(priceListId);

                if (Math.abs(diffPercent) < 0.01) {
                    lbl.setText(container.getBundle().getString("product.price_diff.equal") + " =");
                    lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;"); // slate-400
                } else if (diffPercent > 0) {
                    lbl.setText(String.format("+%.1f%%", diffPercent));
                    lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #22c55e;"); // green-500
                } else {
                    lbl.setText(String.format("%.1f%%", diffPercent));
                    lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #ef4444;"); // red-500
                }
            } else {
                diffLabels.get(priceListId).setText("");
            }
        } catch (Exception e) {
            diffLabels.get(priceListId).setText("");
        }
    }

    private void setupCategoryComboBox() {
        cmbCategory.setConverter(new StringConverter<>() {
            @Override
            public String toString(Category c) {
                return c != null ? c.getName() : "";
            }

            @Override
            public Category fromString(String s) {
                return null;
            }
        });

        cmbCategory.getSelectionModel().selectedItemProperty().addListener((obs, old, nv) -> {
            if (nv != null && nv.getTaxGroupId() != null) {
                // Heredar grupo de impuestos de la categor\u00eda de forma inteligente
                for (TaxGroup tg : cmbTaxGroup.getItems()) {
                    if (tg.getId().equals(nv.getTaxGroupId())) {
                        cmbTaxGroup.setValue(tg);
                        break;
                    }
                }
            }
        });
    }

    private void setupTaxGroupComboBox() {
        cmbTaxGroup.setConverter(new StringConverter<>() {
            @Override
            public String toString(TaxGroup t) {
                return t != null ? t.getName() : container.getBundle().getString("product.form.tax_group.legacy");
            }

            @Override
            public TaxGroup fromString(String s) {
                return null;
            }
        });

        // Listener para sincronizaci\u00f3n espejo (Mirrored Sync)
        cmbTaxGroup.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Modo V2: Calcular tasa total y sincronizar con campo IVA
                double totalRate = newVal.getRates().stream()
                        .mapToDouble(com.mycompany.ventacontrolfx.domain.model.TaxRate::getRate)
                        .sum();
                txtIva.setText(String.valueOf(totalRate));
                txtIva.setDisable(true);
            } else {
                // Modo Legacy: Habilitar edici\u00f3n manual
                txtIva.setDisable(false);
                if (productToEdit != null && productToEdit.getIva() != null) {
                    txtIva.setText(String.valueOf(productToEdit.getIva()));
                } else {
                    txtIva.setText("21.0");
                }
            }
        });
    }

    private void loadTaxGroups() {
        try {
            List<TaxGroup> groups = taxEngineService.getAllGroups();
            cmbTaxGroup.setItems(FXCollections.observableArrayList(groups));
        } catch (Exception e) {
            // No interrumpir si falla el tax engine
        }
    }

    private void loadCategories() {
        try {
            List<Category> categories = categoryUseCase.getAll();
            cmbCategory.setItems(FXCollections.observableArrayList(categories));
        } catch (SQLException e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("product.error.load_categories"));
        }
    }

    public void setProduct(Product product) {
        this.productToEdit = product;
        lblTitle.setText(container.getBundle().getString("product.form.edit_title"));
        txtName.setText(product.getName());
        chkFavorite.setSelected(product.isFavorite());
        if (product.getIva() != null) {
            txtIva.setText(String.valueOf(product.getIva()));
        } else {
            txtIva.setText("");
        }

        chkManageStock.setSelected(product.isManageStock());
        txtStockQuantity.setText(String.valueOf(product.getStockQuantity()));
        txtMinStock.setText(String.valueOf(product.getMinStock()));

        // Select correct category
        for (Category c : cmbCategory.getItems()) {
            if (c.getId() == product.getCategoryId()) {
                cmbCategory.setValue(c);
                break;
            }
        }

        // Select correct tax group
        if (product.getTaxGroupId() != null) {
            for (TaxGroup tg : cmbTaxGroup.getItems()) {
                if (tg.getId() == product.getTaxGroupId()) {
                    cmbTaxGroup.setValue(tg);
                    // Sincronizaci\u00f3n inicial
                    double totalRate = tg.getRates().stream().mapToDouble(r -> r.getRate()).sum();
                    txtIva.setText(String.valueOf(totalRate));
                    txtIva.setDisable(true);
                    break;
                }
            }
        } else {
            cmbTaxGroup.setValue(null);
            txtIva.setDisable(false);
            if (product.getIva() != null) {
                txtIva.setText(String.valueOf(product.getIva()));
            }
        }

        if (product.getImagePath() != null) {
            File f = resolveFile(product.getImagePath());
            if (f != null && f.exists())
                ivProductImage.setImage(new Image(f.toURI().toString()));
        }

        // Load explicit prices asynchronously to not block UI thread during load
        Platform.runLater(() -> {
            for (Map.Entry<Integer, TextField> entry : priceFields.entrySet()) {
                try {
                    Optional<Price> activePrice = priceUseCase.getActivePrice(product.getId(), entry.getKey());
                    if (activePrice.isPresent()) {
                        entry.getValue().setText(String.valueOf(activePrice.get().getValue()));
                    } else if (defaultPriceList != null && entry.getKey() == defaultPriceList.getId()) {
                        // Fallback in case there is no explicit price record yet for the default
                        entry.getValue().setText(String.valueOf(product.getPrice()));
                    }
                } catch (SQLException ex) {
                }
            }
            // Forzar refresco visual de los porcentajes de diferencia iniciales
            updateAllDiffs();
        });
    }

    @FXML
    private void handleSelectImage() {
        FileChooser fc = new FileChooser();
        File file = fc.showOpenDialog(txtName.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            ivProductImage.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    private void handleSave() {
        if (txtName.getText().isEmpty() || cmbCategory.getValue() == null) {
            AlertUtil.showWarning(container.getBundle().getString("alert.validation"),
                    container.getBundle().getString("product.error.name_category_required"));
            return;
        }

        double mainPrice = 0.0;
        if (defaultPriceList != null && priceFields.containsKey(defaultPriceList.getId())) {
            try {
                mainPrice = Double.parseDouble(priceFields.get(defaultPriceList.getId()).getText().replace(",", "."));
            } catch (Exception ex) {
                AlertUtil.showWarning(container.getBundle().getString("alert.validation"),
                        container.getBundle().getString("product.error.invalid_base_price"));
                return;
            }
        } else {
            AlertUtil.showWarning(container.getBundle().getString("alert.validation"),
                    container.getBundle().getString("product.error.base_price_required"));
            return;
        }

        try {
            if (productToEdit == null) {
                productToEdit = new Product();
                productToEdit.setVisible(true);
            }
            productToEdit.setName(txtName.getText());
            productToEdit.setPrice(mainPrice);
            productToEdit.setCategoryId(cmbCategory.getValue().getId());
            productToEdit.setFavorite(chkFavorite.isSelected());

            productToEdit.setManageStock(chkManageStock.isSelected());
            try {
                productToEdit.setStockQuantity(Integer.parseInt(txtStockQuantity.getText().trim()));
            } catch (NumberFormatException e) {
                productToEdit.setStockQuantity(0);
            }
            try {
                productToEdit.setMinStock(Integer.parseInt(txtMinStock.getText().trim()));
            } catch (NumberFormatException e) {
                productToEdit.setMinStock(0);
            }

            String ivaStr = txtIva.getText();
            if (ivaStr == null || ivaStr.trim().isEmpty()) {
                productToEdit.setIva(null);
            } else {
                try {
                    productToEdit.setIva(Double.parseDouble(ivaStr.replace(",", ".")));
                } catch (NumberFormatException e) {
                    AlertUtil.showWarning(container.getBundle().getString("alert.validation"),
                            container.getBundle().getString("category.error.invalid_iva"));
                    productToEdit.setIva(null);
                }
            }

            if (cmbTaxGroup.getValue() != null) {
                productToEdit.setTaxGroupId(cmbTaxGroup.getValue().getId());
            } else {
                productToEdit.setTaxGroupId(null);
            }

            if (selectedImageFile != null) {
                String relativePath = saveImageLocally(selectedImageFile);
                productToEdit.setImagePath(relativePath);
            }

            // Save the product (this automatically persists the default price to ID 1 via
            // Repository logic)
            productUseCase.saveProduct(productToEdit);

            // Wait for DB to ensure productToEdit.getId() is established
            for (Map.Entry<Integer, TextField> entry : priceFields.entrySet()) {
                int priceListId = entry.getKey();
                if (defaultPriceList != null && priceListId == defaultPriceList.getId()) {
                    continue; // Ya guardado autom\u00e1ticamente como fallback en productUseCase.saveProduct
                }

                String rawPrice = entry.getValue().getText();
                if (rawPrice != null && !rawPrice.trim().isEmpty()) {
                    double listPrice = Double.parseDouble(rawPrice.replace(",", "."));

                    Optional<Price> activePrice = priceUseCase.getActivePrice(productToEdit.getId(), priceListId);
                    if (activePrice.isEmpty() || activePrice.get().getValue() != listPrice) {
                        priceUseCase.updateProductPrice(productToEdit.getId(), priceListId, listPrice,
                                "Actualizaci\u00f3n de tarifa", LocalDateTime.now());
                    }
                }
            }

            handleCancel();
        } catch (NumberFormatException e) {
            AlertUtil.showWarning(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("product.error.invalid_price"));
        } catch (SQLException e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("product.error.save"));
        }
    }

    private String saveImageLocally(File sourceFile) {
        try {
            File destDir = new File("data/images/products");
            if (!destDir.exists())
                destDir.mkdirs();

            String fileName = System.currentTimeMillis() + "_" + sourceFile.getName();
            File destFile = new File(destDir, fileName);
            java.nio.file.Files.copy(sourceFile.toPath(), destFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return "data/images/products/" + fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return sourceFile.getAbsolutePath();
        }
    }

    @FXML
    private void handleCancel() {
        ((Stage) txtName.getScene().getWindow()).close();
    }

    private File resolveFile(String path) {
        if (path == null || path.isEmpty())
            return null;
        File f = new File(path);
        if (f.exists())
            return f;
        File defaultDir = new File("data/images/products");
        File f2 = new File(defaultDir, f.getName());
        if (f2.exists())
            return f2;
        File f3 = new File(".", path);
        if (f3.exists())
            return f3;
        return null;
    }

    private double xOffset = 0, yOffset = 0;

    @FXML
    private void handleMousePressed(javafx.scene.input.MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    private void handleMouseDragged(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) txtName.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }
}
