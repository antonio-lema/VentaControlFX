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

    // GestiÃ³n de permisos individuales
    boolean addIndividualPermission(int userId, int permissionId) throws SQLException;

    boolean removeIndividualPermission(int userId, int permissionId) throws SQLException;

    // MÃ©todos para recuperaciÃ³n de contraseÃ±a (Fix V-01)
    void saveRecoveryCode(String email, String codeHash, java.time.LocalDateTime expiresAt) throws SQLException;

    boolean verifyRecoveryCode(String email, String code) throws SQLException;

    void markRecoveryCodeAsUsed(String email, String code) throws SQLException;

    /**
     * Busca el email asociado a un nombre de usuario SIN requerir sesiÃ³n activa.
     * Solo devuelve el email, sin exponer datos sensibles del usuario.
     * Usado para el flujo de recuperaciÃ³n de contraseÃ±a anÃ³nimo.
     */
    String findEmailByUsername(String username) throws SQLException;

    /**
     * Cuenta cuÃ¡ntos intentos de verificaciÃ³n fallidos hay para un email/cÃ³digo
     * activo.
     * Usado para implementar rate limiting en la recuperaciÃ³n de contraseÃ±a.
     */
    int getRecoveryAttempts(String email) throws SQLException;

    /**
     * Incrementa el contador de intentos fallidos para los cÃ³digos activos de ese
     * email.
     * Si supera el lÃ­mite (MAX_ATTEMPTS=5), invalida el cÃ³digo automÃ¡ticamente.
     */
    void incrementRecoveryAttempts(String email) throws SQLException;
}
