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
import com.mycompany.ventacontrolfx.application.service.PriceUpdateService;
import com.mycompany.ventacontrolfx.application.service.PromotionEngine;
import com.mycompany.ventacontrolfx.application.service.PromotionService;
import com.mycompany.ventacontrolfx.application.service.RefundCalculatorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.mycompany.ventacontrolfx.infrastructure.aeat.VerifactuOutboxManager;
import com.mycompany.ventacontrolfx.infrastructure.aeat.AeatHttpClient;
import com.mycompany.ventacontrolfx.infrastructure.aeat.VerifactuXmlBuilder;
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
    private final IMassivePriceUpdateRepository massivePriceUpdateRepository;
    private final IPriceHistoryRepository priceHistoryRepository;
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
    private VerifactuOutboxManager verifactuOutboxManager;
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
    private final PriceUpdateService priceUpdateService;

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
        // Silenciar loggers ruidosos
        java.util.logging.Logger.getLogger("javafx.scene.CssStyleHelper").setLevel(java.util.logging.Level.OFF);
        java.util.logging.Logger.getLogger("javafx.css").setLevel(java.util.logging.Level.OFF);
        try {
            java.util.logging.LogManager.getLogManager()
                    .getLogger("com.mycompany.ventacontrolfx.infrastructure.aeat.VerifactuOutboxManager")
                    .setLevel(java.util.logging.Level.WARNING);
        } catch (Exception ignored) {
        }

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
        
        // Price specialized repositories
        this.priceRepository = new JdbcPriceRepository();
        this.massivePriceUpdateRepository = new JdbcMassivePriceUpdateRepository();
        this.priceHistoryRepository = new JdbcPriceHistoryRepository();
        
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
        
        // Use updated repositories in use cases
        this.massivePriceUpdateUseCase = new MassivePriceUpdateUseCase(massivePriceUpdateRepository, productRepository,
                priceLogRepository);
        this.priceUpdateService = new PriceUpdateService(massivePriceUpdateUseCase);

        this.returnUseCase = new ReturnUseCase(saleRepository, productRepository, seriesRepository, configRepository,
                refundCalculator, null);
        this.saleUseCase = new com.mycompany.ventacontrolfx.application.usecase.SaleUseCase(saleRepository,
                configRepository, authService, taxEngineService, clientRepository, promotionEngine,
                productRepository, seriesRepository, eventBus);
        this.userUseCase = new com.mycompany.ventacontrolfx.application.usecase.UserUseCase(userRepository, emailSender,
                authService);
        this.closureUseCase = new CashClosureUseCase(closureRepository, authService);
        this.closureUseCase.setBackupService(new BackupService());
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
        this.emitFiscalDocumentUseCase.setPdfService(pdfService);
        this.queryFiscalDocumentUseCase = new QueryFiscalDocumentUseCase(fiscalRepository, saleRepository);
        this.loginUseCase = new LoginUseCase(userRepository, auditRepository, roleUseCase, permissionUseCase);
        
        // Update PriceListUseCase with specialized repositories
        this.priceListUseCase = new PriceListUseCase(priceListRepository, priceRepository, priceHistoryRepository, massivePriceUpdateRepository);
        
        this.getSaleTicketUseCase = new GetSaleTicketUseCase(saleRepository);
        this.restoreSuspendedCartUseCase = new RestoreSuspendedCartUseCase(
                suspendedCartRepository, productRepository, clientRepository, cartUseCase);
        this.taxManagementUseCase = new TaxManagementUseCase(taxRepository, productRepository, authService);
        this.promotionUseCase = new PromotionUseCase(promotionRepository);
        this.productImportUseCase = new ProductImportUseCase(productRepository, categoryRepository, authService);
        this.workSessionUseCase = new WorkSessionUseCase(workSessionRepository);
        this.workSessionUseCase.setBackupService(new BackupService());

        // 6. Scheduled Tasks: Daily Auto-Backup
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "BackupScheduler");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            new BackupService().createDefaultBackup();
        }, 1, 24, TimeUnit.HOURS);

        try {
            AeatHttpClient aeatClient = new AeatHttpClient(
                    "https://prewww1.aeat.es/wlpl/TIKE-CONT/ws/SistemaFacturacion/VerifactuSOAP",
                    "/certs/99999910G_prueba.pfx", "1234");
            VerifactuXmlBuilder xmlBuilder = new VerifactuXmlBuilder("99999910G",
                    "(VERI*FACTU) CERTIFICADO FISICA PRUEBAS");
            this.verifactuOutboxManager = new VerifactuOutboxManager(aeatClient, xmlBuilder, this.eventBus);
            this.verifactuOutboxManager.start();

            new Thread(() -> {
                refreshVerifactuCredentials();
            }).start();
        } catch (Exception e) {
            System.err.println("[VeriFactu] Error cr\u00edtico en el arranque: " + e.getMessage());
            e.printStackTrace();
        }

        this.saleUseCase.setCashClosureUseCase(this.closureUseCase);
        this.saleUseCase.setPdfService(this.pdfService);
        this.returnUseCase.setPdfService(this.pdfService);
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

    public RefundCalculatorService getRefundCalculator() {
        return refundCalculator;
    }

    public PriceUpdateService getPriceUpdateService() {
        return priceUpdateService;
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
    
    public IMassivePriceUpdateRepository getMassivePriceUpdateRepository() {
        return massivePriceUpdateRepository;
    }
    
    public IPriceHistoryRepository getPriceHistoryRepository() {
        return priceHistoryRepository;
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

    public void refreshVerifactuCredentials() {
        if (verifactuOutboxManager == null)
            return;
        try {
            String nif = configRepository.load().getCif();
            String name = configRepository.load().getCompanyName();
            if (nif != null && !nif.isEmpty()) {
                verifactuOutboxManager.updateCredentials(nif.toUpperCase(), name);
            }
        } catch (Exception ignored) {
        }
    }

    public VerifactuOutboxManager getVerifactuOutboxManager() {
        return verifactuOutboxManager;
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> clazz) {
        if (clazz.equals(IPriceRepository.class)) return (T) priceRepository;
        if (clazz.equals(IMassivePriceUpdateRepository.class)) return (T) massivePriceUpdateRepository;
        if (clazz.equals(IPriceHistoryRepository.class)) return (T) priceHistoryRepository;
        // Add more mappings if needed, or stick to explicit getters
        return null;
    }
}
