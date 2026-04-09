package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.FiscalDocument;
import com.mycompany.ventacontrolfx.domain.model.FiscalDocument.Status;
import com.mycompany.ventacontrolfx.domain.model.FiscalDocument.Type;
import com.mycompany.ventacontrolfx.domain.repository.IFiscalDocumentRepository;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador JDBC para la persistencia de documentos fiscales.
 * Clean Architecture â€” Capa de Infraestructura.
 *
 * Los documentos fiscales se almacenan DIRECTAMENTE sobre la tabla `sales`
 * (columnas aÃ±adidas por migraciÃ³n), evitando duplicaciÃ³n de datos y
 * manteniendo la atomicidad con la venta.
 *
 * Para snapshots del emisor se usa la tabla `doc_issuer_snapshots`.
 */
public class JdbcFiscalDocumentRepository implements IFiscalDocumentRepository {

    // Columnas aÃ±adidas a `sales` por la migraciÃ³n fiscal
    private static final String SAVE_SQL = """
            UPDATE sales SET
                doc_type        = ?,
                doc_series      = ?,
                doc_number      = ?,
                doc_status      = ?,
                control_hash    = ?
            WHERE sale_id = ?
            """;

    private static final String SNAPSHOT_SQL = """
            INSERT INTO doc_issuer_snapshots
                (sale_id, company_name, tax_id, address, phone, issued_at,
                 receiver_name, receiver_tax_id, receiver_address,
                 base_amount, vat_amount, total_amount)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                company_name     = VALUES(company_name),
                tax_id           = VALUES(tax_id),
                address          = VALUES(address),
                phone            = VALUES(phone),
                issued_at        = VALUES(issued_at),
                receiver_name    = VALUES(receiver_name),
                receiver_tax_id  = VALUES(receiver_tax_id),
                receiver_address = VALUES(receiver_address),
                base_amount      = VALUES(base_amount),
                vat_amount       = VALUES(vat_amount),
                total_amount     = VALUES(total_amount)
            """;

    private static final String FIND_BY_SALE_SQL = """
            SELECT s.sale_id, s.doc_type, s.doc_series, s.doc_number,
                   s.doc_status, s.control_hash, s.sale_datetime,
                   s.total, s.iva,
                   i.company_name, i.tax_id, i.address, i.phone, i.issued_at,
                   i.receiver_name, i.receiver_tax_id, i.receiver_address,
                   i.base_amount, i.vat_amount, i.total_amount
            FROM sales s
            LEFT JOIN doc_issuer_snapshots i ON s.sale_id = i.sale_id
            WHERE s.sale_id = ? AND s.doc_number IS NOT NULL
            """;

    private static final String FIND_BY_REF_SQL = """
            SELECT s.sale_id, s.doc_type, s.doc_series, s.doc_number,
                   s.doc_status, s.control_hash, s.sale_datetime,
                   s.total, s.iva,
                   i.company_name, i.tax_id, i.address, i.phone, i.issued_at,
                   i.receiver_name, i.receiver_tax_id, i.receiver_address,
                   i.base_amount, i.vat_amount, i.total_amount
            FROM sales s
            LEFT JOIN doc_issuer_snapshots i ON s.sale_id = i.sale_id
            WHERE s.doc_series = ? AND s.doc_number = ?
                  AND YEAR(s.sale_datetime) = ?
            """;

    // â”€â”€ save â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    public void save(FiscalDocument doc, Connection conn) throws SQLException {
        // 1. Actualizar la fila de sales con la numeraciÃ³n fiscal
        try (PreparedStatement ps = conn.prepareStatement(SAVE_SQL)) {
            ps.setString(1, doc.getDocType().name());
            ps.setString(2, doc.getDocSeries());
            ps.setInt(3, doc.getDocNumber());
            ps.setString(4, doc.getDocStatus().name());
            ps.setString(5, doc.getControlHash());
            ps.setInt(6, doc.getSaleId());
            ps.executeUpdate();
        }

        // 2. Guardar el snapshot de emisor/receptor en tabla separada
        try (PreparedStatement ps = conn.prepareStatement(SNAPSHOT_SQL)) {
            ps.setInt(1, doc.getSaleId());
            ps.setString(2, doc.getIssuerName());
            ps.setString(3, doc.getIssuerTaxId());
            ps.setString(4, doc.getIssuerAddress());
            ps.setString(5, doc.getIssuerPhone());
            ps.setTimestamp(6, doc.getIssuedAt() != null
                    ? Timestamp.valueOf(doc.getIssuedAt())
                    : null);
            ps.setString(7, doc.getReceiverName());
            ps.setString(8, doc.getReceiverTaxId());
            ps.setString(9, doc.getReceiverAddress());
            ps.setDouble(10, doc.getBaseAmount());
            ps.setDouble(11, doc.getVatAmount());
            ps.setDouble(12, doc.getTotalAmount());
            ps.executeUpdate();
        }
    }

    // â”€â”€ findBySaleId â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    public Optional<FiscalDocument> findBySaleId(int saleId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(FIND_BY_SALE_SQL)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    // â”€â”€ findByReference â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    public Optional<FiscalDocument> findByReference(String reference) throws SQLException {
        // reference = "2026-T-00042" â†’ year=2026, series="T", num=42
        String[] parts = reference.split("-", 3);
        if (parts.length != 3)
            return Optional.empty();
        try {
            int year = Integer.parseInt(parts[0]);
            String series = parts[1];
            int number = Integer.parseInt(parts[2]);
            try (Connection conn = DBConnection.getConnection();
                    PreparedStatement ps = conn.prepareStatement(FIND_BY_REF_SQL)) {
                ps.setString(1, series);
                ps.setInt(2, number);
                ps.setInt(3, year);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next())
                        return Optional.of(mapRow(rs));
                }
            }
        } catch (NumberFormatException ignored) {
        }
        return Optional.empty();
    }

    // â”€â”€ findByFilters â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    public List<FiscalDocument> findByFilters(LocalDate from, LocalDate to,
            Status status, String docType) throws SQLException {

        // El CTE o subconsulta unificada permite aplicar filtros de forma consistente a
        // ambos orÃ­genes
        String baseSql = """
                SELECT * FROM (
                    SELECT s.sale_id, s.doc_type, s.doc_series, s.doc_number,
                           s.doc_status, s.control_hash, s.sale_datetime,
                           s.total, s.iva,
                           i.company_name, i.tax_id, i.address, i.phone, i.issued_at,
                           i.receiver_name, i.receiver_tax_id, i.receiver_address,
                           i.base_amount, i.vat_amount, i.total_amount
                    FROM sales s
                    LEFT JOIN doc_issuer_snapshots i ON s.sale_id = i.sale_id
                    WHERE s.doc_number IS NOT NULL

                    UNION ALL

                    SELECT r.sale_id, r.doc_type, r.doc_series, r.doc_number,
                           r.doc_status, r.control_hash, r.return_datetime as sale_datetime,
                           r.total_refunded as total, 0 as iva,
                           r.issuer_name as company_name, r.issuer_tax_id as tax_id, r.issuer_address as address,
                           '' as phone, r.return_datetime as issued_at,
                           r.customer_name_snapshot as receiver_name, '' as receiver_tax_id, '' as receiver_address,
                           r.total_refunded as base_amount, 0 as vat_amount, r.total_refunded as total_amount
                    FROM returns r
                    WHERE r.doc_number IS NOT NULL
                ) AS unified_docs
                WHERE 1=1
                """;

        StringBuilder sb = new StringBuilder(baseSql);
        List<Object> params = new ArrayList<>();

        if (from != null) {
            sb.append(" AND DATE(sale_datetime) >= ?");
            params.add(Date.valueOf(from));
        }
        if (to != null) {
            sb.append(" AND DATE(sale_datetime) <= ?");
            params.add(Date.valueOf(to));
        }
        if (status != null) {
            sb.append(" AND doc_status = ?");
            params.add(status.name());
        }
        if (docType != null) {
            sb.append(" AND doc_type = ?");
            params.add(docType);
        }
        sb.append(" ORDER BY sale_datetime DESC");

        List<FiscalDocument> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    result.add(mapRow(rs));
            }
        }
        return result;
    }

    // â”€â”€ updateStatus â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    public void updateStatus(int saleId, Status newStatus, Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sales SET doc_status = ? WHERE sale_id = ?")) {
            ps.setString(1, newStatus.name());
            ps.setInt(2, saleId);
            ps.executeUpdate();
        }
    }

    // â”€â”€ mapRow â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private FiscalDocument mapRow(ResultSet rs) throws SQLException {
        String typeStr = rs.getString("doc_type");
        FiscalDocument.Type type = FiscalDocument.Type.TICKET;
        if (typeStr != null) {
            try {
                type = FiscalDocument.Type.valueOf(typeStr);
            } catch (Exception ignored) {
            }
        }

        String statusStr = rs.getString("doc_status");
        FiscalDocument.Status status = FiscalDocument.Status.EMITIDO;
        if (statusStr != null) {
            try {
                status = FiscalDocument.Status.valueOf(statusStr);
            } catch (Exception ignored) {
            }
        }

        // Importes â€” preferimos los del snapshot (mÃ¡s precisos)
        double baseAmt = rs.getDouble("base_amount");
        double vatAmt = rs.getDouble("vat_amount");
        double totAmt = rs.getDouble("total_amount");

        // Fallback a columnas de sales si el snapshot aÃºn no existe
        if (totAmt == 0) {
            totAmt = rs.getDouble("total");
            vatAmt = rs.getDouble("iva");
            baseAmt = totAmt - vatAmt;
        }

        Timestamp issuedAtTs = rs.getTimestamp("issued_at");
        if (issuedAtTs == null)
            issuedAtTs = rs.getTimestamp("sale_datetime");

        return FiscalDocument.builder()
                .saleId(rs.getInt("sale_id"))
                .type(type)
                .series(rs.getString("doc_series"))
                .number(rs.getInt("doc_number"))
                .status(status)
                .issuer(
                        rs.getString("company_name"),
                        rs.getString("tax_id"),
                        rs.getString("address"),
                        rs.getString("phone"))
                .receiver(
                        rs.getString("receiver_name"),
                        rs.getString("receiver_tax_id"),
                        rs.getString("receiver_address"))
                .amounts(baseAmt, vatAmt, totAmt)
                .issuedAt(issuedAtTs != null ? issuedAtTs.toLocalDateTime() : LocalDateTime.now())
                .controlHash(rs.getString("control_hash"))
                .build();
    }
}
