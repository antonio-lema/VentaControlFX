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

    // Gesti\u00f3n de permisos individuales
    boolean addIndividualPermission(int userId, int permissionId) throws SQLException;

    boolean removeIndividualPermission(int userId, int permissionId) throws SQLException;

    // M\u00e9todos para recuperaci\u00f3n de contrase\u00f1a (Fix V-01)
    void saveRecoveryCode(String email, String codeHash, java.time.LocalDateTime expiresAt) throws SQLException;

    boolean verifyRecoveryCode(String email, String code) throws SQLException;

    void markRecoveryCodeAsUsed(String email, String code) throws SQLException;

    /**
     * Busca el email asociado a un nombre de usuario SIN requerir sesi\u00f3n activa.
     * Solo devuelve el email, sin exponer datos sensibles del usuario.
     * Usado para el flujo de recuperaci\u00f3n de contrase\u00f1a an\u00f3nimo.
     */
    String findEmailByUsername(String username) throws SQLException;

    /**
     * Cuenta cu\u00e1ntos intentos de verificaci\u00f3n fallidos hay para un email/c\u00f3digo
     * activo.
     * Usado para implementar rate limiting en la recuperaci\u00f3n de contrase\u00f1a.
     */
    int getRecoveryAttempts(String email) throws SQLException;

    /**
     * Incrementa el contador de intentos fallidos para los c\u00f3digos activos de ese
     * email.
     * Si supera el l\u00edmite (MAX_ATTEMPTS=5), invalida el c\u00f3digo autom\u00e1ticamente.
     */
    void incrementRecoveryAttempts(String email) throws SQLException;
}

