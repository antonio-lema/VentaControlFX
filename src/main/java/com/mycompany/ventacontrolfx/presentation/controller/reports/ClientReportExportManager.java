package com.mycompany.ventacontrolfx.presentation.controller.reports;

import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Gestor de exportación de datos para el informe de clientes.
 */
public class ClientReportExportManager {

    public void exportToCsv(List<ClientReportDataManager.ClientRow> rows) {
        if (rows == null || rows.isEmpty()) return;

        StringBuilder csv = new StringBuilder();
        csv.append("ID;Nombre;Informaci\u00f3n;Total Facturado;Pedidos;Estado;Nivel\n");

        for (ClientReportDataManager.ClientRow row : rows) {
            String totalStr = String.format(Locale.getDefault(), "%.2f", row.total());
            csv.append(row.clientId()).append(";")
               .append("\"").append(row.clientName().replace("\"", "\"\"")).append("\";")
               .append("\"").append(row.info().replace("\"", "\"\"")).append("\";")
               .append("\"").append(totalStr).append("\";")
               .append(row.count()).append(";")
               .append(row.isActive() ? "Activo" : "Inactivo").append(";")
               .append(row.tier()).append("\n");
        }

        try {
            String fileName = "Reporte_Clientes_" + LocalDate.now() + ".csv";
            File file = new File(System.getProperty("user.home") + "/Desktop/" + fileName);
            byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
            byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(bom);
                fos.write(content);
            }
            AlertUtil.showInfo("Exportaci\u00f3n exitosa", "El archivo " + fileName + " se guard\u00f3 en el escritorio.");
        } catch (Exception e) {
            AlertUtil.showError("Error", "No se pudo exportar: " + e.getMessage());
        }
    }
}

