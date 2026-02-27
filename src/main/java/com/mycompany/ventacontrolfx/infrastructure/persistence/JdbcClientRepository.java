package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.domain.repository.IClientRepository;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcClientRepository implements IClientRepository {

    @Override
    public List<Client> getAll() throws SQLException {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT * FROM clients ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                clients.add(mapResultSetToClient(rs));
            }
        }
        return clients;
    }

    @Override
    public List<Client> search(String query) throws SQLException {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT * FROM clients WHERE name LIKE ? OR tax_id LIKE ? OR phone LIKE ? ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

    @Override
    public int save(Client client) throws SQLException {
        String sql = "INSERT INTO clients (name, is_company, tax_id, address, postal_code, city, province, country, email, phone) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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

    @Override
    public void update(Client client) throws SQLException {
        String sql = "UPDATE clients SET name = ?, is_company = ?, tax_id = ?, address = ?, postal_code = ?, city = ?, province = ?, country = ?, email = ?, phone = ? WHERE client_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM clients WHERE client_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    @Override
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM clients";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
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
