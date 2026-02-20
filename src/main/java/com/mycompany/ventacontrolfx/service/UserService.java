package com.mycompany.ventacontrolfx.service;

import com.mycompany.ventacontrolfx.dao.UserDAO;
import com.mycompany.ventacontrolfx.model.User;

import java.sql.SQLException;
import java.util.List;

public class UserService {

    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    // Obtener todos los usuarios
    public List<User> getAllUsers() throws SQLException {
        return userDAO.findAll();
    }

    // Buscar usuario por username (aquí está el método que faltaba)
    public User findByUsername(String username) throws SQLException {
        return userDAO.findByUsername(username);
    }

    // Buscar usuario por email
    public User findByEmail(String email) throws SQLException {
        return userDAO.findByEmail(email);
    }

    // Verificar credenciales para login
    public boolean validateLogin(String username, String password) throws SQLException {
        User user = userDAO.findByUsername(username);
        return user != null && user.getPassword().equals(password);
    }

    // Crear un nuevo usuario
    public boolean createUser(User user) throws SQLException {
        return userDAO.insert(user);
    }

    // Actualizar contraseña
    public boolean updatePassword(int userId, String newPassword) throws SQLException {
        return userDAO.updatePassword(userId, newPassword);
    }

    // Actualizar usuario (nombre, rol, email)
    public boolean updateUser(User user) throws SQLException {
        return userDAO.update(user);
    }

    // Eliminar usuario
    public boolean deleteUser(int userId) throws SQLException {
        return userDAO.delete(userId);
    }
}
