package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.dto.PriceUpdateLogDTO;
import com.mycompany.ventacontrolfx.domain.dto.ProductPriceDTO;
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

        // Fallback: si no hay precio en la tarifa solicitada, obtenemos la de la tarifa
        // por defecto
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
                    // Ajustamos el price_list_id para que coincida con la tarifa buscada y asÃ­ se
                    // muestre
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

    @Override
    public int applyBulkMultiplier(int priceListId, Integer categoryId, double multiplier, String reason,
            java.time.LocalDateTime startDate)
            throws SQLException {

        StringBuilder selectSql = new StringBuilder(
                "SELECT pp.product_id, pp.price FROM product_prices pp " +
                        "JOIN products p ON p.product_id = pp.product_id " +
                        "WHERE pp.price_list_id = ? AND pp.end_date IS NULL AND p.visible = 1 ");

        if (categoryId != null && categoryId > 0) {
            selectSql.append("AND p.category_id = ? ");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePricesExist(priceListId, categoryId, conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql.toString())) {
                    ps.setInt(1, priceListId);
                    if (categoryId != null && categoryId > 0)
                        ps.setInt(2, categoryId);

                    int count = executeBulkUpdate(conn, ps, priceListId, multiplier, reason, startDate, true);
                    conn.commit();
                    return count;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public int applyBulkFixedAmount(int priceListId, Integer categoryId, double amount, String reason,
            java.time.LocalDateTime startDate)
            throws SQLException {

        StringBuilder selectSql = new StringBuilder(
                "SELECT pp.product_id, pp.price FROM product_prices pp " +
                        "JOIN products p ON p.product_id = pp.product_id " +
                        "WHERE pp.price_list_id = ? AND pp.end_date IS NULL AND p.visible = 1 ");

        if (categoryId != null && categoryId > 0) {
            selectSql.append("AND p.category_id = ? ");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePricesExist(priceListId, categoryId, conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql.toString())) {
                    ps.setInt(1, priceListId);
                    if (categoryId != null && categoryId > 0)
                        ps.setInt(2, categoryId);

                    int count = executeBulkUpdate(conn, ps, priceListId, amount, reason, startDate, false);
                    conn.commit();
                    return count;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private int executeBulkRounding(Connection conn, PreparedStatement selectStmt, int priceListId,
            double roundingTarget, String reason, java.time.LocalDateTime startDate, double globalTax)
            throws SQLException {
        boolean pricesIncludeTax = getPricesIncludeTax(conn);
        int updatedCount = 0;
        String closeSql = "UPDATE product_prices SET end_date = ? " +
                "WHERE product_id = ? AND price_list_id = ? AND end_date IS NULL";
        String insertSql = "INSERT INTO product_prices (product_id, price_list_id, price, start_date, end_date, reason) "
                +
                "VALUES (?, ?, ?, ?, NULL, ?)";

        try (ResultSet rs = selectStmt.executeQuery();
                PreparedStatement closeStmt = conn.prepareStatement(closeSql);
                PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            while (rs.next()) {
                int productId = rs.getInt("product_id");
                double oldBasePrice = rs.getDouble("price");

                double pIva = rs.getDouble("p_iva");
                boolean pIvaIsNull = rs.wasNull();
                double cIva = rs.getDouble("c_iva");
                boolean cIvaIsNull = rs.wasNull();

                double effectiveTaxRate = pIvaIsNull ? (cIvaIsNull ? globalTax : cIva) : pIva;
                double taxMultiplier = pricesIncludeTax ? 1.0 : (1.0 + (effectiveTaxRate / 100.0));

                double currentPvp = oldBasePrice * taxMultiplier;
                double roundedPvp = Math.floor(currentPvp) + roundingTarget;

                double realTaxMultiplier = 1.0 + (effectiveTaxRate / 100.0);
                double newBasePrice = pricesIncludeTax ? roundedPvp
                        : (Math.round((roundedPvp / realTaxMultiplier) * 10000.0) / 10000.0);

                if (newBasePrice < 0)
                    newBasePrice = 0.0;

                if (Math.abs(newBasePrice - oldBasePrice) > 0.0001) {
                    closeStmt.setTimestamp(1, Timestamp.valueOf(startDate));
                    closeStmt.setInt(2, productId);
                    closeStmt.setInt(3, priceListId);
                    closeStmt.addBatch();

                    insertStmt.setInt(1, productId);
                    insertStmt.setInt(2, priceListId);
                    insertStmt.setDouble(3, newBasePrice);
                    insertStmt.setTimestamp(4, Timestamp.valueOf(startDate));
                    insertStmt.setString(5, reason);
                    insertStmt.addBatch();
                    updatedCount++;
                }
            }

            if (updatedCount > 0) {
                closeStmt.executeBatch();
                insertStmt.executeBatch();
            }
        }
        return updatedCount;
    }

    @Override
    public int applyBulkRounding(int priceListId, Integer categoryId, double roundingTarget, String reason,
            java.time.LocalDateTime startDate)
            throws SQLException {

        try (Connection conn = DBConnection.getConnection()) {
            double globalTax = getGlobalTax(conn);
            StringBuilder selectSql = new StringBuilder(
                    "SELECT pp.product_id, pp.price, p.iva as p_iva, c.default_iva as c_iva " +
                            "FROM product_prices pp " +
                            "JOIN products p ON p.product_id = pp.product_id " +
                            "LEFT JOIN categories c ON p.category_id = c.category_id " +
                            "WHERE pp.price_list_id = ? AND pp.end_date IS NULL AND p.visible = 1 ");

            if (categoryId != null && categoryId > 0) {
                selectSql.append("AND p.category_id = ? ");
            }

            conn.setAutoCommit(false);
            try {
                ensurePricesExist(priceListId, categoryId, conn);
                int count;
                try (PreparedStatement ps = conn.prepareStatement(selectSql.toString())) {
                    ps.setInt(1, priceListId);
                    if (categoryId != null && categoryId > 0)
                        ps.setInt(2, categoryId);
                    count = executeBulkRounding(conn, ps, priceListId, roundingTarget, reason, startDate, globalTax);
                }
                conn.commit();
                return count;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private double getGlobalTax(Connection conn) throws SQLException {
        String sql = "SELECT config_value FROM system_config WHERE config_key = ? OR config_key = 'taxRate' LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "global_tax");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Double.parseDouble(rs.getString(1));
                }
            }
        } catch (Exception e) {
            return 21.0;
        }
        return 21.0;
    }

    private boolean getPricesIncludeTax(Connection conn) throws SQLException {
        String sql = "SELECT config_value FROM system_config WHERE config_key = 'pricesIncludeTax' LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Boolean.parseBoolean(rs.getString(1));
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    // â”€â”€â”€ HELPER PARA AUTORRELLENAR TARIFAS VACÃAS â”€â”€â”€
    private void ensurePricesExist(int targetPriceListId, Integer categoryId, Connection conn) throws SQLException {
        int defaultPriceListId = -1;
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt
                        .executeQuery("SELECT price_list_id FROM price_lists WHERE is_default = 1 LIMIT 1")) {
            if (rs.next()) {
                defaultPriceListId = rs.getInt(1);
            }
        }
        if (defaultPriceListId == -1 || defaultPriceListId == targetPriceListId) {
            return;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO product_prices (product_id, price_list_id, price, start_date, reason) ");
        sql.append(
                "SELECT p.product_id, ?, COALESCE(pp_def.price, 0), NOW(), 'Copia implÃ­cita de tarifa base por Subida Masiva' ");
        sql.append("FROM products p ");
        sql.append("LEFT JOIN product_prices pp_target ON p.product_id = pp_target.product_id ");
        sql.append("  AND pp_target.price_list_id = ? AND pp_target.end_date IS NULL ");
        sql.append("LEFT JOIN product_prices pp_def ON p.product_id = pp_def.product_id ");
        sql.append("  AND pp_def.price_list_id = ? AND pp_def.end_date IS NULL ");
        sql.append("WHERE p.visible = 1 AND pp_target.price_id IS NULL ");

        if (categoryId != null && categoryId > 0) {
            sql.append("AND p.category_id = ?");
        }

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, targetPriceListId);
            ps.setInt(2, targetPriceListId);
            ps.setInt(3, defaultPriceListId);
            if (categoryId != null && categoryId > 0) {
                ps.setInt(4, categoryId);
            }
            ps.executeUpdate();
        }
    }

    // â”€â”€â”€ ImplementaciÃ³n de helper privado reutilizado â”€â”€â”€

    /**
     * LÃ³gica de cierre+inserciÃ³n en batch a partir de un ResultSet ya abierto.
     * Soporta operaciÃ³n porcentual o de importe fijo.
     */
    private int executeBulkUpdate(Connection conn, PreparedStatement selectStmt,
            int priceListId, double value, String reason,
            java.time.LocalDateTime startDate, boolean isPercentage) throws SQLException {
        int count = 0;
        String closeSql = "UPDATE product_prices SET end_date = ? WHERE product_id = ? AND price_list_id = ? AND end_date IS NULL";
        String insertSql = "INSERT INTO product_prices (product_id, price_list_id, price, start_date, end_date, reason) VALUES (?, ?, ?, ?, NULL, ?)";

        try (ResultSet rs = selectStmt.executeQuery();
                PreparedStatement closeStmt = conn.prepareStatement(closeSql);
                PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            while (rs.next()) {
                int productId = rs.getInt("product_id");
                double currentPrice = rs.getDouble("price");
                double newPrice;

                if (isPercentage) {
                    newPrice = Math.round(currentPrice * value * 100.0) / 100.0;
                } else {
                    newPrice = Math.round((currentPrice + value) * 100.0) / 100.0;
                }

                if (newPrice < 0)
                    newPrice = 0.0;

                // Solo si el precio cambia realmente
                if (Math.abs(newPrice - currentPrice) > 0.0001) {
                    closeStmt.setTimestamp(1, Timestamp.valueOf(startDate));
                    closeStmt.setInt(2, productId);
                    closeStmt.setInt(3, priceListId);
                    closeStmt.addBatch();

                    insertStmt.setInt(1, productId);
                    insertStmt.setInt(2, priceListId);
                    insertStmt.setDouble(3, newPrice);
                    insertStmt.setTimestamp(4, Timestamp.valueOf(startDate));
                    insertStmt.setString(5, reason);
                    insertStmt.addBatch();
                    count++;
                }
            }

            if (count > 0) {
                closeStmt.executeBatch();
                insertStmt.executeBatch();
            }
        }
        return count;
    }

    // â”€â”€â”€ TOP SELLERS â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    public int applyBulkMultiplierToTopSellers(int priceListId, int topN, int daysBack,
            double value, String reason, boolean isPercentage, java.time.LocalDateTime startDate) throws SQLException {

        String selectSql = "SELECT pp.product_id, pp.price " +
                "FROM product_prices pp " +
                "JOIN products p ON p.product_id = pp.product_id " +
                "JOIN ( " +
                "  SELECT * FROM ( " +
                "    SELECT sd.product_id " +
                "    FROM sale_details sd " +
                "    JOIN sales s ON s.sale_id = sd.sale_id " +
                "    WHERE s.sale_datetime >= DATE_SUB(NOW(), INTERVAL ? DAY) AND s.is_return = FALSE " +
                "    GROUP BY sd.product_id " +
                "    ORDER BY SUM(sd.quantity) DESC " +
                "    LIMIT ? " +
                "  ) AS top_inner " +
                ") AS top_ranked ON top_ranked.product_id = pp.product_id " +
                "WHERE pp.price_list_id = ? AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) AND p.visible = 1";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePricesExist(priceListId, null, conn);
                int count;
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, daysBack);
                    ps.setInt(2, topN);
                    ps.setInt(3, priceListId);
                    count = executeBulkUpdate(conn, ps, priceListId, value, reason, startDate, isPercentage);
                    conn.commit();
                    return count;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // â”€â”€â”€ BOTTOM SELLERS (Menos Vendidos) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    public int applyBulkMultiplierToBottomSellers(int priceListId, int bottomN, int daysBack,
            double value, String reason, boolean isPercentage, java.time.LocalDateTime startDate) throws SQLException {

        String selectSql = "SELECT pp.product_id, pp.price " +
                "FROM product_prices pp " +
                "JOIN products p ON p.product_id = pp.product_id " +
                "JOIN ( " +
                "  SELECT * FROM ( " +
                "    SELECT p.product_id " +
                "    FROM products p " +
                "    LEFT JOIN sale_details sd ON sd.product_id = p.product_id " +
                "    LEFT JOIN sales s ON s.sale_id = sd.sale_id " +
                "           AND s.sale_datetime >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                "           AND s.is_return = FALSE " +
                "    WHERE p.visible = 1 " +
                "    GROUP BY p.product_id " +
                "    ORDER BY COALESCE(SUM(sd.quantity), 0) ASC " +
                "    LIMIT ? " +
                "  ) AS bottom_inner " +
                ") AS bottom_ranked ON bottom_ranked.product_id = pp.product_id " +
                "WHERE pp.price_list_id = ? AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) AND p.visible = 1";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePricesExist(priceListId, null, conn);
                int count;
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, daysBack);
                    ps.setInt(2, bottomN);
                    ps.setInt(3, priceListId);
                    count = executeBulkUpdate(conn, ps, priceListId, value, reason, startDate, isPercentage);
                    conn.commit();
                    return count;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // â”€â”€â”€ SLOW MOVERS â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    public int applyBulkMultiplierToSlowMovers(int priceListId, int daysWithoutSale,
            double value, String reason, boolean isPercentage, java.time.LocalDateTime startDate) throws SQLException {

        String selectSql = "SELECT pp.product_id, pp.price " +
                "FROM product_prices pp " +
                "JOIN products p ON p.product_id = pp.product_id " +
                "WHERE pp.price_list_id = ? AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) AND p.visible = 1 "
                +
                "AND NOT EXISTS ( " +
                "  SELECT 1 FROM sale_details sd " +
                "  JOIN sales s ON s.sale_id = sd.sale_id " +
                "  WHERE sd.product_id = pp.product_id " +
                "    AND s.sale_datetime >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                "    AND s.is_return = FALSE " +
                ")";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePricesExist(priceListId, null, conn);
                int count;
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, priceListId);
                    ps.setInt(2, daysWithoutSale);
                    count = executeBulkUpdate(conn, ps, priceListId, value, reason, startDate, isPercentage);
                    conn.commit();
                    return count;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // â”€â”€â”€ RANGO DE PRECIO â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    public int applyBulkMultiplierToPriceRange(int priceListId, double minPrice, double maxPrice,
            double value, String reason, boolean isPercentage, java.time.LocalDateTime startDate) throws SQLException {

        String selectSql = "SELECT pp.product_id, pp.price " +
                "FROM product_prices pp " +
                "JOIN products p ON p.product_id = pp.product_id " +
                "WHERE pp.price_list_id = ? AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) AND p.visible = 1 "
                +
                "AND pp.price BETWEEN ? AND ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePricesExist(priceListId, null, conn);
                int count;
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, priceListId);
                    ps.setDouble(2, minPrice);
                    ps.setDouble(3, maxPrice);
                    count = executeBulkUpdate(conn, ps, priceListId, value, reason, startDate, isPercentage);
                    conn.commit();
                    return count;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // â”€â”€â”€ FAVORITOS â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    public int applyBulkMultiplierToFavorites(int priceListId, double value, String reason, boolean isPercentage,
            java.time.LocalDateTime startDate)
            throws SQLException {

        String selectSql = "SELECT pp.product_id, pp.price " +
                "FROM product_prices pp " +
                "JOIN products p ON p.product_id = pp.product_id " +
                "WHERE pp.price_list_id = ? AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) AND p.visible = 1 "
                +
                "AND p.is_favorite = 1";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePricesExist(priceListId, null, conn);
                int count;
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, priceListId);
                    count = executeBulkUpdate(conn, ps, priceListId, value, reason, startDate, isPercentage);
                    conn.commit();
                    return count;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public void clonePriceList(int sourceId, int targetId) throws SQLException {
        cloneAndAdjustPriceList(sourceId, targetId, 1.0, "ClonaciÃ³n desde tarifa ID " + sourceId,
                LocalDateTime.now());
    }

    @Override
    public void cloneAndAdjustPriceList(int sourceId, int targetId, double multiplier, String reason,
            LocalDateTime startDate)
            throws SQLException {
        // Primero cerramos los precios que pudieran existir ya en la tarifa destino
        // para
        // evitar duplicados activos
        String closeSql = "UPDATE product_prices SET end_date = CURRENT_TIMESTAMP " +
                "WHERE price_list_id = ? AND end_date IS NULL";

        String insertSql = "INSERT INTO product_prices (product_id, price_list_id, price, start_date, end_date, reason) "
                +
                "SELECT product_id, ?, price * ?, ?, NULL, ? " +
                "FROM product_prices " +
                "WHERE price_list_id = ? AND start_date <= CURRENT_TIMESTAMP AND (end_date IS NULL OR end_date > CURRENT_TIMESTAMP)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psClose = conn.prepareStatement(closeSql);
                    PreparedStatement psInsert = conn.prepareStatement(insertSql)) {

                psClose.setInt(1, targetId);
                psClose.executeUpdate();

                psInsert.setInt(1, targetId);
                psInsert.setDouble(2, multiplier);
                psInsert.setTimestamp(3, Timestamp.valueOf(startDate));
                psInsert.setString(4, reason);
                psInsert.setInt(5, sourceId);
                psInsert.executeUpdate();

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
    public List<com.mycompany.ventacontrolfx.domain.dto.ProductPriceDTO> findPricesByList(int priceListId)
            throws SQLException {
        List<com.mycompany.ventacontrolfx.domain.dto.ProductPriceDTO> results = new ArrayList<>();
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
                    results.add(new com.mycompany.ventacontrolfx.domain.dto.ProductPriceDTO(
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
    public String getAveragePercentageDifference(int priceListId) throws SQLException {
        String sql = "SELECT SUM(pp.price) as current_total, SUM(pp_def.price) as default_total " +
                "FROM product_prices pp " +
                "JOIN product_prices pp_def ON pp.product_id = pp_def.product_id " +
                "  AND pp_def.price_list_id = (SELECT price_list_id FROM price_lists WHERE is_default = 1 LIMIT 1) " +
                "  AND pp_def.start_date <= CURRENT_TIMESTAMP AND (pp_def.end_date IS NULL OR pp_def.end_date > CURRENT_TIMESTAMP) "
                +
                "WHERE pp.price_list_id = ? " +
                "  AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP)";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, priceListId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double currTotal = rs.getDouble("current_total");
                    double defTotal = rs.getDouble("default_total");
                    if (defTotal > 0) {
                        double diff = ((currTotal - defTotal) / defTotal) * 100.0;
                        if (Math.abs(diff) < 0.01)
                            return "Media igual (0%)";
                        if (diff > 0)
                            return String.format("+%.1f%% media", diff);
                        return String.format("%.1f%% media", diff);
                    }
                }
            }
        }
        return "Sin precios";
    }

    @Override
    public int applyBulkRoundingToTopSellers(int priceListId, int topN, int daysBack, double roundingTarget,
            String reason, java.time.LocalDateTime startDate) throws SQLException {

        String selectSql = "SELECT pp.product_id, pp.price, p.iva as p_iva, c.default_iva as c_iva " +
                "FROM product_prices pp " +
                "JOIN products p ON p.product_id = pp.product_id " +
                "LEFT JOIN categories c ON p.category_id = c.category_id " +
                "JOIN ( " +
                "  SELECT * FROM ( " +
                "    SELECT sd.product_id " +
                "    FROM sale_details sd " +
                "    JOIN sales s ON s.sale_id = sd.sale_id " +
                "    WHERE s.sale_datetime >= DATE_SUB(NOW(), INTERVAL ? DAY) AND s.is_return = FALSE " +
                "    GROUP BY sd.product_id " +
                "    ORDER BY SUM(sd.quantity) DESC " +
                "    LIMIT ? " +
                "  ) AS top_inner " +
                ") AS top_ranked ON top_ranked.product_id = pp.product_id " +
                "WHERE pp.price_list_id = ? AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) AND p.visible = 1";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePricesExist(priceListId, null, conn);
                double globalTax = getGlobalTax(conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, daysBack);
                    ps.setInt(2, topN);
                    ps.setInt(3, priceListId);
                    int count = executeBulkRounding(conn, ps, priceListId, roundingTarget, reason, startDate,
                            globalTax);
                    conn.commit();
                    return count;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public int applyBulkRoundingToBottomSellers(int priceListId, int bottomN, int daysBack, double roundingTarget,
            String reason, java.time.LocalDateTime startDate) throws SQLException {

        String selectSql = "SELECT pp.product_id, pp.price, p.iva as p_iva, c.default_iva as c_iva " +
                "FROM product_prices pp " +
                "JOIN products p ON p.product_id = pp.product_id " +
                "LEFT JOIN categories c ON p.category_id = c.category_id " +
                "JOIN ( " +
                "  SELECT * FROM ( " +
                "    SELECT p.product_id " +
                "    FROM products p " +
                "    LEFT JOIN sale_details sd ON sd.product_id = p.product_id " +
                "    LEFT JOIN sales s ON s.sale_id = sd.sale_id " +
                "           AND s.sale_datetime >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                "           AND s.is_return = FALSE " +
                "    WHERE p.visible = 1 " +
                "    GROUP BY p.product_id " +
                "    ORDER BY COALESCE(SUM(sd.quantity), 0) ASC " +
                "    LIMIT ? " +
                "  ) AS bottom_inner " +
                ") AS bottom_ranked ON bottom_ranked.product_id = pp.product_id " +
                "WHERE pp.price_list_id = ? AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) AND p.visible = 1";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePricesExist(priceListId, null, conn);
                double globalTax = getGlobalTax(conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, daysBack);
                    ps.setInt(2, bottomN);
                    ps.setInt(3, priceListId);
                    int count = executeBulkRounding(conn, ps, priceListId, roundingTarget, reason, startDate,
                            globalTax);
                    conn.commit();
                    return count;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public int applyBulkRoundingToSlowMovers(int priceListId, int daysWithoutSale, double roundingTarget, String reason,
            java.time.LocalDateTime startDate)
            throws SQLException {

        String selectSql = "SELECT pp.product_id, pp.price, p.iva as p_iva, c.default_iva as c_iva " +
                "FROM product_prices pp " +
                "JOIN products p ON p.product_id = pp.product_id " +
                "LEFT JOIN categories c ON p.category_id = c.category_id " +
                "WHERE pp.price_list_id = ? AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) AND p.visible = 1 "
                +
                "AND NOT EXISTS ( " +
                "  SELECT 1 FROM sale_details sd " +
                "  JOIN sales s ON s.sale_id = sd.sale_id " +
                "  WHERE sd.product_id = pp.product_id " +
                "    AND s.sale_datetime >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                "    AND s.is_return = FALSE " +
                ")";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePricesExist(priceListId, null, conn);
                double globalTax = getGlobalTax(conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, priceListId);
                    ps.setInt(2, daysWithoutSale);
                    int count = executeBulkRounding(conn, ps, priceListId, roundingTarget, reason, startDate,
                            globalTax);
                    conn.commit();
                    return count;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public int applyBulkRoundingToPriceRange(int priceListId, double minPrice, double maxPrice, double roundingTarget,
            String reason, java.time.LocalDateTime startDate) throws SQLException {

        String selectSql = "SELECT pp.product_id, pp.price, p.iva as p_iva, c.default_iva as c_iva " +
                "FROM product_prices pp " +
                "JOIN products p ON p.product_id = pp.product_id " +
                "LEFT JOIN categories c ON p.category_id = c.category_id " +
                "WHERE pp.price_list_id = ? AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) AND p.visible = 1 "
                +
                "AND pp.price BETWEEN ? AND ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePricesExist(priceListId, null, conn);
                double globalTax = getGlobalTax(conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, priceListId);
                    ps.setDouble(2, minPrice);
                    ps.setDouble(3, maxPrice);
                    int count = executeBulkRounding(conn, ps, priceListId, roundingTarget, reason, startDate,
                            globalTax);
                    conn.commit();
                    return count;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public int applyBulkRoundingToFavorites(int priceListId, double roundingTarget, String reason,
            java.time.LocalDateTime startDate) throws SQLException {

        String selectSql = "SELECT pp.product_id, pp.price, p.iva as p_iva, c.default_iva as c_iva " +
                "FROM product_prices pp " +
                "JOIN products p ON p.product_id = pp.product_id " +
                "LEFT JOIN categories c ON p.category_id = c.category_id " +
                "WHERE pp.price_list_id = ? AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) AND p.visible = 1 "
                +
                "AND p.is_favorite = 1";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePricesExist(priceListId, null, conn);
                double globalTax = getGlobalTax(conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, priceListId);
                    int count = executeBulkRounding(conn, ps, priceListId, roundingTarget, reason, startDate,
                            globalTax);
                    conn.commit();
                    return count;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public int applyBulkMultiplierToProducts(int priceListId, java.util.List<Integer> productIds, double multiplier,
            String reason, java.time.LocalDateTime startDate) throws SQLException {
        if (productIds == null || productIds.isEmpty())
            return 0;
        String ids = productIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        String selectSql = "SELECT pp.product_id, pp.price FROM product_prices pp " +
                "JOIN products p ON p.product_id = pp.product_id " +
                "WHERE pp.price_list_id = ? AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) AND p.visible = 1 "
                +
                "AND p.product_id IN (" + ids + ")";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePricesExistForProducts(priceListId, productIds, conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, priceListId);
                    int count = executeBulkUpdate(conn, ps, priceListId, multiplier, reason, startDate, true);
                    conn.commit();
                    return count;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public int applyBulkFixedAmountToProducts(int priceListId, java.util.List<Integer> productIds, double amount,
            String reason, java.time.LocalDateTime startDate) throws SQLException {
        if (productIds == null || productIds.isEmpty())
            return 0;
        String ids = productIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        String selectSql = "SELECT pp.product_id, pp.price FROM product_prices pp " +
                "JOIN products p ON p.product_id = pp.product_id " +
                "WHERE pp.price_list_id = ? AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) AND p.visible = 1 "
                +
                "AND p.product_id IN (" + ids + ")";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePricesExistForProducts(priceListId, productIds, conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, priceListId);
                    int count = executeBulkUpdate(conn, ps, priceListId, amount, reason, startDate, false);
                    conn.commit();
                    return count;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public int applyBulkRoundingToProducts(int priceListId, java.util.List<Integer> productIds, double targetDecimal,
            String reason, java.time.LocalDateTime startDate) throws SQLException {
        if (productIds == null || productIds.isEmpty())
            return 0;
        String ids = productIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        String selectSql = "SELECT pp.product_id, pp.price, p.iva as p_iva, c.default_iva as c_iva " +
                "FROM product_prices pp " +
                "JOIN products p ON p.product_id = pp.product_id " +
                "LEFT JOIN categories c ON p.category_id = c.category_id " +
                "WHERE pp.price_list_id = ? AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) AND p.visible = 1 "
                +
                "AND p.product_id IN (" + ids + ")";

        try (Connection conn = DBConnection.getConnection()) {
            double globalTax = getGlobalTax(conn);
            conn.setAutoCommit(false);
            try {
                ensurePricesExistForProducts(priceListId, productIds, conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, priceListId);
                    int count = executeBulkRounding(conn, ps, priceListId, targetDecimal, reason, startDate, globalTax);
                    conn.commit();
                    return count;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void ensurePricesExistForProducts(int priceListId, java.util.List<Integer> productIds, Connection conn)
            throws SQLException {
        // Enforce that prices rows exist just like ensurePricesExist for general ones
        String sql = "INSERT IGNORE INTO product_prices (product_id, price_list_id, price, start_date, reason) " +
                "SELECT p.product_id, ?, p.base_price, CURRENT_TIMESTAMP, 'Inicial masivo' " +
                "FROM products p WHERE p.product_id IN (" +
                productIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + ") " +
                "AND NOT EXISTS (SELECT 1 FROM product_prices pp WHERE pp.product_id = p.product_id AND pp.price_list_id = ? "
                +
                "AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, priceListId);
            ps.setInt(2, priceListId);
            ps.executeUpdate();
        }
    }

    @Override
    public java.util.List<com.mycompany.ventacontrolfx.domain.dto.ProductPriceDTO> findPricesByListPaginated(
            int priceListId,
            String search, java.time.LocalDateTime startDate, int limit, int offset) throws SQLException {
        java.util.List<com.mycompany.ventacontrolfx.domain.dto.ProductPriceDTO> results = new java.util.ArrayList<>();
        String searchPattern = (search == null || search.trim().isEmpty()) ? null
                : "%" + search.trim().toLowerCase() + "%";

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.product_id, p.name as product_name, c.name as category_name, ");
        sql.append("COALESCE(pp.price, pp_def.price, 0.0) as price, ");
        sql.append("COALESCE(pp_now.price, pp.price, pp_def.price, 0.0) as current_price, ");
        sql.append("COALESCE(pp_def.price, 0.0) as default_price, ");
        sql.append(
                "COALESCE(p.iva, c.default_iva, (SELECT config_value FROM system_config WHERE config_key = 'global_tax' LIMIT 1), 0.0) as effective_tax ");
        sql.append("FROM products p ");
        sql.append("LEFT JOIN categories c ON p.category_id = c.category_id ");
        sql.append("LEFT JOIN product_prices pp ON p.product_id = pp.product_id AND pp.price_list_id = ? ");
        if (startDate == null) {
            sql.append(
                    "  AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) ");
        } else {
            // Use a 1-second window to handle precision mismatches
            sql.append("  AND pp.start_date >= DATE_SUB(?, INTERVAL 1 SECOND) ");
            sql.append("  AND pp.start_date <= DATE_ADD(?, INTERVAL 1 SECOND) ");
        }
        sql.append("LEFT JOIN product_prices pp_now ON p.product_id = pp_now.product_id AND pp_now.price_list_id = ? ");
        sql.append(
                "  AND pp_now.start_date <= CURRENT_TIMESTAMP AND (pp_now.end_date IS NULL OR pp_now.end_date > CURRENT_TIMESTAMP) ");
        sql.append("LEFT JOIN product_prices pp_def ON p.product_id = pp_def.product_id ");
        sql.append(
                "  AND pp_def.price_list_id = (SELECT price_list_id FROM price_lists WHERE is_default = 1 LIMIT 1) ");
        sql.append(
                "  AND pp_def.start_date <= CURRENT_TIMESTAMP AND (pp_def.end_date IS NULL OR pp_def.end_date > CURRENT_TIMESTAMP) ");
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
                java.sql.Timestamp ts = java.sql.Timestamp.valueOf(startDate);
                ps.setTimestamp(paramIdx++, ts);
                ps.setTimestamp(paramIdx++, ts);
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
                    com.mycompany.ventacontrolfx.domain.dto.ProductPriceDTO dto = new com.mycompany.ventacontrolfx.domain.dto.ProductPriceDTO(
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
    public int countPricesByList(int priceListId, String search, java.time.LocalDateTime startDate)
            throws SQLException {
        String searchPattern = (search == null || search.trim().isEmpty()) ? null
                : "%" + search.trim().toLowerCase() + "%";

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM products p ");
        sql.append("LEFT JOIN categories c ON p.category_id = c.category_id ");
        if (startDate != null) {
            sql.append("JOIN product_prices pp ON p.product_id = pp.product_id AND pp.price_list_id = ? ");
            sql.append("AND pp.start_date >= DATE_SUB(?, INTERVAL 1 SECOND) ");
            sql.append("AND pp.start_date <= DATE_ADD(?, INTERVAL 1 SECOND) ");
        }
        sql.append("WHERE p.visible = 1 ");
        if (startDate != null) {
            // Already filtered by join
        }

        if (searchPattern != null) {
            sql.append("AND (LOWER(p.name) LIKE ? OR LOWER(c.name) LIKE ? OR CAST(p.product_id AS CHAR) LIKE ?) ");
        }

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIdx = 1;
            if (startDate != null) {
                ps.setInt(paramIdx++, priceListId);
                java.sql.Timestamp ts = java.sql.Timestamp.valueOf(startDate);
                ps.setTimestamp(paramIdx++, ts);
                ps.setTimestamp(paramIdx++, ts);
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

    @Override
    public List<PriceUpdateLogDTO> findBulkUpdateLog(int priceListId) throws SQLException {
        List<PriceUpdateLogDTO> logs = new ArrayList<>();
        String sql = "SELECT * FROM price_update_log ORDER BY applied_at DESC LIMIT 100";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(new PriceUpdateLogDTO(
                        rs.getInt("log_id"),
                        rs.getString("update_type"),
                        rs.getString("scope"),
                        rs.getDouble("value"),
                        rs.getInt("products_updated"),
                        rs.getString("reason"),
                        rs.getTimestamp("applied_at").toLocalDateTime()));
            }
        }
        return logs;
    }

    @Override
    public List<ProductPriceDTO> findAllPriceHistory(int priceListId) throws SQLException {
        List<ProductPriceDTO> results = new ArrayList<>();
        String sql = "SELECT p.product_id, p.name as product_name, c.name as category_name, "
                + "pp.price, pp.start_date, pp.end_date, pp.reason "
                + "FROM product_prices pp "
                + "JOIN products p ON pp.product_id = p.product_id "
                + "LEFT JOIN categories c ON p.category_id = c.category_id "
                + "WHERE pp.price_list_id = ? AND pp.end_date IS NOT NULL "
                + "ORDER BY pp.end_date DESC LIMIT 200";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, priceListId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProductPriceDTO dto = new ProductPriceDTO(
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getString("category_name"),
                            rs.getDouble("price"),
                            0.0,
                            0.0);
                    Timestamp endTs = rs.getTimestamp("end_date");
                    if (endTs != null) {
                        dto.setEndDate(endTs.toLocalDateTime());
                    }
                    dto.setReason(rs.getString("reason"));
                    results.add(dto);
                }
            }
        }
        return results;
    }
}
