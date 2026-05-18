package com.mycompany.ventacontrolfx.infrastructure.external.aeat;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.mycompany.ventacontrolfx.presentation.model.FiscalOperationModel;
import com.mycompany.ventacontrolfx.presentation.model.FiscalLogModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio encargado exclusivamente de extraer registros pendientes
 * y convertirlos al DTO VerifactuPayload para el procesamiento SOAP.
 * Realiza una consulta unificada (UNION) sobre sales y returns para preservar
 * el orden cronológico estricto que exige el encadenamiento de Hashes.
 */
public class JdbcVerifactuRepository {

    public List<VerifactuPayload> getPendingRecordsOrderByIdAsc(String defaultNif, String defaultRazonSocial) {
        List<VerifactuPayload> pending = new ArrayList<>();
        String sql = "SELECT 'ALTA' AS op_type, s.sale_id AS id, s.doc_type, s.doc_series, s.doc_number, " +
                "s.sale_datetime AS fecha, s.total AS importe, s.total_net AS base, s.total_tax AS cuota, s.control_hash, s.prev_hash, s.gen_timestamp, s.is_correction, "
                +
                "s.customer_name_snapshot, s.customer_nif_snapshot, s.incident_reason, " +
                "NULL AS orig_series, NULL AS orig_num, NULL AS orig_date, 0 AS orig_base, 0 AS orig_cuota " +
                "FROM sales s WHERE s.fiscal_status = 'PENDING' AND s.doc_series IS NOT NULL AND s.doc_number IS NOT NULL "
                +
                "UNION ALL " +
                "SELECT 'ANULACION' AS op_type, s.sale_id AS id, s.doc_type, s.doc_series, s.doc_number, " +
                "s.sale_datetime AS fecha, s.total AS importe, s.total_net AS base, s.total_tax AS cuota, s.control_hash, s.prev_hash, s.gen_timestamp, s.is_correction, "
                +
                "s.customer_name_snapshot, s.customer_nif_snapshot, s.incident_reason, " +
                "NULL AS orig_series, NULL AS orig_num, NULL AS orig_date, 0 AS orig_base, 0 AS orig_cuota " +
                "FROM sales s WHERE s.fiscal_status = 'VOID_PENDING' AND s.doc_series IS NOT NULL AND s.doc_number IS NOT NULL "
                +
                "UNION ALL " +
                "SELECT 'ANULACION' AS op_type, r.return_id AS id, r.doc_type, r.doc_series, r.doc_number, " +
                "r.return_datetime AS fecha, -r.total_refunded AS importe, -r.tax_basis AS base, -r.total_tax AS cuota, r.control_hash, r.prev_hash, r.gen_timestamp, r.is_correction, "
                +
                "r.customer_name_snapshot, r.customer_nif_snapshot, r.incident_reason, " +
                "NULL AS orig_series, NULL AS orig_num, NULL AS orig_date, 0 AS orig_base, 0 AS orig_cuota " +
                "FROM returns r WHERE r.fiscal_status = 'VOID_PENDING' AND r.doc_series IS NOT NULL AND r.doc_number IS NOT NULL "
                +
                "UNION ALL " +
                "SELECT 'RECTIFICATIVA' AS op_type, r.return_id AS id, r.doc_type, r.doc_series, r.doc_number, " +
                "r.return_datetime AS fecha, -r.total_refunded AS importe, -r.tax_basis AS base, -r.total_tax AS cuota, r.control_hash, r.prev_hash, r.gen_timestamp, r.is_correction, "
                +
                "r.customer_name_snapshot, r.customer_nif_snapshot, r.incident_reason, " +
                "s_orig.doc_series AS orig_series, s_orig.doc_number AS orig_num, s_orig.sale_datetime AS orig_date, " +
                "s_orig.total - s_orig.iva AS orig_base, s_orig.iva AS orig_cuota " +
                "FROM returns r " +
                "LEFT JOIN sales s_orig ON r.sale_id = s_orig.sale_id " +
                "WHERE (r.fiscal_status = 'PENDING' OR r.fiscal_status = 'pending') AND r.doc_series IS NOT NULL AND r.doc_number IS NOT NULL AND (s_orig.doc_number IS NOT NULL OR r.sale_id IS NULL) "
                +
                "ORDER BY fecha ASC LIMIT 1000";

        try (Connection conn = com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection.getConnection()) {

            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                    ResultSet rs = pstmt.executeQuery()) {

                DateTimeFormatter aeatFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                int found = 0;
                while (rs.next()) {
                    found++;
                    String opType = rs.getString("op_type");
                    int id = rs.getInt("id");

                    String docSeries = rs.getString("doc_series");
                    int docNumber = rs.getInt("doc_number");

                    java.sql.Timestamp fechaSql = rs.getTimestamp("fecha");
                    if (fechaSql == null) {
                        // System.err.println("[VeriFactu] Saltando " + opType + " con ID " + id + "
                        // porque no tiene fecha.");
                        continue;
                    }
                    LocalDateTime fechaTs = fechaSql.toLocalDateTime();

                    // Formato idéntico al de FiscalDocument.getFullReference() para que el
                    // validador QR de AEAT lo encuentre
                    String fullNumSerie = String.format("%d-%s-%05d", fechaTs.getYear(), docSeries, docNumber);

                    String fechaExpedicion = fechaTs.format(aeatFormatter);

                    double importe = rs.getDouble("importe");
                    double base = rs.getDouble("base");
                    double cuota = rs.getDouble("cuota");

                    String controlHash = rs.getString("control_hash");
                    String prevHash = rs.getString("prev_hash");

                    String prevNumSerie = "";
                    String prevFechaExpedicion = "";
                    if (prevHash != null && !prevHash.isEmpty()) {
                        // Buscar detalles del registro anterior por su hash
                        PrevDetails prev = findPrevDetailsByHash(conn, prevHash);
                        if (prev != null) {
                            prevNumSerie = prev.numSerie;
                            prevFechaExpedicion = prev.fecha;
                        } else {
                            // Fallback defensivo: si no se encuentra (borrado?), usamos la de hoy para
                            // evitar error de esquema
                            prevNumSerie = fullNumSerie;
                            prevFechaExpedicion = fechaExpedicion;
                        }
                    }

                    // Mapeo dinámico de tipo de factura:
                    // Si hay NIF de cliente -> F1 (Completa)
                    // Si no hay NIF -> F2 (Simplificada / Ticket)
                    String customerNif = rs.getString("customer_nif_snapshot");
                    boolean hasCustomer = customerNif != null && !customerNif.trim().isEmpty();

                    String tipoFacturaMapped;
                    if (opType.equals("ANULACION") || "R".equals(docSeries)) {
                        tipoFacturaMapped = "R5";
                    } else {
                        tipoFacturaMapped = hasCustomer ? "F1" : "F2";
                    }

                    boolean isAnulacion = opType.equals("ANULACION");

                    // Reconstruir genTimestamp determinístico para que el hash coincida
                    String genTimestamp = rs.getString("gen_timestamp");
                    // IMPORTANTE: Para ANULACIONES siempre usamos la hora actual para evitar error
                    // [2004] de AEAT
                    if (opType.equals("ANULACION") || genTimestamp == null || genTimestamp.isEmpty()) {
                        genTimestamp = java.time.ZonedDateTime.now(java.time.ZoneId.of("Europe/Madrid"))
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx"));
                    }

                    VerifactuPayload payload = new VerifactuPayload(
                            defaultNif,
                            defaultRazonSocial,
                            tipoFacturaMapped,
                            fullNumSerie,
                            fechaExpedicion,
                            importe,
                            base,
                            cuota,
                            controlHash,
                            prevHash,
                            prevNumSerie,
                            prevFechaExpedicion,
                            isAnulacion,
                            rs.getBoolean("is_correction"),
                            opType + "-" + id, // Id de rastreo interno
                            genTimestamp,
                            rs.getString("customer_name_snapshot"),
                            rs.getString("customer_nif_snapshot"));

                    payload.setIncidentReason(rs.getString("incident_reason"));

                    // Si es rectificativa (y NO una anulación), poblar datos originales.
                    if (payload.isRectificativa() && !isAnulacion) {
                        String oSer = rs.getString("orig_series");
                        int oNum = rs.getInt("orig_num");
                        java.sql.Timestamp oDateTs = rs.getTimestamp("orig_date");
                        if (oSer != null && oDateTs != null && oNum > 0) {
                            LocalDateTime oLdt = oDateTs.toLocalDateTime();
                            String oFullNum = String.format("%d-%s-%05d", oLdt.getYear(), oSer, oNum);
                            String oFechaStr = oLdt.format(aeatFormatter);
                            payload.setRectificacion(oFullNum, oFechaStr, rs.getDouble("orig_base"),
                                    rs.getDouble("orig_cuota"));
                            pending.add(payload);
                        } else {
                            updateStatus(opType + "-" + id, "ERROR",
                                    "Devolución sin venta original válida: NumSerieFactura vacío rompe XSD");
                        }
                    } else {
                        // Si no es rectificativa, añadir siempre
                        pending.add(payload);
                    }
                }
            }

            // Poblar desgloses de IVA para cada registro (Batch local para evitar lentitud
            // excesiva)
            for (VerifactuPayload p : pending) {
                p.setVatBreakdown(fetchVatBreakdown(p.getIdRegistro(), conn));
            }

            if (!pending.isEmpty()) {
                // System.out.println("[VeriFactu] " + pending.size() + " registros listos para
                // enviar.");
            }
        } catch (Exception e) {
            // System.err.println("[VeriFactu] ERROR al leer registros pendientes: " +
            // e.getMessage());
            // e.printStackTrace();
        }
        return pending;
    }

    private java.util.Map<Double, Double[]> fetchVatBreakdown(String traceId, Connection conn) {
        java.util.Map<Double, Double[]> breakdown = new java.util.HashMap<>();
        String[] parts = traceId.split("-");
        String type = parts[0];
        int id = Integer.parseInt(parts[1]);

        String sql = "ALTA".equals(type)
                ? "SELECT iva_rate as tax_rate, SUM(tax_basis) as base, SUM(tax_amount) as cuota FROM sale_details WHERE sale_id = ? GROUP BY iva_rate"
                : "SELECT (tax_amount/net_amount)*100 as tax_rate, SUM(net_amount) as base, SUM(tax_amount) as cuota FROM return_details WHERE return_id = ? GROUP BY tax_rate";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double rate = rs.getDouble("tax_rate");
                    // Redondear rate a 2 decimales para evitar problemas de precisión en el GROUP
                    // BY o en el mapa
                    rate = Math.round(rate * 100.0) / 100.0;

                    double base = rs.getDouble("base");
                    double cuota = rs.getDouble("cuota");
                    breakdown.put(rate, new Double[] { base, cuota });
                }
            }
        } catch (Exception e) {
            // System.err.println("[VeriFactu] Error cargando IVA para " + traceId + ": " +
            // e.getMessage());
        }
        return breakdown;
    }

    /**
     * Actualiza el estado fiscal de la operación tras una respuesta de AEAT.
     */
    public void updateStatus(String registroId, String status, String statusMsg) {
        if (registroId == null || !registroId.contains("-"))
            return;

        String[] parts = registroId.split("-");
        String opType = parts[0];
        int id = Integer.parseInt(parts[1]);

        // Si es una anulación, la tabla siempre es sales
        String table = (opType.equals("ALTA") || opType.equals("ANULACION")) ? "sales" : "returns";
        String idColumn = (opType.equals("ALTA") || opType.equals("ANULACION")) ? "sale_id" : "return_id";

        // SQL base para actualización fiscal
        String sql = "UPDATE " + table + " SET fiscal_status = ?, fiscal_msg = ?, aeat_submission_id = ? WHERE "
                + idColumn + " = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, statusMsg != null && statusMsg.length() > 500 ? statusMsg.substring(0, 500) : statusMsg);

            if ("ACCEPTED".equals(status) && statusMsg != null && statusMsg.length() >= 16) {
                pstmt.setString(3, statusMsg);
            } else {
                pstmt.setString(3, null);
            }

            pstmt.setInt(4, id);
            pstmt.executeUpdate();

            // Lógica Adicional: Si la ANULACIÓN fue aceptada, marcamos el documento como
            // VOIDED
            if (opType.equals("ANULACION") && "ACCEPTED".equals(status)) {
                String voidSql = "UPDATE sales SET doc_status = 'VOIDED' WHERE sale_id = ?";
                try (PreparedStatement psVoid = conn.prepareStatement(voidSql)) {
                    psVoid.setInt(1, id);
                    psVoid.executeUpdate();
                }
            }

            // REGISTRO EN EL HISTORIAL (LOG)
            logOperation(conn, parts[0].equals("ALTA") ? "SALE" : "RETURN", id, opType, status, statusMsg);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateBatchStatus(List<VerifactuPayload> batch, String status, String msg) {
        for (VerifactuPayload p : batch) {
            updateStatus(p.getIdRegistro(), status, msg);
        }
    }

    private void logOperation(Connection conn, String entityType, int entityId, String opType, String status,
            String msg) {
        // Obtenemos los XML actuales para el log (opcionalmente podríamos pasarlos como
        // parámetros)
        String table = entityType.equals("SALE") ? "sales" : "returns";
        String idCol = entityType.equals("SALE") ? "sale_id" : "return_id";

        String xmlSent = null;
        String xmlReceived = null;

        String xmlSql = "SELECT xml_sent, xml_received FROM " + table + " WHERE " + idCol + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(xmlSql)) {
            ps.setInt(1, entityId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    xmlSent = rs.getString("xml_sent");
                    xmlReceived = rs.getString("xml_received");
                }
            }
        } catch (Exception e) {
        }

        String sql = "INSERT INTO verifactu_log (entity_type, entity_id, operation_type, status, message, xml_sent, xml_received) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entityType);
            ps.setInt(2, entityId);
            ps.setString(3, opType);
            ps.setString(4, status);
            ps.setString(5, msg);
            ps.setString(6, xmlSent);
            ps.setString(7, xmlReceived);
            ps.executeUpdate();
        } catch (Exception e) {
            // Si la tabla no existe aún, fallará silenciosamente o podríamos intentar
            // crearla
            // Pero preferimos confiar en el ensureSchemaUpdate del controlador
        }
    }

    public void saveXmlSent(List<VerifactuPayload> batch, String xmlSent) {
        if (batch == null || batch.isEmpty() || xmlSent == null)
            return;

        try (Connection conn = DBConnection.getConnection()) {
            for (VerifactuPayload p : batch) {
                String[] parts = p.getIdRegistro().split("-");
                String table = parts[0].equals("ALTA") ? "sales" : "returns";
                String idCol = parts[0].equals("ALTA") ? "sale_id" : "return_id";
                int id = Integer.parseInt(parts[1]);

                String sql = "UPDATE " + table + " SET xml_sent = ? WHERE " + idCol + " = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, xmlSent);
                    ps.setInt(2, id);
                    ps.executeUpdate();
                }

                // Log the send attempt
                logOperation(conn, parts[0].equals("ALTA") ? "SALE" : "RETURN", id, parts[0], "SENT",
                        "Enviado a Hacienda (Esperando respuesta)");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveXmlReceived(List<VerifactuPayload> batch, String xmlReceived) {
        if (batch == null || batch.isEmpty() || xmlReceived == null)
            return;

        try (Connection conn = DBConnection.getConnection()) {
            for (VerifactuPayload p : batch) {
                String[] parts = p.getIdRegistro().split("-");
                String table = parts[0].equals("ALTA") ? "sales" : "returns";
                String idCol = parts[0].equals("ALTA") ? "sale_id" : "return_id";
                int id = Integer.parseInt(parts[1]);

                String sql = "UPDATE " + table + " SET xml_received = ? WHERE " + idCol + " = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, xmlReceived);
                    ps.setInt(2, id);
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private PrevDetails findPrevDetailsByHash(Connection conn, String hash) {
        String sql = "SELECT sale_datetime AS fecha, doc_series, doc_number FROM sales WHERE control_hash = ? " +
                "UNION " +
                "SELECT return_datetime AS fecha, doc_series, doc_number FROM returns WHERE control_hash = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hash);
            pstmt.setString(2, hash);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    LocalDateTime ts = rs.getTimestamp("fecha").toLocalDateTime();
                    String series = rs.getString("doc_series");
                    int num = rs.getInt("doc_number");
                    String fullNum = String.format("%d-%s-%05d", ts.getYear(), series, num);
                    String fechaStr = ts.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                    return new PrevDetails(fullNum, fechaStr);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class PrevDetails {
        String numSerie;
        String fecha;

        PrevDetails(String n, String f) {
            this.numSerie = n;
            this.fecha = f;
        }
    }

    public void saveIncidentReason(java.util.List<Integer> saleIds, java.util.List<Integer> returnIds, String reason) {
        try (Connection conn = DBConnection.getConnection()) {
            if (saleIds != null && !saleIds.isEmpty()) {
                String sql = "UPDATE sales SET incident_reason = ? WHERE sale_id IN (" + buildInClause(saleIds) + ")";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, reason);
                    ps.executeUpdate();
                }
            }
            if (returnIds != null && !returnIds.isEmpty()) {
                String sql = "UPDATE returns SET incident_reason = ? WHERE return_id IN (" + buildInClause(returnIds)
                        + ")";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, reason);
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<FiscalOperationModel> getRecentFiscalOperations(int limit) {
        List<FiscalOperationModel> list = new ArrayList<>();
        String sql = "SELECT 'ALTA' AS op_type, sale_id AS id, doc_series, doc_number, sale_datetime AS fecha, total, fiscal_status, fiscal_msg, xml_sent, xml_received, doc_status "
                +
                "FROM sales " +
                "UNION ALL " +
                "SELECT 'RECTIFICATIVA' AS op_type, return_id AS id, doc_series, doc_number, return_datetime AS fecha, -total_refunded AS total, fiscal_status, fiscal_msg, xml_sent, xml_received, doc_status "
                +
                "FROM returns " +
                "ORDER BY fecha DESC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String series = rs.getString("doc_series");
                    int num = rs.getInt("doc_number");
                    String doc;
                    if (series != null && num > 0) {
                        doc = series + "-" + String.format("%05d", num);
                    } else {
                        doc = "LEGACY-" + rs.getInt("id");
                    }

                    String status = rs.getString("fiscal_status");
                    if (status == null)
                        status = "NON_FISCAL";

                    list.add(new FiscalOperationModel(
                            rs.getInt("id"),
                            rs.getString("op_type"),
                            doc,
                            rs.getTimestamp("fecha").toString(),
                            rs.getDouble("total"),
                            status,
                            rs.getString("fiscal_msg"),
                            rs.getString("xml_sent"),
                            rs.getString("xml_received"),
                            rs.getString("doc_status")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private String buildInClause(java.util.List<Integer> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i));
            if (i < ids.size() - 1)
                sb.append(",");
        }
        return sb.toString();
    }

    public void requestVoid(String traceId) {
        if (traceId == null || !traceId.contains("-"))
            return;
        String[] parts = traceId.split("-");
        String opType = parts[0];
        int id = Integer.parseInt(parts[1]);

        String table = opType.equals("ALTA") || opType.equals("ANULACION") ? "sales" : "returns";
        String idCol = opType.equals("ALTA") || opType.equals("ANULACION") ? "sale_id" : "return_id";

        String sql = "UPDATE " + table + " SET fiscal_status = 'VOID_PENDING' WHERE " + idCol + " = ?";
        try (java.sql.Connection conn = com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection
                .getConnection();
                java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<FiscalLogModel> getFiscalHistory(String opType, int id) {
        List<FiscalLogModel> history = new ArrayList<>();
        String entityType = (opType.equals("ALTA") || opType.equals("ANULACION")) ? "SALE" : "RETURN";

        String sql = "SELECT log_id, status, message, created_at, xml_sent, xml_received FROM verifactu_log " +
                "WHERE entity_type = ? AND entity_id = ? ORDER BY created_at DESC";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entityType);
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(new FiscalLogModel(
                            rs.getInt("log_id"),
                            rs.getString("status"),
                            rs.getString("message"),
                            rs.getTimestamp("created_at").toString(),
                            rs.getString("xml_sent"),
                            rs.getString("xml_received")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return history;
    }

    public int[] getFiscalSummary() {
        int[] summary = new int[3]; // [Total, Accepted, Rejected]
        String sql = "SELECT fiscal_status, COUNT(*) as cnt FROM (" +
                "SELECT fiscal_status FROM sales WHERE doc_series IS NOT NULL " +
                "UNION ALL " +
                "SELECT fiscal_status FROM returns WHERE doc_series IS NOT NULL" +
                ") t GROUP BY fiscal_status";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String status = rs.getString("fiscal_status");
                if (status == null)
                    status = "NON_FISCAL";
                int count = rs.getInt("cnt");

                if (!"NON_FISCAL".equals(status)) {
                    summary[0] += count; // Total Verifactu
                    if ("ACCEPTED".equals(status)) {
                        summary[1] += count; // Accepted
                    } else if ("REJECTED".equals(status) || "ERROR".equals(status)) {
                        summary[2] += count; // Rejected
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return summary;
    }
}
