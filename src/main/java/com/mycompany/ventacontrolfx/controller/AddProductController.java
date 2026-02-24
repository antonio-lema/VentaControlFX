package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.Category;
import com.mycompany.ventacontrolfx.model.Product;
import com.mycompany.ventacontrolfx.service.CategoryService;
import com.mycompany.ventacontrolfx.service.ProductService;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import javafx.fxml.FXML;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.collections.FXCollections;
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
    private ComboBox<Category> cmbCategory;
    @FXML
    private CheckBox chkFavorite;
    @FXML
    private ImageView ivProductImage;
    @FXML
    private javafx.scene.control.Label lblTitle;
    @FXML
    private javafx.scene.control.Label lblSubtitle;

    private CategoryService categoryService;
    private ProductService productService;

    private File selectedImageFile;
    private Product productToEdit;

    @FXML
    private javafx.scene.layout.StackPane rootStackPane;

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    private void handleMousePressed(javafx.scene.input.MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    private void handleMouseDragged(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) rootStackPane.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) rootStackPane.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void initialize() {
        categoryService = new CategoryService();
        productService = new ProductService();
        loadCategories();

        // Setup StringConverter for ComboBox to display Category names
        cmbCategory.setConverter(new StringConverter<Category>() {
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
            lblTitle.setText("Editar Producto");
            lblSubtitle.setText("Modifica los datos del producto");
            txtName.setText(product.getName());
            txtPrice.setText(String.valueOf(product.getPrice()));
            // Set category
            for (Category category : cmbCategory.getItems()) {
                if (category.getId() == product.getCategoryId()) {
                    cmbCategory.setValue(category);
                    break;
                }
            }
            chkFavorite.setSelected(product.isFavorite());

            // Set image if exists
            if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
                File file = new File(product.getImagePath());
                if (file.exists()) {
                    selectedImageFile = file;
                    Image image = new Image(file.toURI().toString());
                    ivProductImage.setImage(image);
                }
            }
        } else {
            lblTitle.setText("Nuevo Producto");
            lblSubtitle.setText("Introduce los datos del nuevo producto");
        }
    }

    private void loadCategories() {
        try {
            List<Category> categories = categoryService.getAllCategories();
            cmbCategory.setItems(FXCollections.observableArrayList(categories));
        } catch (SQLException e) {
            showAlert("Error", "No se pudieron cargar las categorías: " + e.getMessage());
        }
    }

    @FXML
    private void handleSelectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Imagen del Producto");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = fileChooser.showOpenDialog(rootStackPane.getScene().getWindow());
        if (file != null) {
            selectedImageFile = file;
            Image image = new Image(file.toURI().toString());
            ivProductImage.setImage(image);
        }
    }

    @FXML
    private void handleSave() {
        String name = txtName.getText();
        String priceText = txtPrice.getText();
        Category selectedCategory = cmbCategory.getValue();
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
            Product product;
            if (productToEdit == null) {
                product = new Product(selectedCategory.getId(), name, price, isFavorite);
            } else {
                product = productToEdit;
                product.setName(name);
                product.setPrice(price);
                product.setCategoryId(selectedCategory.getId());
                product.setFavorite(isFavorite);
            }

            if (selectedImageFile != null) {
                product.setImagePath(selectedImageFile.getAbsolutePath());
            }

            if (productToEdit == null) {
                productService.addProduct(product);
            } else {
                productService.updateProduct(product);
            }

            handleCancel(); // Close dialog
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo guardar el producto: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        AlertUtil.showWarning(title, content);
    }
}
