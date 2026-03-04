package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Permission;
import com.mycompany.ventacontrolfx.domain.repository.IPermissionRepository;

import java.sql.SQLException;
import java.util.List;

/**
 * Caso de uso para gestionar permisos de usuarios.
 * Orquesta la lógica entre la UI y el repositorio de permisos.
 */
public class PermissionUseCase {

    private final IPermissionRepository permissionRepository;

    public PermissionUseCase(IPermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    /**
     * Obtiene el catálogo completo de permisos disponibles en el sistema.
     */
    public List<Permission> getAllPermissions() throws SQLException {
        return permissionRepository.listAll();
    }

    /**
     * Obtiene los permisos actualmente asignados a un usuario.
     */
    public List<Permission> getPermissionsForUser(int userId) throws SQLException {
        return permissionRepository.findByUserId(userId);
    }

    /**
     * Guarda el conjunto de permisos de un usuario.
     * Reemplaza completamente los permisos anteriores.
     *
     * @param userId ID del usuario
     * @param codes  Lista de códigos de permisos a asignar (e.g. ["VENTAS",
     *               "HISTORIAL"])
     */
    public void savePermissionsForUser(int userId, List<String> codes) throws SQLException {
        permissionRepository.setUserPermissions(userId, codes);
    }

    /**
     * Guarda el conjunto de permisos de un rol.
     * Reemplaza completamente los permisos anteriores del rol.
     *
     * @param roleId ID del rol
     * @param codes  Lista de códigos de permisos a asignar
     */
    public void savePermissionsForRole(int roleId, List<String> codes) throws SQLException {
        permissionRepository.setRolePermissions(roleId, codes);
    }
}
