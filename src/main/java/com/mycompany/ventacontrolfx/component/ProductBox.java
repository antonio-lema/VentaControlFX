package com.mycompany.ventacontrolfx.component;

import com.mycompany.ventacontrolfx.domain.model.Product;
import java.io.File;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
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
        this.setPrefWidth(200);

        // ── IMAGE SECTION (StackPane allows overlay of price badge) ──
        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("product-image-container");
        imageContainer.setPrefHeight(150);
        imageContainer.setMinHeight(150);
        imageContainer.setMaxHeight(150);
        imageContainer.setAlignment(Pos.CENTER);

        // Clip dinámico: se ajusta al ancho del contenedor y solo redondea arriba
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(imageContainer.widthProperty());
        clip.heightProperty().bind(imageContainer.heightProperty().add(30)); // Altura extra para que el redondeo inferior no se vea
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        imageContainer.setClip(clip);

        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
            File file = resolveFile(product.getImagePath());
            if (file != null && file.exists()) {
                StackPane imageDisplay = new StackPane();
                imageDisplay.getStyleClass().add("product-image-display");
                
                // Margen interno elegante para que la imagen no toque los bordes si se desea
                double margin = 8.0;
                StackPane.setMargin(imageDisplay, new Insets(margin));

                // CSS para "cover" effect con redondeo interno coordinado
                String imageUrl = file.toURI().toString();
                imageDisplay.setStyle(
                        "-fx-background-image: url(\"" + imageUrl + "\"); " +
                                "-fx-background-size: contain; " +
                                "-fx-background-position: center center; " +
                                "-fx-background-repeat: no-repeat; " +
                                "-fx-border-color: rgba(0,0,0,0.05); " +
                                "-fx-border-width: 1px; " +
                                "-fx-background-color: rgba(255,255,255,0.4);"); // Fondo sutil para imágenes transparentes

                imageContainer.getChildren().add(imageDisplay);
            } else {
                imageContainer.getChildren().add(createPlaceholder());
            }
        } else {
            imageContainer.getChildren().add(createPlaceholder());
        }

        // Price Badge (overlaid top-right on image)
        double displayPrice = product.getCurrentPrice();
        if (!pricesIncludeTax) {
            double rate = product.resolveEffectiveIva(globalTaxRate);
            displayPrice = product.getCurrentPrice() * (1 + (rate / 100.0));
        }

        Label priceBadge = new Label(String.format("%.2f€", displayPrice));
        priceBadge.getStyleClass().add("product-price-badge");
        StackPane.setAlignment(priceBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(priceBadge, new Insets(10, 10, 0, 0));
        imageContainer.getChildren().add(priceBadge);

        // ── INFO SECTION ──
        VBox infoBox = new VBox(4);
        infoBox.getStyleClass().add("product-info");
        infoBox.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("product-name");
        nameLabel.setWrapText(false);
        nameLabel.setMaxWidth(Double.MAX_VALUE);

        // Description (use category name as subtitle, or product description if
        // available)
        String descText = (product.getCategoryName() != null && !product.getCategoryName().isEmpty())
                ? product.getCategoryName()
                : "Producto sin categoría";
        Label descLabel = new Label(descText);
        descLabel.getStyleClass().add("product-description");
        descLabel.setWrapText(false);
        descLabel.setMaxWidth(Double.MAX_VALUE);

        infoBox.getChildren().addAll(nameLabel, descLabel);

        // ── ADD BUTTON ──
        FontAwesomeIconView addIcon = new FontAwesomeIconView();
        addIcon.setGlyphName("PLUS");
        addIcon.setSize("11");
        addIcon.getStyleClass().add("product-add-icon");

        Label addLabel = new Label("AÑADIR");
        addLabel.getStyleClass().add("product-add-label");

        HBox addBtnContent = new HBox(6, addIcon, addLabel);
        addBtnContent.setAlignment(Pos.CENTER);

        Button addButton = new Button();
        addButton.setGraphic(addBtnContent);
        addButton.getStyleClass().add("product-add-btn");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnAction(e -> {
            if (onAddToCart != null)
                onAddToCart.accept(product);
        });

        this.getChildren().addAll(imageContainer, infoBox, addButton);

        // Click on card also triggers add
        this.setOnMouseClicked(e -> {
            if (onAddToCart != null)
                onAddToCart.accept(product);
        });
    }

    public ProductBox(Product product, Consumer<Product> onAddToCart) {
        this(product, 21.0, true, onAddToCart);
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

    private VBox createPlaceholder() {
        FontAwesomeIconView icon = new FontAwesomeIconView();
        icon.setGlyphName("SHOPPING_BASKET");
        icon.setSize("54");
        icon.getStyleClass().add("product-placeholder-icon");

        VBox container = new VBox(icon);
        container.setAlignment(Pos.CENTER);
        return container;
    }
}
