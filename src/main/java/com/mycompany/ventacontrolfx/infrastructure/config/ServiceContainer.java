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
import com.mycompany.ventacontrolfx.application.service.PromotionEngine;
import com.mycompany.ventacontrolfx.application.service.PromotionService;
import com.mycompany.ventacontrolfx.application.service.RefundCalculatorService;
// CartService removed, replaced by CartUseCase
import java.util.Locale;
import java.util.ResourceBundle;

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
    private final IPromotionRepository promotionRepository;
    private final PromotionEngine promotionEngine;
    private final IWorkSessionRepository workSessionRepository;

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
    private final MassivePriceUpdateUseCase massivePriceUpdateUseCase;
    private final TaxManagementUseCase taxManagementUseCase;
    private final PromotionUseCase promotionUseCase;
    private final com.mycompany.ventacontrolfx.application.service.PromotionService promotionService;
    private final ProductImportUseCase productImportUseCase;
    private final WorkSessionUseCase workSessionUseCase;
    private final ReturnUseCase returnUseCase;
    private final RefundCalculatorService refundCalculator;

    // Shared Components
    private final GlobalEventBus eventBus;
    private final AsyncManager asyncManager;
    private final UserSession userSession;
    private final AuthorizationService authService;

    // State / Use Cases
    private final CartUseCase cartUseCase;
    private NavigationService navigationService;
    private final com.mycompany.ventacontrolfx.domain.service.TaxEngineService taxEngineService;
    private final com.mycompany.ventacontrolfx.domain.service.PriceResolutionService priceResolutionService;
    private ResourceBundle bundle;
    private Locale currentLocale = new Locale("es");

    public ServiceContainer() {
        // 1. Shared Infrastructure
        this.eventBus = new GlobalEventBus();
        this.asyncManager = new AsyncManager();
        this.userSession = new UserSession();
        this.authService = new AuthorizationService(userSession);
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
        this.promotionRepository = new JdbcPromotionRepository();
        this.workSessionRepository = new JdbcWorkSessionRepository();

        // 3. Domain Services
        this.taxEngineService = new com.mycompany.ventacontrolfx.domain.service.TaxEngineService(taxRepository,
                categoryRepository);
        this.priceResolutionService = new com.mycompany.ventacontrolfx.domain.service.PriceResolutionService(
                priceRepository, clientRepository);
        this.promotionService = new PromotionService(promotionRepository);
        this.promotionEngine = new PromotionEngine(promotionRepository);

        // 4. Cart Initialized after TaxEngineService and PromotionService
        this.cartUseCase = new CartUseCase(configRepository, priceResolutionService, taxEngineService, promotionService,
                promotionEngine, priceRepository, productRepository);

        // 5. Wiring Use Cases (Application Layer)
        this.productUseCase = new com.mycompany.ventacontrolfx.application.usecase.ProductUseCase(productRepository,
                authService, eventBus);
        this.categoryUseCase = new CategoryUseCase(categoryRepository, productRepository, authService);
        this.clientUseCase = new ClientUseCase(clientRepository, authService);
        this.refundCalculator = new RefundCalculatorService();
        this.returnUseCase = new ReturnUseCase(saleRepository, productRepository, seriesRepository, configRepository,
                refundCalculator, null); // Closure se inyecta luego
        this.saleUseCase = new com.mycompany.ventacontrolfx.application.usecase.SaleUseCase(saleRepository,
                configRepository, authService, taxEngineService, clientRepository, promotionService, promotionEngine,
                productRepository, seriesRepository, fiscalRepository, eventBus);
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
        this.massivePriceUpdateUseCase = new MassivePriceUpdateUseCase(priceRepository, productRepository,
                priceLogRepository);
        this.getSaleTicketUseCase = new GetSaleTicketUseCase(saleRepository);
        this.restoreSuspendedCartUseCase = new RestoreSuspendedCartUseCase(
                suspendedCartRepository, productRepository, clientRepository, cartUseCase);
        this.taxManagementUseCase = new TaxManagementUseCase(taxRepository, productRepository, authService);
        this.promotionUseCase = new PromotionUseCase(promotionRepository);
        this.productImportUseCase = new ProductImportUseCase(productRepository, categoryRepository, authService);
        this.workSessionUseCase = new WorkSessionUseCase(workSessionRepository);

        // 4. Domain Services
        // taxEngineService is now initialized above

        // Cross-wiring: SaleUseCase necesita CashClosureUseCase para registrar
        // devoluciones en caja
        this.saleUseCase.setCashClosureUseCase(this.closureUseCase);
        this.saleUseCase.setPdfService(this.pdfService); // Inyectamos el servicio de PDF para archivado de devoluciones

        this.returnUseCase.setPdfService(this.pdfService);
        // El ReturnUseCase necesita el closure inyectado ahora que ya est\u00c3\u00a1 creado
        // Nota: Si ReturnUseCase no tiene setter, habr\u00c3\u00a1 que crearlo o usar reflexi\u00c3\u00b3n si
        // es singleton
        // Para simplificar, lo inyectamos aqu\u00c3\u00ad (asumiendo que tiene acceso o setter)
        // en esta versi\u00c3\u00b3n lo inyectaremos via setter o crearemos el objeto despu\u00c3\u00a9s.

        // Vamos a modificar SaleUseCase para que use el nuevo ReturnUseCase si es
        // necesario,
        // pero por ahora los mantendremos independientes para evitar romper
        // controladores.

        // AI Engine Initialization (Disabled)
        // this.aiSkillDispatcher = new AiSkillDispatcher(this);
        // this.aiToolGenerator = new AiToolDefinitionGenerator();
        // this.aiIntentRouter = new AiIntentRouter(this);
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

    public ClientUseCase getClientUseCase() {
        return clientUseCase;
    }

    public SaleUseCase getSaleUseCase() {
        return saleUseCase;
    }

    public ReturnUseCase getReturnUseCase() {
        return returnUseCase;
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

    public IUserRepository getUserRepository() {
        return userRepository;
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

    public TaxManagementUseCase getTaxManagementUseCase() {
        return taxManagementUseCase;
    }

    public PromotionUseCase getPromotionUseCase() {
        return promotionUseCase;
    }

    public PromotionEngine getPromotionEngine() {
        return promotionEngine;
    }

    public IProductRepository getProductRepository() {
        return productRepository;
    }

    public IEmailSender getEmailSender() {
        return emailSender;
    }

    public IFiscalPdfService getPdfService() {
        return pdfService;
    }

    public ProductImportUseCase getProductImportUseCase() {
        return productImportUseCase;
    }

    public WorkSessionUseCase getWorkSessionUseCase() {
        return workSessionUseCase;
    }

    public ResourceBundle getBundle() {
        if (bundle == null) {
            bundle = ResourceBundle.getBundle("i18n/messages", currentLocale);
        }
        return bundle;
    }

    public void setLanguage(String langCode) {
        this.currentLocale = new Locale(langCode);
        this.bundle = ResourceBundle.getBundle("i18n/messages", currentLocale);
        this.eventBus.publishLocaleChange();
    }

    public Locale getCurrentLocale() {
        return currentLocale;
    }
}
