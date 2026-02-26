package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.Category;
import com.mycompany.ventacontrolfx.model.Product;
import com.mycompany.ventacontrolfx.service.*;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.AppLogger;
import com.mycompany.ventacontrolfx.util.AsyncManager;
import com.mycompany.ventacontrolfx.util.Injectable;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.io.File;
import java.sql.SQLException;
import java.util.List;

/**
 * Enterprise Add/Edit Product Controller.
 * Decoupled from persistence, uses ProductUseCase.
 */
public class AddProductController implements Injectable {
    private static final String TAG = "AddProductController";

    @FXML
    private TextField txtName, txtPrice;
    @FXML
    private ComboBox<Category> cmbCategory;
    @FXML
    private CheckBox chkFavorite;
    @FXML
    private ImageView ivProductImage;
    @FXML
    private Label lblTitle, lblSubtitle;
    @FXML
    private StackPane rootStackPane;

    private ServiceContainer container;
    private File selectedImageFile;
    private Product productToEdit;
    private double xOffset = 0, yOffset = 0;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        setupCategoryComboBox();
        loadCategories();
    }

    private void setupCategoryComboBox() {
        cmbCategory.setConverter(new StringConverter<>() {
            @Override
            public String toString(Category category) {
                return category != null ? category.getName() : "";
            }

            @Override
            public Category fromString(String string) {
                return null;
            }
        });
    }

    private void loadCategories() {
        try {
            List<Category> categories = container.getCategoryService().getAllCategories();
            cmbCategory.setItems(FXCollections.observableArrayList(categories));
            if (productToEdit != null) {
                for (Category c : categories) {
                    if (c.getId() == productToEdit.getCategoryId()) {
                        cmbCategory.setValue(c);
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            AppLogger.error(TAG, "Failed to load categories", e);
            AlertUtil.showError("Error", "No se pudieron cargar las categorías.");
        }
    }

    public void setProduct(Product product) {
        this.productToEdit = product;
        if (product != null) {
            lblTitle.setText("Editar Producto");
            lblSubtitle.setText("Modifica los datos del producto");
            txtName.setText(product.getName());
            txtPrice.setText(String.valueOf(product.getPrice()));
            chkFavorite.setSelected(product.isFavorite());
            if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
                File file = new File(product.getImagePath());
                if (file.exists()) {
                    selectedImageFile = file;
                    ivProductImage.setImage(new Image(file.toURI().toString()));
                }
            }
        }
    }

    @FXML
    private void handleSelectImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar Imagen");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
        File file = fc.showOpenDialog(rootStackPane.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            ivProductImage.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    private void handleSave() {
        if (txtName.getText().isBlank() || txtPrice.getText().isBlank() || cmbCategory.getValue() == null) {
            AlertUtil.showWarning("Validación", "Complete todos los campos obligatorios.");
            return;
        }

        try {
            double price = Double.parseDouble(txtPrice.getText());
            Product product = (productToEdit == null)
                    ? new Product(cmbCategory.getValue().getId(), txtName.getText(), price, chkFavorite.isSelected())
                    : productToEdit;

            if (productToEdit != null) {
                product.setName(txtName.getText());
                product.setPrice(price);
                product.setCategoryId(cmbCategory.getValue().getId());
                product.setFavorite(chkFavorite.isSelected());
            }

            if (selectedImageFile != null)
                product.setImagePath(selectedImageFile.getAbsolutePath());

            // Use Case based Persistence
            AsyncManager.execute(container.getProductUseCase().saveOrUpdateTask(product), v -> {
                AppLogger.info(TAG, "Product saved: " + product.getName());
                handleCancel();
            });

        } catch (NumberFormatException e) {
            AlertUtil.showWarning("Validación", "El precio debe ser un número válido.");
        } catch (Exception e) {
            AppLogger.error(TAG, "General failure saving product", e);
            AlertUtil.showError("Error", "Ocurrió un error al intentar guardar el producto.");
        }
    }

    @FXML
    private void handleCancel() {
        ((Stage) rootStackPane.getScene().getWindow()).close();
    }

    @FXML
    private void handleMousePressed(javafx.scene.input.MouseEvent e) {
        xOffset = e.getSceneX();
        yOffset = e.getSceneY();
    }

    @FXML
    private void handleMouseDragged(javafx.scene.input.MouseEvent e) {
        Stage s = (Stage) rootStackPane.getScene().getWindow();
        s.setX(e.getScreenX() - xOffset);
        s.setY(e.getScreenY() - yOffset);
    }
}
