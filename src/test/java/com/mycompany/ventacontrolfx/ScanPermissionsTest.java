package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import org.junit.jupiter.api.Test;

import java.sql.*;

public class ScanPermissionsTest {

    @Test
    public void scanPermissions() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM permissions")) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            System.out.println("--- PERMISSIONS TABLE ---");
            int foundCount = 0;
            while (rs.next()) {
                boolean rowHasCorrupt = false;
                StringBuilder rowStr = new StringBuilder();
                for (int i = 1; i <= columnCount; i++) {
                    String colName = metaData.getColumnName(i);
                    String val = rs.getString(i);
                    rowStr.append(colName).append("=").append(val).append(" | ");
                    if (val != null && val.contains("??")) {
                        rowHasCorrupt = true;
                    }
                }
                if (rowHasCorrupt) {
                    System.out.println(rowStr.toString());
                    foundCount++;
                }
            }
            System.out.println("Total rows with ?? found: " + foundCount);
        }
    }
}
