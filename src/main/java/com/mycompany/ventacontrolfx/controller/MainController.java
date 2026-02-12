package com.mycompany.ventacontrolfx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

import java.util.ArrayList;
import java.util.List;

public class MainController {

    @FXML
    private TilePane productsPane;

    @FXML
    private VBox cartItemsContainer;

    @FXML
    private VBox emptyCartView;

    @FXML
    private Label subtotalLabel;

    @FXML
    private Label itemsCountLabel;

    @FXML
    private Label totalButtonLabel;

    private List<Product> products = new ArrayList<>();
    private double subtotal = 0.0;
    private int itemCount = 0;

    public static class Product {
        String name;
        double price;
        String imageUrl; // Placeholder for now

        public Product(String name, double price, String imageUrl) {
            this.name = name;
            this.price = price;
            this.imageUrl = imageUrl;
        }
    }

    @FXML
    public void initialize() {
        // Load sample data
        List<Product> sampleProducts = new ArrayList<>();
        sampleProducts.add(new Product("Vela", 3.99, ""));
        sampleProducts.add(new Product("Tazón de cerámica", 9.95, ""));
        sampleProducts.add(new Product("Taza blanca cerám...", 12.90, ""));
        sampleProducts.add(new Product("Maceta", 19.99, ""));
        sampleProducts.add(new Product("Camiseta para ho...", 29.50, ""));
        sampleProducts.add(new Product("Gorra bandera", 25.00, ""));
        sampleProducts.add(new Product("Auriculares estéreo", 69.00, ""));
        sampleProducts.add(new Product("Silla blanca", 49.50, ""));
        sampleProducts.add(new Product("Dados", 5.00, ""));
        sampleProducts.add(new Product("Camisa manga lar...", 19.95, ""));
        sampleProducts.add(new Product("Botella de acero", 25.46, ""));

        loadProducts(sampleProducts);

        updateCartState();
    }

    public void loadProducts(List<Product> newProducts) {
        products.clear();
        products.addAll(newProducts);

        productsPane.getChildren().clear();
        for (Product p : products) {
            VBox productBox = createProductBox(p);
            productsPane.getChildren().add(productBox);
        }
    }

    private VBox createProductBox(Product product) {
        VBox box = new VBox();
        box.getStyleClass().add("product-box");
        box.setPrefWidth(180);
        box.setPrefHeight(220); // Card height

        // Image Container
        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("product-image-container");
        imageContainer.setPrefHeight(140);

        // Placeholder Image (Gray Rectangle)
        Rectangle rect = new Rectangle(120, 100, Color.LIGHTGRAY);
        rect.setArcWidth(5);
        rect.setArcHeight(5);

        imageContainer.getChildren().add(rect);

        // Info Container
        VBox infoBox = new VBox(5);
        infoBox.getStyleClass().add("product-info");

        Label nameLabel = new Label(product.name);
        nameLabel.getStyleClass().add("product-name");
        nameLabel.setWrapText(true);

        Label priceLabel = new Label(String.format("%.2f €", product.price));
        priceLabel.getStyleClass().add("product-price");

        infoBox.getChildren().addAll(nameLabel, priceLabel);

        box.getChildren().addAll(imageContainer, infoBox);

        box.setOnMouseClicked((MouseEvent e) -> addToCart(product));

        return box;
    }

    private void addToCart(Product product) {
        // Add to logic list and view
        subtotal += product.price;
        itemCount++;

        createCartItemRow(product);
        updateCartState();
    }

    private void createCartItemRow(Product product) {
        HBox row = new HBox(15);
        row.getStyleClass().add("cart-item");
        row.getStyleClass().add("cart-item-selected"); // Default style
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 10, 10, 0));

        // 1. Indicator
        Rectangle indicator = new Rectangle(4, 40, Color.web("#039be5"));

        // 2. Image
        Circle circle = new Circle(20, Color.LIGHTGRAY);

        // 3. Name
        Label nameLabel = new Label(product.name);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");

        // 4. Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 5. Price
        Label priceLabel = new Label(String.format("%.2f €", product.price));
        priceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");

        // 6. Delete Button (Trash Can) - Initially Hidden
        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle(
                "-fx-text-fill: #ff5274; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-color: transparent; -fx-font-weight: bold;");
        deleteBtn.setVisible(false);

        // Logic to remove item
        deleteBtn.setOnAction(e -> {
            cartItemsContainer.getChildren().remove(row);
            subtotal -= product.price;
            itemCount--;
            if (subtotal < 0)
                subtotal = 0; // Precision safety
            updateCartState();
        });

        // Hover listeners
        row.setOnMouseEntered(e -> deleteBtn.setVisible(true));
        row.setOnMouseExited(e -> deleteBtn.setVisible(false));

        row.getChildren().addAll(indicator, circle, nameLabel, spacer, priceLabel, deleteBtn);

        cartItemsContainer.getChildren().add(row);
    }

    private void updateCartState() {
        if (itemCount > 0) {
            cartItemsContainer.setVisible(true);
            emptyCartView.setVisible(false);
        } else {
            cartItemsContainer.setVisible(false);
            emptyCartView.setVisible(true);
        }

        String totalText = String.format("%.2f €", subtotal);
        subtotalLabel.setText(totalText);
        totalButtonLabel.setText(totalText);
        itemsCountLabel.setText("Subtotal (" + itemCount + " item" + (itemCount != 1 ? "s" : "") + ")");
    }
}
