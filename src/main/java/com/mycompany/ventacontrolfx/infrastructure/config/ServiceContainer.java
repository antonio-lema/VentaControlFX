package com.mycompany.ventacontrolfx.infrastructure.config;

import com.mycompany.ventacontrolfx.application.usecase.*;
import com.mycompany.ventacontrolfx.domain.repository.*;
import com.mycompany.ventacontrolfx.infrastructure.persistence.*;
import com.mycompany.ventacontrolfx.infrastructure.email.SmtpEmailAdapter;
import com.mycompany.ventacontrolfx.application.ports.IFiscalPdfService;
import com.mycompany.ventacontrolfx.infrastructure.pdf.OpenPdfFiscalService;
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
    private final IPermissionRepository permissionRepository;
    private final ISuspendedCartRepository suspendedCartRepository;
    private final IRoleRepository roleRepository;
    private final IPriceRepository priceRepository;
    private final IAuditRepository auditRepository;
    private final IAppSettingsRepository appSettingsRepository;
    private final IFiscalDocumentRepository fiscalRepository;
    private final IDocumentSeriesRepository seriesRepository;
    private final IFiscalPdfService pdfService;
    private final com.mycompany.ventacontrolfx.presentation.theme.ThemeManager themeManager;
    private final IPriceListRepository priceListRepository;
    private final ITaxRepository taxRepository;
    private final IPriceUpdateLogRepository priceLogRepository;

    // Use Cases (Application Layer)
    private final ProductUseCase productUseCase;
    private final CategoryUseCase categoryUseCase;
    private final ClientUseCase clientUseCase;
    private final SaleUseCase saleUseCase;
    private final UserUseCase userUseCase;
    private final CashClosureUseCase closureUseCase;
    private final ConfigUseCase configUseCase;
    private final DashboardUseCase dashboardUseCase;
    private final PermissionUseCase permissionUseCase;
    private final SuspendedCartUseCase suspendedCartUseCase;
    private final RoleUseCase roleUseCase;
    private final PriceUseCase priceUseCase;
    private final UserPermissionUseCase userPermissionUseCase;
    private final GetSaleTicketUseCase getSaleTicketUseCase;
    private final RestoreSuspendedCartUseCase restoreSuspendedCartUseCase;
    private final EmitFiscalDocumentUseCase emitFiscalDocumentUseCase;
    private final QueryFiscalDocumentUseCase queryFiscalDocumentUseCase;
    private final LoginUseCase loginUseCase;
    private final PriceListUseCase priceListUseCase;
    private final ScheduleVatChangeUseCase vatUseCase;
    private final MassivePriceUpdateUseCase massivePriceUpdateUseCase;

    // Shared Components
    private final GlobalEventBus eventBus;
    private final AsyncManager asyncManager;
    private final UserSession userSession;
    private final AuthorizationService authService;

    // State / Use Cases
    private final CartUseCase cartUseCase;
    private NavigationService navigationService;
    private final com.mycompany.ventacontrolfx.domain.service.TaxEngineService taxEngineService;

    public ServiceContainer() {
        // 1. Shared Infrastructure
        this.eventBus = new GlobalEventBus();
        this.asyncManager = new AsyncManager();
        this.userSession = new UserSession();
        this.authService = new AuthorizationService(userSession);
        // Initialize CartUseCase as a singleton for the session
        this.cartUseCase = new CartUseCase(new JdbcCompanyConfigRepository(), new JdbcPriceRepository());

        // 2. Adapters (Infrastructure Layer)
        this.productRepository = new JdbcProductRepository();
        this.categoryRepository = new JdbcCategoryRepository();
        this.clientRepository = new JdbcClientRepository();
        this.saleRepository = new JdbcSaleRepository();
        this.userRepository = new JdbcUserRepository();
        this.closureRepository = new JdbcCashClosureRepository();
        this.configRepository = new JdbcCompanyConfigRepository();
        this.emailSender = new SmtpEmailAdapter();
        this.permissionRepository = new JdbcPermissionRepository();
        this.suspendedCartRepository = new JdbcSuspendedCartRepository();
        this.roleRepository = new JdbcRoleRepository(permissionRepository);
        this.priceRepository = new JdbcPriceRepository();
        this.auditRepository = new JdbcAuditRepository();
        this.appSettingsRepository = new JdbcAppSettingsRepository();
        this.fiscalRepository = new JdbcFiscalDocumentRepository();
        this.seriesRepository = new JdbcDocumentSeriesRepository();
        this.pdfService = new OpenPdfFiscalService();
        this.themeManager = new com.mycompany.ventacontrolfx.presentation.theme.ThemeManager(appSettingsRepository);
        this.priceListRepository = new JdbcPriceListRepository();
        this.taxRepository = new JdbcTaxRepository();
        this.priceLogRepository = new JdbcPriceUpdateLogRepository();
        // 3. Domain Services
        this.taxEngineService = new com.mycompany.ventacontrolfx.domain.service.TaxEngineService(taxRepository);

        // 4. Wiring Use Cases (Application Layer)
        this.productUseCase = new com.mycompany.ventacontrolfx.application.usecase.ProductUseCase(productRepository,
                authService);
        this.categoryUseCase = new CategoryUseCase(categoryRepository, authService);
        this.clientUseCase = new ClientUseCase(clientRepository, authService);
        this.saleUseCase = new com.mycompany.ventacontrolfx.application.usecase.SaleUseCase(saleRepository,
                configRepository, authService, taxEngineService, clientRepository);
        this.userUseCase = new com.mycompany.ventacontrolfx.application.usecase.UserUseCase(userRepository, emailSender,
                authService);
        this.closureUseCase = new CashClosureUseCase(closureRepository, authService);
        this.configUseCase = new ConfigUseCase(configRepository);
        this.dashboardUseCase = new DashboardUseCase(productRepository, categoryRepository, saleRepository,
                closureRepository, clientRepository, userRepository);
        this.permissionUseCase = new PermissionUseCase(permissionRepository, authService);
        this.suspendedCartUseCase = new SuspendedCartUseCase(suspendedCartRepository);
        this.roleUseCase = new RoleUseCase(roleRepository, authService);
        this.priceUseCase = new PriceUseCase(priceRepository);
        this.userPermissionUseCase = new UserPermissionUseCase(userRepository, auditRepository, userSession);
        this.emitFiscalDocumentUseCase = new EmitFiscalDocumentUseCase(saleRepository, fiscalRepository,
                seriesRepository, configRepository);
        this.emitFiscalDocumentUseCase.setPdfService(pdfService); // Inyectamos el servicio de PDF
        this.queryFiscalDocumentUseCase = new QueryFiscalDocumentUseCase(fiscalRepository, saleRepository);
        this.loginUseCase = new LoginUseCase(userRepository, auditRepository, roleUseCase, permissionUseCase);
        this.priceListUseCase = new PriceListUseCase(priceListRepository, priceRepository);
        this.vatUseCase = new ScheduleVatChangeUseCase(taxRepository);
        this.massivePriceUpdateUseCase = new MassivePriceUpdateUseCase(priceRepository, productRepository,
                priceLogRepository);
        this.getSaleTicketUseCase = new GetSaleTicketUseCase(saleRepository);
        this.restoreSuspendedCartUseCase = new RestoreSuspendedCartUseCase(
                suspendedCartRepository, productRepository, clientRepository, cartUseCase);

        // 4. Domain Services
        // taxEngineService is now initialized above

        // Cross-wiring: SaleUseCase necesita CashClosureUseCase para registrar
        // devoluciones en caja
        this.saleUseCase.setCashClosureUseCase(this.closureUseCase);
    }

    // --- Getters ---
    public com.mycompany.ventacontrolfx.domain.service.TaxEngineService getTaxEngineService() {
        return taxEngineService;
    }

    public ProductUseCase getProductUseCase() {
        return productUseCase;
    }

    public CategoryUseCase getCategoryUseCase() {
        return categoryUseCase;
    }

    public ScheduleVatChangeUseCase getVatUseCase() {
        return vatUseCase;
    }

    public ClientUseCase getClientUseCase() {
        return clientUseCase;
    }

    public SaleUseCase getSaleUseCase() {
        return saleUseCase;
    }

    public GetSaleTicketUseCase getGetSaleTicketUseCase() {
        return getSaleTicketUseCase;
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

    public PermissionUseCase getPermissionUseCase() {
        return permissionUseCase;
    }

    public SuspendedCartUseCase getSuspendedCartUseCase() {
        return suspendedCartUseCase;
    }

    public RestoreSuspendedCartUseCase getRestoreSuspendedCartUseCase() {
        return restoreSuspendedCartUseCase;
    }

    public RoleUseCase getRoleUseCase() {
        return roleUseCase;
    }

    public IPriceRepository getPriceRepository() {
        return priceRepository;
    }

    public PriceUseCase getPriceUseCase() {
        return priceUseCase;
    }

    public UserPermissionUseCase getUserPermissionUseCase() {
        return userPermissionUseCase;
    }

    public IAuditRepository getAuditRepository() {
        return auditRepository;
    }

    public IAppSettingsRepository getAppSettingsRepository() {
        return appSettingsRepository;
    }

    public com.mycompany.ventacontrolfx.presentation.theme.ThemeManager getThemeManager() {
        return themeManager;
    }

    public EmitFiscalDocumentUseCase getEmitFiscalDocumentUseCase() {
        return emitFiscalDocumentUseCase;
    }

    public QueryFiscalDocumentUseCase getQueryFiscalDocumentUseCase() {
        return queryFiscalDocumentUseCase;
    }

    public LoginUseCase getLoginUseCase() {
        return loginUseCase;
    }

    public ICompanyConfigRepository getICompanyConfigRepository() {
        return configRepository;
    }

    public PriceListUseCase getPriceListUseCase() {
        return priceListUseCase;
    }

    public MassivePriceUpdateUseCase getMassivePriceUpdateUseCase() {
        return massivePriceUpdateUseCase;
    }

    public IPriceUpdateLogRepository getPriceLogRepository() {
        return priceLogRepository;
    }

    public ITaxRepository getTaxRepository() {
        return taxRepository;
    }
}
