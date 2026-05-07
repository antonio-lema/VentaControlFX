package com.mycompany.ventacontrolfx.shared.util;

import com.mycompany.ventacontrolfx.infrastructure.persistence.JdbcProductRepository;
import java.sql.SQLException;

public class CountTest {
    public static void main(String[] args) throws SQLException {
        JdbcProductRepository repo = new JdbcProductRepository();
        System.out.println("TOTAL PRODUCTS IN DB: " + repo.count());
    }
}

