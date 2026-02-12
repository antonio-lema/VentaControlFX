package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.Category;
import com.mycompany.ventacontrolfx.model.Product;
import com.mycompany.ventacontrolfx.service.CategoryService;
import com.mycompany.ventacontrolfx.service.ProductService;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class AddProductController {

    @FXML
    private TextField txtName;
    @FXML
    private TextField txtPrice;
    @FXML
    private ComboBox<Category> cbCategory;
    @FXML
    private CheckBox chkFavorite;
    @FXML
    private Label lblImageName;
    @FXML
    private ImageView imgPreview;

    private CategoryService categoryService;
    private ProductService productService;

    private File selectedImageFile;
    private Product productToEdit;

    @FXML
    public void initialize() {
        categoryService = new CategoryService();
        productService = new ProductService();
        loadCategories();

        // Setup StringConverter for ComboBox to display Category names
        cbCategory.setConverter(new StringConverter<Category>() {
            @Override
            public String toString(Category category) {
                return category != null ? category.getName() : "";
            }

            @Override
            public Category fromString(String string) {
                return null; // Not needed for selection
            }
        });
    }

    public void setProduct(Product product) {
        this.productToEdit = product;
        if (product != null) {
            txtName.setText(product.getName());
            txtPrice.setText(String.valueOf(product.getPrice()));
            chkFavorite.setSelected(product.isFavorite());

            // Set category
            for (Category category : cbCategory.getItems()) {
                if (category.getId() == product.getCategoryId()) {
                    cbCategory.setValue(category);
                    break;
                }
            }

            // Set image if exists
            if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
                File file = new File(product.getImagePath());
                if (file.exists()) {
                    selectedImageFile = file;
                    lblImageName.setText(file.getName());
                    Image image = new Image(file.toURI().toString());
                    imgPreview.setImage(image);
                }
            }
        }
    }

    private void loadCategories() {
        try {
            List<Category> categories = categoryService.getAllCategories();
            cbCategory.setItems(FXCollections.observableArrayList(categories));
        } catch (SQLException e) {
            showAlert("Error", "No se pudieron cargar las categorías: " + e.getMessage());
        }
    }

    @FXML
    private void selectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Imagen del Producto");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = fileChooser.showOpenDialog(txtName.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            lblImageName.setText(file.getName());
            Image image = new Image(file.toURI().toString());
            imgPreview.setImage(image);
        }
    }

    @FXML
    private void saveProduct() {
        String name = txtName.getText();
        String priceText = txtPrice.getText();
        Category selectedCategory = cbCategory.getValue();
        boolean isFavorite = chkFavorite.isSelected();

        // Validation
        if (name == null || name.trim().isEmpty()) {
            showAlert("Validación", "Por favor, introduce un nombre.");
            return;
        }
        if (priceText == null || priceText.trim().isEmpty()) {
            showAlert("Validación", "Por favor, introduce un precio.");
            return;
        }
        if (selectedCategory == null) {
            showAlert("Validación", "Por favor, selecciona una categoría.");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            showAlert("Validación", "El precio debe ser un número válido.");
            return;
        }

        // Create/Update Product
        try {
            if (productToEdit == null) {
                Product product = new Product(selectedCategory.getId(), name, price, isFavorite);
                if (selectedImageFile != null) {
                    product.setImagePath(selectedImageFile.getAbsolutePath());
                }
                productService.addProduct(product);
            } else {
                productToEdit.setName(name);
                productToEdit.setPrice(price);
                productToEdit.setCategoryId(selectedCategory.getId());
                productToEdit.setFavorite(isFavorite);
                if (selectedImageFile != null) {
                    productToEdit.setImagePath(selectedImageFile.getAbsolutePath());
                }
                productService.updateProduct(productToEdit);
            }
            closeDialog();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo guardar el producto: " + e.getMessage());
        }
    }

    @FXML
    private void cancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) txtName.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
