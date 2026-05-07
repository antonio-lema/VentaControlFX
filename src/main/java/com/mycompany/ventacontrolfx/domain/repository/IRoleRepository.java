package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.Role;
import java.sql.SQLException;
import java.util.List;

public interface IRoleRepository {
    List<Role> listAll() throws SQLException;

    Role findByName(String name) throws SQLException;

    Role findById(int roleId) throws SQLException;

    boolean create(Role role) throws SQLException;

    boolean update(Role role) throws SQLException;

    boolean delete(int roleId) throws SQLException;
}

