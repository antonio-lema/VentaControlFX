package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.Permission;
import java.sql.SQLException;
import java.util.List;

/**
 * Puerto de salida para permisos.
 * Define el contrato que la capa de infraestructura debe implementar.
 */
public interface IPermissionRepository {

    /** Obtiene el cat\u00e1logo completo de permisos disponibles en el sistema. */
    List<Permission> listAll() throws SQLException;

    /** Obtiene los permisos asignados a un usuario concreto. */
    List<Permission> findByUserId(int userId) throws SQLException;

    /** Obtiene los permisos asignados a un rol concreto. */
    List<Permission> findByRoleId(int roleId) throws SQLException;

    /**
     * Reemplaza TODOS los permisos de un usuario.
     * Borra los anteriores y asigna los nuevos c\u00f3digos facilitados.
     */
    void setUserPermissions(int userId, List<String> codes) throws SQLException;

    /**
     * Reemplaza TODOS los permisos de un rol.
     * Borra los anteriores y asigna los nuevos c\u00f3digos facilitados.
     */
    void setRolePermissions(int roleId, List<String> codes) throws SQLException;
}
