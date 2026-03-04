package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.domain.repository.IUserRepository;
import com.mycompany.ventacontrolfx.domain.repository.IEmailSender;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

public class UserUseCase {
    private final IUserRepository userRepository;
    private final IEmailSender emailSender;

    // In-memory recovery state (Legacy mode, should be persisted in DB in prod)
    private String lastGeneratedCode;
    private String lastRecoverEmail;

    public UserUseCase(IUserRepository userRepository, IEmailSender emailSender) {
        this.userRepository = userRepository;
        this.emailSender = emailSender;
    }

    public User getUserByUsername(String username) throws SQLException {
        return userRepository.findByUsername(username);
    }

    public boolean validateLogin(String username, String password) throws SQLException {
        User user = userRepository.findByUsername(username);
        return user != null && user.getPassword().equals(password);
    }

    public int getUserCount() throws SQLException {
        return userRepository.count();
    }

    public User login(String username, String password) throws SQLException {
        User user = userRepository.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    public List<User> listUsers() throws SQLException {
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
        userRepository.create(user);
    }

    public boolean updateUser(User user) throws SQLException {
        return userRepository.update(user);
    }

    public boolean deleteUser(int userId) throws SQLException {
        return userRepository.delete(userId);
    }

    public void recoverPassword(String email) throws Exception {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new Exception("No existe ningún usuario con ese correo electrónico.");
        }

        lastGeneratedCode = String.format("%06d", new Random().nextInt(999999));
        lastRecoverEmail = email;

        String subject = "Código de Recuperación de Contraseña - TPV";
        String body = "Hola " + user.getFullName() + ",\n\nTu código de recuperación es: " + lastGeneratedCode;

        emailSender.send(email, subject, body);
    }

    public boolean verifyCode(String email, String code) {
        return code != null && code.equals(lastGeneratedCode) && email != null && email.equals(lastRecoverEmail);
    }

    public void resetPassword(String email, String newPassword) throws Exception {
        User user = userRepository.findByEmail(email);
        if (user == null)
            throw new Exception("Usuario no encontrado.");

        userRepository.changePassword(user.getUserId(), newPassword);
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
