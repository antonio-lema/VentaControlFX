package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.Role;
import com.mycompany.ventacontrolfx.domain.model.Permission;
import com.mycompany.ventacontrolfx.domain.repository.IRoleRepository;
import com.mycompany.ventacontrolfx.domain.repository.IPermissionRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcRoleRepository implements IRoleRepository {

    private final IPermissionRepository permissionRepository;

    public JdbcRoleRepository(IPermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public List<Role> listAll() throws SQLException {
        List<Role> roles = new ArrayList<>();
        String sql = "SELECT role_id, name, description, is_system FROM roles ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                roles.add(mapRole(rs));
            }
        }
        return roles;
    }

    @Override
    public Role findByName(String name) throws SQLException {
        String sql = "SELECT role_id, name, description, is_system FROM roles WHERE name = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRole(rs);
                }
            }
        }
        return null;
    }

    @Override
    public Role findById(int roleId) throws SQLException {
        String sql = "SELECT role_id, name, description, is_system FROM roles WHERE role_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRole(rs);
                }
            }
        }
        return null;
    }

    @Override
    public boolean create(Role role) throws SQLException {
        String sql = "INSERT INTO roles (name, description, is_system) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, role.getName());
            ps.setString(2, role.getDescription());
            ps.setBoolean(3, role.isSystem());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        role.setRoleId(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean update(Role role) throws SQLException {
        String sql = "UPDATE roles SET name = ?, description = ?, is_system = ? WHERE role_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.getName());
            ps.setString(2, role.getDescription());
            ps.setBoolean(3, role.isSystem());
            ps.setInt(4, role.getRoleId());
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int roleId) throws SQLException {
        String sql = "DELETE FROM roles WHERE role_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roleId);
            return ps.executeUpdate() > 0;
        }
    }

    private Role mapRole(ResultSet rs) throws SQLException {
        Role role = new Role(
                rs.getInt("role_id"),
                rs.getString("name"),
                rs.getString("description"));
        role.setSystem(rs.getBoolean("is_system"));
        // Cargar permisos asociados al rol
        List<Permission> permissions = permissionRepository.findByRoleId(role.getRoleId());
        role.setPermissions(permissions);
        return role;
    }
}

