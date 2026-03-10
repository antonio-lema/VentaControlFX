package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.User;
import java.sql.SQLException;
import java.util.List;

public interface IUserRepository {
    User findByUsername(String username) throws SQLException;

    User findByEmail(String email) throws SQLException;

    List<User> listAll() throws SQLException;

    boolean create(User user) throws SQLException;

    boolean update(User user) throws SQLException;

    boolean delete(int userId) throws SQLException;

    boolean changePassword(int userId, String newPassword) throws SQLException;

    List<User> listByCompany(int companyId) throws SQLException;

    int count() throws SQLException;

    // Gestión de permisos individuales
    boolean addIndividualPermission(int userId, int permissionId) throws SQLException;

    boolean removeIndividualPermission(int userId, int permissionId) throws SQLException;

    // Métodos para recuperación de contraseña (Fix V-01)
    void saveRecoveryCode(String email, String codeHash, java.time.LocalDateTime expiresAt) throws SQLException;

    boolean verifyRecoveryCode(String email, String code) throws SQLException;

    void markRecoveryCodeAsUsed(String email, String code) throws SQLException;

    /**
     * Busca el email asociado a un nombre de usuario SIN requerir sesión activa.
     * Solo devuelve el email, sin exponer datos sensibles del usuario.
     * Usado para el flujo de recuperación de contraseña anónimo.
     */
    String findEmailByUsername(String username) throws SQLException;

    /**
     * Cuenta cuántos intentos de verificación fallidos hay para un email/código
     * activo.
     * Usado para implementar rate limiting en la recuperación de contraseña.
     */
    int getRecoveryAttempts(String email) throws SQLException;

    /**
     * Incrementa el contador de intentos fallidos para los códigos activos de ese
     * email.
     * Si supera el límite (MAX_ATTEMPTS=5), invalida el código automáticamente.
     */
    void incrementRecoveryAttempts(String email) throws SQLException;
}
