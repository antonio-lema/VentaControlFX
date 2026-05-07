package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.model.SuspendedCart;
import com.mycompany.ventacontrolfx.domain.model.SuspendedCartItem;
import com.mycompany.ventacontrolfx.domain.repository.ISuspendedCartRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcSuspendedCartRepository implements ISuspendedCartRepository {

    @Override
    public int save(SuspendedCart cart) throws SQLException {
        String cartSql = "INSERT INTO suspended_carts (alias, user_id, client_id, total) VALUES (?, ?, ?, ?)";
        String itemSql = "INSERT INTO suspended_cart_items (cart_id, product_id, quantity, price_at_suspension) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int cartId;
                try (PreparedStatement ps = conn.prepareStatement(cartSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, cart.getAlias());
                    ps.setInt(2, cart.getUserId());
                    if (cart.getClientId() != null)
                        ps.setInt(3, cart.getClientId());
                    else
                        ps.setNull(3, Types.INTEGER);
                    ps.setDouble(4, cart.getTotal());
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next())
                            cartId = rs.getInt(1);
                        else
                            throw new SQLException("Failed to get cart ID");
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                    for (SuspendedCartItem item : cart.getItems()) {
                        ps.setInt(1, cartId);
                        ps.setInt(2, item.getProduct().getId());
                        ps.setInt(3, item.getQuantity());
                        ps.setDouble(4, item.getPriceAtSuspension());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
                return cartId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    @Override
    public List<SuspendedCart> findAll() throws SQLException {
        String sql = "SELECT sc.*, u.username, c.name as client_name " +
                "FROM suspended_carts sc " +
                "LEFT JOIN users u ON sc.user_id = u.user_id " +
                "LEFT JOIN clients c ON sc.client_id = c.client_id " +
                "ORDER BY sc.suspended_at DESC";
        List<SuspendedCart> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapCart(rs));
            }
        }
        return list;
    }

    @Override
    public List<SuspendedCart> findByUserId(int userId) throws SQLException {
        String sql = "SELECT sc.*, u.username, c.name as client_name " +
                "FROM suspended_carts sc " +
                "LEFT JOIN users u ON sc.user_id = u.user_id " +
                "LEFT JOIN clients c ON sc.client_id = c.client_id " +
                "WHERE sc.user_id = ? " +
                "ORDER BY sc.suspended_at DESC";
        List<SuspendedCart> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapCart(rs));
                }
            }
        }
        return list;
    }

    @Override
    public SuspendedCart findById(int id) throws SQLException {
        String cartSql = "SELECT sc.*, u.username, c.name as client_name " +
                "FROM suspended_carts sc " +
                "LEFT JOIN users u ON sc.user_id = u.user_id " +
                "LEFT JOIN clients c ON sc.client_id = c.client_id " +
                "WHERE sc.cart_id = ?";

        String itemsSql = "SELECT sci.*, p.name as product_name, p.iva as product_iva, " +
                "COALESCE((SELECT pp.price FROM product_prices pp " +
                "  JOIN price_lists pl ON pp.price_list_id = pl.price_list_id " +
                "  WHERE pp.product_id = p.product_id AND pl.is_default = 1 " +
                "  AND pp.start_date <= CURRENT_TIMESTAMP AND (pp.end_date IS NULL OR pp.end_date > CURRENT_TIMESTAMP) "
                +
                "  ORDER BY pp.start_date DESC LIMIT 1), 0.0) AS current_price " +
                "FROM suspended_cart_items sci " +
                "JOIN products p ON sci.product_id = p.product_id " +
                "WHERE sci.cart_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            SuspendedCart cart = null;
            try (PreparedStatement ps = conn.prepareStatement(cartSql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next())
                        cart = mapCart(rs);
                }
            }

            if (cart != null) {
                try (PreparedStatement ps = conn.prepareStatement(itemsSql)) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            SuspendedCartItem item = new SuspendedCartItem();
                            item.setId(rs.getInt("item_id"));
                            item.setCartId(id);
                            item.setQuantity(rs.getInt("quantity"));
                            item.setPriceAtSuspension(rs.getDouble("price_at_suspension"));

                            Product p = new Product();
                            p.setId(rs.getInt("product_id"));
                            p.setName(rs.getString("product_name"));
                            p.setPrice(rs.getDouble("current_price"));
                            // Read IVA so tax is recalculated correctly when cart is restored
                            double ivaVal = rs.getDouble("product_iva");
                            if (!rs.wasNull()) {
                                p.setIva(ivaVal);
                            }
                            item.setProduct(p);

                            cart.getItems().add(item);
                        }
                    }
                }
            }
            return cart;
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM suspended_carts WHERE cart_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private SuspendedCart mapCart(ResultSet rs) throws SQLException {
        SuspendedCart cart = new SuspendedCart();
        cart.setId(rs.getInt("cart_id"));
        cart.setAlias(rs.getString("alias"));
        cart.setUserId(rs.getInt("user_id"));
        cart.setUsername(rs.getString("username"));
        int clientId = rs.getInt("client_id");
        if (!rs.wasNull()) {
            cart.setClientId(clientId);
            cart.setClientName(rs.getString("client_name"));
        }
        cart.setTotal(rs.getDouble("total"));
        Timestamp ts = rs.getTimestamp("suspended_at");
        if (ts != null) {
            cart.setSuspendedAt(ts.toLocalDateTime());
        }
        return cart;
    }
}

