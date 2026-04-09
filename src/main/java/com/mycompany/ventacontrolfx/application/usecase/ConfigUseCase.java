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
        // Podr\u00c3\u00adamos realizar validaciones de negocio aqu\u00c3\u00ad,
        // por ejemplo validar el formato del CIF o Email.
        repository.save(config);
    }

    public void resetConfig() {
        repository.reset();
    }
}
