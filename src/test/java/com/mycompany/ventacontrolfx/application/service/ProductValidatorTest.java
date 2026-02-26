package com.mycompany.ventacontrolfx.application.service;

import com.mycompany.ventacontrolfx.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for ProductValidator.
 */
public class ProductValidatorTest {
    private ProductValidator validator;

    @BeforeEach
    public void setUp() {
        validator = new ProductValidator();
    }

    @Test
    public void testValidProduct() {
        Product p = new Product(1, "Test Product", 10.5, true);
        ProductValidator.ValidationResult result = validator.validate(p);
        assertTrue(result.isValid(), "Product should be valid");
    }

    @Test
    public void testEmptyName() {
        Product p = new Product(1, " ", 10.5, true);
        ProductValidator.ValidationResult result = validator.validate(p);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("El nombre del producto es obligatorio."));
    }

    @Test
    public void testNegativePrice() {
        Product p = new Product(1, "Product", -1.0, true);
        ProductValidator.ValidationResult result = validator.validate(p);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("El precio no puede ser negativo."));
    }

    @Test
    public void testInvalidCategory() {
        Product p = new Product(0, "Product", 10.0, true);
        ProductValidator.ValidationResult result = validator.validate(p);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Debe seleccionar una categoría válida."));
    }
}
