package com.mycompany.ventacontrolfx.domain.service;

import com.mycompany.ventacontrolfx.domain.model.*;
import com.mycompany.ventacontrolfx.domain.repository.ICategoryRepository;
import com.mycompany.ventacontrolfx.domain.repository.ITaxRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio de domino (Domain Service) responsable de calcular los impuestos
 * aplicables a una venta o línea de venta. Implementa Clean Architecture
 * separando la lógica pura de la persistencia.
 */
public class TaxEngineService {

    private final ITaxRepository taxRepository;
    private final ICategoryRepository categoryRepository;

    public TaxEngineService(ITaxRepository taxRepository, ICategoryRepository categoryRepository) {
        this.taxRepository = taxRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Calcula los impuestos de una única línea de venta de producto.
     */
    public TaxCalculationResult calculateLine(
            Product product,
            Client client,
            double unitPrice,
            double quantity,
            boolean pricesIncludeTax) throws SQLException {

        // 1. Validar exención de cliente y regímenes especiales
        if (client != null && client.isTaxExempt()) {
            double net = pricesIncludeTax ? extractNet(unitPrice, getEffectiveTotalRate(product)) * quantity
                    : unitPrice * quantity;
            return new TaxCalculationResult(net, net, new ArrayList<>());
        }

        if (client != null) {
            String regime = client.getTaxRegime();
            if ("INTRACOMMUNITY".equals(regime) || "EXPORT".equals(regime)) {
                double net = pricesIncludeTax ? extractNet(unitPrice, getEffectiveTotalRate(product)) * quantity
                        : unitPrice * quantity;
                return new TaxCalculationResult(net, net, new ArrayList<>());
            }
        }

        // 2. Determinar el grupo de impuestos aplicable
        TaxGroup taxGroup = resolveTaxGroup(product);
        if (taxGroup == null || taxGroup.getRates() == null || taxGroup.getRates().isEmpty()) {
            return fallbackCalculation(product, unitPrice, quantity, pricesIncludeTax);
        }

        // 3. Cálculos matemáticos
        double totalRatePercentage = taxGroup.getRates().stream().mapToDouble(TaxRate::getRate).sum();

        double netUnit;
        if (pricesIncludeTax) {
            netUnit = extractNet(unitPrice, totalRatePercentage);
        } else {
            netUnit = unitPrice;
        }

        double netTotalRaw = netUnit * quantity;
        double netTotal = round(netTotalRaw);

        List<AppliedTax> appliedTaxes = new ArrayList<>();
        double totalTaxes = 0.0;

        for (TaxRate rate : taxGroup.getRates()) {
            double taxAmount = round(netTotal * (rate.getRate() / 100.0));
            appliedTaxes.add(new AppliedTax(rate.getId(), rate.getName(), rate.getRate(), taxAmount));
            totalTaxes += taxAmount;
        }

        double grossTotal = round(netTotal + totalTaxes);

        return new TaxCalculationResult(netTotal, grossTotal, appliedTaxes);
    }

    /**
     * Resuelve el grupo de impuestos aplicable al producto con herencia:
     * 1. Grupo asignado directamente al Producto.
     * 2. Grupo asignado a la Categoría del producto.
     * 3. Grupo predeterminado del sistema.
     */
    private TaxGroup resolveTaxGroup(Product product) throws SQLException {
        // 1. Grupo directo del producto
        if (product.getTaxGroupId() != null && product.getTaxGroupId() > 0) {
            Optional<TaxGroup> group = taxRepository.getTaxGroupById(product.getTaxGroupId());
            if (group.isPresent()) {
                return group.get();
            }
        }

        // 2. Grupo de la Categoría
        if (product.getCategoryId() > 0 && categoryRepository != null) {
            Category category = categoryRepository.getById(product.getCategoryId());
            if (category != null && category.getTaxGroupId() != null && category.getTaxGroupId() > 0) {
                Optional<TaxGroup> group = taxRepository.getTaxGroupById(category.getTaxGroupId());
                if (group.isPresent()) {
                    return group.get();
                }
            }
        }

        // 3. Predeterminado del sistema
        return taxRepository.getDefaultTaxGroup().orElse(null);
    }

    /**
     * Obtiene la tasa total efectiva para un producto utilizando legacy fallbacks
     * si es necesario. (Utilizado temporalmente para revertir importes con IVA
     * incluido
     * si el cliente termina estando exento).
     */
    private double getEffectiveTotalRate(Product product) throws SQLException {
        TaxGroup group = resolveTaxGroup(product);
        if (group != null && group.getRates() != null) {
            return group.getRates().stream().mapToDouble(TaxRate::getRate).sum();
        }
        return product.resolveEffectiveIva(21.0); // Fallback predeterminado
    }

    /**
     * Cálculo transitorio (Legacy Fallback) por si no hay tabla de impuestos V2
     * configurada aún.
     */
    private TaxCalculationResult fallbackCalculation(Product product, double unitPrice, double quantity,
            boolean pricesIncludeTax) {
        double rate = product.resolveEffectiveIva(21.0);
        double netUnit;
        if (pricesIncludeTax) {
            netUnit = extractNet(unitPrice, rate);
        } else {
            netUnit = unitPrice;
        }

        double netTotal = round(netUnit * quantity);
        double taxAmount = round(netTotal * (rate / 100.0));
        double grossTotal = round(netTotal + taxAmount);

        List<AppliedTax> appliedTaxes = new ArrayList<>();
        if (rate > 0 || rate == 0) {
            // Asignar ID basado en tasas estándar para evitar errores de FK
            int taxId = 1; // Default 21%
            if (rate == 10.0)
                taxId = 2;
            else if (rate == 4.0)
                taxId = 3;
            else if (rate == 0.0)
                taxId = 4;

            appliedTaxes.add(new AppliedTax(taxId, "IVA " + rate + "%", rate, taxAmount));
        }

        return new TaxCalculationResult(netTotal, grossTotal, appliedTaxes);
    }

    /**
     * Obtiene todos los grupos de impuestos disponibles.
     */
    public List<TaxGroup> getAllGroups() throws SQLException {
        return taxRepository.getAllTaxGroups();
    }

    /**
     * Agrupa y resume los impuestos de múltiples líneas para el pie de factura.
     */
    public List<SaleTaxSummary> summarizeTaxes(List<TaxCalculationResult> lineResults) {
        Map<Integer, SaleTaxSummary> map = new HashMap<>();

        for (TaxCalculationResult result : lineResults) {
            for (AppliedTax tax : result.getAppliedTaxes()) {
                SaleTaxSummary summary = map.getOrDefault(tax.getTaxRateId(),
                        new SaleTaxSummary(0, tax.getTaxRateId(), tax.getTaxName(), tax.getTaxRate(), 0.0, 0.0));

                // La suma de la base de este impuesto es el netTotal de la línea donde aplicó.
                summary.setTaxBasis(summary.getTaxBasis() + result.getNetTotal());
                summary.setTaxAmount(summary.getTaxAmount() + tax.getTaxAmount());

                map.put(tax.getTaxRateId(), summary);
            }
        }

        // Redondear el resumen final
        List<SaleTaxSummary> finalized = new ArrayList<>();
        for (SaleTaxSummary sum : map.values()) {
            sum.setTaxBasis(round(sum.getTaxBasis()));
            sum.setTaxAmount(round(sum.getTaxAmount()));
            finalized.add(sum);
        }

        return finalized;
    }

    private double extractNet(double grossAmount, double ratePercentage) {
        return grossAmount / (1.0 + (ratePercentage / 100.0));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
