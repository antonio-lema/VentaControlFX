package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.TaxGroup;
import com.mycompany.ventacontrolfx.domain.model.TaxRate;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de repositorio para gestionar los Tipos de Impuestos (Tax Rates)
 * y los Grupos Fiscales (Tax Groups) del Motor Fiscal V2.
 */
public interface ITaxRepository {

        // --- Tipos Impositivos (Tax Rates) ---
        List<TaxRate> getAllTaxRates() throws SQLException;

        List<TaxRate> getActiveTaxRates() throws SQLException;

        Optional<TaxRate> getTaxRateById(int taxRateId) throws SQLException;

        void saveTaxRate(TaxRate rate) throws SQLException;

        void updateTaxRate(TaxRate rate) throws SQLException;

        void deleteTaxRate(int taxRateId) throws SQLException;

        // --- Grupos Fiscales (Tax Groups) ---
        List<TaxGroup> getAllTaxGroups() throws SQLException;

        Optional<TaxGroup> getTaxGroupById(int taxGroupId) throws SQLException;

        Optional<TaxGroup> getDefaultTaxGroup() throws SQLException;

        void saveTaxGroup(TaxGroup group) throws SQLException;

        void updateTaxGroup(TaxGroup group) throws SQLException;

        void deleteTaxGroup(int taxGroupId) throws SQLException;

        void setDefaultTaxGroup(int taxGroupId) throws SQLException;
}
