package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import org.junit.jupiter.api.Test;

import java.sql.*;

public class ListTablesTest {

    @Test
    public void listTables() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(null, null, "%", new String[] { "TABLE" });
            System.out.println("--- TABLES IN DATABASE ---");
            while (rs.next()) {
                System.out.println(rs.getString("TABLE_NAME"));
            }
            System.out.println("--------------------------");
        }
    }
}
