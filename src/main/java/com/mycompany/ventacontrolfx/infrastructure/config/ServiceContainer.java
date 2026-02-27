package com.mycompany.ventacontrolfx.infrastructure.config;

import com.mycompany.ventacontrolfx.application.usecase.*;
import com.mycompany.ventacontrolfx.domain.repository.*;
import com.mycompany.ventacontrolfx.infrastructure.persistence.*;
import com.mycompany.ventacontrolfx.infrastructure.email.SmtpEmailAdapter;
import com.mycompany.ventacontrolfx.shared.bus.GlobalEventBus;
import com.mycompany.ventacontrolfx.shared.async.AsyncManager;
import com.mycompany.ventacontrolfx.util.UserSession;
import com.mycompany.ventacontrolfx.util.AuthorizationService;
import com.mycompany.ventacontrolfx.infrastructure.navigation.NavigationService;
// CartService removed, replaced by CartUseCase

/**
 * Enterprise Service Registry (Dependency Injection Container).
 * Clean Architecture Implementation.
 */
public class ServiceContainer {

    // Interfaces (Ports)
    private final IProductRepository productRepository;
    private final ICategoryRepository categoryRepository;
    private final IClientRepository clientRepository;
    private final ISaleRepository saleRepository;
    private final IUserRepository userRepository;
    private final ICashClosureRepository closureRepository;
    private final ICompanyConfigRepository configRepository;
    private final IEmailSender emailSender;

    // Use Cases (Application Layer)
    private final ProductUseCase productUseCase;
    private final CategoryUseCase categoryUseCase;
    private final ClientUseCase clientUseCase;
    private final SaleUseCase saleUseCase;
    private final UserUseCase userUseCase;
    private final CashClosureUseCase closureUseCase;
    private final ConfigUseCase configUseCase;
    private final DashboardUseCase dashboardUseCase;

    // Shared Components
    private final GlobalEventBus eventBus;
    private final AsyncManager asyncManager;
    private final UserSession userSession;
    private final AuthorizationService authService;

    // State / Use Cases
    private final CartUseCase cartUseCase;
    private NavigationService navigationService;

    public ServiceContainer() {
        // 1. Shared Infrastructure
        this.eventBus = new GlobalEventBus();
        this.asyncManager = new AsyncManager();
        this.userSession = new UserSession();
        this.authService = new AuthorizationService(userSession);
        // Initialize CartUseCase as a singleton for the session
        this.cartUseCase = new CartUseCase(new JdbcCompanyConfigRepository());

        // 2. Adapters (Infrastructure Layer)
        this.productRepository = new JdbcProductRepository();
        this.categoryRepository = new JdbcCategoryRepository();
        this.clientRepository = new JdbcClientRepository();
        this.saleRepository = new JdbcSaleRepository();
        this.userRepository = new JdbcUserRepository();
        this.closureRepository = new JdbcCashClosureRepository();
        this.configRepository = new JdbcCompanyConfigRepository();
        this.emailSender = new SmtpEmailAdapter();

        // 3. Wiring Use Cases (Application Layer)
        this.productUseCase = new ProductUseCase(productRepository);
        this.categoryUseCase = new CategoryUseCase(categoryRepository);
        this.clientUseCase = new ClientUseCase(clientRepository);
        this.saleUseCase = new SaleUseCase(saleRepository, configRepository);
        this.userUseCase = new UserUseCase(userRepository, emailSender);
        this.closureUseCase = new CashClosureUseCase(closureRepository);
        this.configUseCase = new ConfigUseCase(configRepository);
        this.dashboardUseCase = new DashboardUseCase(productRepository, categoryRepository, saleRepository,
                closureRepository, clientRepository, userRepository);
    }

    // --- Getters ---
    public ProductUseCase getProductUseCase() {
        return productUseCase;
    }

    public CategoryUseCase getCategoryUseCase() {
        return categoryUseCase;
    }

    public ClientUseCase getClientUseCase() {
        return clientUseCase;
    }

    public SaleUseCase getSaleUseCase() {
        return saleUseCase;
    }

    public UserUseCase getUserUseCase() {
        return userUseCase;
    }

    public CashClosureUseCase getClosureUseCase() {
        return closureUseCase;
    }

    public ConfigUseCase getConfigUseCase() {
        return configUseCase;
    }

    public DashboardUseCase getDashboardUseCase() {
        return dashboardUseCase;
    }

    public GlobalEventBus getEventBus() {
        return eventBus;
    }

    public AsyncManager getAsyncManager() {
        return asyncManager;
    }

    public UserSession getUserSession() {
        return userSession;
    }

    public AuthorizationService getAuthService() {
        return authService;
    }

    public CartUseCase getCartUseCase() {
        return cartUseCase;
    }

    public NavigationService getNavigationService() {
        return navigationService;
    }

    public void setNavigationService(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    // Factory methods for non-singleton usecases
    public ProductFilterUseCase createProductFilterUseCase() {
        return new ProductFilterUseCase();
    }
}
