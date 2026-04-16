package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.domain.repository.ICompanyConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.mycompany.ventacontrolfx.domain.repository.IPriceRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class CartUseCaseTest {

    private ICompanyConfigRepository configRepository;
    private com.mycompany.ventacontrolfx.domain.service.TaxEngineService taxEngineService;
    private com.mycompany.ventacontrolfx.application.service.PromotionService promotionService;
    private com.mycompany.ventacontrolfx.application.service.PromotionEngine promotionEngine;
    private CartUseCase cartUseCase;

    @BeforeEach
    public void setUp() throws java.sql.SQLException {
        configRepository = Mockito.mock(ICompanyConfigRepository.class);
        taxEngineService = Mockito.mock(com.mycompany.ventacontrolfx.domain.service.TaxEngineService.class);
        promotionService = Mockito.mock(com.mycompany.ventacontrolfx.application.service.PromotionService.class);
        promotionEngine = Mockito.mock(com.mycompany.ventacontrolfx.application.service.PromotionEngine.class);
        when(promotionEngine.process(Mockito.any()))
                .thenReturn(new com.mycompany.ventacontrolfx.application.service.PromotionResult());

        SaleConfig config = new SaleConfig();
        config.setTaxRate(21.0); // 21% IVA
        config.setPricesIncludeTax(true);
        when(configRepository.load()).thenReturn(config);

        // Simple mock for calculateLine that handles the 21% IVA case for tests
        Mockito.lenient().when(taxEngineService.calculateLine(
                Mockito.any(), Mockito.any(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyBoolean()))
                .thenAnswer(invocation -> {
                    double price = invocation.getArgument(2);
                    double qty = invocation.getArgument(3);
                    boolean inclusive = invocation.getArgument(4);

                    double net, gross;
                    if (inclusive) {
                        gross = price * qty;
                        net = gross / 1.21;
                    } else {
                        net = price * qty;
                        gross = net * 1.21;
                    }

                    return new com.mycompany.ventacontrolfx.domain.model.TaxCalculationResult(
                            Math.round(net * 100.0) / 100.0,
                            Math.round(gross * 100.0) / 100.0,
                            new java.util.ArrayList<>());
                });

        // Mock PromotionService to return same price by default
        Mockito.lenient()
                .when(promotionService.applyPromotions(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyDouble()))
                .thenAnswer(invocation -> (Double) invocation.getArgument(2));

        com.mycompany.ventacontrolfx.domain.service.PriceResolutionService priceResolutionService = Mockito
                .mock(com.mycompany.ventacontrolfx.domain.service.PriceResolutionService.class);
        IPriceRepository priceRepository = Mockito.mock(IPriceRepository.class);
        com.mycompany.ventacontrolfx.domain.repository.IProductRepository productRepository = Mockito
                .mock(com.mycompany.ventacontrolfx.domain.repository.IProductRepository.class);

        cartUseCase = new CartUseCase(configRepository,
                priceResolutionService,
                taxEngineService,
                promotionService,
                promotionEngine,
                priceRepository,
                productRepository);
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
