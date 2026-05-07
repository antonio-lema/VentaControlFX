package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.repository.IMassivePriceUpdateRepository;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class JdbcMassivePriceUpdateRepository implements IMassivePriceUpdateRepository {

    @Override
    public int applyBulkMultiplier(int priceListId, Integer categoryId, double multiplier, String reason,
            java.time.LocalDateTime startDate, Integer logId)
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
                ensurePricesExist(priceListId, categoryId, startDate, logId, conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql.toString())) {
                    ps.setInt(1, priceListId);
                    if (categoryId != null && categoryId > 0)
                        ps.setInt(2, categoryId);

                    int count = executeBulkUpdate(conn, ps, priceListId, multiplier, reason, startDate, true, logId);
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
            java.time.LocalDateTime startDate, Integer logId)
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
                ensurePricesExist(priceListId, categoryId, startDate, logId, conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql.toString())) {
                    ps.setInt(1, priceListId);
                    if (categoryId != null && categoryId > 0)
                        ps.setInt(2, categoryId);

                    int count = executeBulkUpdate(conn, ps, priceListId, amount, reason, startDate, false, logId);
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
    public int applyBulkRounding(int priceListId, Integer categoryId, double roundingTarget, String reason,
            java.time.LocalDateTime startDate, Integer logId)
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
                ensurePricesExist(priceListId, categoryId, startDate, logId, conn);
                int count;
                try (PreparedStatement ps = conn.prepareStatement(selectSql.toString())) {
                    ps.setInt(1, priceListId);
                    if (categoryId != null && categoryId > 0)
                        ps.setInt(2, categoryId);
                    count = executeBulkRounding(conn, ps, priceListId, roundingTarget, reason, startDate, globalTax, logId);
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

    @Override
    public int applyBulkMultiplierToTopSellers(int priceListId, int topN, int daysBack,
            double value, String reason, boolean isPercentage, java.time.LocalDateTime startDate, Integer logId) throws SQLException {

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
                ensurePricesExist(priceListId, null, startDate, logId, conn);
                int count;
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, daysBack);
                    ps.setInt(2, topN);
                    ps.setInt(3, priceListId);
                    count = executeBulkUpdate(conn, ps, priceListId, value, reason, startDate, isPercentage, logId);
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
    public int applyBulkMultiplierToBottomSellers(int priceListId, int bottomN, int daysBack,
            double value, String reason, boolean isPercentage, java.time.LocalDateTime startDate, Integer logId) throws SQLException {

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
                ensurePricesExist(priceListId, null, startDate, logId, conn);
                int count;
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, daysBack);
                    ps.setInt(2, bottomN);
                    ps.setInt(3, priceListId);
                    count = executeBulkUpdate(conn, ps, priceListId, value, reason, startDate, isPercentage, logId);
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
    public int applyBulkMultiplierToSlowMovers(int priceListId, int daysWithoutSale,
            double value, String reason, boolean isPercentage, java.time.LocalDateTime startDate, Integer logId) throws SQLException {

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
                ensurePricesExist(priceListId, null, startDate, logId, conn);
                int count;
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, priceListId);
                    ps.setInt(2, daysWithoutSale);
                    count = executeBulkUpdate(conn, ps, priceListId, value, reason, startDate, isPercentage, logId);
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
    public int applyBulkMultiplierToPriceRange(int priceListId, double minPrice, double maxPrice,
            double value, String reason, boolean isPercentage, java.time.LocalDateTime startDate, Integer logId) throws SQLException {

        String selectSql = "SELECT pp.product_id, pp.price " +
                "FROM product_prices pp " +
                "JOIN products p ON p.product_id = pp.product_id " +
                "WHERE pp.price_list_id = ? AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) AND p.visible = 1 "
                +
                "AND pp.price BETWEEN ? AND ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                ensurePricesExist(priceListId, null, startDate, logId, conn);
                int count;
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, priceListId);
                    ps.setDouble(2, minPrice);
                    ps.setDouble(3, maxPrice);
                    count = executeBulkUpdate(conn, ps, priceListId, value, reason, startDate, isPercentage, logId);
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
    public int applyBulkMultiplierToFavorites(int priceListId, double value, String reason, boolean isPercentage,
            java.time.LocalDateTime startDate, Integer logId)
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
                ensurePricesExist(priceListId, null, startDate, logId, conn);
                int count;
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, priceListId);
                    count = executeBulkUpdate(conn, ps, priceListId, value, reason, startDate, isPercentage, logId);
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
    public int applyBulkRoundingToTopSellers(int priceListId, int topN, int daysBack, double roundingTarget,
            String reason, java.time.LocalDateTime startDate, Integer logId) throws SQLException {

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
                ensurePricesExist(priceListId, null, startDate, logId, conn);
                double globalTax = getGlobalTax(conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, daysBack);
                    ps.setInt(2, topN);
                    ps.setInt(3, priceListId);
                    int count = executeBulkRounding(conn, ps, priceListId, roundingTarget, reason, startDate,
                            globalTax, logId);
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
            String reason, java.time.LocalDateTime startDate, Integer logId) throws SQLException {

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
                ensurePricesExist(priceListId, null, startDate, logId, conn);
                double globalTax = getGlobalTax(conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, daysBack);
                    ps.setInt(2, bottomN);
                    ps.setInt(3, priceListId);
                    int count = executeBulkRounding(conn, ps, priceListId, roundingTarget, reason, startDate,
                            globalTax, logId);
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
            java.time.LocalDateTime startDate, Integer logId)
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
                ensurePricesExist(priceListId, null, startDate, logId, conn);
                double globalTax = getGlobalTax(conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, priceListId);
                    ps.setInt(2, daysWithoutSale);
                    int count = executeBulkRounding(conn, ps, priceListId, roundingTarget, reason, startDate,
                            globalTax, logId);
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
            String reason, java.time.LocalDateTime startDate, Integer logId) throws SQLException {

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
                ensurePricesExist(priceListId, null, startDate, logId, conn);
                double globalTax = getGlobalTax(conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, priceListId);
                    ps.setDouble(2, minPrice);
                    ps.setDouble(3, maxPrice);
                    int count = executeBulkRounding(conn, ps, priceListId, roundingTarget, reason, startDate,
                            globalTax, logId);
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
            java.time.LocalDateTime startDate, Integer logId) throws SQLException {

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
                ensurePricesExist(priceListId, null, startDate, logId, conn);
                double globalTax = getGlobalTax(conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, priceListId);
                    int count = executeBulkRounding(conn, ps, priceListId, roundingTarget, reason, startDate,
                            globalTax, logId);
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
            String reason, java.time.LocalDateTime startDate, Integer logId) throws SQLException {
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
                ensurePricesExistForProducts(priceListId, productIds, startDate, logId, conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, priceListId);
                    int count = executeBulkUpdate(conn, ps, priceListId, multiplier, reason, startDate, true, logId);
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
            String reason, java.time.LocalDateTime startDate, Integer logId) throws SQLException {
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
                ensurePricesExistForProducts(priceListId, productIds, startDate, logId, conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, priceListId);
                    int count = executeBulkUpdate(conn, ps, priceListId, amount, reason, startDate, false, logId);
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
            String reason, java.time.LocalDateTime startDate, Integer logId) throws SQLException {
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
                ensurePricesExistForProducts(priceListId, productIds, startDate, logId, conn);
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, priceListId);
                    int count = executeBulkRounding(conn, ps, priceListId, targetDecimal, reason, startDate, globalTax, logId);
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
        cloneAndAdjustPriceList(sourceId, targetId, 1.0, "Clonaci\u00f3n desde tarifa ID " + sourceId,
                LocalDateTime.now(), null);
    }

    @Override
    public void cloneAndAdjustPriceList(int sourceId, int targetId, double multiplier, String reason,
            LocalDateTime startDate, Integer logId)
            throws SQLException {
        String closeSql = "UPDATE product_prices SET end_date = ? " +
                "WHERE price_list_id = ? AND end_date IS NULL";

        String insertSql = "INSERT INTO product_prices (product_id, price_list_id, price, start_date, end_date, reason, update_log_id) "
                +
                "SELECT product_id, ?, price * ?, ?, NULL, ?, ? " +
                "FROM product_prices " +
                "WHERE price_list_id = ? AND start_date <= CURRENT_TIMESTAMP AND (end_date IS NULL OR end_date > CURRENT_TIMESTAMP)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psClose = conn.prepareStatement(closeSql);
                    PreparedStatement psInsert = conn.prepareStatement(insertSql)) {

                psClose.setTimestamp(1, Timestamp.valueOf(startDate));
                psClose.setInt(2, targetId);
                psClose.executeUpdate();

                psInsert.setInt(1, targetId);
                psInsert.setDouble(2, multiplier);
                psInsert.setTimestamp(3, Timestamp.valueOf(startDate));
                psInsert.setString(4, reason);
                if (logId != null) psInsert.setInt(5, logId); else psInsert.setNull(5, Types.INTEGER);
                psInsert.setInt(6, sourceId);
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

    // --- Core Logic Engines ---

    private int executeBulkUpdate(Connection conn, PreparedStatement selectStmt,
            int priceListId, double value, String reason,
            java.time.LocalDateTime startDate, boolean isPercentage, Integer logId) throws SQLException {
        int count = 0;
        String closeSql = "UPDATE product_prices SET end_date = ? WHERE product_id = ? AND price_list_id = ? AND end_date IS NULL";
        String insertSql = "INSERT INTO product_prices (product_id, price_list_id, price, start_date, end_date, reason, update_log_id) VALUES (?, ?, ?, ?, NULL, ?, ?)";

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
                    if (logId != null) insertStmt.setInt(6, logId); else insertStmt.setNull(6, Types.INTEGER);
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

    private int executeBulkRounding(Connection conn, PreparedStatement selectStmt, int priceListId,
            double roundingTarget, String reason, java.time.LocalDateTime startDate, double globalTax, Integer logId)
            throws SQLException {
        boolean pricesIncludeTax = getPricesIncludeTax(conn);
        int updatedCount = 0;
        String closeSql = "UPDATE product_prices SET end_date = ? " +
                "WHERE product_id = ? AND price_list_id = ? AND end_date IS NULL";
        String insertSql = "INSERT INTO product_prices (product_id, price_list_id, price, start_date, end_date, reason, update_log_id) "
                +
                "VALUES (?, ?, ?, ?, NULL, ?, ?)";

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
                    if (logId != null) insertStmt.setInt(6, logId); else insertStmt.setNull(6, Types.INTEGER);
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

    private void ensurePricesExist(int targetPriceListId, Integer categoryId, java.time.LocalDateTime startDate, Integer logId, Connection conn) throws SQLException {
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
        sql.append("INSERT INTO product_prices (product_id, price_list_id, price, start_date, reason, update_log_id) ");
        sql.append(
                "SELECT p.product_id, ?, COALESCE(pp_def.price, 0), ?, 'Copia impl\u00edcita de tarifa base por Subida Masiva', ? ");
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
            ps.setTimestamp(2, Timestamp.valueOf(startDate));
            if (logId != null) ps.setInt(3, logId); else ps.setNull(3, Types.INTEGER);
            ps.setInt(4, targetPriceListId);
            ps.setInt(5, defaultPriceListId);
            if (categoryId != null && categoryId > 0) {
                ps.setInt(6, categoryId);
            }
            ps.executeUpdate();
        }
    }

    private void ensurePricesExistForProducts(int priceListId, java.util.List<Integer> productIds,
            java.time.LocalDateTime startDate, Integer logId, Connection conn)
            throws SQLException {
        String sql = "INSERT IGNORE INTO product_prices (product_id, price_list_id, price, start_date, reason, update_log_id) " +
                "SELECT p.product_id, ?, p.base_price, ?, 'Inicial masivo', ? " +
                "FROM products p WHERE p.product_id IN (" +
                productIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + ") " +
                "AND NOT EXISTS (SELECT 1 FROM product_prices pp WHERE pp.product_id = p.product_id AND pp.price_list_id = ? "
                +
                "AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, priceListId);
            ps.setTimestamp(2, Timestamp.valueOf(startDate));
            if (logId != null) ps.setInt(3, logId); else ps.setNull(3, Types.INTEGER);
            ps.setInt(4, priceListId);
            ps.executeUpdate();
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

