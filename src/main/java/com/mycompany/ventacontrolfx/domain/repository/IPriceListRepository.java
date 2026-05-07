package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.PriceList;
import java.sql.SQLException;
import java.util.List;

public interface IPriceListRepository {
    List<PriceList> getAll() throws SQLException;

    PriceList getById(int id) throws SQLException;

    PriceList getDefault() throws SQLException;

    int save(PriceList priceList) throws SQLException;

    void update(PriceList priceList) throws SQLException;

    void delete(int id) throws SQLException;

    void setAsDefault(int id) throws SQLException;
}

