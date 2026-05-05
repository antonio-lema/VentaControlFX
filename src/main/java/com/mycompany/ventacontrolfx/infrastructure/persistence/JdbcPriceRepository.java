package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.dto.ProductPriceDTO;
import com.mycompany.ventacontrolfx.domain.model.Price;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.domain.repository.IPriceRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcPriceRepository implements IPriceRepository {

    @Override
    public Optional<Price> getActivePrice(int productId, int priceListId) throws SQLException {
        String sql = "SELECT * FROM product_prices " +
                "WHERE product_id = ? AND price_list_id = ? " +
                "AND (end_date IS NULL OR end_date > CURRENT_TIMESTAMP) " +
                "ORDER BY start_date DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, priceListId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPrice(rs));
                }
            }
        }

        String fallbackSql = "SELECT pp.* FROM product_prices pp " +
                "JOIN price_lists pl ON pp.price_list_id = pl.price_list_id " +
                "WHERE pp.product_id = ? AND pl.is_default = 1 " +
                "AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) " +
                "ORDER BY pp.start_date DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(fallbackSql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Price fallback = mapPrice(rs);
                    fallback.setPriceListId(priceListId);
                    return Optional.of(fallback);
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public void save(Price price) throws SQLException {
        String sql = "INSERT INTO product_prices (product_id, price_list_id, price, start_date, end_date, reason, update_log_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, price.getProductId());
            ps.setInt(2, price.getPriceListId());
            ps.setDouble(3, price.getValue());
            ps.setTimestamp(4, Timestamp.valueOf(price.getStartDate()));
            if (price.getEndDate() != null)
                ps.setTimestamp(5, Timestamp.valueOf(price.getEndDate()));
            else
                ps.setNull(5, Types.TIMESTAMP);
            ps.setString(6, price.getReason());
            if (price.getUpdateLogId() != null)
                ps.setInt(7, price.getUpdateLogId());
            else
                ps.setNull(7, Types.INTEGER);
            ps.executeUpdate();
        }
    }

    @Override
    public void closeCurrentPrice(int productId, int priceListId) throws SQLException {
        String sql = "UPDATE product_prices SET end_date = CURRENT_TIMESTAMP " +
                "WHERE product_id = ? AND price_list_id = ? AND end_date IS NULL";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, priceListId);
            ps.executeUpdate();
        }
    }

    @Override
    public void updateCurrentAndSave(Price newPrice) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sqlClose = "UPDATE product_prices SET end_date = ? " +
                        "WHERE product_id = ? AND price_list_id = ? AND end_date IS NULL";
                try (PreparedStatement ps = conn.prepareStatement(sqlClose)) {
                    ps.setTimestamp(1, Timestamp.valueOf(newPrice.getStartDate()));
                    ps.setInt(2, newPrice.getProductId());
                    ps.setInt(3, newPrice.getPriceListId());
                    ps.executeUpdate();
                }

                String sqlInsert = "INSERT INTO product_prices (product_id, price_list_id, price, start_date, end_date, reason, update_log_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
                    ps.setInt(1, newPrice.getProductId());
                    ps.setInt(2, newPrice.getPriceListId());
                    ps.setDouble(3, newPrice.getValue());
                    ps.setTimestamp(4, Timestamp.valueOf(newPrice.getStartDate()));
                    if (newPrice.getEndDate() != null)
                        ps.setTimestamp(5, Timestamp.valueOf(newPrice.getEndDate()));
                    else
                        ps.setNull(5, Types.TIMESTAMP);
                    ps.setString(6, newPrice.getReason());
                    if (newPrice.getUpdateLogId() != null)
                        ps.setInt(7, newPrice.getUpdateLogId());
                    else
                        ps.setNull(7, Types.INTEGER);
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

    @Override
    public List<PriceList> getAllPriceLists() throws SQLException {
        String sql = "SELECT * FROM price_lists ORDER BY name ASC";
        List<PriceList> lists = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                PriceList pl = new PriceList();
                pl.setId(rs.getInt("price_list_id"));
                pl.setName(rs.getString("name"));
                pl.setDefault(rs.getBoolean("is_default"));
                lists.add(pl);
            }
        }
        return lists;
    }

    @Override
    public PriceList getDefaultPriceList() throws SQLException {
        String sql = "SELECT * FROM price_lists WHERE is_default = TRUE LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                PriceList pl = new PriceList();
                pl.setId(rs.getInt("price_list_id"));
                pl.setName(rs.getString("name"));
                pl.setDefault(rs.getBoolean("is_default"));
                return pl;
            }
        }
        throw new SQLException("No default price list found in database");
    }

    @Override
    public List<ProductPriceDTO> findPricesByList(int priceListId) throws SQLException {
        List<ProductPriceDTO> results = new ArrayList<>();
        String sql = "SELECT p.product_id, p.name as product_name, c.name as category_name, "
                + "COALESCE(pp.price, pp_def.price, 0.0) as price, COALESCE(pp_def.price, 0.0) as default_price, "
                + "COALESCE(p.iva, c.default_iva, (SELECT config_value FROM system_config WHERE config_key = 'global_tax' LIMIT 1), 0.0) as effective_tax "
                + "FROM products p "
                + "LEFT JOIN categories c ON p.category_id = c.category_id "
                + "LEFT JOIN product_prices pp ON p.product_id = pp.product_id AND pp.price_list_id = ? "
                + "  AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) "
                + "LEFT JOIN product_prices pp_def ON p.product_id = pp_def.product_id "
                + "  AND pp_def.price_list_id = (SELECT price_list_id FROM price_lists WHERE is_default = 1 LIMIT 1) "
                + "  AND pp_def.start_date <= CURRENT_TIMESTAMP AND (pp_def.end_date IS NULL OR pp_def.end_date > CURRENT_TIMESTAMP) "
                + "WHERE p.visible = 1 "
                + "ORDER BY c.name, p.name";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, priceListId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new ProductPriceDTO(
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getString("category_name"),
                            rs.getDouble("price"),
                            rs.getDouble("default_price"),
                            rs.getDouble("effective_tax")));
                }
            }
        }
        return results;
    }

    @Override
    public List<ProductPriceDTO> findPricesByListPaginated(int priceListId, String search, java.time.LocalDateTime startDate, Integer logId, int limit, int offset) throws SQLException {
        List<ProductPriceDTO> results = new ArrayList<>();
        String searchPattern = (search == null || search.trim().isEmpty()) ? null : "%" + search.trim().toLowerCase() + "%";

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.product_id, p.name as product_name, c.name as category_name, ");
        sql.append("COALESCE(pp.price, pp_def.price, 0.0) as price, ");
        sql.append("COALESCE(pp_now.price, pp.price, pp_def.price, 0.0) as current_price, ");
        sql.append("COALESCE(pp_def.price, 0.0) as default_price, ");
        sql.append("COALESCE(p.iva, c.default_iva, (SELECT config_value FROM system_config WHERE config_key = 'global_tax' LIMIT 1), 0.0) as effective_tax ");
        sql.append("FROM products p ");
        sql.append("LEFT JOIN categories c ON p.category_id = c.category_id ");
        sql.append("LEFT JOIN product_prices pp ON p.product_id = pp.product_id AND pp.price_list_id = ? ");
        if (startDate == null) {
            sql.append("  AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) ");
        } else if (logId != null && logId > 0) {
            // Hybrid lookup: Prioritize logId, fallback to timestamp if update_log_id is NULL (legacy)
            sql.append("  AND (pp.update_log_id = ? OR (pp.update_log_id IS NULL AND pp.start_date >= DATE_SUB(?, INTERVAL 2 SECOND) AND pp.start_date <= DATE_ADD(?, INTERVAL 2 SECOND))) ");
        } else {
            sql.append("  AND pp.start_date >= DATE_SUB(?, INTERVAL 5 SECOND) ");
            sql.append("  AND pp.start_date <= DATE_ADD(?, INTERVAL 5 SECOND) ");
        }
        sql.append("LEFT JOIN product_prices pp_now ON p.product_id = pp_now.product_id AND pp_now.price_list_id = ? ");
        sql.append("  AND pp_now.start_date <= CURRENT_TIMESTAMP AND (pp_now.end_date IS NULL OR pp_now.end_date > CURRENT_TIMESTAMP) ");
        sql.append("LEFT JOIN product_prices pp_def ON p.product_id = pp_def.product_id ");
        sql.append("  AND pp_def.price_list_id = (SELECT price_list_id FROM price_lists WHERE is_default = 1 LIMIT 1) ");
        sql.append("  AND pp_def.start_date <= CURRENT_TIMESTAMP AND (pp_def.end_date IS NULL OR pp_def.end_date > CURRENT_TIMESTAMP) ");
        sql.append("WHERE p.visible = 1 ");
        if (startDate != null) {
            sql.append("AND pp.price_id IS NOT NULL ");
        }

        if (searchPattern != null) {
            sql.append("AND (LOWER(p.name) LIKE ? OR LOWER(c.name) LIKE ? OR CAST(p.product_id AS CHAR) LIKE ?) ");
        }

        sql.append("ORDER BY c.name, p.name LIMIT ? OFFSET ?");

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIdx = 1;
            ps.setInt(paramIdx++, priceListId);
            if (startDate != null) {
                if (logId != null && logId > 0) {
                    ps.setInt(paramIdx++, logId);
                    Timestamp ts = Timestamp.valueOf(startDate);
                    ps.setTimestamp(paramIdx++, ts);
                    ps.setTimestamp(paramIdx++, ts);
                } else {
                    Timestamp ts = Timestamp.valueOf(startDate);
                    ps.setTimestamp(paramIdx++, ts);
                    ps.setTimestamp(paramIdx++, ts);
                }
            }
            ps.setInt(paramIdx++, priceListId);
            if (searchPattern != null) {
                ps.setString(paramIdx++, searchPattern);
                ps.setString(paramIdx++, searchPattern);
                ps.setString(paramIdx++, searchPattern);
            }
            ps.setInt(paramIdx++, limit);
            ps.setInt(paramIdx++, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProductPriceDTO dto = new ProductPriceDTO(
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getString("category_name"),
                            rs.getDouble("price"),
                            rs.getDouble("default_price"),
                            rs.getDouble("effective_tax"));
                    dto.setCurrentPrice(rs.getDouble("current_price"));
                    results.add(dto);
                }
            }
        }
        return results;
    }

    @Override
    public int countPricesByList(int priceListId, String search, java.time.LocalDateTime startDate, Integer logId) throws SQLException {
        String searchPattern = (search == null || search.trim().isEmpty()) ? null : "%" + search.trim().toLowerCase() + "%";

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM products p ");
        sql.append("LEFT JOIN categories c ON p.category_id = c.category_id ");
        if (startDate != null) {
            sql.append("JOIN product_prices pp ON p.product_id = pp.product_id AND pp.price_list_id = ? ");
            if (logId != null && logId > 0) {
                sql.append("AND (pp.update_log_id = ? OR (pp.update_log_id IS NULL AND pp.start_date >= DATE_SUB(?, INTERVAL 2 SECOND) AND pp.start_date <= DATE_ADD(?, INTERVAL 2 SECOND))) ");
            } else {
                sql.append("AND pp.start_date >= DATE_SUB(?, INTERVAL 5 SECOND) ");
                sql.append("AND pp.start_date <= DATE_ADD(?, INTERVAL 5 SECOND) ");
            }
        }
        sql.append("WHERE p.visible = 1 ");

        if (searchPattern != null) {
            sql.append("AND (LOWER(p.name) LIKE ? OR LOWER(c.name) LIKE ? OR CAST(p.product_id AS CHAR) LIKE ?) ");
        }

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIdx = 1;
            if (startDate != null) {
                ps.setInt(paramIdx++, priceListId);
                if (logId != null && logId > 0) {
                    ps.setInt(paramIdx++, logId);
                    Timestamp ts = Timestamp.valueOf(startDate);
                    ps.setTimestamp(paramIdx++, ts);
                    ps.setTimestamp(paramIdx++, ts);
                } else {
                    Timestamp ts = Timestamp.valueOf(startDate);
                    ps.setTimestamp(paramIdx++, ts);
                    ps.setTimestamp(paramIdx++, ts);
                }
            }
            if (searchPattern != null) {
                ps.setString(paramIdx++, searchPattern);
                ps.setString(paramIdx++, searchPattern);
                ps.setString(paramIdx++, searchPattern);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private Price mapPrice(ResultSet rs) throws SQLException {
        Price p = new Price();
        p.setId(rs.getInt("price_id"));
        p.setProductId(rs.getInt("product_id"));
        p.setPriceListId(rs.getInt("price_list_id"));
        p.setValue(rs.getDouble("price"));
        p.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
        Timestamp endTs = rs.getTimestamp("end_date");
        if (endTs != null)
            p.setEndDate(endTs.toLocalDateTime());
        p.setReason(rs.getString("reason"));
        int logId = rs.getInt("update_log_id");
        if (!rs.wasNull())
            p.setUpdateLogId(logId);
        return p;
    }

    @Override
    public void updateLogIdForPricesAtTimestamp(int priceListId, java.time.LocalDateTime timestamp, int logId) throws SQLException {
        String sql = "UPDATE product_prices SET update_log_id = ? " +
                "WHERE price_list_id = ? " +
                "AND start_date >= DATE_SUB(?, INTERVAL 5 SECOND) " +
                "AND start_date <= DATE_ADD(?, INTERVAL 5 SECOND)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, logId);
            ps.setInt(2, priceListId);
            Timestamp ts = Timestamp.valueOf(timestamp);
            ps.setTimestamp(3, ts);
            ps.setTimestamp(4, ts);
            ps.executeUpdate();
        }
    }
}
