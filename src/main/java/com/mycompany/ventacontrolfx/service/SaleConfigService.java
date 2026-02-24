package com.mycompany.ventacontrolfx.service;

import com.mycompany.ventacontrolfx.model.SaleConfig;
import com.mycompany.ventacontrolfx.dao.ConfigDAO;

/**
 * Servicio que persiste y recupera la SaleConfig usando
 * la base de datos (tabla system_config).
 */
public class SaleConfigService {

    private final ConfigDAO dao = new ConfigDAO();

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
        cfg.setCompanyName(getString(K_COMPANY_NAME, d.getCompanyName()));
        cfg.setCif(getString(K_CIF, d.getCif()));
        cfg.setAddress(getString(K_ADDRESS, d.getAddress()));
        cfg.setPhone(getString(K_PHONE, d.getPhone()));
        cfg.setEmail(getString(K_EMAIL, d.getEmail()));
        cfg.setLogoPath(getString(K_LOGO_PATH, d.getLogoPath()));
        cfg.setTaxRate(getDouble(K_TAX_RATE, d.getTaxRate()));
        cfg.setTaxType(getString(K_TAX_TYPE, d.getTaxType()));
        cfg.setPricesIncludeTax(getBoolean(K_PRICES_INC_TAX, d.isPricesIncludeTax()));
        cfg.setCurrency(getString(K_CURRENCY, d.getCurrency()));
        cfg.setDecimals(getString(K_DECIMALS, d.getDecimals()));
        cfg.setShowLogo(getBoolean(K_SHOW_LOGO, d.isShowLogo()));
        cfg.setShowAddress(getBoolean(K_SHOW_ADDRESS, d.isShowAddress()));
        cfg.setShowPhone(getBoolean(K_SHOW_PHONE, d.isShowPhone()));
        cfg.setShowCif(getBoolean(K_SHOW_CIF, d.isShowCif()));
        cfg.setFooterMessage(getString(K_FOOTER_MSG, d.getFooterMessage()));
        cfg.setTicketCopies(getString(K_TICKET_COPIES, d.getTicketCopies()));
        cfg.setAutoPrint(getBoolean(K_AUTO_PRINT, d.isAutoPrint()));
        cfg.setCash(getBoolean(K_CASH, d.isCash()));
        cfg.setCard(getBoolean(K_CARD, d.isCard()));
        cfg.setTransfer(getBoolean(K_TRANSFER, d.isTransfer()));
        cfg.setCheck(getBoolean(K_CHECK, d.isCheck()));
        cfg.setCredit(getBoolean(K_CREDIT, d.isCredit()));
        cfg.setRounding(getString(K_ROUNDING, d.getRounding()));
        cfg.setAllowDiscount(getBoolean(K_ALLOW_DISCOUNT, d.isAllowDiscount()));
        cfg.setAllowGlobalDiscount(getBoolean(K_ALLOW_GLOB_DISC, d.isAllowGlobalDiscount()));
        cfg.setRequireClient(getBoolean(K_REQUIRE_CLIENT, d.isRequireClient()));
        cfg.setStockAlert(getBoolean(K_STOCK_ALERT, d.isStockAlert()));
        cfg.setNegativeStock(getBoolean(K_NEGATIVE_STOCK, d.isNegativeStock()));
        cfg.setShowBarcode(getBoolean(K_SHOW_BARCODE, d.isShowBarcode()));
        cfg.setSoundOnAdd(getBoolean(K_SOUND_ON_ADD, d.isSoundOnAdd()));
        cfg.setConfirmDelete(getBoolean(K_CONFIRM_DELETE, d.isConfirmDelete()));

        return cfg;
    }

    // ─────────────────────────────────────────────────────────────────
    /** Persiste todos los valores de la configuración. */
    public void save(SaleConfig cfg) {
        dao.setValue(K_COMPANY_NAME, orEmpty(cfg.getCompanyName()));
        dao.setValue(K_CIF, orEmpty(cfg.getCif()));
        dao.setValue(K_ADDRESS, orEmpty(cfg.getAddress()));
        dao.setValue(K_PHONE, orEmpty(cfg.getPhone()));
        dao.setValue(K_EMAIL, orEmpty(cfg.getEmail()));
        dao.setValue(K_LOGO_PATH, orEmpty(cfg.getLogoPath()));
        dao.setValue(K_TAX_RATE, String.valueOf(cfg.getTaxRate()));
        dao.setValue(K_TAX_TYPE, orEmpty(cfg.getTaxType()));
        dao.setValue(K_PRICES_INC_TAX, String.valueOf(cfg.isPricesIncludeTax()));
        dao.setValue(K_CURRENCY, orEmpty(cfg.getCurrency()));
        dao.setValue(K_DECIMALS, orEmpty(cfg.getDecimals()));
        dao.setValue(K_SHOW_LOGO, String.valueOf(cfg.isShowLogo()));
        dao.setValue(K_SHOW_ADDRESS, String.valueOf(cfg.isShowAddress()));
        dao.setValue(K_SHOW_PHONE, String.valueOf(cfg.isShowPhone()));
        dao.setValue(K_SHOW_CIF, String.valueOf(cfg.isShowCif()));
        dao.setValue(K_FOOTER_MSG, orEmpty(cfg.getFooterMessage()));
        dao.setValue(K_TICKET_COPIES, orEmpty(cfg.getTicketCopies()));
        dao.setValue(K_AUTO_PRINT, String.valueOf(cfg.isAutoPrint()));
        dao.setValue(K_CASH, String.valueOf(cfg.isCash()));
        dao.setValue(K_CARD, String.valueOf(cfg.isCard()));
        dao.setValue(K_TRANSFER, String.valueOf(cfg.isTransfer()));
        dao.setValue(K_CHECK, String.valueOf(cfg.isCheck()));
        dao.setValue(K_CREDIT, String.valueOf(cfg.isCredit()));
        dao.setValue(K_ROUNDING, orEmpty(cfg.getRounding()));
        dao.setValue(K_ALLOW_DISCOUNT, String.valueOf(cfg.isAllowDiscount()));
        dao.setValue(K_ALLOW_GLOB_DISC, String.valueOf(cfg.isAllowGlobalDiscount()));
        dao.setValue(K_REQUIRE_CLIENT, String.valueOf(cfg.isRequireClient()));
        dao.setValue(K_STOCK_ALERT, String.valueOf(cfg.isStockAlert()));
        dao.setValue(K_NEGATIVE_STOCK, String.valueOf(cfg.isNegativeStock()));
        dao.setValue(K_SHOW_BARCODE, String.valueOf(cfg.isShowBarcode()));
        dao.setValue(K_SOUND_ON_ADD, String.valueOf(cfg.isSoundOnAdd()));
        dao.setValue(K_CONFIRM_DELETE, String.valueOf(cfg.isConfirmDelete()));
    }

    // ─────────────────────────────────────────────────────────────────
    /**
     * Elimina todas las preferencias guardadas y recarga los valores por defecto.
     * Al usar base de datos, insertamos los valores de un modelo vacío.
     */
    public void reset() {
        try {
            save(new SaleConfig());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    private String getString(String key, String def) {
        String val = dao.getValue(key);
        return val != null ? val : def;
    }

    private double getDouble(String key, double def) {
        String val = dao.getValue(key);
        if (val != null) {
            try {
                return Double.parseDouble(val);
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    private boolean getBoolean(String key, boolean def) {
        String val = dao.getValue(key);
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return def;
    }

    private String orEmpty(String val) {
        return val != null ? val : "";
    }
}
