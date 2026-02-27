package com.mycompany.ventacontrolfx.component;

import com.mycompany.ventacontrolfx.domain.model.CartItem;
import com.mycompany.ventacontrolfx.domain.model.Product;
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
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;

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
        VBox infoBox = new VBox(4);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("cart-item-name");

        // Quantity Controls
        HBox quantityBox = new HBox(2);
        quantityBox.setAlignment(Pos.CENTER);
        quantityBox.getStyleClass().add("quantity-selector-pill");

        Button decreaseBtn = new Button();
        decreaseBtn.getStyleClass().add("quantity-btn");
        FontAwesomeIconView minusIcon = new FontAwesomeIconView(FontAwesomeIcon.MINUS);
        minusIcon.setSize("10");
        minusIcon.getStyleClass().add("cart-icon-minus");
        decreaseBtn.setGraphic(minusIcon);
        decreaseBtn.setOnAction(e -> {
            if (cartItem.getQuantity() > 1) {
                onDecrement.run();
            } else {
                onDelete.run();
            }
        });

        TextField quantityField = new TextField();
        quantityField.getStyleClass().add("quantity-field-modern");
        quantityField.setAlignment(Pos.CENTER);
        quantityField.setPrefWidth(35);
        quantityField.setText(String.valueOf(cartItem.getQuantity()));

        // COMMIT logic: when user presses Enter or focus leaves
        quantityField.setOnAction(e -> commitQuantity(quantityField, onSetQuantity, onDelete));
        quantityField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Focus lost
                commitQuantity(quantityField, onSetQuantity, onDelete);
            } else {
                javafx.application.Platform.runLater(quantityField::selectAll);
            }
        });

        // Sync field if quantity changed from elsewhere
        cartItem.quantityProperty().addListener((obs, oldVal, newVal) -> {
            if (!quantityField.isFocused()) {
                quantityField.setText(String.valueOf(newVal));
            }
        });

        Button increaseBtn = new Button();
        increaseBtn.getStyleClass().add("quantity-btn");
        FontAwesomeIconView plusIcon = new FontAwesomeIconView(FontAwesomeIcon.PLUS);
        plusIcon.setSize("10");
        plusIcon.getStyleClass().add("cart-icon-plus");
        increaseBtn.setGraphic(plusIcon);
        increaseBtn.setOnAction(e -> onIncrement.run());

        quantityBox.getChildren().addAll(decreaseBtn, quantityField, increaseBtn);
        infoBox.getChildren().addAll(nameLabel, quantityBox);

        // 3. Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 4. Right side
        StackPane rightSide = new StackPane();
        rightSide.setMinWidth(100);

        Label priceLabel = new Label(String.format("%.2f €", product.getPrice() * cartItem.getQuantity()));
        priceLabel.getStyleClass().add("cart-item-price");
        StackPane.setAlignment(priceLabel, Pos.CENTER_RIGHT);

        Button deleteBtn = new Button();
        deleteBtn.getStyleClass().add("cart-delete-btn-reveal");
        FontAwesomeIconView trashIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
        trashIcon.setSize("14");
        trashIcon.getStyleClass().add("icon-white");
        deleteBtn.setGraphic(trashIcon);
        StackPane.setAlignment(deleteBtn, Pos.CENTER_RIGHT);
        deleteBtn.setOpacity(0);
        deleteBtn.setTranslateX(20); // Start slightly outside
        deleteBtn.setOnAction(e -> onDelete.run());

        // Update price label when quantity changes
        cartItem.quantityProperty().addListener((obs, oldVal, newVal) -> {
            priceLabel.setText(String.format("%.2f €", product.getPrice() * newVal.intValue()));
        });

        rightSide.getChildren().addAll(deleteBtn, priceLabel);

        this.getChildren().addAll(imageNode, infoBox, spacer, rightSide);

        // Hover effects
        this.setOnMouseEntered(e -> {
            priceLabel.setTranslateX(-45);
            priceLabel.setOpacity(0.5);
            deleteBtn.setOpacity(1);
            deleteBtn.setTranslateX(0);
        });

        this.setOnMouseExited(e -> {
            priceLabel.setTranslateX(0);
            priceLabel.setOpacity(1.0);
            deleteBtn.setOpacity(0);
            deleteBtn.setTranslateX(20);
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

    private void commitQuantity(TextField field, Consumer<Integer> onSetQuantity, Runnable onDelete) {
        try {
            String text = field.getText();
            if (text == null || text.trim().isEmpty()) {
                field.setText(String.valueOf(cartItem.getQuantity()));
                return;
            }
            int newQty = Integer.parseInt(text);
            if (newQty <= 0) {
                onDelete.run();
            } else {
                onSetQuantity.accept(newQty);
                field.setText(String.valueOf(newQty));
            }
        } catch (NumberFormatException e) {
            field.setText(String.valueOf(cartItem.getQuantity()));
        }
    }
}
