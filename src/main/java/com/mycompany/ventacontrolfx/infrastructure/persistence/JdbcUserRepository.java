package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.domain.repository.IUserRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcUserRepository implements IUserRepository {

    @Override
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT u.*, c.name as company_name FROM users u LEFT JOIN clients c ON u.company_id = c.client_id WHERE u.username = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        }
        return null;
    }

    @Override
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT u.*, c.name as company_name FROM users u LEFT JOIN clients c ON u.company_id = c.client_id WHERE u.email = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<User> listAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.*, c.name as company_name FROM users u LEFT JOIN clients c ON u.company_id = c.client_id ORDER BY u.full_name ASC";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapUser(rs));
            }
        }
        return users;
    }

    @Override
    public boolean create(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, full_name, role, email, company_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getFullName());
            pstmt.setString(4, user.getRole());
            pstmt.setString(5, user.getEmail());
            pstmt.setInt(6, user.getCompanyId());

            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean update(User user) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, role = ?, email = ?, company_id = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getFullName());
            pstmt.setString(2, user.getRole());
            pstmt.setString(3, user.getEmail());
            pstmt.setInt(4, user.getCompanyId());
            pstmt.setInt(5, user.getUserId());

            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean changePassword(int userId, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newPassword);
            pstmt.setInt(2, userId);

            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public List<User> listByCompany(int companyId) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.*, c.name as company_name FROM users u LEFT JOIN clients c ON u.company_id = c.client_id WHERE u.company_id = ? ORDER BY u.full_name ASC";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, companyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        }
        return users;
    }

    @Override
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(rs.getString("role"));
        user.setEmail(rs.getString("email"));

        try {
            user.setCompanyId(rs.getInt("company_id"));
            user.setCompanyName(rs.getString("company_name"));
        } catch (SQLException e) {
            // Se ignoran columnas si no existen en esta versión de la DB
        }

        // Cargar permisos del ROL
        user.setRolePermissions(loadRolePermissions(user.getRole()));
        // Cargar permisos INDIVIDUALES
        user.setIndividualPermissions(loadIndividualPermissions(user.getUserId()));

        return user;
    }

    private List<com.mycompany.ventacontrolfx.domain.model.Permission> loadRolePermissions(String roleName)
            throws SQLException {
        List<com.mycompany.ventacontrolfx.domain.model.Permission> perms = new ArrayList<>();
        String sql = "SELECT p.* FROM permissions p " +
                "JOIN role_permissions rp ON p.permission_id = rp.permission_id " +
                "JOIN roles r ON rp.role_id = r.role_id " +
                "WHERE r.name = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roleName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.mycompany.ventacontrolfx.domain.model.Permission p = new com.mycompany.ventacontrolfx.domain.model.Permission();
                    p.setPermissionId(rs.getInt("permission_id"));
                    p.setCode(rs.getString("code"));
                    perms.add(p);
                }
            }
        }
        return perms;
    }

    private List<com.mycompany.ventacontrolfx.domain.model.Permission> loadIndividualPermissions(int userId)
            throws SQLException {
        List<com.mycompany.ventacontrolfx.domain.model.Permission> perms = new ArrayList<>();
        String sql = "SELECT p.* FROM permissions p " +
                "JOIN user_permissions up ON p.permission_id = up.permission_id " +
                "WHERE up.user_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.mycompany.ventacontrolfx.domain.model.Permission p = new com.mycompany.ventacontrolfx.domain.model.Permission();
                    p.setPermissionId(rs.getInt("permission_id"));
                    p.setCode(rs.getString("code"));
                    perms.add(p);
                }
            }
        }
        return perms;
    }

    @Override
    public boolean addIndividualPermission(int userId, int permissionId) throws SQLException {
        String sql = "INSERT IGNORE INTO user_permissions (user_id, permission_id) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, permissionId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean removeIndividualPermission(int userId, int permissionId) throws SQLException {
        String sql = "DELETE FROM user_permissions WHERE user_id = ? AND permission_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, permissionId);
            return ps.executeUpdate() > 0;
        }
    }
}
