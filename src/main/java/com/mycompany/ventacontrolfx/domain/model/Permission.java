package com.mycompany.ventacontrolfx.domain.model;

/**
 * Representa un permiso de acceso a una zona del sistema.
 * Cada usuario puede tener un conjunto de estos permisos asignados.
 */
public class Permission {

    private int permissionId;
    private String code; // e.g. "VENTAS", "HISTORIAL", "PRODUCTOS"
    private String description; // e.g. "Acceso al punto de venta"

    public Permission() {
    }

    public Permission(int permissionId, String code, String description) {
        this.permissionId = permissionId;
        this.code = code;
        this.description = description;
    }

    public int getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(int permissionId) {
        this.permissionId = permissionId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}

