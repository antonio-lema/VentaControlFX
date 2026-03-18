package com.mycompany.ventacontrolfx.application.service;

import com.mycompany.ventacontrolfx.application.usecase.ProductUseCase;
import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for ProductUseCase using Mockito.
 * Covers: getAllProducts, getVisibleProducts, saveProduct (create/update),
 * delete, toggleFavorite.
 */
public class ProductUseCaseTest {

    private IProductRepository mockRepo;
    private ProductUseCase useCase;

    @BeforeEach
    public void setUp() {
        mockRepo = Mockito.mock(IProductRepository.class);
        com.mycompany.ventacontrolfx.util.AuthorizationService dummyAuth = new com.mycompany.ventacontrolfx.util.AuthorizationService(
                new com.mycompany.ventacontrolfx.util.UserSession()) {
            @Override
            public void checkPermission(String code) {
            }

            @Override
            public boolean hasPermission(String code) {
                return true;
            }
        };
        useCase = new ProductUseCase(mockRepo, dummyAuth, null);
    }

    @Test
    public void testGetAllProducts() throws SQLException {
        Product p1 = new Product();
        p1.setId(1);
        p1.setName("Product A");
        when(mockRepo.getAll()).thenReturn(Arrays.asList(p1));

        List<Product> result = useCase.getAllProducts();

        assertEquals(1, result.size());
        assertEquals("Product A", result.get(0).getName());
        verify(mockRepo, times(1)).getAll();
    }

    @Test
    public void testGetVisibleProducts() throws SQLException {
        Product visible = new Product();
        visible.setId(1);
        visible.setName("Visible");
        when(mockRepo.getAllVisible()).thenReturn(Arrays.asList(visible));

        List<Product> result = useCase.getVisibleProducts();

        assertEquals(1, result.size());
        verify(mockRepo, times(1)).getAllVisible();
    }

    @Test
    public void testGetFavorites() throws SQLException {
        Product fav = new Product();
        fav.setId(2);
        fav.setName("Fav Product");
        fav.setFavorite(true);
        when(mockRepo.getFavorites()).thenReturn(Arrays.asList(fav));

        List<Product> result = useCase.getFavorites();

        assertEquals(1, result.size());
        assertTrue(result.get(0).isFavorite());
        verify(mockRepo).getFavorites();
    }

    @Test
    public void testSaveNewProduct() throws SQLException {
        Product newProduct = new Product();
        newProduct.setId(0); // ID 0 = new product
        newProduct.setName("Nuevo Producto");
        newProduct.setPrice(9.99);

        useCase.saveProduct(newProduct);

        verify(mockRepo, times(1)).save(newProduct);
        verify(mockRepo, never()).update(any());
    }

    @Test
    public void testUpdateExistingProduct() throws SQLException {
        Product existing = new Product();
        existing.setId(10); // existing ID
        existing.setName("Updated Product");
        existing.setPrice(19.99);

        useCase.saveProduct(existing);

        verify(mockRepo, times(1)).update(existing);
        verify(mockRepo, never()).save(any());
    }

    @Test
    public void testDeleteProduct() throws SQLException {
        useCase.deleteProduct(5);
        verify(mockRepo, times(1)).delete(5);
    }

    @Test
    public void testToggleFavorite() throws SQLException {
        useCase.toggleFavorite(3, true);
        verify(mockRepo, times(1)).updateFavorite(3, true);

        useCase.toggleFavorite(3, false);
        verify(mockRepo, times(1)).updateFavorite(3, false);
    }

    @Test
    public void testToggleVisibility() throws SQLException {
        useCase.toggleVisibility(7, false);
        verify(mockRepo, times(1)).updateVisibility(7, false);
    }

    @Test
    public void testGetAllProductsReturnsEmptyList() throws SQLException {
        when(mockRepo.getAll()).thenReturn(Collections.emptyList());

        List<Product> result = useCase.getAllProducts();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
