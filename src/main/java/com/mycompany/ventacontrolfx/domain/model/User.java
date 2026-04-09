package com.mycompany.ventacontrolfx.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class User {

    private int userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String role;

    private String email;
    private int companyId;
    private String companyName;
    private Role roleObject;
    private Integer currentBranchId; // ID de la sucursal actual

    // Mapeo de BranchID -> Lista de Permisos Contextuales
    private java.util.Map<Integer, List<Permission>> branchPermissions = new java.util.HashMap<>();

    // Permisos heredados del ROL
    private List<Permission> rolePermissions = new ArrayList<>();
    // Permisos adicionales asignados DIRECTAMENTE al usuario
    private List<Permission> individualPermissions = new ArrayList<>();

    public User() {
        // Constructor vac\u00edo
    }

    public User(int userId, String username, String passwordHash, String fullName, String role, String email) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.email = email;
    }

    public Role getRoleObject() {
        return roleObject;
    }

    public void setRoleObject(Role role) {
        this.roleObject = role;
        if (role != null) {
            this.role = role.getName();
            setRolePermissions(role.getPermissions());
        }
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public List<Permission> getRolePermissions() {
        return rolePermissions;
    }

    public void setRolePermissions(List<Permission> permissions) {
        this.rolePermissions = permissions != null ? permissions : new ArrayList<>();
    }

    public List<Permission> getIndividualPermissions() {
        return individualPermissions;
    }

    public void setIndividualPermissions(List<Permission> permissions) {
        this.individualPermissions = permissions != null ? permissions : new ArrayList<>();
    }

    /**
     * Retorna la uni\u00f3n de permisos del rol y permisos individuales.
     */
    public List<Permission> getEffectivePermissions() {
        List<Permission> effective = new ArrayList<>(rolePermissions);
        for (Permission p : individualPermissions) {
            if (effective.stream().noneMatch(ep -> ep.getCode().equals(p.getCode()))) {
                effective.add(p);
            }
        }
        return effective;
    }

    public void setPermissions(List<Permission> permissions) {
        this.individualPermissions = permissions != null ? permissions : new ArrayList<>();
    }

    public Integer getCurrentBranchId() {
        return currentBranchId;
    }

    public void setCurrentBranchId(Integer currentBranchId) {
        this.currentBranchId = currentBranchId;
    }

    public void setBranchPermissions(int branchId, List<Permission> permissions) {
        this.branchPermissions.put(branchId, permissions != null ? permissions : new ArrayList<>());
    }

    /**
     * Comprueba si el usuario tiene un permiso, considerando el contexto
     * global y el contexto de la sucursal actual.
     */
    public boolean hasPermission(String code) {
        if (code == null)
            return false;

        // 1. Administrador Global siempre tiene todo
        if (this.role != null && (this.role.equalsIgnoreCase("Administrador") ||
                this.role.equalsIgnoreCase("admin") ||
                this.role.equalsIgnoreCase("SUPERADMIN")))
            return true;

        // 2. Comprobar permisos globales (Rol + Individuales)
        if (rolePermissions.stream().anyMatch(p -> code.equalsIgnoreCase(p.getCode())))
            return true;
        if (individualPermissions.stream().anyMatch(p -> code.equalsIgnoreCase(p.getCode())))
            return true;

        // 3. Comprobar permisos contextuales de la sucursal actual
        if (currentBranchId != null && branchPermissions.containsKey(currentBranchId)) {
            return branchPermissions.get(currentBranchId).stream()
                    .anyMatch(p -> code.equalsIgnoreCase(p.getCode()));
        }

        return false;
    }
}
