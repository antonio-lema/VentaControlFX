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
        String sql = "INSERT INTO users (username, password_hash, full_name, role, email, company_id, has_custom_permissions) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getFullName());
            pstmt.setString(4, user.getRole());
            pstmt.setString(5, user.getEmail());
            pstmt.setInt(6, user.getCompanyId());
            pstmt.setBoolean(7, user.isHasCustomPermissions());

            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean update(User user) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, role = ?, email = ?, company_id = ?, has_custom_permissions = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getFullName());
            pstmt.setString(2, user.getRole());
            pstmt.setString(3, user.getEmail());
            pstmt.setInt(4, user.getCompanyId());
            pstmt.setBoolean(5, user.isHasCustomPermissions());
            pstmt.setInt(6, user.getUserId());

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
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(rs.getString("role"));
        user.setEmail(rs.getString("email"));
        try {
            user.setHasCustomPermissions(rs.getBoolean("has_custom_permissions"));
        } catch (SQLException e) {
            // Ignorar si no existe todav\u00eda
        }

        try {
            user.setCompanyId(rs.getInt("company_id"));
            user.setCompanyName(rs.getString("company_name"));
        } catch (SQLException e) {
            // Se ignoran columnas si no existen en esta versi\u00f3n de la DB
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

    @Override
    public void saveRecoveryCode(String email, String codeHash, java.time.LocalDateTime expiresAt) throws SQLException {
        String sql = "INSERT INTO password_recoveries (email, code_hash, expires_at) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, codeHash);
            ps.setTimestamp(3, java.sql.Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
        }
    }

    @Override
    public boolean verifyRecoveryCode(String email, String code) throws SQLException {
        // 1. Verificar que no se hayan superado los intentos m\u00e1ximos (fix V-01: Rate
        // Limiting)
        if (getRecoveryAttempts(email) >= 5) {
            return false; // C\u00f3digo bloqueado por demasiados intentos
        }

        String sql = "SELECT code_hash FROM password_recoveries WHERE email = ? AND is_used = FALSE AND expires_at > ? ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setTimestamp(2, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String storedHash = rs.getString("code_hash");
                    boolean correct = org.mindrot.jbcrypt.BCrypt.checkpw(code, storedHash);
                    if (correct) {
                        return true;
                    }
                }

                // Si salimos del bucle y nada coincidi\u00f3, sumamos intento fallido
                incrementRecoveryAttempts(email);
            }
        }
        return false;
    }

    @Override
    public void markRecoveryCodeAsUsed(String email, String code) throws SQLException {
        String sql = "UPDATE password_recoveries SET is_used = TRUE WHERE email = ? AND is_used = FALSE";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
        }
    }

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u2500
    // Fix V-01: M\u00e9todos de seguridad para recuperaci\u00f3n de contrase\u00f1a
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u2500

    @Override
    public String findEmailByUsername(String username) throws SQLException {
        // Este m\u00e9todo NO requiere permisos. Solo devuelve el email, sin datos
        // sensibles.
        String sql = "SELECT email FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("email");
                }
            }
        }
        return null;
    }

    @Override
    public int getRecoveryAttempts(String email) throws SQLException {
        // Cuenta los intentos fallidos en los registros activos (no usados, no
        // expirados)
        String sql = "SELECT COALESCE(SUM(attempts), 0) FROM password_recoveries " +
                "WHERE email = ? AND is_used = FALSE AND expires_at > ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setTimestamp(2, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public void incrementRecoveryAttempts(String email) throws SQLException {
        // Incrementa el contador de intentos; si llega a 5, invalida el c\u00f3digo
        String sql = "UPDATE password_recoveries SET attempts = attempts + 1, " +
                "is_used = CASE WHEN attempts + 1 >= 5 THEN TRUE ELSE FALSE END " +
                "WHERE email = ? AND is_used = FALSE AND expires_at > ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setTimestamp(2, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.executeUpdate();
        }
    }
}

