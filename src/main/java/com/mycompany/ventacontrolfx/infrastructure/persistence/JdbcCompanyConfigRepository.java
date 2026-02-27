package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.domain.repository.ICompanyConfigRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcCompanyConfigRepository implements ICompanyConfigRepository {

    private static final String K_COMPANY_NAME = "companyName";
    private static final String K_CIF = "cif";
    private static final String K_ADDRESS = "address";
    private static final String K_PHONE = "phone";
    private static final String K_EMAIL = "email";
    private static final String K_LOGO_PATH = "logoPath";
    private static final String K_APP_ICON_PATH = "appIconPath";
    private static final String K_APP_NAME = "appName";
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
    private static final String K_TICKET_FORMAT = "ticketFormat";
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

    @Override
    public SaleConfig load() {
        SaleConfig d = new SaleConfig();
        SaleConfig cfg = new SaleConfig();
        cfg.setCompanyName(getString(K_COMPANY_NAME, d.getCompanyName()));
        cfg.setCif(getString(K_CIF, d.getCif()));
        cfg.setAddress(getString(K_ADDRESS, d.getAddress()));
        cfg.setPhone(getString(K_PHONE, d.getPhone()));
        cfg.setEmail(getString(K_EMAIL, d.getEmail()));
        cfg.setLogoPath(getString(K_LOGO_PATH, d.getLogoPath()));
        cfg.setAppIconPath(getString(K_APP_ICON_PATH, d.getAppIconPath()));
        cfg.setAppName(getString(K_APP_NAME, d.getAppName()));
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
        cfg.setTicketFormat(getString(K_TICKET_FORMAT, d.getTicketFormat()));
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

    @Override
    public void save(SaleConfig cfg) {
        setValue(K_COMPANY_NAME, orEmpty(cfg.getCompanyName()));
        setValue(K_CIF, orEmpty(cfg.getCif()));
        setValue(K_ADDRESS, orEmpty(cfg.getAddress()));
        setValue(K_PHONE, orEmpty(cfg.getPhone()));
        setValue(K_EMAIL, orEmpty(cfg.getEmail()));
        setValue(K_LOGO_PATH, orEmpty(cfg.getLogoPath()));
        setValue(K_APP_ICON_PATH, orEmpty(cfg.getAppIconPath()));
        setValue(K_APP_NAME, orEmpty(cfg.getAppName()));
        setValue(K_TAX_RATE, String.valueOf(cfg.getTaxRate()));
        setValue(K_TAX_TYPE, orEmpty(cfg.getTaxType()));
        setValue(K_PRICES_INC_TAX, String.valueOf(cfg.isPricesIncludeTax()));
        setValue(K_CURRENCY, orEmpty(cfg.getCurrency()));
        setValue(K_DECIMALS, orEmpty(cfg.getDecimals()));
        setValue(K_SHOW_LOGO, String.valueOf(cfg.isShowLogo()));
        setValue(K_SHOW_ADDRESS, String.valueOf(cfg.isShowAddress()));
        setValue(K_SHOW_PHONE, String.valueOf(cfg.isShowPhone()));
        setValue(K_SHOW_CIF, String.valueOf(cfg.isShowCif()));
        setValue(K_FOOTER_MSG, orEmpty(cfg.getFooterMessage()));
        setValue(K_TICKET_COPIES, orEmpty(cfg.getTicketCopies()));
        setValue(K_TICKET_FORMAT, orEmpty(cfg.getTicketFormat()));
        setValue(K_AUTO_PRINT, String.valueOf(cfg.isAutoPrint()));
        setValue(K_CASH, String.valueOf(cfg.isCash()));
        setValue(K_CARD, String.valueOf(cfg.isCard()));
        setValue(K_TRANSFER, String.valueOf(cfg.isTransfer()));
        setValue(K_CHECK, String.valueOf(cfg.isCheck()));
        setValue(K_CREDIT, String.valueOf(cfg.isCredit()));
        setValue(K_ROUNDING, orEmpty(cfg.getRounding()));
        setValue(K_ALLOW_DISCOUNT, String.valueOf(cfg.isAllowDiscount()));
        setValue(K_ALLOW_GLOB_DISC, String.valueOf(cfg.isAllowGlobalDiscount()));
        setValue(K_REQUIRE_CLIENT, String.valueOf(cfg.isRequireClient()));
        setValue(K_STOCK_ALERT, String.valueOf(cfg.isStockAlert()));
        setValue(K_NEGATIVE_STOCK, String.valueOf(cfg.isNegativeStock()));
        setValue(K_SHOW_BARCODE, String.valueOf(cfg.isShowBarcode()));
        setValue(K_SOUND_ON_ADD, String.valueOf(cfg.isSoundOnAdd()));
        setValue(K_CONFIRM_DELETE, String.valueOf(cfg.isConfirmDelete()));
    }

    @Override
    public void reset() {
        save(new SaleConfig());
    }

    @Override
    public String getValue(String key) {
        String sql = "SELECT config_value FROM system_config WHERE config_key = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("config_value");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getString(String key, String def) {
        String val = getValue(key);
        return val != null ? val : def;
    }

    private void setValue(String key, String value) {
        String sql = "INSERT INTO system_config (config_key, config_value) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE config_value = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.setString(3, value);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private double getDouble(String key, double def) {
        String val = getString(key, null);
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
        String val = getString(key, null);
        return val != null ? Boolean.parseBoolean(val) : def;
    }

    private String orEmpty(String val) {
        return val != null ? val : "";
    }
}
