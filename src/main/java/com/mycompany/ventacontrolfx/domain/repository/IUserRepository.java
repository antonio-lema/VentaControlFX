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
}
