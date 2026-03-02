package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.CartItem;
import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.repository.ISaleRepository;
import com.mycompany.ventacontrolfx.domain.repository.ICompanyConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SaleUseCaseTest {

    private ISaleRepository saleRepository;
    private ICompanyConfigRepository configRepository;
    private SaleUseCase saleUseCase;

    @BeforeEach
    public void setUp() {
        saleRepository = mock(ISaleRepository.class);
        configRepository = mock(ICompanyConfigRepository.class);

        SaleConfig config = new SaleConfig();
        config.setTaxRate(21.0);
        when(configRepository.load()).thenReturn(config);

        saleUseCase = new SaleUseCase(saleRepository, configRepository);
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
