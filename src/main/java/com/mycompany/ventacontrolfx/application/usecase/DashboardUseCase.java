package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.repository.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DashboardUseCase {
    private final IProductRepository productRepo;
    private final ICategoryRepository categoryRepo;
    private final ISaleRepository saleRepo;
    private final ICashClosureRepository closureRepo;
    private final IClientRepository clientRepo;
    private final IUserRepository userRepo;

    public DashboardUseCase(IProductRepository productRepo, ICategoryRepository categoryRepo,
            ISaleRepository saleRepo, ICashClosureRepository closureRepo,
            IClientRepository clientRepo, IUserRepository userRepo) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.saleRepo = saleRepo;
        this.closureRepo = closureRepo;
        this.clientRepo = clientRepo;
        this.userRepo = userRepo;
    }

    public Map<String, Integer> getStats() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("products", productRepo.count());
        stats.put("categories", categoryRepo.count());
        stats.put("sales", saleRepo.count());
        stats.put("closures", closureRepo.count());
        stats.put("clients", clientRepo.count());
        stats.put("users", userRepo.count());
        return stats;
    }

    public javafx.concurrent.Task<Map<String, Integer>> getAllCountsTask() {
        return new javafx.concurrent.Task<>() {
            @Override
            protected Map<String, Integer> call() throws Exception {
                return getStats();
            }
        };
    }

    public javafx.concurrent.Task<java.util.List<com.mycompany.ventacontrolfx.domain.model.Product>> getLowStockProductsTask() {
        return new javafx.concurrent.Task<>() {
            @Override
            protected java.util.List<com.mycompany.ventacontrolfx.domain.model.Product> call() throws Exception {
                return productRepo.getLowStock();
            }
        };
    }
}
