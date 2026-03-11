package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.TaxGroup;
import com.mycompany.ventacontrolfx.domain.model.TaxRate;
import com.mycompany.ventacontrolfx.domain.repository.ITaxRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC para el nuevo Tax Engine V2.
 */
public class JdbcTaxRepository implements ITaxRepository {

    // =========================================================================
    // TAX RATES
    // =========================================================================

    @Override
    public List<TaxRate> getAllTaxRates() throws SQLException {
        String sql = "SELECT * FROM tax_rates";
        return queryTaxRates(sql, null);
    }

    @Override
    public List<TaxRate> getActiveTaxRates() throws SQLException {
        String sql = "SELECT * FROM tax_rates WHERE active = TRUE AND valid_from <= CURRENT_TIMESTAMP AND (valid_to IS NULL OR valid_to > CURRENT_TIMESTAMP)";
        return queryTaxRates(sql, null);
    }

    @Override
    public Optional<TaxRate> getTaxRateById(int taxRateId) throws SQLException {
        String sql = "SELECT * FROM tax_rates WHERE tax_rate_id = ?";
        List<TaxRate> results = queryTaxRates(sql, ps -> ps.setInt(1, taxRateId));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public void saveTaxRate(TaxRate rate) throws SQLException {
        String sql = "INSERT INTO tax_rates (name, rate, country, region, valid_from, valid_to, active) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindTaxRate(ps, rate);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    rate.setTaxRateId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public void updateTaxRate(TaxRate rate) throws SQLException {
        String sql = "UPDATE tax_rates SET name = ?, rate = ?, country = ?, region = ?, valid_from = ?, valid_to = ?, active = ? WHERE tax_rate_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            bindTaxRate(ps, rate);
            ps.setInt(8, rate.getTaxRateId());
            ps.executeUpdate();
        }
    }

    @Override
    public void deleteTaxRate(int taxRateId) throws SQLException {
        String sql = "DELETE FROM tax_rates WHERE tax_rate_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taxRateId);
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // TAX GROUPS
    // =========================================================================

    @Override
    public List<TaxGroup> getAllTaxGroups() throws SQLException {
        String sql = "SELECT * FROM tax_groups";
        List<TaxGroup> groups = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TaxGroup group = mapTaxGroup(rs);
                group.setTaxRates(getTaxRatesForGroup(conn, group.getTaxGroupId()));
                groups.add(group);
            }
        }
        return groups;
    }

    @Override
    public Optional<TaxGroup> getTaxGroupById(int taxGroupId) throws SQLException {
        String sql = "SELECT * FROM tax_groups WHERE tax_group_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taxGroupId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TaxGroup group = mapTaxGroup(rs);
                    group.setTaxRates(getTaxRatesForGroup(conn, group.getTaxGroupId()));
                    return Optional.of(group);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<TaxGroup> getDefaultTaxGroup() throws SQLException {
        String sql = "SELECT * FROM tax_groups WHERE is_default = TRUE LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                TaxGroup group = mapTaxGroup(rs);
                group.setTaxRates(getTaxRatesForGroup(conn, group.getTaxGroupId()));
                return Optional.of(group);
            }
        }
        return Optional.empty();
    }

    @Override
    public void saveTaxGroup(TaxGroup group) throws SQLException {
        String sql = "INSERT INTO tax_groups (name, is_default) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                if (group.isDefault()) {
                    clearOtherDefaults(conn);
                }

                ps.setString(1, group.getName());
                ps.setBoolean(2, group.isDefault());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        group.setTaxGroupId(keys.getInt(1));
                    }
                }

                assignRatesToGroup(conn, group.getTaxGroupId(), group.getTaxRates());

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public void updateTaxGroup(TaxGroup group) throws SQLException {
        String sql = "UPDATE tax_groups SET name = ?, is_default = ? WHERE tax_group_id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {

                if (group.isDefault()) {
                    clearOtherDefaults(conn);
                }

                ps.setString(1, group.getName());
                ps.setBoolean(2, group.isDefault());
                ps.setInt(3, group.getTaxGroupId());
                ps.executeUpdate();

                // Limpiar items antiguos y asignar nuevos
                clearGroupRates(conn, group.getTaxGroupId());
                assignRatesToGroup(conn, group.getTaxGroupId(), group.getTaxRates());

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public void deleteTaxGroup(int taxGroupId) throws SQLException {
        String sql = "DELETE FROM tax_groups WHERE tax_group_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taxGroupId);
            ps.executeUpdate();
        }
    }

    @Override
    public void setDefaultTaxGroup(int taxGroupId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                clearOtherDefaults(conn);
                String sql = "UPDATE tax_groups SET is_default = TRUE WHERE tax_group_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, taxGroupId);
                    ps.executeUpdate();
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

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

    private void clearOtherDefaults(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE tax_groups SET is_default = FALSE")) {
            ps.executeUpdate();
        }
    }

    private void clearGroupRates(Connection conn, int taxGroupId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM tax_group_items WHERE tax_group_id = ?")) {
            ps.setInt(1, taxGroupId);
            ps.executeUpdate();
        }
    }

    private void assignRatesToGroup(Connection conn, int taxGroupId, List<TaxRate> rates) throws SQLException {
        if (rates == null || rates.isEmpty())
            return;
        String sql = "INSERT INTO tax_group_items (tax_group_id, tax_rate_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (TaxRate rate : rates) {
                ps.setInt(1, taxGroupId);
                ps.setInt(2, rate.getTaxRateId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private List<TaxRate> getTaxRatesForGroup(Connection conn, int taxGroupId) throws SQLException {
        String sql = "SELECT tr.* FROM tax_rates tr " +
                "JOIN tax_group_items tgi ON tr.tax_rate_id = tgi.tax_rate_id " +
                "WHERE tgi.tax_group_id = ?";
        List<TaxRate> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taxGroupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapTaxRate(rs));
                }
            }
        }
        return result;
    }

    @FunctionalInterface
    private interface PreparedStatementSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<TaxRate> queryTaxRates(String sql, PreparedStatementSetter setter) throws SQLException {
        List<TaxRate> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            if (setter != null) {
                setter.set(ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapTaxRate(rs));
                }
            }
        }
        return result;
    }

    private void bindTaxRate(PreparedStatement ps, TaxRate rate) throws SQLException {
        ps.setString(1, rate.getName());
        ps.setDouble(2, rate.getRate());
        ps.setString(3, rate.getCountry() != null ? rate.getCountry() : "España");

        if (rate.getRegion() != null) {
            ps.setString(4, rate.getRegion());
        } else {
            ps.setNull(4, Types.VARCHAR);
        }

        // valid_from by default is now if null
        LocalDateTime vf = rate.getValidFrom() != null ? rate.getValidFrom() : LocalDateTime.now();
        ps.setTimestamp(5, Timestamp.valueOf(vf));

        if (rate.getValidTo() != null) {
            ps.setTimestamp(6, Timestamp.valueOf(rate.getValidTo()));
        } else {
            ps.setNull(6, Types.TIMESTAMP);
        }

        ps.setBoolean(7, rate.isActive());
    }

    private TaxRate mapTaxRate(ResultSet rs) throws SQLException {
        TaxRate t = new TaxRate();
        t.setTaxRateId(rs.getInt("tax_rate_id"));
        t.setName(rs.getString("name"));
        t.setRate(rs.getDouble("rate"));
        t.setCountry(rs.getString("country"));
        t.setRegion(rs.getString("region"));
        t.setValidFrom(rs.getTimestamp("valid_from").toLocalDateTime());

        Timestamp vt = rs.getTimestamp("valid_to");
        if (vt != null) {
            t.setValidTo(vt.toLocalDateTime());
        }

        t.setActive(rs.getBoolean("active"));
        return t;
    }

    private TaxGroup mapTaxGroup(ResultSet rs) throws SQLException {
        TaxGroup g = new TaxGroup();
        g.setTaxGroupId(rs.getInt("tax_group_id"));
        g.setName(rs.getString("name"));
        g.setDefault(rs.getBoolean("is_default"));
        return g;
    }
}
