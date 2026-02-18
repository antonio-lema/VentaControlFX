package com.mycompany.ventacontrolfx.component;

import com.mycompany.ventacontrolfx.model.CartItem;
import com.mycompany.ventacontrolfx.model.Product;
import java.io.File;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import java.util.function.Consumer;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class CartItemRow extends HBox {

    private final CartItem cartItem;

    public CartItemRow(CartItem cartItem, Runnable onIncrement, Runnable onDecrement, Runnable onDelete,
            Consumer<Integer> onSetQuantity) {
        this.cartItem = cartItem;
        Product product = cartItem.getProduct();

        this.getStyleClass().add("cart-item");
        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(new Insets(15));
        this.setSpacing(15);

        // 1. Image
        Node imageNode = createProductImage(product);

        // 2. Info
        VBox infoBox = new VBox(8);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("cart-product-name");
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Quantity Controls
        HBox quantityBox = new HBox(5); // Reduce spacing
        quantityBox.setAlignment(Pos.CENTER_LEFT);

        Button decreaseBtn = new Button("-");
        decreaseBtn.getStyleClass().add("quantity-btn");
        decreaseBtn.setStyle("-fx-min-width: 30px; -fx-min-height: 30px; -fx-font-size: 14px;");
        decreaseBtn.setOnAction(e -> onDecrement.run());

        TextField quantityField = new TextField();
        quantityField.getStyleClass().add("quantity-field");
        quantityField.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-alignment: center; -fx-pref-width: 50px; -fx-min-width: 40px;");

        // Validation: Only numbers
        quantityField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                quantityField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        // Initialize value
        quantityField.setText(String.valueOf(cartItem.getQuantity()));

        // Listener: Update Cart when User changes text (and commits via Focus Loss or
        // Enter)
        quantityField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                commitQuantity(quantityField, onSetQuantity);
            }
        });
        quantityField.setOnAction(e -> {
            commitQuantity(quantityField, onSetQuantity);
            quantityField.getParent().requestFocus(); // Remove focus
        });

        // Listener: Update Text when Cart changes (e.g. via buttons)
        cartItem.quantityProperty().addListener((obs, oldVal, newVal) -> {
            if (!quantityField.isFocused()) {
                quantityField.setText(String.valueOf(newVal));
            }
        });

        Button increaseBtn = new Button("+");
        increaseBtn.getStyleClass().add("quantity-btn");
        increaseBtn.setStyle("-fx-min-width: 30px; -fx-min-height: 30px; -fx-font-size: 14px;");
        increaseBtn.setOnAction(e -> onIncrement.run());

        quantityBox.getChildren().addAll(decreaseBtn, quantityField, increaseBtn);
        infoBox.getChildren().addAll(nameLabel, quantityBox);

        // 3. Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 4. Right side
        StackPane rightSide = new StackPane();
        rightSide.setMinWidth(120);

        Label priceLabel = new Label(String.format("%.2f €", product.getPrice()));
        priceLabel.getStyleClass().add("cart-product-price");
        priceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        StackPane.setAlignment(priceLabel, Pos.CENTER_RIGHT);

        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("cart-delete-btn");
        deleteBtn.setStyle("-fx-font-size: 16px; -fx-padding: 8 12;");
        StackPane.setAlignment(deleteBtn, Pos.CENTER_RIGHT);
        deleteBtn.setOpacity(0);
        deleteBtn.setOnAction(e -> onDelete.run());

        rightSide.getChildren().addAll(deleteBtn, priceLabel);

        this.getChildren().addAll(imageNode, infoBox, spacer, rightSide);

        // Hover effects
        this.setOnMouseEntered(e -> {
            priceLabel.setTranslateX(-50);
            deleteBtn.setOpacity(1);
        });

        this.setOnMouseExited(e -> {
            priceLabel.setTranslateX(0);
            deleteBtn.setOpacity(0);
        });
    }

    private Node createProductImage(Product product) {
        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
            File file = new File(product.getImagePath());
            if (file.exists()) {
                ImageView iv = new ImageView(new Image(file.toURI().toString()));
                iv.setFitHeight(70);
                iv.setFitWidth(70);
                iv.setPreserveRatio(false);
                Circle clip = new Circle(35, 35, 35);
                iv.setClip(clip);
                iv.getStyleClass().add("cart-product-image");
                return iv;
            }
        }
        Circle placeholder = new Circle(35, Color.LIGHTGRAY);
        placeholder.getStyleClass().add("cart-product-image");
        return placeholder;
    }

    public CartItem getCartItem() {
        return cartItem;
    }

    private void commitQuantity(TextField field, Consumer<Integer> onSetQuantity) {
        try {
            String text = field.getText();
            if (text == null || text.trim().isEmpty()) {
                // Revert to current quantity if empty
                field.setText(String.valueOf(cartItem.getQuantity()));
                return;
            }
            int newQty = Integer.parseInt(text);
            if (newQty < 1) {
                newQty = 1;
                field.setText("1");
            }
            onSetQuantity.accept(newQty);
        } catch (NumberFormatException e) {
            // Revert on error
            field.setText(String.valueOf(cartItem.getQuantity()));
        }
    }
}
