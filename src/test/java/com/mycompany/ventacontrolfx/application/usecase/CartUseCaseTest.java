package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.domain.repository.ICompanyConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class CartUseCaseTest {

    private ICompanyConfigRepository configRepository;
    private CartUseCase cartUseCase;

    @BeforeEach
    public void setUp() {
        configRepository = Mockito.mock(ICompanyConfigRepository.class);
        SaleConfig config = new SaleConfig();
        config.setTaxRate(21.0); // 21% IVA
        when(configRepository.load()).thenReturn(config);

        cartUseCase = new CartUseCase(configRepository);
    }

    @Test
    public void testCalculateTotalsWith21IVA() {
        Product p1 = new Product();
        p1.setId(1);
        p1.setName("Test Product");
        p1.setPrice(121.0); // price including tax

        cartUseCase.addItem(p1);

        // Grand total should be 121
        assertEquals(121.0, cartUseCase.getGrandTotal(), 0.001);

        // Subtotal = 121 / 1.21 = 100
        assertEquals(100.0, cartUseCase.subtotalProperty().get(), 0.001);

        // Tax = 121 - 100 = 21
        assertEquals(21.0, cartUseCase.taxProperty().get(), 0.001);
    }

    @Test
    public void testIncrementDecrement() {
        Product p1 = new Product();
        p1.setId(1);
        p1.setName("Test Product");
        p1.setPrice(10.0);

        cartUseCase.addItem(p1);
        assertEquals(1, cartUseCase.getItemCount());

        cartUseCase.incrementQuantity(p1);
        assertEquals(2, cartUseCase.getItemCount());
        assertEquals(20.0, cartUseCase.getGrandTotal());

        cartUseCase.decrementQuantity(p1);
        assertEquals(1, cartUseCase.getItemCount());

        cartUseCase.decrementQuantity(p1);
        assertEquals(0, cartUseCase.getItemCount());
    }
}
