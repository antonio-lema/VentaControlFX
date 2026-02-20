package com.mycompany.ventacontrolfx.service;

import com.mycompany.ventacontrolfx.dao.DBConnection;
import com.mycompany.ventacontrolfx.dao.ClientDAO;
import com.mycompany.ventacontrolfx.model.Client;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ClientService {

    public List<Client> getAllClients() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return new ClientDAO(conn).getAllClients();
        }
    }

    public List<Client> searchClients(String query) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return new ClientDAO(conn).searchClients(query);
        }
    }

    public int saveClient(Client client) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            ClientDAO dao = new ClientDAO(conn);
            if (client.getId() > 0) {
                dao.updateClient(client);
                return client.getId();
            } else {
                return dao.createClient(client);
            }
        }
    }

    public void deleteClient(int clientId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            new ClientDAO(conn).deleteClient(clientId);
        }
    }
}
