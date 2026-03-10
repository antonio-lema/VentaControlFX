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
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ProductBox extends VBox {

    public ProductBox(Product product, double globalTaxRate, boolean pricesIncludeTax, Consumer<Product> onAddToCart) {
        this.getStyleClass().add("product-box");
        this.setPrefWidth(200); // Antes 180

        // Image Container
        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("product-image-container");
        imageContainer.setPrefHeight(170);
        imageContainer.setMinHeight(170);
        imageContainer.setMaxHeight(170);
        imageContainer.setAlignment(Pos.CENTER);

        // Clip for top corners
        Rectangle clip = new Rectangle(200, 170);
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        imageContainer.setClip(clip);

        // Image Logic
        ImageView imageView = new ImageView();
        imageView.setFitHeight(170);
        imageView.setFitWidth(200);
        imageView.setPreserveRatio(false);

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
        VBox infoBox = new VBox(6);
        infoBox.getStyleClass().add("product-info");
        infoBox.setAlignment(Pos.TOP_LEFT);

        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("product-name");
        nameLabel.setWrapText(true);
        nameLabel.setMinHeight(40);
        nameLabel.setMaxHeight(40);

        double displayPrice = product.getCurrentPrice();
        if (!pricesIncludeTax) {
            double rate = product.resolveEffectiveIva(globalTaxRate);
            displayPrice = product.getCurrentPrice() * (1 + (rate / 100.0));
        }

        Label priceLabel = new Label(String.format("%.2f €", displayPrice));
        priceLabel.getStyleClass().add("product-price-badge");

        infoBox.getChildren().addAll(nameLabel, priceLabel);

        this.getChildren().addAll(imageContainer, infoBox);

        // Click event
        this.setOnMouseClicked(e -> {
            if (onAddToCart != null) {
                onAddToCart.accept(product);
            }
        });
    }

    // Constructor legacy por si acaso
    public ProductBox(Product product, Consumer<Product> onAddToCart) {
        this(product, 21.0, true, onAddToCart);
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

    private VBox createPlaceholder() {
        FontAwesomeIconView icon = new FontAwesomeIconView();
        icon.setGlyphName("IMAGE");
        icon.setSize("40");
        icon.setFill(Color.web("#cbd5e1"));

        Label placeholder = new Label("Sin imagen");
        placeholder
                .setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #94a3b8; -fx-padding: 8 0 0 0;");

        VBox container = new VBox(icon, placeholder);
        container.setAlignment(Pos.CENTER);
        return container;
    }
}
