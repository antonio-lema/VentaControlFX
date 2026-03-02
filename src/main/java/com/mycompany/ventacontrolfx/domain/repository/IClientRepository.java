package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.Client;
import java.sql.SQLException;
import java.util.List;

public interface IClientRepository {
    List<Client> getAll() throws SQLException;

    List<Client> search(String query) throws SQLException;

    int save(Client client) throws SQLException;

    void update(Client client) throws SQLException;

    void delete(int id) throws SQLException;

    int count() throws SQLException;

    Client getById(int id) throws SQLException;
}
