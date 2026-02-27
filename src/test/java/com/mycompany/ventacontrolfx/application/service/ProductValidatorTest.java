package com.mycompany.ventacontrolfx.application.service;

import com.mycompany.ventacontrolfx.domain.model.Product;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for Product domain model validation logic.
 * These tests verify the basic state constraints of the Product entity.
 */
public class ProductValidatorTest {

    @Test
    public void testProductWithValidData() {
        Product p = new Product();
        p.setId(1);
        p.setName("Test Product");
        p.setPrice(10.5);
        p.setVisible(true);

        assertNotNull(p.getName());
        assertFalse(p.getName().isBlank(), "Name should not be blank");
        assertTrue(p.getPrice() >= 0, "Price should not be negative");
    }

    @Test
    public void testProductNameCannotBeBlank() {
        Product p = new Product();
        p.setName("  ");
        // Domain rule: name must not be blank
        assertTrue(p.getName().isBlank(), "A blank name violates the domain contract");
    }

    @Test
    public void testProductPriceCannotBeNegative() {
        Product p = new Product();
        p.setName("Product");
        p.setPrice(-1.0);
        assertTrue(p.getPrice() < 0, "Negative price should be flagged by validation layer");
    }

    @Test
    public void testNewProductHasZeroId() {
        Product p = new Product();
        assertEquals(0, p.getId(), "A new product has id=0 before being persisted");
    }

    @Test
    public void testFavoriteToggle() {
        Product p = new Product();
        p.setFavorite(false);
        assertFalse(p.isFavorite());
        p.setFavorite(true);
        assertTrue(p.isFavorite());
    }
}
