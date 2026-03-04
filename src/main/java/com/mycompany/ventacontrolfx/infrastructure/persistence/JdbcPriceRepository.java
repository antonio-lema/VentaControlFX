package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.Price;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.domain.repository.IPriceRepository;

import java.sql.*;
import java.time.LocalDateTime;
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
        return Optional.empty();
    }

    @Override
    public void save(Price price) throws SQLException {
        String sql = "INSERT INTO product_prices (product_id, price_list_id, price, start_date, end_date, reason, update_log_id) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
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
                // 1. Cerrar actual
                String sqlClose = "UPDATE product_prices SET end_date = ? " +
                        "WHERE product_id = ? AND price_list_id = ? AND end_date IS NULL";
                try (PreparedStatement ps = conn.prepareStatement(sqlClose)) {
                    ps.setTimestamp(1, Timestamp.valueOf(newPrice.getStartDate())); // Cerramos justo cuando empieza el
                                                                                    // nuevo
                    ps.setInt(2, newPrice.getProductId());
                    ps.setInt(3, newPrice.getPriceListId());
                    ps.executeUpdate();
                }

                // 2. Insertar nuevo
                String sqlInsert = "INSERT INTO product_prices (product_id, price_list_id, price, start_date, end_date, reason, update_log_id) "
                        +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
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
    public List<Price> findPriceHistory(int productId) throws SQLException {
        String sql = "SELECT * FROM product_prices WHERE product_id = ? ORDER BY start_date DESC";
        List<Price> history = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(mapPrice(rs));
                }
            }
        }
        return history;
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
}
