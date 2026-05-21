package com.mycompany.ventacontrolfx.domain.model;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class UserPermissionTest {

    @Test
    public void testUserWithoutCustomPermissionsInheritsRolePermissions() {
        User user = new User();
        user.setRole("Cajero");
        user.setHasCustomPermissions(false);

        Role role = new Role();
        role.setName("Cajero");
        List<Permission> rolePerms = new ArrayList<>();
        rolePerms.add(new Permission(1, "caja.ver_totales", "Ver totales"));
        role.setPermissions(rolePerms);
        user.setRoleObject(role);

        // Individual permissions list is empty
        user.setPermissions(new ArrayList<>());

        // User should have role permissions
        assertTrue(user.hasPermission("caja.ver_totales"));
        assertFalse(user.hasPermission("caja.abrir"));
    }

    @Test
    public void testUserWithCustomPermissionsIgnoresRolePermissions() {
        User user = new User();
        user.setRole("Cajero");
        user.setHasCustomPermissions(true);

        Role role = new Role();
        role.setName("Cajero");
        List<Permission> rolePerms = new ArrayList<>();
        rolePerms.add(new Permission(1, "caja.ver_totales", "Ver totales"));
        role.setPermissions(rolePerms);
        user.setRoleObject(role);

        // Individual permissions list only has different permissions
        List<Permission> individualPerms = new ArrayList<>();
        individualPerms.add(new Permission(2, "caja.abrir", "Abrir caja"));
        user.setPermissions(individualPerms);

        // User should ignore role permissions and only have individual permissions
        assertFalse(user.hasPermission("caja.ver_totales"));
        assertTrue(user.hasPermission("caja.abrir"));
    }

    @Test
    public void testAdminAlwaysHasAllPermissions() {
        User user = new User();
        user.setRole("Administrador");
        user.setHasCustomPermissions(true); // Should not affect admin

        // User should always have any permission
        assertTrue(user.hasPermission("caja.ver_totales"));
        assertTrue(user.hasPermission("cualquier.permiso.inexistente"));
    }
}
