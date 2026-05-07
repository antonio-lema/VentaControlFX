package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.SuspendedCart;
import java.sql.SQLException;
import java.util.List;

public interface ISuspendedCartRepository {
    int save(SuspendedCart cart) throws SQLException;

    List<SuspendedCart> findAll() throws SQLException;

    List<SuspendedCart> findByUserId(int userId) throws SQLException;

    SuspendedCart findById(int id) throws SQLException;

    void delete(int id) throws SQLException;
}

