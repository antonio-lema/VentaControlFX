package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.application.usecase.ProductUseCase;
import com.mycompany.ventacontrolfx.application.usecase.CategoryUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

public class AddProductController implements Injectable {

    @FXML
    private TextField txtName, txtPrice, txtIva;
    @FXML
    private ComboBox<Category> cmbCategory;
    @FXML
    private CheckBox chkFavorite;
    @FXML
    private ImageView ivProductImage;
    @FXML
    private Label lblTitle;

    private ProductUseCase productUseCase;
    private CategoryUseCase categoryUseCase;
    private Product productToEdit;
    private File selectedImageFile;

    @Override
    public void inject(ServiceContainer container) {
        this.productUseCase = container.getProductUseCase();
        this.categoryUseCase = container.getCategoryUseCase();
        setupCategoryComboBox();
        loadCategories();
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
    }

    private void loadCategories() {
        try {
            List<Category> categories = categoryUseCase.getAll();
            cmbCategory.setItems(FXCollections.observableArrayList(categories));
        } catch (SQLException e) {
            AlertUtil.showError("Error", "No se pudieron cargar categorías.");
        }
    }

    public void setProduct(Product product) {
        this.productToEdit = product;
        if (product != null) {
            lblTitle.setText("Editar Producto");
            txtName.setText(product.getName());
            txtPrice.setText(String.valueOf(product.getPrice()));
            chkFavorite.setSelected(product.isFavorite());
            if (product.getIva() != null) {
                txtIva.setText(String.valueOf(product.getIva()));
            } else {
                txtIva.setText("");
            }

            // Select correct category
            for (Category c : cmbCategory.getItems()) {
                if (c.getId() == product.getCategoryId()) {
                    cmbCategory.setValue(c);
                    break;
                }
            }

            if (product.getImagePath() != null) {
                File f = resolveFile(product.getImagePath());
                if (f != null && f.exists())
                    ivProductImage.setImage(new Image(f.toURI().toString()));
            }
        }
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
        if (txtName.getText().isEmpty() || txtPrice.getText().isEmpty() || cmbCategory.getValue() == null) {
            AlertUtil.showWarning("Validación", "Complete todos los campos.");
            return;
        }

        try {
            double price = Double.parseDouble(txtPrice.getText());
            if (productToEdit == null) {
                productToEdit = new Product();
                productToEdit.setVisible(true);
            }
            productToEdit.setName(txtName.getText());
            productToEdit.setPrice(price);
            productToEdit.setCategoryId(cmbCategory.getValue().getId());
            productToEdit.setFavorite(chkFavorite.isSelected());

            String ivaStr = txtIva.getText();
            if (ivaStr == null || ivaStr.trim().isEmpty()) {
                productToEdit.setIva(null);
            } else {
                try {
                    productToEdit.setIva(Double.parseDouble(ivaStr));
                } catch (NumberFormatException e) {
                    AlertUtil.showWarning("Validación", "IVA inválido. Se usará el defecto de categoría.");
                    productToEdit.setIva(null);
                }
            }

            if (selectedImageFile != null) {
                String relativePath = saveImageLocally(selectedImageFile);
                productToEdit.setImagePath(relativePath);
            }

            productUseCase.saveProduct(productToEdit);
            handleCancel();
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("Error", "Precio inválido.");
        } catch (SQLException e) {
            AlertUtil.showError("Error", "Error al guardar producto.");
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
