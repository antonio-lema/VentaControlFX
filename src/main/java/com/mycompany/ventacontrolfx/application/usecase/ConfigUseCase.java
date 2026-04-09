package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.domain.repository.ICompanyConfigRepository;

public class ConfigUseCase {
    private final ICompanyConfigRepository repository;

    public ConfigUseCase(ICompanyConfigRepository repository) {
        this.repository = repository;
    }

    public SaleConfig getConfig() {
        return repository.load();
    }

    public void saveConfig(SaleConfig config) {
        // PodrÃ­amos realizar validaciones de negocio aquÃ­,
        // por ejemplo validar el formato del CIF o Email.
        repository.save(config);
    }

    public void resetConfig() {
        repository.reset();
    }
}
