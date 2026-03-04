package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.Permission;
import com.mycompany.ventacontrolfx.domain.repository.IPermissionRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC del repositorio de permisos.
 * Gestiona el catálogo de permisos y la asignación usuario-permiso.
 */
public class JdbcPermissionRepository implements IPermissionRepository {

    @Override
    public List<Permission> listAll() throws SQLException {
        List<Permission> list = new ArrayList<>();
        String sql = "SELECT permission_id, code, description FROM permissions ORDER BY permission_id ASC";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapPermission(rs));
            }
        }
        return list;
    }

    @Override
    public List<Permission> findByUserId(int userId) throws SQLException {
        List<Permission> list = new ArrayList<>();
        String sql = "SELECT p.permission_id, p.code, p.description " +
                "FROM permissions p " +
                "INNER JOIN user_permissions up ON p.permission_id = up.permission_id " +
                "WHERE up.user_id = ? ORDER BY p.permission_id ASC";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPermission(rs));
                }
            }
        }
        return list;
    }

    @Override
    public List<Permission> findByRoleId(int roleId) throws SQLException {
        List<Permission> list = new ArrayList<>();
        String sql = "SELECT p.permission_id, p.code, p.description " +
                "FROM permissions p " +
                "INNER JOIN role_permissions rp ON p.permission_id = rp.permission_id " +
                "WHERE rp.role_id = ? ORDER BY p.permission_id ASC";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPermission(rs));
                }
            }
        }
        return list;
    }

    @Override
    public void setUserPermissions(int userId, List<String> codes) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Borrar permisos anteriores del usuario
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM user_permissions WHERE user_id = ?")) {
                    del.setInt(1, userId);
                    del.executeUpdate();
                }

                // 2. Insertar los nuevos permisos seleccionados
                if (codes != null && !codes.isEmpty()) {
                    String insertSql = "INSERT INTO user_permissions (user_id, permission_id) " +
                            "SELECT ?, permission_id FROM permissions WHERE code = ?";
                    try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                        for (String code : codes) {
                            ins.setInt(1, userId);
                            ins.setString(2, code);
                            ins.addBatch();
                        }
                        ins.executeBatch();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public void setRolePermissions(int roleId, List<String> codes) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Borrar permisos anteriores del rol
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM role_permissions WHERE role_id = ?")) {
                    del.setInt(1, roleId);
                    del.executeUpdate();
                }

                // 2. Insertar los nuevos permisos seleccionados
                if (codes != null && !codes.isEmpty()) {
                    String insertSql = "INSERT INTO role_permissions (role_id, permission_id) " +
                            "SELECT ?, permission_id FROM permissions WHERE code = ?";
                    try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                        for (String code : codes) {
                            ins.setInt(1, roleId);
                            ins.setString(2, code);
                            ins.addBatch();
                        }
                        ins.executeBatch();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private Permission mapPermission(ResultSet rs) throws SQLException {
        return new Permission(
                rs.getInt("permission_id"),
                rs.getString("code"),
                rs.getString("description"));
    }
}
