package com.mycompany.ventacontrolfx.service;

import java.util.prefs.Preferences;
import com.mycompany.ventacontrolfx.model.SaleConfig;

/**
 * Servicio que persiste y recupera la SaleConfig usando
 * java.util.prefs.Preferences
 * (no necesita base de datos adicional).
 */
public class SaleConfigService {

    private static final String NODE = "com/mycompany/ventacontrolfx/saleconfig";
    private final Preferences prefs = Preferences.userRoot().node(NODE);

    // ── Claves ────────────────────────────────────────────────────────
    private static final String K_COMPANY_NAME = "companyName";
    private static final String K_CIF = "cif";
    private static final String K_ADDRESS = "address";
    private static final String K_PHONE = "phone";
    private static final String K_EMAIL = "email";
    private static final String K_LOGO_PATH = "logoPath";
    private static final String K_TAX_RATE = "taxRate";
    private static final String K_TAX_TYPE = "taxType";
    private static final String K_PRICES_INC_TAX = "pricesIncludeTax";
    private static final String K_CURRENCY = "currency";
    private static final String K_DECIMALS = "decimals";
    private static final String K_SHOW_LOGO = "showLogo";
    private static final String K_SHOW_ADDRESS = "showAddress";
    private static final String K_SHOW_PHONE = "showPhone";
    private static final String K_SHOW_CIF = "showCif";
    private static final String K_FOOTER_MSG = "footerMessage";
    private static final String K_TICKET_COPIES = "ticketCopies";
    private static final String K_AUTO_PRINT = "autoPrint";
    private static final String K_CASH = "cash";
    private static final String K_CARD = "card";
    private static final String K_TRANSFER = "transfer";
    private static final String K_CHECK = "check";
    private static final String K_CREDIT = "credit";
    private static final String K_ROUNDING = "rounding";
    private static final String K_ALLOW_DISCOUNT = "allowDiscount";
    private static final String K_ALLOW_GLOB_DISC = "allowGlobalDiscount";
    private static final String K_REQUIRE_CLIENT = "requireClient";
    private static final String K_STOCK_ALERT = "stockAlert";
    private static final String K_NEGATIVE_STOCK = "negativeStock";
    private static final String K_SHOW_BARCODE = "showBarcode";
    private static final String K_SOUND_ON_ADD = "soundOnAdd";
    private static final String K_CONFIRM_DELETE = "confirmDelete";

    // ─────────────────────────────────────────────────────────────────
    /**
     * Carga la configuración guardada. Si no existe clave devuelve el valor por
     * defecto del modelo.
     */
    public SaleConfig load() {
        SaleConfig d = new SaleConfig(); // defaults

        SaleConfig cfg = new SaleConfig();
        cfg.setCompanyName(prefs.get(K_COMPANY_NAME, d.getCompanyName()));
        cfg.setCif(prefs.get(K_CIF, d.getCif()));
        cfg.setAddress(prefs.get(K_ADDRESS, d.getAddress()));
        cfg.setPhone(prefs.get(K_PHONE, d.getPhone()));
        cfg.setEmail(prefs.get(K_EMAIL, d.getEmail()));
        cfg.setLogoPath(prefs.get(K_LOGO_PATH, d.getLogoPath()));
        cfg.setTaxRate(prefs.getDouble(K_TAX_RATE, d.getTaxRate()));
        cfg.setTaxType(prefs.get(K_TAX_TYPE, d.getTaxType()));
        cfg.setPricesIncludeTax(prefs.getBoolean(K_PRICES_INC_TAX, d.isPricesIncludeTax()));
        cfg.setCurrency(prefs.get(K_CURRENCY, d.getCurrency()));
        cfg.setDecimals(prefs.get(K_DECIMALS, d.getDecimals()));
        cfg.setShowLogo(prefs.getBoolean(K_SHOW_LOGO, d.isShowLogo()));
        cfg.setShowAddress(prefs.getBoolean(K_SHOW_ADDRESS, d.isShowAddress()));
        cfg.setShowPhone(prefs.getBoolean(K_SHOW_PHONE, d.isShowPhone()));
        cfg.setShowCif(prefs.getBoolean(K_SHOW_CIF, d.isShowCif()));
        cfg.setFooterMessage(prefs.get(K_FOOTER_MSG, d.getFooterMessage()));
        cfg.setTicketCopies(prefs.get(K_TICKET_COPIES, d.getTicketCopies()));
        cfg.setAutoPrint(prefs.getBoolean(K_AUTO_PRINT, d.isAutoPrint()));
        cfg.setCash(prefs.getBoolean(K_CASH, d.isCash()));
        cfg.setCard(prefs.getBoolean(K_CARD, d.isCard()));
        cfg.setTransfer(prefs.getBoolean(K_TRANSFER, d.isTransfer()));
        cfg.setCheck(prefs.getBoolean(K_CHECK, d.isCheck()));
        cfg.setCredit(prefs.getBoolean(K_CREDIT, d.isCredit()));
        cfg.setRounding(prefs.get(K_ROUNDING, d.getRounding()));
        cfg.setAllowDiscount(prefs.getBoolean(K_ALLOW_DISCOUNT, d.isAllowDiscount()));
        cfg.setAllowGlobalDiscount(prefs.getBoolean(K_ALLOW_GLOB_DISC, d.isAllowGlobalDiscount()));
        cfg.setRequireClient(prefs.getBoolean(K_REQUIRE_CLIENT, d.isRequireClient()));
        cfg.setStockAlert(prefs.getBoolean(K_STOCK_ALERT, d.isStockAlert()));
        cfg.setNegativeStock(prefs.getBoolean(K_NEGATIVE_STOCK, d.isNegativeStock()));
        cfg.setShowBarcode(prefs.getBoolean(K_SHOW_BARCODE, d.isShowBarcode()));
        cfg.setSoundOnAdd(prefs.getBoolean(K_SOUND_ON_ADD, d.isSoundOnAdd()));
        cfg.setConfirmDelete(prefs.getBoolean(K_CONFIRM_DELETE, d.isConfirmDelete()));

        return cfg;
    }

    // ─────────────────────────────────────────────────────────────────
    /** Persiste todos los valores de la configuración. */
    public void save(SaleConfig cfg) {
        prefs.put(K_COMPANY_NAME, cfg.getCompanyName());
        prefs.put(K_CIF, cfg.getCif());
        prefs.put(K_ADDRESS, cfg.getAddress());
        prefs.put(K_PHONE, cfg.getPhone());
        prefs.put(K_EMAIL, cfg.getEmail());
        prefs.put(K_LOGO_PATH, orEmpty(cfg.getLogoPath()));
        prefs.putDouble(K_TAX_RATE, cfg.getTaxRate());
        prefs.put(K_TAX_TYPE, orEmpty(cfg.getTaxType()));
        prefs.putBoolean(K_PRICES_INC_TAX, cfg.isPricesIncludeTax());
        prefs.put(K_CURRENCY, orEmpty(cfg.getCurrency()));
        prefs.put(K_DECIMALS, orEmpty(cfg.getDecimals()));
        prefs.putBoolean(K_SHOW_LOGO, cfg.isShowLogo());
        prefs.putBoolean(K_SHOW_ADDRESS, cfg.isShowAddress());
        prefs.putBoolean(K_SHOW_PHONE, cfg.isShowPhone());
        prefs.putBoolean(K_SHOW_CIF, cfg.isShowCif());
        prefs.put(K_FOOTER_MSG, cfg.getFooterMessage());
        prefs.put(K_TICKET_COPIES, orEmpty(cfg.getTicketCopies()));
        prefs.putBoolean(K_AUTO_PRINT, cfg.isAutoPrint());
        prefs.putBoolean(K_CASH, cfg.isCash());
        prefs.putBoolean(K_CARD, cfg.isCard());
        prefs.putBoolean(K_TRANSFER, cfg.isTransfer());
        prefs.putBoolean(K_CHECK, cfg.isCheck());
        prefs.putBoolean(K_CREDIT, cfg.isCredit());
        prefs.put(K_ROUNDING, orEmpty(cfg.getRounding()));
        prefs.putBoolean(K_ALLOW_DISCOUNT, cfg.isAllowDiscount());
        prefs.putBoolean(K_ALLOW_GLOB_DISC, cfg.isAllowGlobalDiscount());
        prefs.putBoolean(K_REQUIRE_CLIENT, cfg.isRequireClient());
        prefs.putBoolean(K_STOCK_ALERT, cfg.isStockAlert());
        prefs.putBoolean(K_NEGATIVE_STOCK, cfg.isNegativeStock());
        prefs.putBoolean(K_SHOW_BARCODE, cfg.isShowBarcode());
        prefs.putBoolean(K_SOUND_ON_ADD, cfg.isSoundOnAdd());
        prefs.putBoolean(K_CONFIRM_DELETE, cfg.isConfirmDelete());
    }

    // ─────────────────────────────────────────────────────────────────
    /**
     * Elimina todas las preferencias guardadas y recarga los valores por defecto.
     */
    public void reset() {
        try {
            prefs.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    private String orEmpty(String val) {
        return val != null ? val : "";
    }
}
