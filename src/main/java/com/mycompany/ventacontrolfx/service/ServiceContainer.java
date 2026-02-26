package com.mycompany.ventacontrolfx.service;

import com.mycompany.ventacontrolfx.application.service.DashboardUseCase;
import com.mycompany.ventacontrolfx.application.service.ProductFilterUseCase;
import com.mycompany.ventacontrolfx.application.service.ProductUseCase;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import com.mycompany.ventacontrolfx.infrastructure.persistence.JdbcProductRepository;
import com.mycompany.ventacontrolfx.util.AuthorizationService;
import com.mycompany.ventacontrolfx.util.UserSession;

/**
 * Enterprise Service Registry / Dependency Injection Hub.
 */
public class ServiceContainer {
    // Infrastructure
    private final IProductRepository productRepository;

    // Application
    private final ProductUseCase productUseCase;
    private final DashboardUseCase dashboardUseCase;
    // NOTA: ProductFilterUseCase NO es un singleton; se crea bajo demanda con
    // createProductFilterUseCase().
    // Esto evita contaminación de estado de filtro entre SellViewController y
    // ProductsController.

    // Core Utilities
    private final GlobalEventBus eventBus;
    private final UserSession userSession;
    private final AuthorizationService authService;

    // Legacy / Domain Services
    private final CategoryService categoryService;
    private final CartService cartService;
    private final ProductFilterService filterService;
    private final SaleService saleService;
    private final UserService userService;
    private final CashClosureService closureService;
    private final ClientService clientService;
    private final SaleConfigService configService;
    private final SaleApplicationService saleAppService;

    public ServiceContainer() {
        this.eventBus = new GlobalEventBus();
        this.userSession = new UserSession();
        this.authService = new AuthorizationService(userSession);
        this.productRepository = new JdbcProductRepository();

        // Wiring Services
        this.categoryService = new CategoryService();
        this.cartService = new CartService();
        this.filterService = new ProductFilterService();
        this.saleService = new SaleService(userSession);
        this.userService = new UserService();
        this.closureService = new CashClosureService(userSession);
        this.clientService = new ClientService();
        this.configService = new SaleConfigService();

        // Wiring Use Cases
        this.productUseCase = new ProductUseCase(productRepository, eventBus);
        // productFilterUseCase se crea por demanda (factory) — ver
        // createProductFilterUseCase()
        this.dashboardUseCase = new DashboardUseCase(productRepository, categoryService, saleService,
                closureService, clientService, userService);

        // Orchestration
        this.saleAppService = new SaleApplicationService(saleService, cartService, eventBus);
    }

    // Getters for Use Cases
    public ProductUseCase getProductUseCase() {
        return productUseCase;
    }

    /**
     * Factory method: crea una instancia NUEVA e independiente de
     * ProductFilterUseCase
     * con su propio ProductFilterService, evitando contaminación de estado entre
     * vistas.
     *
     * REGLA: Cada controlador que necesite filtrado debe llamar a este método en
     * inject()
     * para obtener su propio filtro aislado.
     */
    public ProductFilterUseCase createProductFilterUseCase() {
        return new ProductFilterUseCase(new ProductFilterService());
    }

    /**
     * @deprecated Usar createProductFilterUseCase() para obtener un filtro
     *             independiente por vista.
     *             Mantenido por compatibilidad hasta migrar todos los usos.
     */
    @Deprecated
    public ProductFilterUseCase getProductFilterUseCase() {
        return createProductFilterUseCase();
    }

    public DashboardUseCase getDashboardUseCase() {
        return dashboardUseCase;
    }

    // Getters for Core Utilities
    public GlobalEventBus getEventBus() {
        return eventBus;
    }

    public UserSession getUserSession() {
        return userSession;
    }

    public AuthorizationService getAuthService() {
        return authService;
    }

    // Getters for Legacy Services
    public CartService getCartService() {
        return cartService;
    }

    public ProductFilterService getFilterService() {
        return filterService;
    }

    public CategoryService getCategoryService() {
        return categoryService;
    }

    public SaleService getSaleService() {
        return saleService;
    }

    public UserService getUserService() {
        return userService;
    }

    public CashClosureService getClosureService() {
        return closureService;
    }

    public ClientService getClientService() {
        return clientService;
    }

    public SaleConfigService getConfigService() {
        return configService;
    }

    public SaleApplicationService getSaleAppService() {
        return saleAppService;
    }

    /** @deprecated Use ProductUseCase to manage product data. */
    public ProductService getProductService() {
        return new ProductService();
    }
}
