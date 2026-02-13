package com.mycompany.ventacontrolfx.component;

import com.mycompany.ventacontrolfx.model.Product;
import java.io.File;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ProductBox extends VBox {

    public ProductBox(Product product, Consumer<Product> onAddToCart) {
        this.getStyleClass().add("product-box");
        this.setPrefWidth(180);

        // Image Container
        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("product-image-container");
        imageContainer.setPrefHeight(140);
        imageContainer.setMinHeight(140);
        imageContainer.setMaxHeight(140);
        imageContainer.setAlignment(Pos.CENTER);

        // Clip for top corners
        Rectangle clip = new Rectangle(180, 140);
        clip.setArcWidth(15);
        clip.setArcHeight(15);
        imageContainer.setClip(clip);

        // Image Logic
        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
            File file = new File(product.getImagePath());
            if (file.exists()) {
                ImageView imageView = new ImageView(new Image(file.toURI().toString()));
                imageView.setFitHeight(140);
                imageView.setFitWidth(180);
                // imageView.setPreserveRatio(true); // Crop approach might be better
                imageContainer.getChildren().add(imageView);
            } else {
                imageContainer.getChildren().add(createPlaceholder());
            }
        } else {
            imageContainer.getChildren().add(createPlaceholder());
        }

        // Info Container
        VBox infoBox = new VBox(2);
        infoBox.getStyleClass().add("product-info");

        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("product-name");
        nameLabel.setWrapText(true);
        nameLabel.setMaxHeight(40);
        nameLabel.setAlignment(Pos.TOP_LEFT);

        Label priceLabel = new Label(String.format("%.2f €", product.getPrice()));
        priceLabel.getStyleClass().add("product-price");

        infoBox.getChildren().addAll(nameLabel, priceLabel);

        this.getChildren().addAll(imageContainer, infoBox);

        // Click event
        this.setOnMouseClicked(e -> {
            if (onAddToCart != null) {
                onAddToCart.accept(product);
            }
        });
    }

    private Label createPlaceholder() {
        Label placeholder = new Label("No Image");
        placeholder.setTextFill(Color.GRAY);
        return placeholder;
    }
}
