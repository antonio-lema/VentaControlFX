package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;

public class DiagnosticClientReport {
    public static void main(String[] args) {
        try (FileWriter fw = new FileWriter("C:\\Users\\practicassoftware1\\Desktop\\diagnostic_log.txt")) {
            fw.write("Iniciando diagnostico...\n");

            try (Connection conn = DBConnection.getConnection()) {
                fw.write("Conexion exitosa.\n");

                LocalDate end = LocalDate.now();
                LocalDate start = end.minusYears(20);

                String sql = "SELECT client_id, COUNT(sale_id) as orders, SUM(total) as spent, MAX(sale_datetime) as last_date "
                        + "FROM sales WHERE sale_datetime >= ? AND sale_datetime <= ? AND (is_return IS FALSE OR is_return IS NULL) AND client_id IS NOT NULL "
                        + "GROUP BY client_id";

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setTimestamp(1, Timestamp.valueOf(start.atStartOfDay()));
                    pstmt.setTimestamp(2, Timestamp.valueOf(end.atTime(23, 59, 59)));

                    try (ResultSet rs = pstmt.executeQuery()) {
                        int count = 0;
                        while (rs.next()) {
                            count++;
                            fw.write("Fila " + count + ": ClientID=" + rs.getInt("client_id") + ", Orders="
                                    + rs.getInt("orders") + "\n");
                        }
                        fw.write("Total filas encontradas: " + count + "\n");
                    }
                }
            } catch (Exception e) {
                fw.write("ERROR SQL: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
            fw.write("Diagnostico finalizado.\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
