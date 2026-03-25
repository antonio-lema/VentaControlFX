package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.Return;
import com.mycompany.ventacontrolfx.domain.model.ReturnDetail;
import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.model.SaleDetail;
import com.mycompany.ventacontrolfx.domain.repository.ISaleRepository;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JdbcSaleRepository implements ISaleRepository {

    @Override
    public int saveSale(Sale sale) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return saveSale(sale, conn);
        }
    }

    @Override
    public int saveSale(Sale sale, Connection conn) throws SQLException {
        String sql = "INSERT INTO sales (user_id, client_id, total, payment_method, iva, sale_datetime, is_return, doc_type, doc_series, doc_number, doc_status, control_hash, total_net, total_tax, customer_name_snapshot, discount_amount, discount_reason, cash_amount, card_amount, observations) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, sale.getUserId());
            if (sale.getClientId() != null && sale.getClientId() > 0) {
                pstmt.setInt(2, sale.getClientId());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setDouble(3, sale.getTotal());
            pstmt.setString(4, sale.getPaymentMethod());
            pstmt.setDouble(5, sale.getIva());
            pstmt.setTimestamp(6, Timestamp.valueOf(sale.getSaleDateTime()));
            pstmt.setBoolean(7, sale.isReturn());
            pstmt.setString(8, sale.getDocType() != null ? sale.getDocType() : "TICKET");
            pstmt.setString(9, sale.getDocSeries());
            if (sale.getDocNumber() != null) {
                pstmt.setInt(10, sale.getDocNumber());
            } else {
                pstmt.setNull(10, Types.INTEGER);
            }
            pstmt.setString(11, sale.getDocStatus() != null ? sale.getDocStatus() : "ISSUED");
            pstmt.setString(12, sale.getControlHash());
            pstmt.setDouble(13, sale.getTotalNet());
            pstmt.setDouble(14, sale.getTotalTax());
            pstmt.setString(15, sale.getCustomerNameSnapshot());
            pstmt.setDouble(16, sale.getDiscountAmount());
            pstmt.setString(17, sale.getDiscountReason());
            pstmt.setDouble(18, sale.getCashAmount());
            pstmt.setDouble(19, sale.getCardAmount());
            pstmt.setString(20, sale.getObservations());
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    sale.setSaleId(id);
                    return id;
                } else {
                    throw new SQLException("Error al crear la venta, no se obtuvo el ID generado.");
                }
            }
        }
    }

    @Override
    public void saveSaleDetails(List<SaleDetail> details, int saleId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            saveSaleDetails(details, saleId, conn);
        }
    }

    @Override
    public void saveSaleDetails(List<SaleDetail> details, int saleId, Connection conn) throws SQLException {
        String sql = "INSERT INTO sale_details (sale_id, product_id, quantity, unit_price, line_total, iva_rate, iva_amount, product_name_snapshot, net_unit_price, tax_basis, tax_amount, gross_total, applied_tax_group, sku_snapshot, category_name_snapshot, observations) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (SaleDetail detail : details) {
                pstmt.setInt(1, saleId);
                pstmt.setInt(2, detail.getProductId());
                pstmt.setInt(3, detail.getQuantity());
                pstmt.setDouble(4, detail.getUnitPrice());
                pstmt.setDouble(5, detail.getLineTotal());
                pstmt.setDouble(6, detail.getIvaRate());
                pstmt.setDouble(7, detail.getIvaAmount());
                pstmt.setString(8, detail.getProductName());
                pstmt.setDouble(9, detail.getNetUnitPrice());
                pstmt.setDouble(10, detail.getTaxBasis());
                pstmt.setDouble(11, detail.getTaxAmount());
                pstmt.setDouble(12, detail.getGrossTotal());
                pstmt.setString(13, detail.getAppliedTaxGroup());
                pstmt.setString(14, detail.getSkuSnapshot());
                pstmt.setString(15, detail.getCategoryNameSnapshot());
                pstmt.setString(16, detail.getObservations());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    @Override
    public void saveSaleTaxSummaries(List<com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary> summaries,
            int saleId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            saveSaleTaxSummaries(summaries, saleId, conn);
        }
    }

    @Override
    public void saveSaleTaxSummaries(List<com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary> summaries,
            int saleId, Connection conn) throws SQLException {
        if (summaries == null || summaries.isEmpty())
            return;
        String sql = "INSERT INTO sale_tax_summary (sale_id, tax_rate_id, tax_name, tax_rate, tax_basis, tax_amount) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary summary : summaries) {
                pstmt.setInt(1, saleId);
                pstmt.setInt(2, summary.getTaxRateId());
                pstmt.setString(3, summary.getTaxName());
                pstmt.setDouble(4, summary.getTaxRate());
                pstmt.setDouble(5, summary.getTaxBasis());
                pstmt.setDouble(6, summary.getTaxAmount());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    @Override
    public List<SaleDetail> getDetailsBySaleId(int saleId) throws SQLException {
        List<SaleDetail> details = new ArrayList<>();
        String sql = "SELECT * FROM sale_details WHERE sale_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
                    detail.setIvaRate(rs.getDouble("iva_rate"));
                    detail.setIvaAmount(rs.getDouble("iva_amount"));
                    detail.setProductName(rs.getString("product_name_snapshot"));
                    detail.setReturnedQuantity(rs.getInt("returned_quantity"));
                    // Snapshots fiscales
                    detail.setNetUnitPrice(rs.getDouble("net_unit_price"));
                    detail.setTaxBasis(rs.getDouble("tax_basis"));
                    detail.setTaxAmount(rs.getDouble("tax_amount"));
                    detail.setGrossTotal(rs.getDouble("gross_total"));
                    detail.setAppliedTaxGroup(rs.getString("applied_tax_group"));
                    detail.setSkuSnapshot(rs.getString("sku_snapshot"));
                    detail.setCategoryNameSnapshot(rs.getString("category_name_snapshot"));
                    detail.setObservations(rs.getString("observations"));
                    details.add(detail);
                }
            }
        }
        return details;
    }

    @Override
    public List<com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary> getTaxSummariesBySaleId(int saleId)
            throws SQLException {
        List<com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary> summaries = new ArrayList<>();
        String sql = "SELECT * FROM sale_tax_summary WHERE sale_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, saleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary summary = new com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary();
                    summary.setId(rs.getInt("summary_id"));
                    summary.setSaleId(rs.getInt("sale_id"));
                    summary.setTaxRateId(rs.getInt("tax_rate_id"));
                    summary.setTaxName(rs.getString("tax_name"));
                    summary.setTaxRate(rs.getDouble("tax_rate"));
                    summary.setTaxBasis(rs.getDouble("tax_basis"));
                    summary.setTaxAmount(rs.getDouble("tax_amount"));
                    summaries.add(summary);
                }
            }
        }
        return summaries;
    }

    @Override
    public Sale getById(int saleId) throws SQLException {
        String sql = "SELECT s.*, u.username FROM sales s LEFT JOIN users u ON s.user_id = u.user_id WHERE s.sale_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, saleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Sale sale = mapResultSetToSale(rs);
                    sale.setDetails(getDetailsBySaleId(sale.getSaleId()));
                    sale.setTaxSummaries(getTaxSummariesBySaleId(sale.getSaleId()));
                    return sale;
                }
            }
        }
        return null;
    }

    @Override
    public List<Sale> getByRange(LocalDate start, LocalDate end) throws SQLException {
        List<Sale> sales = new ArrayList<>();
        String sql = "SELECT s.*, u.username FROM sales s " +
                "LEFT JOIN users u ON s.user_id = u.user_id " +
                "WHERE DATE(s.sale_datetime) BETWEEN ? AND ? " +
                "ORDER BY s.sale_datetime DESC";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(start));
            pstmt.setDate(2, Date.valueOf(end));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    sales.add(mapResultSetToSale(rs));
                }
            }
        }
        return sales;
    }

    @Override
    public int saveReturn(Return returnRecord) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return saveReturn(returnRecord, conn);
        }
    }

    @Override
    public int saveReturn(Return returnRecord, Connection conn) throws SQLException {
        String sql = "INSERT INTO returns (sale_id, user_id, return_datetime, total_refunded, reason, payment_method) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, returnRecord.getSaleId());
            if (returnRecord.getUserId() > 0) {
                pstmt.setInt(2, returnRecord.getUserId());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setTimestamp(3, Timestamp.valueOf(returnRecord.getReturnDatetime()));
            pstmt.setDouble(4, returnRecord.getTotalRefunded());
            pstmt.setString(5, returnRecord.getReason());
            pstmt.setString(6, returnRecord.getPaymentMethod());

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    returnRecord.setReturnId(id);
                    return id;
                } else {
                    throw new SQLException("Error al crear la devolución, no se obtuvo el ID generado.");
                }
            }
        }
    }

    @Override
    public void saveReturnDetails(List<ReturnDetail> details, int returnId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            saveReturnDetails(details, returnId, conn);
        }
    }

    @Override
    public void saveReturnDetails(List<ReturnDetail> details, int returnId, Connection conn) throws SQLException {
        String sql = "INSERT INTO return_details (return_id, product_id, quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (ReturnDetail detail : details) {
                pstmt.setInt(1, returnId);
                pstmt.setInt(2, detail.getProductId());
                pstmt.setInt(3, detail.getQuantity());
                pstmt.setDouble(4, detail.getUnitPrice());
                pstmt.setDouble(5, detail.getSubtotal());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    @Override
    public void updateSaleReturnStatus(int saleId, boolean isReturn, String reason, double returnedAmount)
            throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            updateSaleReturnStatus(saleId, isReturn, reason, returnedAmount, conn);
        }
    }

    @Override
    public void updateSaleReturnStatus(int saleId, boolean isReturn, String reason, double returnedAmount,
            Connection conn) throws SQLException {
        String sql = "UPDATE sales SET is_return = ?, return_reason = ?, returned_amount = ? WHERE sale_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, isReturn);
            pstmt.setString(2, reason);
            pstmt.setDouble(3, returnedAmount);
            pstmt.setInt(4, saleId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void updateDetailReturnedQuantity(int detailId, int quantity) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            updateDetailReturnedQuantity(detailId, quantity, conn);
        }
    }

    @Override
    public void updateDetailReturnedQuantity(int detailId, int quantity, Connection conn) throws SQLException {
        String sql = "UPDATE sale_details SET returned_quantity = ? WHERE detail_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, quantity);
            pstmt.setInt(2, detailId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public List<Return> getReturnsByRange(LocalDate start, LocalDate end) throws SQLException {
        List<Return> returns = new ArrayList<>();
        String sql = "SELECT r.*, u.username FROM returns r " +
                "LEFT JOIN users u ON r.user_id = u.user_id " +
                "WHERE DATE(r.return_datetime) BETWEEN ? AND ? " +
                "ORDER BY r.return_datetime DESC";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, Date.valueOf(start));
            pstmt.setDate(2, Date.valueOf(end));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Return ret = new Return();
                    ret.setReturnId(rs.getInt("return_id"));
                    ret.setSaleId(rs.getInt("sale_id"));
                    ret.setUserId(rs.getInt("user_id"));
                    ret.setUserName(rs.getString("username"));
                    Timestamp ts = rs.getTimestamp("return_datetime");
                    if (ts != null) {
                        ret.setReturnDatetime(ts.toLocalDateTime());
                    }
                    ret.setTotalRefunded(rs.getDouble("total_refunded"));
                    ret.setReason(rs.getString("reason"));
                    ret.setClosureId((Integer) rs.getObject("closure_id"));
                    try {
                        ret.setPaymentMethod(rs.getString("payment_method"));
                    } catch (SQLException ignored) {
                    }
                    returns.add(ret);
                }
            }
        }
        return returns;
    }

    @Override
    public List<ReturnDetail> getReturnDetailsByReturnId(int returnId) throws SQLException {
        List<ReturnDetail> details = new ArrayList<>();
        String sql = "SELECT rd.*, p.name as product_name " +
                "FROM return_details rd " +
                "LEFT JOIN products p ON rd.product_id = p.product_id " +
                "WHERE rd.return_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, returnId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ReturnDetail detail = new ReturnDetail();
                    detail.setReturnDetailId(rs.getInt("return_detail_id"));
                    detail.setReturnId(rs.getInt("return_id"));
                    detail.setProductId(rs.getInt("product_id"));
                    detail.setQuantity(rs.getInt("quantity"));
                    detail.setUnitPrice(rs.getDouble("unit_price"));
                    detail.setSubtotal(rs.getDouble("subtotal"));
                    detail.setProductName(rs.getString("product_name"));
                    details.add(detail);
                }
            }
        }
        return details;
    }

    @Override
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM sales";
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private Sale mapResultSetToSale(ResultSet rs) throws SQLException {
        Sale sale = new Sale();
        sale.setSaleId(rs.getInt("sale_id"));
        Timestamp ts = rs.getTimestamp("sale_datetime");
        if (ts != null) {
            sale.setSaleDateTime(ts.toLocalDateTime());
        }
        sale.setUserId(rs.getInt("user_id"));
        sale.setUserName(rs.getString("username"));
        sale.setClientId((Integer) rs.getObject("client_id"));
        sale.setTotal(rs.getDouble("total"));
        sale.setPaymentMethod(rs.getString("payment_method"));
        sale.setIva(rs.getDouble("iva"));
        sale.setReturn(rs.getBoolean("is_return"));
        sale.setReturnReason(rs.getString("return_reason"));
        sale.setReturnedAmount(rs.getDouble("returned_amount"));
        sale.setClosureId((Integer) rs.getObject("closure_id"));
        sale.setCashAmount(rs.getDouble("cash_amount"));
        sale.setCardAmount(rs.getDouble("card_amount"));

        // Atributos fiscales
        sale.setDocType(rs.getString("doc_type"));
        sale.setDocSeries(rs.getString("doc_series"));
        sale.setDocNumber((Integer) rs.getObject("doc_number"));
        sale.setDocStatus(rs.getString("doc_status"));
        sale.setControlHash(rs.getString("control_hash"));

        // Snapshots de inmutabilidad
        sale.setTotalNet(rs.getDouble("total_net"));
        sale.setTotalTax(rs.getDouble("total_tax"));
        sale.setCustomerNameSnapshot(rs.getString("customer_name_snapshot"));
        sale.setObservations(rs.getString("observations"));

        return sale;
    }
}
