package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.CartItem;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

public class EditCartItemController implements Injectable {

    @FXML
    private Label lblProductName, lblDiscountHint;
    @FXML
    private TextField txtUnitPrice, txtDiscount;
    @FXML
    private ToggleButton toggleDiscountType;
    @FXML
    private javafx.scene.control.TextArea txtObservations;

    private CartItem cartItem;
    private Runnable onSaveCallback;
    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
    }

    public void setData(CartItem item, Runnable onSave) {
        this.cartItem = item;
        this.onSaveCallback = onSave;

        lblProductName.setText(item.getProduct().getName());
        txtUnitPrice.setText(String.format("%.2f", item.getUnitPrice()).replace(",", "."));

        // Default to fixed discount amount already in item
        txtDiscount.setText(String.format("%.2f", item.getManualDiscountAmount()).replace(",", "."));
        txtObservations.setText(item.getObservations());

        setupListeners();
    }

    private void setupListeners() {
        toggleDiscountType.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                toggleDiscountType.setText("€");
                lblDiscountHint.setText(container != null ? container.getBundle().getString("cart.edit.discount.hint.fixed") : "Descuento en valor absoluto (€)");
            } else {
                toggleDiscountType.setText("%");
                lblDiscountHint.setText(container != null ? container.getBundle().getString("cart.edit.discount.hint.percent") : "Descuento en porcentaje (%)");
            }
        });
    }

    @FXML
    private void handleSave() {
        try {
            double newPrice = Double.parseDouble(txtUnitPrice.getText().replace(",", "."));
            double discountValue = Double.parseDouble(txtDiscount.getText().replace(",", "."));

            double finalDiscountAmount;
            if (toggleDiscountType.isSelected()) {
                // Fixed amount
                finalDiscountAmount = discountValue;
            } else {
                // Percentage
                finalDiscountAmount = (newPrice * cartItem.getQuantity()) * (discountValue / 100.0);
            }

            cartItem.updateUnitPrice(newPrice);
            cartItem.setManualDiscountAmount(finalDiscountAmount);
            cartItem.setObservations(txtObservations.getText());

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            close();
        } catch (NumberFormatException e) {
            // Simple validation
            txtUnitPrice.getStyleClass().add("error");
            txtDiscount.getStyleClass().add("error");
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) lblProductName.getScene().getWindow();
        stage.close();
    }
}
