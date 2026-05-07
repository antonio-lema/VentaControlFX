package com.mycompany.ventacontrolfx.presentation.controller.product;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Gestor de imágenes para productos.
 * Maneja la selección, previsualización y persistencia física de archivos.
 */
public class ProductImageManager {

    private final ImageView imageView;
    private File selectedFile;

    public ProductImageManager(ImageView imageView) {
        this.imageView = imageView;
    }

    public void selectImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar Imagen de Producto");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        
        File file = fc.showOpenDialog(imageView.getScene().getWindow());
        if (file != null) {
            this.selectedFile = file;
            this.imageView.setImage(new Image(file.toURI().toString()));
        }
    }

    public String saveImageLocally() {
        if (selectedFile == null) return null;
        try {
            File destDir = new File("data/images/products");
            if (!destDir.exists()) destDir.mkdirs();

            String fileName = System.currentTimeMillis() + "_" + selectedFile.getName().replaceAll("\\s+", "_");
            File destFile = new File(destDir, fileName);
            Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            return "data/images/products/" + fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return selectedFile.getAbsolutePath();
        }
    }

    public void loadPreview(String path) {
        if (path == null || path.isEmpty()) return;
        File f = resolveFile(path);
        if (f != null && f.exists()) {
            imageView.setImage(new Image(f.toURI().toString()));
        }
    }

    private File resolveFile(String path) {
        File f = new File(path);
        if (f.exists()) return f;
        
        File defaultDir = new File("data/images/products");
        File f2 = new File(defaultDir, f.getName());
        if (f2.exists()) return f2;
        
        File f3 = new File(".", path);
        if (f3.exists()) return f3;
        
        return null;
    }

    public File getSelectedFile() { return selectedFile; }
}

