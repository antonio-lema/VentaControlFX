package com.mycompany.ventacontrolfx.service;

import com.mycompany.ventacontrolfx.dao.DBConnection;
import com.mycompany.ventacontrolfx.dao.UserDAO;
import com.mycompany.ventacontrolfx.model.User;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class UserService {

    private final UserDAO userDAO = new UserDAO();

    public User findByUsername(String username) throws SQLException {
        return userDAO.findByUsername(username);
    }

    public User findByEmail(String email) throws SQLException {
        return userDAO.findByEmail(email);
    }

    public boolean validateLogin(String username, String password) throws SQLException {
        return userDAO.validateLogin(username, password);
    }

    public boolean createUser(User user) throws SQLException {
        return userDAO.insert(user);
    }

    public boolean updateUser(User user) throws SQLException {
        return userDAO.update(user);
    }

    public boolean updatePassword(int userId, String newPassword) throws SQLException {
        return userDAO.updatePassword(userId, newPassword);
    }

    public List<User> getAllUsers() throws SQLException {
        return userDAO.findAll();
    }

    public boolean deleteUser(int userId) throws SQLException {
        return userDAO.delete(userId);
    }

    public int getCount() throws SQLException {
        return userDAO.getCount();
    }
}
