package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.domain.repository.IUserRepository;
import com.mycompany.ventacontrolfx.domain.repository.IEmailSender;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.SQLException;
import java.util.List;
import java.security.SecureRandom;
import java.time.LocalDateTime;

public class UserUseCase {
    private final IUserRepository userRepository;
    private final IEmailSender emailSender;
    private final com.mycompany.ventacontrolfx.util.AuthorizationService authService;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserUseCase(IUserRepository userRepository, IEmailSender emailSender,
            com.mycompany.ventacontrolfx.util.AuthorizationService authService) {
        this.userRepository = userRepository;
        this.emailSender = emailSender;
        this.authService = authService;
    }

    public User getUserByUsername(String username) throws SQLException {
        return userRepository.findByUsername(username);
    }

    /**
     * Obtiene el email de un usuario por su nombre de usuario SIN requerir
     * permisos.
     * Diseñado para el flujo de recuperación de contraseña anónimo (Fix V-01).
     * No expone datos sensibles del usuario, solo el email enmascarado.
     */
    public String findEmailByUsername(String username) throws SQLException {
        return userRepository.findEmailByUsername(username);
    }

    public boolean validateLogin(String username, String password) throws SQLException {
        User user = userRepository.findByUsername(username);
        return user != null && BCrypt.checkpw(password, user.getPasswordHash());
    }

    public int getUserCount() throws SQLException {
        return userRepository.count();
    }

    public User login(String username, String password) throws SQLException {
        User user = userRepository.findByUsername(username);
        if (user != null && BCrypt.checkpw(password, user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    public List<User> listUsers() throws SQLException {
        authService.checkPermission("USUARIOS");
        return userRepository.listAll();
    }

    /** Alias de listUsers para los controladores de informes */
    public List<User> getAllUsers() throws SQLException {
        return userRepository.listAll();
    }

    /** Obtiene un usuario por su ID */
    public User getUserById(int userId) throws SQLException {
        return userRepository.listAll().stream()
                .filter(u -> u.getUserId() == userId)
                .findFirst()
                .orElse(null);
    }

    public void registerUser(User user) throws SQLException {
        authService.checkPermission("USUARIOS");
        // Encriptar password antes de guardar
        user.setPasswordHash(BCrypt.hashpw(user.getPasswordHash(), BCrypt.gensalt()));
        userRepository.create(user);
    }

    public boolean updateUser(User user) throws SQLException {
        authService.checkPermission("USUARIOS");
        return userRepository.update(user);
    }

    public boolean deleteUser(int userId) throws SQLException {
        authService.checkPermission("USUARIOS");
        return userRepository.delete(userId);
    }

    public void recoverPassword(String email) throws Exception {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new Exception("No existe ningún usuario con ese correo electrónico.");
        }

        // Generar código seguro de 6 dígitos
        String code = String.format("%06d", secureRandom.nextInt(1000000));

        // Persistir en DB con expiración (15 minutos)
        String codeHash = BCrypt.hashpw(code, BCrypt.gensalt());
        userRepository.saveRecoveryCode(email, codeHash, LocalDateTime.now().plusMinutes(15));

        String subject = "Código de Recuperación de Contraseña - TPV";
        String body = "Hola " + user.getFullName() + ",\n\nTu código de recuperación es: " + code +
                "\n\nEste código expirará en 15 minutos.";

        emailSender.send(email, subject, body);
    }

    public boolean verifyCode(String email, String code) throws SQLException {
        return userRepository.verifyRecoveryCode(email, code);
    }

    /**
     * Verifica si el código de recuperación está bloqueado por exceso de intentos.
     * Retorna true si se han superado los 5 intentos fallidos.
     */
    public boolean isRecoveryBlocked(String email) throws SQLException {
        return userRepository.getRecoveryAttempts(email) >= 5;
    }

    public void resetPassword(String email, String newPassword) throws Exception {
        User user = userRepository.findByEmail(email);
        if (user == null)
            throw new Exception("Usuario no encontrado.");

        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        userRepository.changePassword(user.getUserId(), hashedPassword);

        // Marcar código como usado para evitar reutilización
        userRepository.markRecoveryCodeAsUsed(email, null);
    }

    public void sendUsernameReminder(String email) throws Exception {
        User user = userRepository.findByEmail(email);
        if (user == null)
            throw new Exception("Correo no registrado.");

        String subject = "Recordatorio de Usuario - TPV";
        String body = "Hola,\n\nTu nombre de usuario es: " + user.getUsername();
        emailSender.send(email, subject, body);
    }
}
