package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.TaxRevision;
import com.mycompany.ventacontrolfx.domain.repository.ITaxRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC del repositorio de historial de IVA.
 *
 * Asume la existencia de la tabla 'tax_rates' en la base de datos con la
 * siguiente estructura mínima:
 *
 * CREATE TABLE IF NOT EXISTS tax_rates (
 * tax_rate_id INT AUTO_INCREMENT PRIMARY KEY,
 * product_id INT NULL,
 * category_id INT NULL,
 * scope VARCHAR(10) NOT NULL, -- 'PRODUCT', 'CATEGORY', 'GLOBAL'
 * rate DECIMAL(5,2) NOT NULL,
 * label VARCHAR(50),
 * start_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 * end_date DATETIME NULL,
 * reason VARCHAR(255)
 * );
 */
public class JdbcTaxRepository implements ITaxRepository {

    // -------------------------------------------------------------------------
    // CONSULTA DE TASA EFECTIVA
    // -------------------------------------------------------------------------

    @Override
    public double resolveActiveRate(int productId, int categoryId, LocalDateTime at)
            throws SQLException {
        Timestamp atTs = Timestamp.valueOf(at);

        // 1. Buscar tasa propia del producto (mayor prioridad)
        String productSql = "SELECT rate FROM tax_rates "
                + "WHERE scope = 'PRODUCT' AND product_id = ? "
                + "AND start_date <= ? AND (end_date IS NULL OR end_date > ?) "
                + "ORDER BY start_date DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(productSql)) {
            ps.setInt(1, productId);
            ps.setTimestamp(2, atTs);
            ps.setTimestamp(3, atTs);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getDouble("rate");
            }
        }

        // 2. Buscar tasa de la categoría (segunda prioridad)
        String categorySql = "SELECT rate FROM tax_rates "
                + "WHERE scope = 'CATEGORY' AND category_id = ? "
                + "AND start_date <= ? AND (end_date IS NULL OR end_date > ?) "
                + "ORDER BY start_date DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(categorySql)) {
            ps.setInt(1, categoryId);
            ps.setTimestamp(2, atTs);
            ps.setTimestamp(3, atTs);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getDouble("rate");
            }
        }

        // 3. Buscar tasa global (fallback)
        String globalSql = "SELECT rate FROM tax_rates "
                + "WHERE scope = 'GLOBAL' "
                + "AND start_date <= ? AND (end_date IS NULL OR end_date > ?) "
                + "ORDER BY start_date DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(globalSql)) {
            ps.setTimestamp(1, atTs);
            ps.setTimestamp(2, atTs);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getDouble("rate");
            }
        }

        // 4. Si no hay nada configurado, devolver 21% por defecto (IVA general español)
        return 21.0;
    }

    // -------------------------------------------------------------------------
    // PERSISTENCIA
    // -------------------------------------------------------------------------

    @Override
    public void save(TaxRevision revision) throws SQLException {
        String sql = "INSERT INTO tax_rates "
                + "(product_id, category_id, scope, rate, label, start_date, end_date, reason) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindRevision(ps, revision);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next())
                    revision.setId(keys.getInt(1));
            }
        }
    }

    @Override
    public void closeCurrentAndSave(TaxRevision newRevision) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Cerrar la revisión actual del mismo scope
                closeCurrent(conn, newRevision);

                // 2. Insertar la nueva revisión
                String insertSql = "INSERT INTO tax_rates "
                        + "(product_id, category_id, scope, rate, label, start_date, end_date, reason) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    bindRevision(ps, newRevision);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next())
                            newRevision.setId(keys.getInt(1));
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // -------------------------------------------------------------------------
    // HISTORIAL
    // -------------------------------------------------------------------------

    @Override
    public List<TaxRevision> findProductHistory(int productId) throws SQLException {
        String sql = "SELECT * FROM tax_rates WHERE scope = 'PRODUCT' AND product_id = ? "
                + "ORDER BY start_date DESC";
        return queryList(sql, ps -> ps.setInt(1, productId));
    }

    @Override
    public List<TaxRevision> findCategoryHistory(int categoryId) throws SQLException {
        String sql = "SELECT * FROM tax_rates WHERE scope = 'CATEGORY' AND category_id = ? "
                + "ORDER BY start_date DESC";
        return queryList(sql, ps -> ps.setInt(1, categoryId));
    }

    @Override
    public List<TaxRevision> findGlobalHistory() throws SQLException {
        String sql = "SELECT * FROM tax_rates WHERE scope = 'GLOBAL' ORDER BY start_date DESC";
        return queryList(sql, ps -> {
        });
    }

    @Override
    public Optional<TaxRevision> getActiveGlobalRate() throws SQLException {
        String sql = "SELECT * FROM tax_rates WHERE scope = 'GLOBAL' "
                + "AND start_date <= CURRENT_TIMESTAMP "
                + "AND (end_date IS NULL OR end_date > CURRENT_TIMESTAMP) "
                + "ORDER BY start_date DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                return Optional.of(mapTaxRevision(rs));
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // OPERACIONES MASIVAS
    // -------------------------------------------------------------------------

    @Override
    public int applyBulkVatChangeToCategory(int categoryId, double newRate,
            LocalDateTime effectiveFrom, String reason) throws SQLException {
        int updated = 0;
        LocalDateTime startDate = (effectiveFrom != null) ? effectiveFrom : LocalDateTime.now();
        Timestamp startTs = Timestamp.valueOf(startDate);

        // Obtener productos afectados: los que NO tienen IVA propio activo
        String selectSql = "SELECT p.product_id FROM products p "
                + "WHERE p.category_id = ? "
                + "AND NOT EXISTS ("
                + "  SELECT 1 FROM tax_rates tr "
                + "  WHERE tr.scope = 'PRODUCT' AND tr.product_id = p.product_id "
                + "  AND tr.end_date IS NULL"
                + ")";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Cerrar la tasa de categoría actual
                String closeCategorySql = "UPDATE tax_rates SET end_date = ? "
                        + "WHERE scope = 'CATEGORY' AND category_id = ? AND end_date IS NULL";
                try (PreparedStatement closePs = conn.prepareStatement(closeCategorySql)) {
                    closePs.setTimestamp(1, startTs);
                    closePs.setInt(2, categoryId);
                    closePs.executeUpdate();
                }

                // 2. Insertar nueva tasa de categoría
                String insertCategorySql = "INSERT INTO tax_rates "
                        + "(category_id, scope, rate, label, start_date, reason) "
                        + "VALUES (?, 'CATEGORY', ?, ?, ?, ?)";
                try (PreparedStatement insertPs = conn.prepareStatement(insertCategorySql)) {
                    insertPs.setInt(1, categoryId);
                    insertPs.setDouble(2, newRate);
                    insertPs.setString(3, "IVA " + newRate + "%");
                    insertPs.setTimestamp(4, startTs);
                    insertPs.setString(5, reason);
                    insertPs.executeUpdate();
                    updated++;
                }

                // 3. Actualizar también el campo denormalizado de la tabla categories y
                // productos
                // Limpiamos products.iva para que hereden el nuevo default_iva de la categoría
                String updateCatSql = "UPDATE categories SET default_iva = ?, tax_rate = ? WHERE category_id = ?";
                try (PreparedStatement updateCatPs = conn.prepareStatement(updateCatSql)) {
                    updateCatPs.setDouble(1, newRate);
                    updateCatPs.setDouble(2, newRate); // Legacy compat
                    updateCatPs.setInt(3, categoryId);
                    updateCatPs.executeUpdate();
                }

                String clearProdIvaSql = "UPDATE products SET iva = NULL, tax_rate = NULL WHERE category_id = ?";
                try (PreparedStatement clearPs = conn.prepareStatement(clearProdIvaSql)) {
                    clearPs.setInt(1, categoryId);
                    clearPs.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return updated;
    }

    @Override
    public void applyGlobalVatChange(double newRate, LocalDateTime effectiveFrom, String reason)
            throws SQLException {
        LocalDateTime startDate = (effectiveFrom != null) ? effectiveFrom : LocalDateTime.now();
        TaxRevision globalRevision = new TaxRevision(
                null, null, TaxRevision.Scope.GLOBAL,
                newRate, "IVA Global " + newRate + "%",
                startDate, reason);
        closeCurrentAndSave(globalRevision);

        // --- Sincronización con legacy system_config ---
        String syncSql = "INSERT INTO system_config (config_key, config_value) VALUES ('taxRate', ?) " +
                "ON DUPLICATE KEY UPDATE config_value = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(syncSql)) {
            ps.setString(1, String.valueOf(newRate));
            ps.setString(2, String.valueOf(newRate));
            ps.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // HELPERS PRIVADOS
    // -------------------------------------------------------------------------

    /** Cierra la revisión activa vigente para el mismo scope/producto/categoría. */
    private void closeCurrent(Connection conn, TaxRevision newRevision) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "UPDATE tax_rates SET end_date = ? WHERE scope = ? AND end_date IS NULL");
        if (newRevision.getProductId() != null)
            sql.append(" AND product_id = ?");
        if (newRevision.getCategoryId() != null)
            sql.append(" AND category_id = ?");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setTimestamp(1, Timestamp.valueOf(newRevision.getStartDate()));
            ps.setString(2, newRevision.getScope().name());
            int paramIdx = 3;
            if (newRevision.getProductId() != null)
                ps.setInt(paramIdx++, newRevision.getProductId());
            if (newRevision.getCategoryId() != null)
                ps.setInt(paramIdx, newRevision.getCategoryId());
            ps.executeUpdate();
        }
    }

    @FunctionalInterface
    private interface PreparedStatementSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<TaxRevision> queryList(String sql, PreparedStatementSetter setter)
            throws SQLException {
        List<TaxRevision> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    result.add(mapTaxRevision(rs));
            }
        }
        return result;
    }

    private void bindRevision(PreparedStatement ps, TaxRevision r) throws SQLException {
        if (r.getProductId() != null)
            ps.setInt(1, r.getProductId());
        else
            ps.setNull(1, Types.INTEGER);
        if (r.getCategoryId() != null)
            ps.setInt(2, r.getCategoryId());
        else
            ps.setNull(2, Types.INTEGER);
        ps.setString(3, r.getScope().name());
        ps.setDouble(4, r.getRate());
        ps.setString(5, r.getLabel());
        ps.setTimestamp(6, Timestamp.valueOf(r.getStartDate()));
        if (r.getEndDate() != null)
            ps.setTimestamp(7, Timestamp.valueOf(r.getEndDate()));
        else
            ps.setNull(7, Types.TIMESTAMP);
        ps.setString(8, r.getReason());
    }

    private TaxRevision mapTaxRevision(ResultSet rs) throws SQLException {
        TaxRevision t = new TaxRevision();
        t.setId(rs.getInt("tax_rate_id"));
        int productId = rs.getInt("product_id");
        if (!rs.wasNull())
            t.setProductId(productId);
        int categoryId = rs.getInt("category_id");
        if (!rs.wasNull())
            t.setCategoryId(categoryId);
        t.setScope(TaxRevision.Scope.valueOf(rs.getString("scope")));
        t.setRate(rs.getDouble("rate"));
        t.setLabel(rs.getString("label"));
        t.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
        Timestamp endTs = rs.getTimestamp("end_date");
        if (endTs != null)
            t.setEndDate(endTs.toLocalDateTime());
        t.setReason(rs.getString("reason"));
        return t;
    }
}
