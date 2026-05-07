package com.mycompany.ventacontrolfx.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa un Rol de usuario (e.g. Cajero, Administrador).
 * Un rol define un conjunto de permisos predeterminados.
 */
public class Role {

    private int roleId;
    private String name;
    private String description;
    private boolean isSystem;
    private List<Permission> permissions = new ArrayList<>();

    public Role() {
    }

    public Role(int roleId, String name, String description) {
        this.roleId = roleId;
        this.name = name;
        this.description = description;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public void setSystem(boolean system) {
        isSystem = system;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions != null ? permissions : new ArrayList<>();
    }

    @Override
    public String toString() {
        return name;
    }
}

