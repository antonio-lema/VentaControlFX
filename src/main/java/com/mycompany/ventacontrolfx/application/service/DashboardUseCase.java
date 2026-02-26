package com.mycompany.ventacontrolfx.application.service;

import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import com.mycompany.ventacontrolfx.service.*;
import javafx.concurrent.Task;
import java.util.Map;
import java.util.HashMap;

/**
 * Aggregates statistics for the dashboard.
 * Clean Architecture compliant: Uses repositories, not UI services.
 */
public class DashboardUseCase {
    private final IProductRepository productRepository;
    private final CategoryService categoryService;
    private final SaleService saleService;
    private final CashClosureService closureService;
    private final ClientService clientService;
    private final UserService userService;

    public DashboardUseCase(IProductRepository productRepository,
            CategoryService categoryService,
            SaleService saleService,
            CashClosureService closureService,
            ClientService clientService,
            UserService userService) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.saleService = saleService;
        this.closureService = closureService;
        this.clientService = clientService;
        this.userService = userService;
    }

    public Task<Map<String, Integer>> getAllCountsTask() {
        return new Task<>() {
            @Override
            protected Map<String, Integer> call() throws Exception {
                Map<String, Integer> counts = new HashMap<>();
                counts.put("products", productRepository.countTotal());
                counts.put("categories", categoryService.getCount());
                counts.put("sales", saleService.getTotalCount());
                counts.put("closures", closureService.getCount());
                counts.put("clients", clientService.getCount());
                counts.put("users", userService.getCount());
                return counts;
            }
        };
    }
}
