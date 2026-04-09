package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Permission;
import com.mycompany.ventacontrolfx.domain.repository.IPermissionRepository;
import com.mycompany.ventacontrolfx.util.AuthorizationService;

import java.sql.SQLException;
import java.util.List;

/**
 * Caso de uso para gestionar permisos de usuarios.
 * Orquesta la l\u00c3\u00b3gica entre la UI y el repositorio de permisos.
 * Fix V-03: Los m\u00c3\u00a9todos de escritura requieren el permiso USUARIOS.
 */
public class PermissionUseCase {

    private final IPermissionRepository permissionRepository;
    private final AuthorizationService authService;

    public PermissionUseCase(IPermissionRepository permissionRepository, AuthorizationService authService) {
        this.permissionRepository = permissionRepository;
        this.authService = authService;
    }

    /**
     * Obtiene el cat\u00c3\u00a1logo completo de permisos disponibles en el sistema.
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
     * Fix V-03: Requiere permiso USUARIOS.
     *
     * @param userId ID del usuario
     * @param codes  Lista de c\u00c3\u00b3digos de permisos a asignar (e.g. ["VENTAS",
     *               "HISTORIAL"])
     */
    public void savePermissionsForUser(int userId, List<String> codes) throws SQLException {
        authService.checkPermission("USUARIOS"); // Fix V-03
        permissionRepository.setUserPermissions(userId, codes);
    }

    /**
     * Guarda el conjunto de permisos de un rol.
     * Reemplaza completamente los permisos anteriores del rol.
     * Fix V-03: Requiere permiso USUARIOS.
     *
     * @param roleId ID del rol
     * @param codes  Lista de c\u00c3\u00b3digos de permisos a asignar
     */
    public void savePermissionsForRole(int roleId, List<String> codes) throws SQLException {
        authService.checkPermission("USUARIOS"); // Fix V-03
        permissionRepository.setRolePermissions(roleId, codes);
    }
}
