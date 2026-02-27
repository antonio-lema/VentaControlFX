package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.SaleConfig;

public interface ICompanyConfigRepository {
    SaleConfig load();

    void save(SaleConfig config);

    void reset();

    String getValue(String key);
}
