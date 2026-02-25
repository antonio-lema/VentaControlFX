package com.mycompany.ventacontrolfx.controller;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.io.File;
import javafx.stage.FileChooser;

import com.mycompany.ventacontrolfx.service.SaleConfigService;
import com.mycompany.ventacontrolfx.model.SaleConfig;
import com.mycompany.ventacontrolfx.dao.ConfigDAO;
import javafx.scene.control.PasswordField;

public class SaleConfigController {

    // ── Datos empresa ──────────────────────────────────────────────────
    @FXML
    private TextField txtCompanyName;
    @FXML
    private TextField txtCif;
    @FXML
    private TextField txtAddress;
    @FXML
    private TextField txtPhone;
    @FXML
    private TextField txtEmail;
    @FXML
    private TextField txtLogoPath;
    @FXML
    private TextField txtAppIconPath;
    @FXML
    private TextField txtAppName;

    // ── Email / SMTP ──────────────────────────────────────────────────
    @FXML
    private TextField txtSmtpHost;
    @FXML
    private TextField txtSmtpPort;
    @FXML
    private TextField txtEmailFrom;
    @FXML
    private PasswordField txtEmailPassword;

    // ── Fiscal ────────────────────────────────────────────────────────
    @FXML
    private TextField txtTaxRate;
    @FXML
    private ComboBox<String> cmbTaxType;
    @FXML
    private CheckBox chkPricesIncludeTax;
    @FXML
    private ComboBox<String> cmbCurrency;
    @FXML
    private ComboBox<String> cmbDecimals;

    // ── Ticket ────────────────────────────────────────────────────────
    @FXML
    private CheckBox chkShowLogo;
    @FXML
    private CheckBox chkShowAddress;
    @FXML
    private CheckBox chkShowPhone;
    @FXML
    private CheckBox chkShowCif;
    @FXML
    private TextField txtFooterMessage;
    @FXML
    private ComboBox<String> cmbTicketCopies;
    @FXML
    private ComboBox<String> cmbTicketFormat;
    @FXML
    private CheckBox chkAutoPrint;

    // ── Banner ────────────────────────────────────────────────────────
    @FXML
    private HBox bannerSaved;

    private final SaleConfigService configService = new SaleConfigService();
    private final ConfigDAO configDAO = new ConfigDAO();

    // ─────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        loadConfig();
    }

    // ─────────────────────────────────────────────────────────────────
    private void loadConfig() {
        SaleConfig cfg = configService.load();

        // Empresa
        txtCompanyName.setText(cfg.getCompanyName());
        txtCif.setText(cfg.getCif());
        txtAddress.setText(cfg.getAddress());
        txtPhone.setText(cfg.getPhone());
        txtEmail.setText(cfg.getEmail());
        if (txtLogoPath != null) {
            txtLogoPath.setText(cfg.getLogoPath());
        }
        if (txtAppIconPath != null) {
            txtAppIconPath.setText(cfg.getAppIconPath());
        }
        if (txtAppName != null) {
            txtAppName.setText(cfg.getAppName());
        }

        // Fiscal
        txtTaxRate.setText(String.valueOf(cfg.getTaxRate()));
        setComboValue(cmbTaxType, cfg.getTaxType());
        chkPricesIncludeTax.setSelected(cfg.isPricesIncludeTax());
        setComboValue(cmbCurrency, cfg.getCurrency());
        setComboValue(cmbDecimals, cfg.getDecimals());

        // Ticket
        chkShowLogo.setSelected(cfg.isShowLogo());
        chkShowAddress.setSelected(cfg.isShowAddress());
        chkShowPhone.setSelected(cfg.isShowPhone());
        chkShowCif.setSelected(cfg.isShowCif());
        txtFooterMessage.setText(cfg.getFooterMessage());
        setComboValue(cmbTicketCopies, cfg.getTicketCopies());
        setComboValue(cmbTicketFormat, cfg.getTicketFormat());
        chkAutoPrint.setSelected(cfg.isAutoPrint());

        // SMTP
        txtSmtpHost.setText(configDAO.getValue("smtp_host"));
        txtSmtpPort.setText(configDAO.getValue("smtp_port"));
        txtEmailFrom.setText(configDAO.getValue("email_from"));
        txtEmailPassword.setText(configDAO.getValue("email_password"));
    }

    // ─────────────────────────────────────────────────────────────────
    @FXML
    private void handleSave() {
        SaleConfig cfg = buildConfig();
        configService.save(cfg);

        // Save SMTP
        configDAO.setValue("smtp_host", txtSmtpHost.getText() != null ? txtSmtpHost.getText().trim() : "");
        configDAO.setValue("smtp_port", txtSmtpPort.getText() != null ? txtSmtpPort.getText().trim() : "");
        configDAO.setValue("email_from", txtEmailFrom.getText() != null ? txtEmailFrom.getText().trim() : "");
        configDAO.setValue("email_password",
                txtEmailPassword.getText() != null ? txtEmailPassword.getText().trim() : "");

        showBanner();
    }

    // ─────────────────────────────────────────────────────────────────
    @FXML
    private void handleReset() {
        configService.reset();
        loadConfig();
    }

    // ─────────────────────────────────────────────────────────────────
    @FXML
    private void handleBrowseLogo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Logo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = fileChooser.showOpenDialog(txtLogoPath.getScene().getWindow());
        if (file != null) {
            txtLogoPath.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleBrowseIcon() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Icono de Aplicación");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Iconos", "*.png", "*.jpg", "*.jpeg", "*.ico"));
        File file = fileChooser.showOpenDialog(txtAppIconPath.getScene().getWindow());
        if (file != null) {
            txtAppIconPath.setText(file.getAbsolutePath());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    private SaleConfig buildConfig() {
        SaleConfig cfg = new SaleConfig();

        // Empresa
        cfg.setCompanyName(txtCompanyName.getText().trim());
        cfg.setCif(txtCif.getText().trim());
        cfg.setAddress(txtAddress.getText().trim());
        cfg.setPhone(txtPhone.getText().trim());
        cfg.setEmail(txtEmail.getText().trim());
        if (txtLogoPath != null) {
            cfg.setLogoPath(txtLogoPath.getText().trim());
        }
        if (txtAppIconPath != null) {
            cfg.setAppIconPath(txtAppIconPath.getText().trim());
        }
        if (txtAppName != null) {
            cfg.setAppName(txtAppName.getText().trim());
        }

        // Fiscal
        try {
            cfg.setTaxRate(Double.parseDouble(txtTaxRate.getText().trim().replace(",", ".")));
        } catch (NumberFormatException e) {
            cfg.setTaxRate(21.0);
        }
        cfg.setTaxType(cmbTaxType.getValue());
        cfg.setPricesIncludeTax(chkPricesIncludeTax.isSelected());
        cfg.setCurrency(cmbCurrency.getValue());
        cfg.setDecimals(cmbDecimals.getValue());

        // Ticket
        cfg.setShowLogo(chkShowLogo.isSelected());
        cfg.setShowAddress(chkShowAddress.isSelected());
        cfg.setShowPhone(chkShowPhone.isSelected());
        cfg.setShowCif(chkShowCif.isSelected());
        cfg.setFooterMessage(txtFooterMessage.getText().trim());
        cfg.setTicketCopies(cmbTicketCopies.getValue());
        cfg.setTicketFormat(cmbTicketFormat.getValue());
        cfg.setAutoPrint(chkAutoPrint.isSelected());

        // Métodos de pago y opciones generales: secciones eliminadas

        return cfg;
    }

    // ─────────────────────────────────────────────────────────────────
    private void showBanner() {
        bannerSaved.setVisible(true);
        bannerSaved.setManaged(true);

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> {
            bannerSaved.setVisible(false);
            bannerSaved.setManaged(false);
        });
        pause.play();
    }

    // ─────────────────────────────────────────────────────────────────
    /** Selecciona el elemento en el ComboBox que contenga el texto dado */
    private void setComboValue(ComboBox<String> combo, String value) {
        if (value == null || combo == null)
            return;
        for (String item : combo.getItems()) {
            if (item != null && item.equals(value)) {
                combo.setValue(item);
                return;
            }
        }
        // Si no coincide exactamente, seleccionar el primero si existe
        if (!combo.getItems().isEmpty()) {
            combo.setValue(combo.getItems().get(0));
        }
    }
}
