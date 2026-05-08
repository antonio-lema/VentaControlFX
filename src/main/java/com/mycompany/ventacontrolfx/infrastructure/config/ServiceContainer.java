package com.mycompany.ventacontrolfx.infrastructure.config;

import com.mycompany.ventacontrolfx.application.usecase.*;
import com.mycompany.ventacontrolfx.application.service.*;
import com.mycompany.ventacontrolfx.domain.repository.*;
import com.mycompany.ventacontrolfx.domain.model.UserSession;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.domain.service.*;
import com.mycompany.ventacontrolfx.infrastructure.persistence.*;
import com.mycompany.ventacontrolfx.infrastructure.security.AuthorizationService;
import com.mycompany.ventacontrolfx.infrastructure.navigation.NavigationService;
import com.mycompany.ventacontrolfx.infrastructure.external.aeat.AeatHttpClient;
import com.mycompany.ventacontrolfx.infrastructure.external.aeat.VerifactuXmlBuilder;
import com.mycompany.ventacontrolfx.infrastructure.external.aeat.VerifactuOutboxManager;
import com.mycompany.ventacontrolfx.infrastructure.external.pdf.OpenPdfFiscalService;
import com.mycompany.ventacontrolfx.infrastructure.external.email.SmtpEmailAdapter;
import com.mycompany.ventacontrolfx.shared.bus.GlobalEventBus;
import com.mycompany.ventacontrolfx.shared.async.AsyncManager;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;

/**
 * Enterprise Service Container (Dependency Injector).
 * Manages the lifecycle and dependencies of all system components.
 * Final stable version with correct repository and use case wiring.
 */
public class ServiceContainer {

    // Repositories
    private final IProductRepository productRepository;
    private final ICategoryRepository categoryRepository;
    private final ITaxRepository taxRepository;
    private final ISaleRepository saleRepository;
    private final IClientRepository clientRepository;
    private final IUserRepository userRepository;
    private final IPriceListRepository priceListRepository;
    private final ICompanyConfigRepository configRepository;
    private final IAppSettingsRepository appSettingsRepository;
    private final IPromotionRepository promotionRepository;
    private final IWorkSessionRepository workSessionRepository;
    private final IAuditRepository auditRepository;
    private final IRoleRepository roleRepository;
    private final IPermissionRepository permissionRepository;
    private final IDocumentSeriesRepository seriesRepository;
    private final IPriceRepository priceRepository;
    private final IPriceHistoryRepository priceHistoryRepository;
    private final IMassivePriceUpdateRepository massivePriceUpdateRepository;
    private final IPriceUpdateLogRepository priceUpdateLogRepository;
    private final ICashClosureRepository cashClosureRepository;
    private final IFiscalDocumentRepository fiscalDocumentRepository;
    private final ISuspendedCartRepository suspendedCartRepository;

    // Services
    private final TaxEngineService taxEngineService;
    private final AuthorizationService authService;
    private final OpenPdfFiscalService pdfService;
    private final IEmailSender emailSender;
    private final PromotionEngine promotionEngine;
    private final PromotionService promotionService;
    private final PriceResolutionService priceResolutionService;
    private final PriceUpdateService priceUpdateService;
    private final RefundCalculatorService refundCalculator;
    private NavigationService navigationService;

    // Use Cases
    private final ProductUseCase productUseCase;
    private final CategoryUseCase categoryUseCase;
    private final SaleUseCase saleUseCase;
    private final ClientUseCase clientUseCase;
    private final UserUseCase userUseCase;
    private final LoginUseCase loginUseCase;
    private final CashClosureUseCase closureUseCase;
    private final ReturnUseCase returnUseCase;
    private final PriceListUseCase priceListUseCase;
    private final PromotionUseCase promotionUseCase;
    private final ProductImportUseCase productImportUseCase;
    private final WorkSessionUseCase workSessionUseCase;
    private final RoleUseCase roleUseCase;
    private final PermissionUseCase permissionUseCase;
    private final DashboardUseCase dashboardUseCase;
    private final GetSaleTicketUseCase getSaleTicketUseCase;
    private final CartUseCase cartUseCase;
    private final EmitFiscalDocumentUseCase emitFiscalDocumentUseCase;
    private final QueryFiscalDocumentUseCase queryFiscalDocumentUseCase;
    private final SuspendedCartUseCase suspendedCartUseCase;
    private final RestoreSuspendedCartUseCase restoreSuspendedCartUseCase;
    private final MassivePriceUpdateUseCase massivePriceUpdateUseCase;
    private final TaxManagementUseCase taxManagementUseCase;
    private final ScheduleVatChangeUseCase scheduleVatChangeUseCase;
    private final ConfigUseCase configUseCase;
    private final PriceUseCase priceUseCase;

    // Infrastructure & UI
    private final UserSession userSession;
    private final GlobalEventBus eventBus;
    private final AsyncManager asyncManager;
    private final com.mycompany.ventacontrolfx.presentation.theme.ThemeManager themeManager;

    private ResourceBundle bundle;
    private Locale currentLocale;
    private VerifactuOutboxManager verifactuOutboxManager;
    
    // Service Map for getService()
    private final Map<Class<?>, Object> serviceMap = new HashMap<>();

    public ServiceContainer() {
        // 1. Foundation & Shared
        this.eventBus = new GlobalEventBus();
        this.asyncManager = new AsyncManager();
        this.userSession = new UserSession();
        this.currentLocale = new Locale("es");
        this.bundle = ResourceBundle.getBundle("i18n/messages", currentLocale);

        // 2. Repositories (Infrastructure)
        this.productRepository = new JdbcProductRepository();
        this.categoryRepository = new JdbcCategoryRepository();
        this.taxRepository = new JdbcTaxRepository();
        this.saleRepository = new JdbcSaleRepository();
        this.clientRepository = new JdbcClientRepository();
        this.userRepository = new JdbcUserRepository();
        this.priceListRepository = new JdbcPriceListRepository();
        this.configRepository = new JdbcCompanyConfigRepository();
        this.appSettingsRepository = new JdbcAppSettingsRepository();
        this.promotionRepository = new JdbcPromotionRepository();
        this.workSessionRepository = new JdbcWorkSessionRepository();
        this.auditRepository = new JdbcAuditRepository();
        this.permissionRepository = new JdbcPermissionRepository(); // permissionRepository needs to be initialized BEFORE roleRepository
        this.roleRepository = new JdbcRoleRepository(permissionRepository); // Passing permissionRepository to JdbcRoleRepository
        this.seriesRepository = new JdbcDocumentSeriesRepository();
        this.priceRepository = new JdbcPriceRepository();
        this.priceHistoryRepository = new JdbcPriceHistoryRepository();
        this.massivePriceUpdateRepository = new JdbcMassivePriceUpdateRepository();
        this.priceUpdateLogRepository = new JdbcPriceUpdateLogRepository();
        this.cashClosureRepository = new JdbcCashClosureRepository();
        this.fiscalDocumentRepository = new JdbcFiscalDocumentRepository();
        this.suspendedCartRepository = new JdbcSuspendedCartRepository();

        // Initialize AlertUtil
        com.mycompany.ventacontrolfx.presentation.util.AlertUtil.setBundle(getBundle());

        // 3. Domain & Application Services
        this.taxEngineService = new TaxEngineService(taxRepository, categoryRepository);
        this.authService = new AuthorizationService(userSession);
        this.emailSender = new SmtpEmailAdapter();
        this.promotionEngine = new PromotionEngine(promotionRepository);
        this.promotionService = new PromotionService(promotionRepository);
        this.priceResolutionService = new PriceResolutionService(priceRepository, clientRepository);
        // Moved massivePriceUpdateUseCase initialization up
        this.massivePriceUpdateUseCase = new MassivePriceUpdateUseCase(massivePriceUpdateRepository, productRepository, priceUpdateLogRepository);
        this.priceUpdateService = new PriceUpdateService(massivePriceUpdateUseCase);
        this.refundCalculator = new RefundCalculatorService();
        this.pdfService = new OpenPdfFiscalService();

        // 4. Use Cases (Application)
        this.permissionUseCase = new PermissionUseCase(permissionRepository, authService);
        this.roleUseCase = new RoleUseCase(roleRepository, authService);
        
        this.productUseCase = new ProductUseCase(productRepository, authService, eventBus);
        this.categoryUseCase = new CategoryUseCase(categoryRepository, productRepository, authService);
        this.saleUseCase = new SaleUseCase(saleRepository, configRepository, authService, taxEngineService, clientRepository, promotionEngine, productRepository, seriesRepository, eventBus);
        this.clientUseCase = new ClientUseCase(clientRepository, authService);
        this.userUseCase = new UserUseCase(userRepository, emailSender, authService);
        this.loginUseCase = new LoginUseCase(userRepository, auditRepository, roleUseCase, permissionUseCase);
        this.closureUseCase = new CashClosureUseCase(cashClosureRepository, authService);
        this.returnUseCase = new ReturnUseCase(saleRepository, productRepository, seriesRepository, configRepository, refundCalculator, closureUseCase);
        this.priceListUseCase = new PriceListUseCase(priceListRepository, priceRepository, priceHistoryRepository, massivePriceUpdateRepository);
        this.promotionUseCase = new PromotionUseCase(promotionRepository);
        this.productImportUseCase = new ProductImportUseCase(productRepository, categoryRepository, authService);
        this.workSessionUseCase = new WorkSessionUseCase(workSessionRepository);
        this.workSessionUseCase.setBackupService(new BackupService());
        
        this.dashboardUseCase = new DashboardUseCase(productRepository, categoryRepository, saleRepository, cashClosureRepository, clientRepository, userRepository);
        this.getSaleTicketUseCase = new GetSaleTicketUseCase(saleRepository);
        this.cartUseCase = new CartUseCase(configRepository, priceResolutionService, taxEngineService, promotionService, promotionEngine, priceRepository, productRepository);
        this.emitFiscalDocumentUseCase = new EmitFiscalDocumentUseCase(saleRepository, fiscalDocumentRepository, seriesRepository, configRepository);
        this.queryFiscalDocumentUseCase = new QueryFiscalDocumentUseCase(fiscalDocumentRepository, saleRepository);
        this.suspendedCartUseCase = new SuspendedCartUseCase(suspendedCartRepository);
        this.restoreSuspendedCartUseCase = new RestoreSuspendedCartUseCase(suspendedCartRepository, productRepository, clientRepository, cartUseCase);
        this.taxManagementUseCase = new TaxManagementUseCase(taxRepository, productRepository, authService);
        this.scheduleVatChangeUseCase = new ScheduleVatChangeUseCase(taxRepository);
        this.configUseCase = new ConfigUseCase(configRepository);
        this.priceUseCase = new PriceUseCase(priceRepository);

        // 5. Aesthetic Management
        this.themeManager = new com.mycompany.ventacontrolfx.presentation.theme.ThemeManager(appSettingsRepository);

        // 6. Scheduled Tasks: Daily Auto-Backup
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "BackupScheduler");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            new BackupService().createDefaultBackup();
        }, 1, 24, TimeUnit.HOURS);

        // 7. Infrastructure Services (External: VeriFactu)
        try {
            SaleConfig config = configRepository.load();
            
            String aeatUrl = config.getVerifactuUrl().isEmpty() ? 
                    "https://prewww1.aeat.es/wlpl/TIKE-CONT/ws/SistemaFacturacion/VerifactuSOAP" : config.getVerifactuUrl();
            String certPath = config.getVerifactuCertPath().isEmpty() ? 
                    "/certs/99999910G_prueba.pfx" : config.getVerifactuCertPath();
            String certPass = config.getVerifactuCertPass().isEmpty() ? 
                    "1234" : config.getVerifactuCertPass();
            String verifactuNif = config.getVerifactuNif().isEmpty() ? 
                    "99999910G" : config.getVerifactuNif();
            String certName = config.getVerifactuCertName().isEmpty() ? 
                    "(VERI*FACTU) CERTIFICADO FISICA PRUEBAS" : config.getVerifactuCertName();

            AeatHttpClient aeatClient = new AeatHttpClient(aeatUrl, certPath, certPass);
            VerifactuXmlBuilder xmlBuilder = new VerifactuXmlBuilder(verifactuNif, certName);
            
            this.verifactuOutboxManager = new VerifactuOutboxManager(aeatClient, xmlBuilder, this.eventBus);
            this.verifactuOutboxManager.start();
        } catch (Exception e) {
            System.err.println("[VeriFactu] Error cr\u00edtico en el arranque: " + e.getMessage());
        }

        // 8. Register Services in Map for getService()
        registerServices();

        // Post-initialization wiring
        this.saleUseCase.setCashClosureUseCase(this.closureUseCase);
        this.saleUseCase.setPdfService(this.pdfService);
        this.returnUseCase.setPdfService(this.pdfService);
        this.emitFiscalDocumentUseCase.setPdfService(this.pdfService);
    }

    private void registerServices() {
        serviceMap.put(IProductRepository.class, productRepository);
        serviceMap.put(ICategoryRepository.class, categoryRepository);
        serviceMap.put(ITaxRepository.class, taxRepository);
        serviceMap.put(ISaleRepository.class, saleRepository);
        serviceMap.put(IClientRepository.class, clientRepository);
        serviceMap.put(IUserRepository.class, userRepository);
        serviceMap.put(IPriceListRepository.class, priceListRepository);
        serviceMap.put(ICompanyConfigRepository.class, configRepository);
        serviceMap.put(IAppSettingsRepository.class, appSettingsRepository);
        serviceMap.put(IPromotionRepository.class, promotionRepository);
        serviceMap.put(IWorkSessionRepository.class, workSessionRepository);
        serviceMap.put(IAuditRepository.class, auditRepository);
        serviceMap.put(IRoleRepository.class, roleRepository);
        serviceMap.put(IPermissionRepository.class, permissionRepository);
        serviceMap.put(IDocumentSeriesRepository.class, seriesRepository);
        serviceMap.put(IPriceRepository.class, priceRepository);
        serviceMap.put(IPriceHistoryRepository.class, priceHistoryRepository);
        serviceMap.put(IMassivePriceUpdateRepository.class, massivePriceUpdateRepository);
        serviceMap.put(IPriceUpdateLogRepository.class, priceUpdateLogRepository);
        serviceMap.put(ICashClosureRepository.class, cashClosureRepository);
        serviceMap.put(IFiscalDocumentRepository.class, fiscalDocumentRepository);
        serviceMap.put(ISuspendedCartRepository.class, suspendedCartRepository);
    }

    public void setLanguage(String langCode) {
        this.currentLocale = new Locale(langCode);
        Locale.setDefault(currentLocale); // Establecer como defecto global para Java/JavaFX
        this.bundle = ResourceBundle.getBundle("i18n/messages", currentLocale);
        com.mycompany.ventacontrolfx.presentation.util.AlertUtil.setBundle(this.bundle);
        this.eventBus.publishLocaleChange();
    }

    // --- Factory Methods ---
    public ProductFilterUseCase createProductFilterUseCase() { return new ProductFilterUseCase(); }

    // --- Generic Service Accessor ---
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) {
        return (T) serviceMap.get(serviceClass);
    }

    // --- Getters & Setters ---
    public NavigationService getNavigationService() { return navigationService; }
    public void setNavigationService(NavigationService navigationService) { this.navigationService = navigationService; }

    public TaxEngineService getTaxEngineService() { return taxEngineService; }
    public ProductUseCase getProductUseCase() { return productUseCase; }
    public CategoryUseCase getCategoryUseCase() { return categoryUseCase; }
    public SaleUseCase getSaleUseCase() { return saleUseCase; }
    public ClientUseCase getClientUseCase() { return clientUseCase; }
    public UserUseCase getUserUseCase() { return userUseCase; }
    public LoginUseCase getLoginUseCase() { return loginUseCase; }
    public UserSession getUserSession() { return userSession; }
    public GlobalEventBus getEventBus() { return eventBus; }
    public CashClosureUseCase getClosureUseCase() { return closureUseCase; }
    public ResourceBundle getBundle() { return bundle; }
    public AsyncManager getAsyncManager() { return asyncManager; }
    public ReturnUseCase getReturnUseCase() { return returnUseCase; }
    public PriceListUseCase getPriceListUseCase() { return priceListUseCase; }
    public PromotionUseCase getPromotionUseCase() { return promotionUseCase; }
    public ProductImportUseCase getProductImportUseCase() { return productImportUseCase; }
    public com.mycompany.ventacontrolfx.presentation.theme.ThemeManager getThemeManager() { return themeManager; }
    public ICompanyConfigRepository getConfigRepository() { return configRepository; }
    public ICompanyConfigRepository getICompanyConfigRepository() { return configRepository; }
    public IAppSettingsRepository getAppSettingsRepository() { return appSettingsRepository; }
    public IAppSettingsRepository getIAppSettingsRepository() { return appSettingsRepository; }
    public OpenPdfFiscalService getPdfService() { return pdfService; }
    public AuthorizationService getAuthService() { return authService; }
    public PermissionUseCase getPermissionUseCase() { return permissionUseCase; }
    public RoleUseCase getRoleUseCase() { return roleUseCase; }
    public WorkSessionUseCase getWorkSessionUseCase() { return workSessionUseCase; }
    public DashboardUseCase getDashboardUseCase() { return dashboardUseCase; }
    public GetSaleTicketUseCase getGetSaleTicketUseCase() { return getSaleTicketUseCase; }
    public CartUseCase getCartUseCase() { return cartUseCase; }
    public EmitFiscalDocumentUseCase getEmitFiscalDocumentUseCase() { return emitFiscalDocumentUseCase; }
    public QueryFiscalDocumentUseCase getQueryFiscalDocumentUseCase() { return queryFiscalDocumentUseCase; }
    public SuspendedCartUseCase getSuspendedCartUseCase() { return suspendedCartUseCase; }
    public RestoreSuspendedCartUseCase getRestoreSuspendedCartUseCase() { return restoreSuspendedCartUseCase; }
    public MassivePriceUpdateUseCase getMassivePriceUpdateUseCase() { return massivePriceUpdateUseCase; }
    public TaxManagementUseCase getTaxManagementUseCase() { return taxManagementUseCase; }
    public ScheduleVatChangeUseCase getScheduleVatChangeUseCase() { return scheduleVatChangeUseCase; }
    public ConfigUseCase getConfigUseCase() { return configUseCase; }
    public PriceUseCase getPriceUseCase() { return priceUseCase; }
    public VerifactuOutboxManager getVerifactuOutboxManager() { return verifactuOutboxManager; }
    public Locale getCurrentLocale() { return currentLocale; }
    public IEmailSender getEmailSender() { return emailSender; }
    
    public PriceUpdateService getPriceUpdateService() { return priceUpdateService; }
    
    // Repository Getters (Some controllers/managers use them directly)
    public IProductRepository getProductRepository() { return productRepository; }
    public ICategoryRepository getCategoryRepository() { return categoryRepository; }
    public ITaxRepository getTaxRepository() { return taxRepository; }
    public ISaleRepository getSaleRepository() { return saleRepository; }
    public IClientRepository getClientRepository() { return clientRepository; }
    public IUserRepository getUserRepository() { return userRepository; }
    public IPromotionRepository getPromotionRepository() { return promotionRepository; }
    public IAuditRepository getAuditRepository() { return auditRepository; }
    public ICashClosureRepository getCashClosureRepository() { return cashClosureRepository; }
    public IPriceHistoryRepository getPriceHistoryRepository() { return priceHistoryRepository; }
    public IPriceUpdateLogRepository getPriceUpdateLogRepository() { return priceUpdateLogRepository; }
    public IPriceRepository getPriceRepository() { return priceRepository; }
    public IRoleRepository getRoleRepository() { return roleRepository; }
    public IPermissionRepository getPermissionRepository() { return permissionRepository; }
}
