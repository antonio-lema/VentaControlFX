package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ScanAllTablesTest {

    @Test
    public void scanAllTables() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // Get all user tables
            ResultSet tables = metaData.getTables(null, null, "%", new String[] { "TABLE" });
            List<String> tableNames = new ArrayList<>();
            while (tables.next()) {
                tableNames.add(tables.getString("TABLE_NAME"));
            }

            System.out.println("Scanning " + tableNames.size() + " tables...");

            for (String tableName : tableNames) {
                // Get VARCHAR or TEXT columns for this table
                ResultSet columns = metaData.getColumns(null, null, tableName, null);
                List<String> textColumns = new ArrayList<>();
                String primaryKeyColumn = null;

                // Find primary key
                ResultSet pks = metaData.getPrimaryKeys(null, null, tableName);
                if (pks.next()) {
                    primaryKeyColumn = pks.getString("COLUMN_NAME");
                }

                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    int dataType = columns.getInt("DATA_TYPE");
                    if (dataType == Types.VARCHAR || dataType == Types.CHAR || dataType == Types.LONGVARCHAR) {
                        textColumns.add(columnName);
                    }
                }

                if (textColumns.isEmpty())
                    continue;

                // Build query: SELECT pk, cols FROM table WHERE col1 LIKE '%??%' OR col2 LIKE
                // '%??%' ...
                StringBuilder sql = new StringBuilder("SELECT ");
                if (primaryKeyColumn != null) {
                    sql.append(primaryKeyColumn).append(", ");
                } else {
                    sql.append("* "); // Fallback
                }
                for (int i = 0; i < textColumns.size(); i++) {
                    sql.append(textColumns.get(i));
                    if (i < textColumns.size() - 1)
                        sql.append(", ");
                }
                sql.append(" FROM ").append(tableName).append(" WHERE ");

                for (int i = 0; i < textColumns.size(); i++) {
                    sql.append(textColumns.get(i)).append(" LIKE '%??%'");
                    if (i < textColumns.size() - 1)
                        sql.append(" OR ");
                }

                try (Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery(sql.toString())) {

                    boolean found = false;
                    while (rs.next()) {
                        if (!found) {
                            System.out.println("\n--- Table: " + tableName + " ---");
                            found = true;
                        }
                        String pkVal = primaryKeyColumn != null ? rs.getString(1) : "N/A";
                        System.out.print("PK (" + primaryKeyColumn + "=" + pkVal + "): ");
                        for (int i = 0; i < textColumns.size(); i++) {
                            String colName = textColumns.get(i);
                            String val = rs.getString(primaryKeyColumn != null ? i + 2 : i + 1);
                            if (val != null && val.contains("??")) {
                                System.out.print("[" + colName + "=" + val + "] ");
                            }
                        }
                        System.out.println();
                    }
                } catch (SQLException e) {
                    System.err.println("Error querying table " + tableName + ": " + e.getMessage());
                }
            }
        }
    }
}
