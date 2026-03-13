package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.TaxGroup;
import com.mycompany.ventacontrolfx.domain.model.TaxRate;
import com.mycompany.ventacontrolfx.domain.repository.ITaxRepository;
import com.mycompany.ventacontrolfx.util.AuthorizationService;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Enterprise Use Case for Tax Management (V2).
 * Handles CRUD operations for Tax Groups and Tax Rates.
 */
public class TaxManagementUseCase {

    private final ITaxRepository taxRepository;
    private final AuthorizationService authService;

    public TaxManagementUseCase(ITaxRepository taxRepository, AuthorizationService authService) {
        this.taxRepository = taxRepository;
        this.authService = authService;
    }

    // --- Tax Rates ---

    public List<TaxRate> getAllTaxRates() throws SQLException {
        return taxRepository.getAllTaxRates();
    }

    public List<TaxRate> getActiveTaxRates() throws SQLException {
        return taxRepository.getActiveTaxRates();
    }

    public void saveTaxRate(TaxRate rate) throws SQLException {
        authService.checkPermission("admin.iva");
        taxRepository.saveTaxRate(rate);
    }

    public void updateTaxRate(TaxRate rate) throws SQLException {
        authService.checkPermission("admin.iva");
        taxRepository.updateTaxRate(rate);
        taxRepository.syncMirroredValues();
    }

    public void deleteTaxRate(int taxRateId) throws SQLException {
        authService.checkPermission("admin.iva");
        taxRepository.deleteTaxRate(taxRateId);
    }

    public Optional<TaxRate> getTaxRateById(int id) throws SQLException {
        return taxRepository.getTaxRateById(id);
    }

    // --- Tax Groups ---

    public List<TaxGroup> getAllTaxGroups() throws SQLException {
        return taxRepository.getAllTaxGroups();
    }

    public Optional<TaxGroup> getTaxGroupById(int id) throws SQLException {
        return taxRepository.getTaxGroupById(id);
    }

    public Optional<TaxGroup> getDefaultTaxGroup() throws SQLException {
        return taxRepository.getDefaultTaxGroup();
    }

    public void saveTaxGroup(TaxGroup group) throws SQLException {
        authService.checkPermission("admin.iva");
        taxRepository.saveTaxGroup(group);
    }

    public void updateTaxGroup(TaxGroup group) throws SQLException {
        authService.checkPermission("admin.iva");
        taxRepository.updateTaxGroup(group);
        taxRepository.syncMirroredValues();
    }

    public void deleteTaxGroup(int taxGroupId) throws SQLException {
        authService.checkPermission("admin.iva");
        taxRepository.deleteTaxGroup(taxGroupId);
    }

    public void setDefaultTaxGroup(int taxGroupId) throws SQLException {
        authService.checkPermission("admin.iva");
        taxRepository.setDefaultTaxGroup(taxGroupId);
    }
}
