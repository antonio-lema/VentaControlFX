package com.mycompany.ventacontrolfx.presentation.component;

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

    private final double globalTaxRate;
    private final boolean pricesIncludeTax;

    private final java.util.ResourceBundle bundle;

    public CartItemRow(CartItem cartItem, double globalTaxRate, boolean pricesIncludeTax,
            java.util.ResourceBundle bundle,
            Runnable onIncrement, Runnable onDecrement, Runnable onDelete,
            Consumer<Integer> onSetQuantity, Runnable onEdit) {
        this.cartItem = cartItem;
        this.bundle = bundle;
        this.globalTaxRate = globalTaxRate;
        this.pricesIncludeTax = pricesIncludeTax;
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

        Label nameLabel = new Label(translateDynamic(product.getName()));
        nameLabel.getStyleClass().add("cart-item-name");

        Label observationLabel = new Label();
        observationLabel.getStyleClass().add("cart-item-observation");
        observationLabel.setStyle("-fx-text-fill: #2196f3; -fx-font-size: 11px; -fx-font-style: italic;");
        observationLabel.textProperty().bind(cartItem.observationsProperty());
        observationLabel.visibleProperty().bind(cartItem.observationsProperty().isNotEmpty());
        observationLabel.managedProperty().bind(observationLabel.visibleProperty());

        // Quantity Controls
        HBox quantityBox = new HBox(2);
        quantityBox.setAlignment(Pos.CENTER);
        quantityBox.getStyleClass().add("quantity-selector-pill");

        Button decreaseBtn = new Button();
        decreaseBtn.getStyleClass().addAll("quantity-btn", "quantity-btn-minus");
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
        increaseBtn.getStyleClass().addAll("quantity-btn", "quantity-btn-plus");
        FontAwesomeIconView plusIcon = new FontAwesomeIconView(FontAwesomeIcon.PLUS);
        plusIcon.setSize("10");
        plusIcon.getStyleClass().add("cart-icon-plus");
        increaseBtn.setGraphic(plusIcon);
        increaseBtn.setOnAction(e -> onIncrement.run());

        quantityBox.getChildren().addAll(decreaseBtn, quantityField, increaseBtn);
        infoBox.getChildren().addAll(nameLabel, observationLabel, quantityBox);

        // 3. Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 4. Right side
        StackPane rightSide = new StackPane();
        rightSide.setMinWidth(100);

        double taxRate = product.resolveEffectiveIva(globalTaxRate);
        double taxMultiplier = pricesIncludeTax ? 1.0 : (1 + (taxRate / 100.0));

        // Calculamos el precio final (con IVA incluido si procede) de forma reactiva
        VBox priceContainer = new VBox(0);
        priceContainer.setAlignment(Pos.CENTER_RIGHT);
        StackPane.setAlignment(priceContainer, Pos.CENTER_RIGHT);

        Label priceLabel = new Label();
        priceLabel.getStyleClass().add("cart-item-price-original"); // Old price style

        Label discountLabel = new Label();
        discountLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
        discountLabel.setVisible(false);
        discountLabel.setManaged(false);

        Label finalPriceLabel = new Label();
        finalPriceLabel.getStyleClass().add("cart-item-price"); // Modern price style

        // Este Runnable actualiza la etiqueta cuando cambie precio, cantidad o
        // descuento
        Runnable refreshPrice = () -> {
            double originalUnitPrice = cartItem.getUnitPrice();
            double originalLineTotal = (originalUnitPrice * cartItem.getQuantity()) * taxMultiplier;
            double discountValueToSubtract = cartItem.getDiscountAmount() * taxMultiplier;
            double finalLineTotal = Math.max(0, originalLineTotal - discountValueToSubtract);

            if (discountValueToSubtract > 0) {
                // Show original price small and crossed out (or just muted)
                priceLabel.setText(String.format("%.2f \u20ac", originalLineTotal));
                priceLabel.setVisible(true);
                priceLabel.setManaged(true);
                priceLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #999;");

                discountLabel.setText(String.format("-%.2f \u20ac", discountValueToSubtract));
                discountLabel.setVisible(true);
                discountLabel.setManaged(true);

                finalPriceLabel.setText(String.format("%.2f \u20ac", finalLineTotal));
            } else {
                priceLabel.setVisible(false);
                priceLabel.setManaged(false);
                discountLabel.setVisible(false);
                discountLabel.setManaged(false);
                finalPriceLabel.setText(String.format("%.2f \u20ac", originalLineTotal));
            }
        };
        refreshPrice.run(); // Pintar el valor inicial

        // Cuando cambia el precio (por cambio de tarifa)
        cartItem.unitPriceProperty().addListener((obs, oldVal, newVal) -> refreshPrice.run());

        // Cuando cambia la cantidad
        cartItem.quantityProperty().addListener((obs, oldVal, newVal) -> refreshPrice.run());

        // Cuando cambia el descuento (por promociones)
        cartItem.discountAmountProperty().addListener((obs, oldVal, newVal) -> refreshPrice.run());

        priceContainer.getChildren().addAll(priceLabel, discountLabel, finalPriceLabel);

        Button editBtn = new Button();
        editBtn.getStyleClass().add("cart-edit-btn-reveal"); // New CSS class needed or reuse delete styles
        FontAwesomeIconView editIcon = new FontAwesomeIconView(FontAwesomeIcon.PENCIL);
        editIcon.setSize("14");
        editIcon.getStyleClass().add("icon-white");
        editBtn.setGraphic(editIcon);
        StackPane.setAlignment(editBtn, Pos.CENTER_RIGHT);
        editBtn.setOpacity(0);
        editBtn.setTranslateX(20);
        editBtn.setOnAction(e -> onEdit.run());

        Button deleteBtn = new Button();
        deleteBtn.getStyleClass().add("cart-delete-btn-reveal");
        FontAwesomeIconView trashIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH_ALT);
        trashIcon.setSize("16");
        trashIcon.getStyleClass().add("icon-white");
        deleteBtn.setGraphic(trashIcon);
        StackPane.setAlignment(deleteBtn, Pos.CENTER_RIGHT);
        deleteBtn.setOpacity(0);
        deleteBtn.setTranslateX(20);
        deleteBtn.setOnAction(e -> onDelete.run());

        rightSide.getChildren().addAll(editBtn, deleteBtn, priceContainer);

        this.getChildren().addAll(imageNode, infoBox, spacer, rightSide);

        // Hover effects
        this.setOnMouseEntered(e -> {
            priceContainer.setTranslateX(-85); // Shift more to fit two buttons
            priceContainer.setOpacity(0.3);

            editBtn.setOpacity(1);
            editBtn.setTranslateX(-40); // Standard position for edit

            deleteBtn.setOpacity(1);
            deleteBtn.setTranslateX(0); // Standard position for delete
        });

        this.setOnMouseExited(e -> {
            priceContainer.setTranslateX(0);
            priceContainer.setOpacity(1.0);

            editBtn.setOpacity(0);
            editBtn.setTranslateX(20);

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

    private String translateDynamic(String text) {
        if (text == null || text.isBlank())
            return text;
        if (bundle != null && bundle.containsKey(text)) {
            return bundle.getString(text);
        }
        return text;
    }
}


