package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.domain.repository.IClientRepository;
import java.sql.SQLException;
import java.util.List;

public class ClientUseCase {
    private final IClientRepository repository;

    public ClientUseCase(IClientRepository repository) {
        this.repository = repository;
    }

    public List<Client> getAllClients() throws SQLException {
        return repository.getAll();
    }

    public List<Client> searchClients(String query) throws SQLException {
        return repository.search(query);
    }

    public void addClient(Client client) throws SQLException {
        int id = repository.save(client);
        if (id != -1) {
            client.setId(id);
        }
    }

    public void updateClient(Client client) throws SQLException {
        repository.update(client);
    }

    public void deleteClient(int id) throws SQLException {
        repository.delete(id);
    }

    public int getCount() throws SQLException {
        return repository.count();
    }
}
