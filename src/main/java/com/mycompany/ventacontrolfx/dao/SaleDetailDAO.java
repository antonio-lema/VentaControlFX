package com.mycompany.ventacontrolfx.dao;

import com.mycompany.ventacontrolfx.model.SaleDetail;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SaleDetailDAO {
    private Connection connection;

    public SaleDetailDAO(Connection connection) {
        this.connection = connection;
    }

    public List<SaleDetail> getDetailsBySaleId(int saleId) throws SQLException {
        List<SaleDetail> details = new ArrayList<>();
        String sql = "SELECT sd.*, p.name as product_name FROM sale_details sd " +
                "JOIN products p ON sd.product_id = p.product_id " +
                "WHERE sd.sale_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, saleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    SaleDetail detail = new SaleDetail();
                    detail.setDetailId(rs.getInt("detail_id"));
                    detail.setSaleId(rs.getInt("sale_id"));
                    detail.setProductId(rs.getInt("product_id"));
                    detail.setQuantity(rs.getInt("quantity"));
                    detail.setUnitPrice(rs.getDouble("unit_price"));
                    detail.setLineTotal(rs.getDouble("line_total"));
                    detail.setProductName(rs.getString("product_name"));

                    // Safely try to get returned_quantity, default to 0 if column doesn't exist yet
                    try {
                        detail.setReturnedQuantity(rs.getInt("returned_quantity"));
                    } catch (SQLException e) {
                        detail.setReturnedQuantity(0);
                    }

                    details.add(detail);
                }
            }
        }
        return details;
    }

    public void updateReturnedQuantity(int detailId, int returnedQuantity) throws SQLException {
        String sql = "UPDATE sale_details SET returned_quantity = ? WHERE detail_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, returnedQuantity);
            pstmt.setInt(2, detailId);
            pstmt.executeUpdate();
        }
    }
}
