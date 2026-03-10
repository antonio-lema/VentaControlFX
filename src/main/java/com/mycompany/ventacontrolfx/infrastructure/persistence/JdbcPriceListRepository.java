package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.domain.repository.IPriceListRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class JdbcPriceListRepository implements IPriceListRepository {

    public JdbcPriceListRepository() {
        checkAndAddColumns();
    }

    private void checkAndAddColumns() {
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement()) {

            // Revisa si existe columna description
            try {
                stmt.executeQuery("SELECT description FROM price_lists LIMIT 1");
            } catch (SQLException e) {
                stmt.executeUpdate("ALTER TABLE price_lists ADD COLUMN description VARCHAR(255) AFTER name");
            }

            // Revisa si existe columna is_active
            try {
                stmt.executeQuery("SELECT is_active FROM price_lists LIMIT 1");
            } catch (SQLException e) {
                stmt.executeUpdate(
                        "ALTER TABLE price_lists ADD COLUMN is_active BOOLEAN DEFAULT TRUE AFTER is_default");
            }
        } catch (SQLException e) {
            // Error al verificar o alterar, probablemente por inexistencia de la tabla o
            // permisos
        }
    }

    @Override
    public List<PriceList> getAll() throws SQLException {
        List<PriceList> lists = new ArrayList<>();
        String sql = "SELECT * FROM price_lists ORDER BY is_default DESC, name ASC";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lists.add(mapResultSet(rs));
            }
        }
        return lists;
    }

    @Override
    public PriceList getById(int id) throws SQLException {
        String sql = "SELECT * FROM price_lists WHERE price_list_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        }
        return null;
    }

    @Override
    public PriceList getDefault() throws SQLException {
        String sql = "SELECT * FROM price_lists WHERE is_default = TRUE LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return mapResultSet(rs);
            }
        }
        // Fallback robusto por si ninguna es default (no deberia pasar)
        List<PriceList> all = getAll();
        if (!all.isEmpty())
            return all.get(0);
        return null;
    }

    @Override
    public int save(PriceList priceList) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            if (priceList.isDefault()) {
                resetDefault(conn);
            }

            String sql = "INSERT INTO price_lists (name, description, is_default, is_active) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, priceList.getName());
                pstmt.setString(2, priceList.getDescription());
                pstmt.setBoolean(3, priceList.isDefault());
                pstmt.setBoolean(4, priceList.isActive());
                pstmt.executeUpdate();

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int id = generatedKeys.getInt(1);
                        priceList.setId(id);
                        conn.commit();
                        return id;
                    }
                }
            }
            conn.commit();
            return 0;
        } catch (SQLException e) {
            if (conn != null)
                conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    @Override
    public void update(PriceList priceList) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            if (priceList.isDefault()) {
                resetDefault(conn);
            } else {
                // Prevenir quitar el default si solo hay uno
                preventRemoveLastDefault(conn, priceList.getId());
            }

            String sql = "UPDATE price_lists SET name = ?, description = ?, is_default = ?, is_active = ? WHERE price_list_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, priceList.getName());
                pstmt.setString(2, priceList.getDescription());
                pstmt.setBoolean(3, priceList.isDefault());
                pstmt.setBoolean(4, priceList.isActive());
                pstmt.setInt(5, priceList.getId());
                pstmt.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            if (conn != null)
                conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM price_lists WHERE price_list_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("La tarifa no existe (ID: " + id + ").");
            }
        }
    }

    @Override
    public void setAsDefault(int id) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            resetDefault(conn);

            String sql = "UPDATE price_lists SET is_default = TRUE WHERE price_list_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            if (conn != null)
                conn.rollback();
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    private void resetDefault(Connection conn) throws SQLException {
        String resetSql = "UPDATE price_lists SET is_default = FALSE";
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(resetSql);
        }
    }

    private void preventRemoveLastDefault(Connection conn, int currentId) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM price_lists WHERE is_default = TRUE AND price_list_id != ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, currentId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    throw new SQLException(
                            "Debe existir al menos una Tarifa por Defecto en el sistema. Seleccione otra tarifa como por defecto primero.");
                }
            }
        }
    }

    private PriceList mapResultSet(ResultSet rs) throws SQLException {
        return new PriceList(
                rs.getInt("price_list_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBoolean("is_default"),
                rs.getBoolean("is_active"));
    }
}
