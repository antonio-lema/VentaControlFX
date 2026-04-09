package com.mycompany.ventacontrolfx.domain.service;

import com.mycompany.ventacontrolfx.domain.model.Product;
import java.util.ArrayList;
import java.util.List;

/**
 * Enterprise Product Validator.
 * Separates business rules from UI logic.
 */
public class ProductValidator {

    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();

        public void addError(String msg) {
            errors.add(msg);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    public ValidationResult validate(Product product) {
        ValidationResult result = new ValidationResult();

        if (product.getName() == null || product.getName().trim().isBlank()) {
            result.addError("El nombre del producto es obligatorio.");
        }

        if (product.getPrice() < 0) {
            result.addError("El precio no puede ser negativo.");
        }

        if (product.getCategoryId() <= 0) {
            result.addError("Debe seleccionar una categor\u00c3\u00ada v\u00c3\u00a1lida.");
        }

        return result;
    }
}
