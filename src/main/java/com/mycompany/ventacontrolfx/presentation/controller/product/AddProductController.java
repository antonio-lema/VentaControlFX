package com.mycompany.ventacontrolfx.presentation.controller.product;

import com.mycompany.ventacontrolfx.domain.model.*;
import com.mycompany.ventacontrolfx.application.usecase.*;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public class AddProductController implements Injectable {

    @FXML private TextField txtName, txtIva, txtStockQuantity, txtMinStock;
    @FXML private VBox vboxPrices;
    @FXML private HBox hboxStockDetails;
    @FXML private ComboBox<Category> cmbCategory;
    @FXML private ComboBox<TaxGroup> cmbTaxGroup;
    @FXML private CheckBox chkFavorite, chkManageStock;
    @FXML private ImageView ivProductImage;
    @FXML private Label lblTitle;

    private ServiceContainer container;
    private ProductUseCase productUseCase;
    private ProductPriceManager priceManager;
    private ProductImageManager imageManager;
    private ProductTaxManager taxManager;
    private Product productToEdit;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.productUseCase = container.getProductUseCase();
        
        // 1. Inicializar Managers
        this.priceManager = new ProductPriceManager(container, vboxPrices);
        this.imageManager = new ProductImageManager(ivProductImage);
        this.taxManager = new ProductTaxManager(container, cmbCategory, cmbTaxGroup, txtIva);
        this.taxManager.init();

        // 2. Cargar datos base
        try {
            priceManager.renderPriceLists(container.getPriceListUseCase().getAll());
            taxManager.loadData(container.getCategoryUseCase().getAll(), container.getTaxEngineService().getAllTaxGroups());
        } catch (Exception e) { e.printStackTrace(); }

        hboxStockDetails.disableProperty().bind(chkManageStock.selectedProperty().not());
        txtStockQuantity.setText("0"); txtMinStock.setText("0");
    }

    public void setProduct(Product product) {
        this.productToEdit = product;
        lblTitle.setText(container.getBundle().getString("product.form.edit_title"));
        txtName.setText(product.getName());
        chkFavorite.setSelected(product.isFavorite());
        chkManageStock.setSelected(product.isManageStock());
        txtStockQuantity.setText(String.valueOf(product.getStockQuantity()));
        txtMinStock.setText(String.valueOf(product.getMinStock()));
        txtIva.setText(product.getIva() != null ? String.valueOf(product.getIva()) : "");

        taxManager.selectCategoryById(product.getCategoryId());
        taxManager.selectTaxGroupById(product.getTaxGroupId());
        imageManager.loadPreview(product.getImagePath());

        Platform.runLater(() -> priceManager.loadProductPrices(product.getId(), product.getPrice()));
    }

    @FXML private void handleSelectImage() { imageManager.selectImage(); }

    @FXML private void handleSave() {
        if (!validateForm()) return;

        try {
            if (productToEdit == null) { productToEdit = new Product(); productToEdit.setVisible(true); }
            
            // Mapear campos básicos
            productToEdit.setName(txtName.getText());
            productToEdit.setPrice(Double.parseDouble(priceManager.getPriceFields().get(priceManager.getDefaultPriceList().getId()).getText().replace(",", ".")));
            productToEdit.setCategoryId(cmbCategory.getValue().getId());
            productToEdit.setFavorite(chkFavorite.isSelected());
            productToEdit.setManageStock(chkManageStock.isSelected());
            productToEdit.setStockQuantity(Integer.parseInt(txtStockQuantity.getText()));
            productToEdit.setMinStock(Integer.parseInt(txtMinStock.getText()));
            productToEdit.setIva(txtIva.getText().isEmpty() ? null : Double.parseDouble(txtIva.getText().replace(",", ".")));
            productToEdit.setTaxGroupId(cmbTaxGroup.getValue() != null ? cmbTaxGroup.getValue().getId() : null);

            // Guardar Imagen
            String newPath = imageManager.saveImageLocally();
            if (newPath != null) productToEdit.setImagePath(newPath);

            // 3. Persistir Producto y Tarifas
            productUseCase.saveProduct(productToEdit);
            saveExtraPrices();

            handleCancel();
        } catch (Exception e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"), container.getBundle().getString("product.error.save") + ": " + e.getMessage());
        }
    }

    private void saveExtraPrices() throws java.sql.SQLException {
        PriceUseCase priceUseCase = container.getPriceUseCase();
        for (Map.Entry<Integer, TextField> entry : priceManager.getPriceFields().entrySet()) {
            int priceListId = entry.getKey();
            if (priceManager.getDefaultPriceList() != null && priceListId == priceManager.getDefaultPriceList().getId()) continue;

            String raw = entry.getValue().getText();
            if (raw != null && !raw.trim().isEmpty()) {
                double listPrice = Double.parseDouble(raw.replace(",", "."));
                Optional<Price> active = priceUseCase.getActivePrice(productToEdit.getId(), priceListId);
                if (active.isEmpty() || active.get().getValue() != listPrice) {
                    priceUseCase.updateProductPrice(productToEdit.getId(), priceListId, listPrice, container.getBundle().getString("product.price.manual_update"), LocalDateTime.now());
                }
            }
        }
    }

    private boolean validateForm() {
        if (txtName.getText().isEmpty() || cmbCategory.getValue() == null) {
            AlertUtil.showWarning(container.getBundle().getString("alert.validation"), container.getBundle().getString("product.error.name_category_required"));
            return false;
        }
        if (priceManager.getDefaultPriceList() == null || priceManager.getPriceFields().get(priceManager.getDefaultPriceList().getId()).getText().isEmpty()) {
            AlertUtil.showWarning(container.getBundle().getString("alert.validation"), container.getBundle().getString("product.error.base_price_required"));
            return false;
        }
        return true;
    }

    @FXML private void handleCancel() { ((Stage) txtName.getScene().getWindow()).close(); }

    // Ventana Drag-and-Drop
    private double xOffset = 0, yOffset = 0;
    @FXML private void handleMousePressed(javafx.scene.input.MouseEvent event) { xOffset = event.getSceneX(); yOffset = event.getSceneY(); }
    @FXML private void handleMouseDragged(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) txtName.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset); stage.setY(event.getScreenY() - yOffset);
    }
}

