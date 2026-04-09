package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Role;
import com.mycompany.ventacontrolfx.domain.repository.IRoleRepository;
import com.mycompany.ventacontrolfx.util.AuthorizationService;

import java.sql.SQLException;
import java.util.List;

/**
 * Caso de uso para gestionar roles del sistema.
 * Fix V-03: Todos los m\u00c3\u00a9todos de escritura ahora requieren el permiso USUARIOS.
 */
public class RoleUseCase {

    private final IRoleRepository roleRepository;
    private final AuthorizationService authService;

    public RoleUseCase(IRoleRepository roleRepository, AuthorizationService authService) {
        this.roleRepository = roleRepository;
        this.authService = authService;
    }

    public List<Role> getAllRoles() throws SQLException {
        return roleRepository.listAll();
    }

    public Role getRoleByName(String name) throws SQLException {
        return roleRepository.findByName(name);
    }

    public Role getRoleById(int roleId) throws SQLException {
        return roleRepository.findById(roleId);
    }

    public boolean createRole(Role role) throws SQLException {
        authService.checkPermission("USUARIOS"); // Fix V-03
        return roleRepository.create(role);
    }

    public boolean updateRole(Role role) throws SQLException {
        authService.checkPermission("USUARIOS"); // Fix V-03
        Role existing = roleRepository.findById(role.getRoleId());
        if (existing != null && existing.isSystem()) {
            // No permitir cambiar el nombre de un rol de sistema
            role.setName(existing.getName());
            // No permitir desactivar el flag is_system si ya lo era
            role.setSystem(true);
        }
        return roleRepository.update(role);
    }

    public boolean deleteRole(int roleId) throws SQLException {
        authService.checkPermission("USUARIOS"); // Fix V-03
        Role role = roleRepository.findById(roleId);
        if (role != null && role.isSystem()) {
            throw new RuntimeException("No se puede eliminar un rol protegido por el sistema.");
        }
        return roleRepository.delete(roleId);
    }
}
