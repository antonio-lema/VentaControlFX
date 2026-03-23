package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.CartItem;
import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.repository.ISaleRepository;
import com.mycompany.ventacontrolfx.domain.repository.ICompanyConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SaleUseCaseTest {

    private ISaleRepository saleRepository;
    private ICompanyConfigRepository configRepository;
    private com.mycompany.ventacontrolfx.domain.service.TaxEngineService taxEngineService;
    private com.mycompany.ventacontrolfx.domain.repository.IClientRepository clientRepository;
    private com.mycompany.ventacontrolfx.application.service.PromotionService promotionService;
    private com.mycompany.ventacontrolfx.application.service.PromotionEngine promotionEngine;
    private com.mycompany.ventacontrolfx.domain.repository.IProductRepository productRepository;
    private SaleUseCase saleUseCase;

    @BeforeEach
    public void setUp() throws Exception {
        saleRepository = mock(ISaleRepository.class);
        configRepository = mock(ICompanyConfigRepository.class);

        taxEngineService = mock(com.mycompany.ventacontrolfx.domain.service.TaxEngineService.class);
        clientRepository = mock(com.mycompany.ventacontrolfx.domain.repository.IClientRepository.class);
        promotionService = mock(com.mycompany.ventacontrolfx.application.service.PromotionService.class);
        promotionEngine = mock(com.mycompany.ventacontrolfx.application.service.PromotionEngine.class);
        productRepository = mock(com.mycompany.ventacontrolfx.domain.repository.IProductRepository.class);
        when(promotionEngine.process(any()))
                .thenReturn(new com.mycompany.ventacontrolfx.application.service.PromotionResult());

        // Mock TaxEngineService to return a basic result
        com.mycompany.ventacontrolfx.domain.model.TaxCalculationResult mockResult = new com.mycompany.ventacontrolfx.domain.model.TaxCalculationResult(
                100.0, 121.0, new ArrayList<>());
        when(taxEngineService.calculateLine(any(), any(), anyDouble(), anyInt(), anyBoolean())).thenReturn(mockResult);

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
        saleUseCase = new SaleUseCase(saleRepository, configRepository, dummyAuth, taxEngineService, clientRepository,
                promotionService, promotionEngine, productRepository, null);
    }

    @Test
    public void testProcessSaleCalculatesIVAAndSaves() throws SQLException {
        List<CartItem> items = new ArrayList<>();
        Product p = new Product();
        p.setId(101);
        p.setPrice(121.0);
        items.add(new CartItem(p, 1));

        double total = 121.0;
        String method = "EFECTIVO";
        int userId = 1;

        saleUseCase.processSale(items, total, method, null, userId);

        ArgumentCaptor<Sale> saleCaptor = ArgumentCaptor.forClass(Sale.class);
        verify(saleRepository).saveSale(saleCaptor.capture());

        Sale savedSale = saleCaptor.getValue();
        assertEquals(121.0, savedSale.getTotal());
        assertEquals(21.0, savedSale.getIva(), 0.001); // 121 - (121/1.21) = 21
        assertEquals(method, savedSale.getPaymentMethod());
        assertEquals(1, savedSale.getDetails().size());
        assertEquals(101, savedSale.getDetails().get(0).getProductId());
    }

    @Test
    public void testGetHistoryDelegatesToRepository() throws SQLException {
        saleUseCase.getHistory(null, null);
        verify(saleRepository).getByRange(null, null);
    }
}
