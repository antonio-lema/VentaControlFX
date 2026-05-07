package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.domain.repository.ICompanyConfigRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import com.mycompany.ventacontrolfx.domain.model.BusinessDay;

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
    private static final String K_ROUNDING_METHOD = "roundingMethod"; // Tax Engine V2
    private static final String K_ALLOW_DISCOUNT = "allowDiscount";
    private static final String K_ALLOW_GLOB_DISC = "allowGlobalDiscount";
    private static final String K_REQUIRE_CLIENT = "requireClient";
    private static final String K_STOCK_ALERT = "stockAlert";
    private static final String K_NEGATIVE_STOCK = "negativeStock";
    private static final String K_SHOW_BARCODE = "showBarcode";
    private static final String K_SOUND_ON_ADD = "soundOnAdd";
    private static final String K_CONFIRM_DELETE = "confirmDelete";
    private static final String K_SMTP_HOST = "smtp_host";
    private static final String K_SMTP_PORT = "smtp_port";
    private static final String K_EMAIL_FROM = "email_from";
    private static final String K_EMAIL_PASS = "email_password";
    private static final String K_VERIFACTU_NIF = "verifactu.nif";
    private static final String K_VERIFACTU_CERT_NAME = "verifactu.cert_name";
    private static final String K_VERIFACTU_CERT_PATH = "verifactu.cert_path";
    private static final String K_VERIFACTU_CERT_PASS = "verifactu.cert_pass";
    private static final String K_VERIFACTU_URL = "verifactu.url";

    private static SaleConfig cachedConfig = null;

    @Override
    public SaleConfig load() {
        if (cachedConfig != null) {
            return cachedConfig;
        }

        java.util.Map<String, String> cache = new java.util.HashMap<>();
        String sql = "SELECT config_key, config_value FROM system_config";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                cache.put(rs.getString("config_key"), rs.getString("config_value"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        SaleConfig cfg = new SaleConfig();
        SaleConfig d = new SaleConfig();
        cfg.setCompanyName(getFromCache(cache, K_COMPANY_NAME, d.getCompanyName()));
        cfg.setCif(getFromCache(cache, K_CIF, d.getCif()));
        cfg.setAddress(getFromCache(cache, K_ADDRESS, d.getAddress()));
        cfg.setPhone(getFromCache(cache, K_PHONE, d.getPhone()));
        cfg.setEmail(getFromCache(cache, K_EMAIL, d.getEmail()));
        cfg.setLogoPath(getFromCache(cache, K_LOGO_PATH, d.getLogoPath()));
        cfg.setAppIconPath(getFromCache(cache, K_APP_ICON_PATH, d.getAppIconPath()));
        cfg.setAppName(getFromCache(cache, K_APP_NAME, d.getAppName()));
        cfg.setTaxRate(getDoubleFromCache(cache, K_TAX_RATE, d.getTaxRate()));
        cfg.setTaxType(getFromCache(cache, K_TAX_TYPE, d.getTaxType()));
        cfg.setPricesIncludeTax(getBooleanFromCache(cache, K_PRICES_INC_TAX, d.isPricesIncludeTax()));
        cfg.setCurrency(getFromCache(cache, K_CURRENCY, d.getCurrency()));
        cfg.setDecimals(getFromCache(cache, K_DECIMALS, d.getDecimals()));
        cfg.setShowLogo(getBooleanFromCache(cache, K_SHOW_LOGO, d.isShowLogo()));
        cfg.setShowAddress(getBooleanFromCache(cache, K_SHOW_ADDRESS, d.isShowAddress()));
        cfg.setShowPhone(getBooleanFromCache(cache, K_SHOW_PHONE, d.isShowPhone()));
        cfg.setShowCif(getBooleanFromCache(cache, K_SHOW_CIF, d.isShowCif()));
        cfg.setFooterMessage(getFromCache(cache, K_FOOTER_MSG, d.getFooterMessage()));
        cfg.setTicketCopies(getFromCache(cache, K_TICKET_COPIES, d.getTicketCopies()));
        cfg.setTicketFormat(getFromCache(cache, K_TICKET_FORMAT, d.getTicketFormat()));
        cfg.setAutoPrint(getBooleanFromCache(cache, K_AUTO_PRINT, d.isAutoPrint()));
        cfg.setCash(getBooleanFromCache(cache, K_CASH, d.isCash()));
        cfg.setCard(getBooleanFromCache(cache, K_CARD, d.isCard()));
        cfg.setTransfer(getBooleanFromCache(cache, K_TRANSFER, d.isTransfer()));
        cfg.setCheck(getBooleanFromCache(cache, K_CHECK, d.isCheck()));
        cfg.setCredit(getBooleanFromCache(cache, K_CREDIT, d.isCredit()));
        cfg.setRounding(getFromCache(cache, K_ROUNDING, d.getRounding()));
        cfg.setRoundingMethod(getFromCache(cache, K_ROUNDING_METHOD, d.getRoundingMethod()));
        cfg.setAllowDiscount(getBooleanFromCache(cache, K_ALLOW_DISCOUNT, d.isAllowDiscount()));
        cfg.setAllowGlobalDiscount(getBooleanFromCache(cache, K_ALLOW_GLOB_DISC, d.isAllowGlobalDiscount()));
        cfg.setRequireClient(getBooleanFromCache(cache, K_REQUIRE_CLIENT, d.isRequireClient()));
        cfg.setStockAlert(getBooleanFromCache(cache, K_STOCK_ALERT, d.isStockAlert()));
        cfg.setNegativeStock(getBooleanFromCache(cache, K_NEGATIVE_STOCK, d.isNegativeStock()));
        cfg.setShowBarcode(getBooleanFromCache(cache, K_SHOW_BARCODE, d.isShowBarcode()));
        cfg.setSoundOnAdd(getBooleanFromCache(cache, K_SOUND_ON_ADD, d.isSoundOnAdd()));
        cfg.setConfirmDelete(getBooleanFromCache(cache, K_CONFIRM_DELETE, d.isConfirmDelete()));
        cfg.setSmtpHost(getFromCache(cache, K_SMTP_HOST, d.getSmtpHost()));
        cfg.setSmtpPort(getFromCache(cache, K_SMTP_PORT, d.getSmtpPort()));
        cfg.setEmailFrom(getFromCache(cache, K_EMAIL_FROM, d.getEmailFrom()));
        cfg.setEmailPassword(getFromCache(cache, K_EMAIL_PASS, d.getEmailPassword()));
        cfg.setVerifactuNif(getFromCache(cache, K_VERIFACTU_NIF, d.getVerifactuNif()));
        cfg.setVerifactuCertName(getFromCache(cache, K_VERIFACTU_CERT_NAME, d.getVerifactuCertName()));
        cfg.setVerifactuCertPath(getFromCache(cache, K_VERIFACTU_CERT_PATH, d.getVerifactuCertPath()));
        cfg.setVerifactuCertPass(getFromCache(cache, K_VERIFACTU_CERT_PASS, d.getVerifactuCertPass()));
        cfg.setVerifactuUrl(getFromCache(cache, K_VERIFACTU_URL, d.getVerifactuUrl()));

        // Cargar horario semanal din\u00e1mico
        List<BusinessDay> schedule = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            boolean isClosed = getBooleanFromCache(cache, "schedule." + i + ".closed", false);
            BusinessDay day = new BusinessDay(i, isClosed);

            String countStr = getFromCache(cache, "schedule." + i + ".count", null);
            if (countStr != null) {
                int shiftCount = Integer.parseInt(countStr);
                for (int s = 0; s < shiftCount; s++) {
                    String op = getFromCache(cache, "schedule." + i + "." + s + ".open", "09:00");
                    String cl = getFromCache(cache, "schedule." + i + "." + s + ".close", "20:00");
                    List<Integer> users = new ArrayList<>();
                    String userIdsStr = getFromCache(cache, "schedule." + i + "." + s + ".users", "");
                    if (!userIdsStr.isEmpty()) {
                        for (String id : userIdsStr.split(","))
                            users.add(Integer.parseInt(id));
                    }
                    day.getShifts().add(new BusinessDay.TimeRange(LocalTime.parse(op), LocalTime.parse(cl), users));
                }
            } else {
                String op1 = getFromCache(cache, "schedule." + i + ".open", "09:00");
                String cl1 = getFromCache(cache, "schedule." + i + ".close", "14:00");
                day.getShifts().add(new BusinessDay.TimeRange(LocalTime.parse(op1), LocalTime.parse(cl1)));
                if (getBooleanFromCache(cache, "schedule." + i + ".split", false)) {
                    String op2 = getFromCache(cache, "schedule." + i + ".open2", "17:00");
                    String cl2 = getFromCache(cache, "schedule." + i + ".close2", "21:00");
                    day.getShifts().add(new BusinessDay.TimeRange(LocalTime.parse(op2), LocalTime.parse(cl2)));
                }
            }
            schedule.add(day);
        }
        cfg.setSchedule(schedule);

        // Cargar d\u00edas especiales
        List<SaleConfig.SpecialDay> specials = new ArrayList<>();
        int specCount = Integer.parseInt(getFromCache(cache, "schedule.special.count", "0"));
        for (int i = 0; i < specCount; i++) {
            String dateStr = getFromCache(cache, "schedule.special." + i + ".date", null);
            if (dateStr == null)
                continue;
            boolean closed = getBooleanFromCache(cache, "schedule.special." + i + ".closed", true);
            String reason = getFromCache(cache, "schedule.special." + i + ".reason", "Festivo");
            SaleConfig.SpecialDay sd = new SaleConfig.SpecialDay(java.time.LocalDate.parse(dateStr), closed, reason);

            int sCount = Integer.parseInt(getFromCache(cache, "schedule.special." + i + ".count", "0"));
            for (int s = 0; s < sCount; s++) {
                String op = getFromCache(cache, "schedule.special." + i + "." + s + ".open", "09:00");
                String cl = getFromCache(cache, "schedule.special." + i + "." + s + ".close", "20:00");
                sd.getShifts().add(new BusinessDay.TimeRange(LocalTime.parse(op), LocalTime.parse(cl)));
            }
            specials.add(sd);
        }
        cfg.setSpecialDays(specials);
        cfg.setScheduleGracePeriodMins(Integer.parseInt(getFromCache(cache, "schedule.grace", "10")));

        cachedConfig = cfg;
        return cfg;
    }

    private String getFromCache(java.util.Map<String, String> cache, String key, String def) {
        String val = cache.get(key);
        return val != null ? val : def;
    }

    private boolean getBooleanFromCache(java.util.Map<String, String> cache, String key, boolean def) {
        String val = cache.get(key);
        return val != null ? Boolean.parseBoolean(val) : def;
    }

    private double getDoubleFromCache(java.util.Map<String, String> cache, String key, double def) {
        String val = cache.get(key);
        if (val != null) {
            try {
                return Double.parseDouble(val);
            } catch (Exception e) {
                return def;
            }
        }
        return def;
    }

    @Override
    public void save(SaleConfig cfg) {
        cachedConfig = null; // Invalidate cache
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
        setValue(K_ROUNDING_METHOD, orEmpty(cfg.getRoundingMethod()));
        setValue(K_ALLOW_DISCOUNT, String.valueOf(cfg.isAllowDiscount()));
        setValue(K_ALLOW_GLOB_DISC, String.valueOf(cfg.isAllowGlobalDiscount()));
        setValue(K_REQUIRE_CLIENT, String.valueOf(cfg.isRequireClient()));
        setValue(K_STOCK_ALERT, String.valueOf(cfg.isStockAlert()));
        setValue(K_NEGATIVE_STOCK, String.valueOf(cfg.isNegativeStock()));
        setValue(K_SHOW_BARCODE, String.valueOf(cfg.isShowBarcode()));
        setValue(K_SOUND_ON_ADD, String.valueOf(cfg.isSoundOnAdd()));
        setValue(K_CONFIRM_DELETE, String.valueOf(cfg.isConfirmDelete()));
        setValue(K_SMTP_HOST, orEmpty(cfg.getSmtpHost()));
        setValue(K_SMTP_PORT, orEmpty(cfg.getSmtpPort()));
        setValue(K_EMAIL_FROM, orEmpty(cfg.getEmailFrom()));
        setValue(K_EMAIL_PASS, orEmpty(cfg.getEmailPassword()));
        setValue(K_VERIFACTU_NIF, orEmpty(cfg.getVerifactuNif()));
        setValue(K_VERIFACTU_CERT_NAME, orEmpty(cfg.getVerifactuCertName()));
        setValue(K_VERIFACTU_CERT_PATH, orEmpty(cfg.getVerifactuCertPath()));
        setValue(K_VERIFACTU_CERT_PASS, orEmpty(cfg.getVerifactuCertPass()));
        setValue(K_VERIFACTU_URL, orEmpty(cfg.getVerifactuUrl()));

        // Guardar horario semanal din\u00e1mico
        if (cfg.getSchedule() != null) {
            for (BusinessDay day : cfg.getSchedule()) {
                setValue("schedule." + day.getDayOfWeek() + ".closed", String.valueOf(day.isClosed()));
                setValue("schedule." + day.getDayOfWeek() + ".count", String.valueOf(day.getShifts().size()));
                for (int s = 0; s < day.getShifts().size(); s++) {
                    BusinessDay.TimeRange range = day.getShifts().get(s);
                    setValue("schedule." + day.getDayOfWeek() + "." + s + ".open", range.getOpen().toString());
                    setValue("schedule." + day.getDayOfWeek() + "." + s + ".close", range.getClose().toString());
                    String usersStr = range.getAssignedUserIds().stream().map(String::valueOf)
                            .collect(java.util.stream.Collectors.joining(","));
                    setValue("schedule." + day.getDayOfWeek() + "." + s + ".users", usersStr);
                }
            }
        }

        // Guardar d\u00edas especiales
        setValue("schedule.special.count", String.valueOf(cfg.getSpecialDays().size()));
        for (int i = 0; i < cfg.getSpecialDays().size(); i++) {
            SaleConfig.SpecialDay sd = cfg.getSpecialDays().get(i);
            setValue("schedule.special." + i + ".date", sd.getDate().toString());
            setValue("schedule.special." + i + ".closed", String.valueOf(sd.isClosed()));
            setValue("schedule.special." + i + ".reason", sd.getReason());
            setValue("schedule.special." + i + ".count", String.valueOf(sd.getShifts().size()));
            for (int s = 0; s < sd.getShifts().size(); s++) {
                BusinessDay.TimeRange r = sd.getShifts().get(s);
                setValue("schedule.special." + i + "." + s + ".open", r.getOpen().toString());
                setValue("schedule.special." + i + "." + s + ".close", r.getClose().toString());
            }
        }
        setValue("schedule.grace", String.valueOf(cfg.getScheduleGracePeriodMins()));
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

