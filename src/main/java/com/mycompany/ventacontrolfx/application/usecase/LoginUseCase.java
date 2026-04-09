package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.domain.model.Permission;
import com.mycompany.ventacontrolfx.domain.model.Role;
import com.mycompany.ventacontrolfx.domain.repository.IUserRepository;
import com.mycompany.ventacontrolfx.domain.exception.UserNotFoundException;
import com.mycompany.ventacontrolfx.domain.exception.InvalidPasswordException;
import com.mycompany.ventacontrolfx.domain.repository.IAuditRepository;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Caso de Uso para la autenticaci\u00f3n de usuarios.
 * Sigue los principios de Clean Architecture separando la l\u00f3gica de negocio del
 * controlador.
 */
public class LoginUseCase {
    private final IUserRepository userRepository;
    private final IAuditRepository auditRepository;
    private final RoleUseCase roleUseCase;
    private final PermissionUseCase permissionUseCase;

    public LoginUseCase(IUserRepository userRepository,
            IAuditRepository auditRepository,
            RoleUseCase roleUseCase,
            PermissionUseCase permissionUseCase) {
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
        this.roleUseCase = roleUseCase;
        this.permissionUseCase = permissionUseCase;
    }

    /**
     * Valida las credenciales y carga el perfil completo del usuario.
     * 
     * @param username Nombre de usuario.
     * @param password Contrase\u00f1a plana (se comparar\u00e1 con el hash almacenado).
     * @return El usuario autenticado con todos sus permisos cargados.
     * @throws UserNotFoundException    Si el nombre de usuario no existe.
     * @throws InvalidPasswordException Si la contrase\u00f1a es incorrecta.
     * @throws SQLException             Si hay un error persistente.
     */
    public User execute(String username, String password) throws SQLException {
        // 1. Buscar usuario
        User user = userRepository.findByUsername(username);
        if (user == null) {
            logFailedAttempt(username, "Usuario no encontrado");
            throw new UserNotFoundException(username);
        }

        // 2. Validar contrase\u00f1a con BCrypt
        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            logFailedAttempt(username, "Contrase\u00f1a incorrecta");
            throw new InvalidPasswordException();
        }

        // 3. Cargar perfil extendido (Roles y Permisos)
        loadExtendedProfile(user);

        // 4. Auditor\u00eda de \u00e9xito
        auditRepository.logAccess(user.getUserId(), "LOGIN", "AUTH", "Inicio de sesi\u00f3n exitoso");

        return user;
    }

    private void loadExtendedProfile(User user) {
        List<Permission> effectivePerms = new ArrayList<>();
        try {
            String roleName = user.getRole();
            Role role = roleUseCase.getRoleByName(roleName);

            if (role != null) {
                user.setRoleObject(role);
                List<Permission> rolePerms = role.getPermissions();
                List<Permission> userPerms = permissionUseCase.getPermissionsForUser(user.getUserId());

                Set<String> uniqueCodes = new HashSet<>();
                for (Permission p : rolePerms) {
                    if (uniqueCodes.add(p.getCode()))
                        effectivePerms.add(p);
                }
                for (Permission p : userPerms) {
                    if (uniqueCodes.add(p.getCode()))
                        effectivePerms.add(p);
                }
            } else {
                effectivePerms.addAll(permissionUseCase.getPermissionsForUser(user.getUserId()));
            }

            // Caso administrador inicial: si no tiene permisos, asignar todos por defecto
            if (effectivePerms.isEmpty() && isAdministratorRole(user.getRole())) {
                effectivePerms = permissionUseCase.getAllPermissions();
                List<String> codes = effectivePerms.stream().map(Permission::getCode).toList();
                permissionUseCase.savePermissionsForUser(user.getUserId(), codes);
            }

            user.setPermissions(effectivePerms);
        } catch (Exception e) {
            System.err.println("Error al cargar perfil de usuario: " + e.getMessage());
        }
    }

    private boolean isAdministratorRole(String role) {
        return "admin".equalsIgnoreCase(role) || "Administrador".equalsIgnoreCase(role);
    }

    private void logFailedAttempt(String username, String reason) {
        try {
            auditRepository.logAccess(-1, "LOGIN_FAILED", "AUTH", "Intento fallido: " + reason + " (" + username + ")");
        } catch (Exception ignored) {
        }
    }
}
