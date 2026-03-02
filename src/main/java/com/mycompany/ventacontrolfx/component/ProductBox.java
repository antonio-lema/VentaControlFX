package com.mycompany.ventacontrolfx.component;

import com.mycompany.ventacontrolfx.domain.model.Product;
import java.io.File;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ProductBox extends VBox {

    public ProductBox(Product product, Consumer<Product> onAddToCart) {
        this.getStyleClass().add("product-box");
        this.setPrefWidth(200); // Antes 180

        // Image Container
        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("product-image-container");
        imageContainer.setPrefHeight(150); // Antes 140
        imageContainer.setMinHeight(150);
        imageContainer.setMaxHeight(150);
        imageContainer.setAlignment(Pos.CENTER);

        // Clip for top corners
        Rectangle clip = new Rectangle(200, 150);
        clip.setArcWidth(15);
        clip.setArcHeight(15);
        imageContainer.setClip(clip);

        // Image Logic
        ImageView imageView = new ImageView();
        imageView.setFitHeight(150);
        imageView.setFitWidth(200);
        imageView.setPreserveRatio(true);

        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
            File file = resolveFile(product.getImagePath());
            if (file != null && file.exists()) {
                imageView.setImage(new Image(file.toURI().toString()));
                imageContainer.getChildren().add(imageView);
            } else {
                imageContainer.getChildren().add(createPlaceholder());
            }
        } else {
            imageContainer.getChildren().add(createPlaceholder());
        }

        // Info Container
        VBox infoBox = new VBox(8);
        infoBox.getStyleClass().add("product-info");

        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("product-name");
        nameLabel.setWrapText(true);
        nameLabel.setMinHeight(45); // Antes 40
        nameLabel.setMaxHeight(45);

        HBox priceBox = new HBox(10);
        priceBox.setAlignment(Pos.CENTER_LEFT);

        Label priceLabel = new Label(String.format("%.2f €", product.getPrice()));
        priceLabel.getStyleClass().add("product-price-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        priceBox.getChildren().addAll(priceLabel, spacer);

        infoBox.getChildren().addAll(nameLabel, priceBox);

        this.getChildren().addAll(imageContainer, infoBox);

        // Click event
        this.setOnMouseClicked(e -> {
            if (onAddToCart != null) {
                onAddToCart.accept(product);
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

    private Label createPlaceholder() {
        Label placeholder = new Label("No Image");
        placeholder.setTextFill(Color.GRAY);
        return placeholder;
    }
}
