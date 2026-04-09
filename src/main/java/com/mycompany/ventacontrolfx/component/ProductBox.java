package com.mycompany.ventacontrolfx.component;

import com.mycompany.ventacontrolfx.domain.model.Product;
import java.io.File;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.scene.shape.Rectangle;
import java.util.ResourceBundle;

public class ProductBox extends VBox {
    private final ResourceBundle bundle;

    public ProductBox(Product product, double globalTaxRate, boolean pricesIncludeTax, String discountDesc,
            ResourceBundle bundle, Consumer<Product> onAddToCart) {
        this.bundle = bundle;
        this.getStyleClass().add("product-box");
        this.setPrefWidth(200);
        this.setCache(true);
        this.setCacheHint(javafx.scene.CacheHint.SPEED);

        // \u00e2\u201d\u20ac\u00e2\u201d\u20ac IMAGE SECTION (StackPane allows overlay of price badge) \u00e2\u201d\u20ac\u00e2\u201d\u20ac
        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("product-image-container");
        imageContainer.setPrefHeight(150);
        imageContainer.setMinHeight(150);
        imageContainer.setMaxHeight(150);
        imageContainer.setAlignment(Pos.CENTER);

        // Clip din\u00c3\u00a1mico: se ajusta al ancho del contenedor y solo redondea arriba
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(imageContainer.widthProperty());
        clip.heightProperty().bind(imageContainer.heightProperty().add(30)); // Altura extra para que el redondeo
                                                                             // inferior no se vea
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        imageContainer.setClip(clip);

        StackPane imageDisplayContainer = new StackPane();
        imageDisplayContainer.getStyleClass().add("product-image-display");
        double margin = 8.0;
        StackPane.setMargin(imageDisplayContainer, new Insets(margin));
        imageDisplayContainer.getChildren().add(createPlaceholder());
        imageContainer.getChildren().add(imageDisplayContainer);

        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
            // Load image asynchronously to avoid blocking UI thread with file.exists()
            java.util.concurrent.CompletableFuture.supplyAsync(() -> resolveFile(product.getImagePath()))
                    .thenAcceptAsync(file -> {
                        if (file != null) {
                            String imageUrl = file.toURI().toString();
                            Platform.runLater(() -> {
                                imageDisplayContainer.getChildren().clear();
                                imageDisplayContainer.setStyle(
                                        "-fx-background-image: url(\"" + imageUrl + "\"); " +
                                                "-fx-background-size: contain; " +
                                                "-fx-background-position: center center; " +
                                                "-fx-background-repeat: no-repeat; " +
                                                "-fx-border-color: rgba(0,0,0,0.05); " +
                                                "-fx-border-width: 1px; " +
                                                "-fx-background-color: rgba(255,255,255,0.4);");
                            });
                        }
                    }, Platform::runLater);
        }

        // Price Badge (overlaid top-right on image)
        double displayPrice = product.getCurrentPrice();
        if (!pricesIncludeTax) {
            double rate = product.resolveEffectiveIva(globalTaxRate);
            displayPrice = product.getCurrentPrice() * (1 + (rate / 100.0));
        }

        Label priceBadge = new Label(String.format("%.2f\u20AC", displayPrice));
        priceBadge.getStyleClass().add("product-price-badge");
        StackPane.setAlignment(priceBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(priceBadge, new Insets(10, 10, 0, 0));
        imageContainer.getChildren().add(priceBadge);

        if (discountDesc != null && !discountDesc.isEmpty()) {
            Label discountBadge = new Label(discountDesc);
            discountBadge.getStyleClass().add("product-discount-badge");
            StackPane.setAlignment(discountBadge, Pos.TOP_LEFT);
            StackPane.setMargin(discountBadge, new Insets(10, 0, 0, 10));
            imageContainer.getChildren().add(discountBadge);
        }

        // \u00e2\u201d\u20ac\u00e2\u201d\u20ac INFO SECTION \u00e2\u201d\u20ac\u00e2\u201d\u20ac
        VBox infoBox = new VBox(4);
        infoBox.getStyleClass().add("product-info");
        infoBox.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(translateDynamic(product.getName()));
        nameLabel.getStyleClass().add("product-name");
        nameLabel.setWrapText(false);
        nameLabel.setMaxWidth(Double.MAX_VALUE);

        // Description (use category name as subtitle, or product description if
        // available)
        String descText = (product.getCategoryName() != null && !product.getCategoryName().isEmpty())
                ? translateDynamic(product.getCategoryName())
                : bundle.getString("product.no_category");
        Label descLabel = new Label(descText);
        descLabel.getStyleClass().add("product-description");
        descLabel.setWrapText(false);
        descLabel.setMaxWidth(Double.MAX_VALUE);

        infoBox.getChildren().addAll(nameLabel, descLabel);

        // Stock and SKU fields
        Label stockLabel = new Label(bundle.getString("product.stock") + product.getStockQuantity());
        if (product.getStockQuantity() <= 0) {
            stockLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-font-weight: bold;"); // Red
        } else {
            stockLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981; -fx-font-weight: bold;"); // Green
        }
        infoBox.getChildren().add(stockLabel);

        if (product.getSku() != null && !product.getSku().isEmpty()) {
            Label skuLabel = new Label(bundle.getString("product.sku") + product.getSku());
            skuLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
            infoBox.getChildren().add(skuLabel);
        }

        // \u00e2\u201d\u20ac\u00e2\u201d\u20ac ADD BUTTON \u00e2\u201d\u20ac\u00e2\u201d\u20ac
        FontAwesomeIconView addIcon = new FontAwesomeIconView();
        addIcon.setGlyphName("PLUS");
        addIcon.setSize("11");
        addIcon.getStyleClass().add("product-add-icon");

        Label addLabel = new Label(bundle.getString("product.add"));
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

    public ProductBox(Product product, Consumer<Product> onAddToCart, ResourceBundle bundle) {
        this(product, 21.0, true, null, bundle, onAddToCart);
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

    private String translateDynamic(String text) {
        if (text == null || text.isBlank()) return text;
        if (bundle != null && bundle.containsKey(text)) {
            return bundle.getString(text);
        }
        return text;
    }
}
