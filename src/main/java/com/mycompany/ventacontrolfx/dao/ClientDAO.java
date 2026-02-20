package com.mycompany.ventacontrolfx.dao;

import com.mycompany.ventacontrolfx.model.Client;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientDAO {
    private final Connection conn;

    public ClientDAO(Connection conn) {
        this.conn = conn;
    }

    public List<Client> getAllClients() throws SQLException {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT * FROM clients ORDER BY name ASC";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                clients.add(mapResultSetToClient(rs));
            }
        }
        return clients;
    }

    public List<Client> searchClients(String query) throws SQLException {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT * FROM clients WHERE name LIKE ? OR tax_id LIKE ? OR phone LIKE ? ORDER BY name ASC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String searchStr = "%" + query + "%";
            pstmt.setString(1, searchStr);
            pstmt.setString(2, searchStr);
            pstmt.setString(3, searchStr);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    clients.add(mapResultSetToClient(rs));
                }
            }
        }
        return clients;
    }

    public int createClient(Client client) throws SQLException {
        String sql = "INSERT INTO clients (name, is_company, tax_id, address, postal_code, city, province, country, email, phone) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, client.getName());
            pstmt.setBoolean(2, client.isIsCompany());
            pstmt.setString(3, client.getTaxId());
            pstmt.setString(4, client.getAddress());
            pstmt.setString(5, client.getPostalCode());
            pstmt.setString(6, client.getCity());
            pstmt.setString(7, client.getProvince());
            pstmt.setString(8, client.getCountry());
            pstmt.setString(9, client.getEmail());
            pstmt.setString(10, client.getPhone());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public void updateClient(Client client) throws SQLException {
        String sql = "UPDATE clients SET name = ?, is_company = ?, tax_id = ?, address = ?, postal_code = ?, city = ?, province = ?, country = ?, email = ?, phone = ? WHERE client_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, client.getName());
            pstmt.setBoolean(2, client.isIsCompany());
            pstmt.setString(3, client.getTaxId());
            pstmt.setString(4, client.getAddress());
            pstmt.setString(5, client.getPostalCode());
            pstmt.setString(6, client.getCity());
            pstmt.setString(7, client.getProvince());
            pstmt.setString(8, client.getCountry());
            pstmt.setString(9, client.getEmail());
            pstmt.setString(10, client.getPhone());
            pstmt.setInt(11, client.getId());
            pstmt.executeUpdate();
        }
    }

    public void deleteClient(int clientId) throws SQLException {
        String sql = "DELETE FROM clients WHERE client_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, clientId);
            pstmt.executeUpdate();
        }
    }

    private Client mapResultSetToClient(ResultSet rs) throws SQLException {
        return new Client(
                rs.getInt("client_id"),
                rs.getString("name"),
                rs.getBoolean("is_company"),
                rs.getString("tax_id"),
                rs.getString("address"),
                rs.getString("postal_code"),
                rs.getString("city"),
                rs.getString("province"),
                rs.getString("country"),
                rs.getString("email"),
                rs.getString("phone"));
    }
}
