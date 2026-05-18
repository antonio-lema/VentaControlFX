package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.model.Return;
import com.mycompany.ventacontrolfx.domain.model.ReturnDetail;
import com.mycompany.ventacontrolfx.domain.repository.IReturnRepository;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcReturnRepository implements IReturnRepository {

    private static final String INSERT_RETURN = "INSERT INTO returns (sale_id, user_id, return_datetime, total_refunded, reason, payment_method, cash_amount, card_amount, doc_type, doc_series, doc_number, doc_status, control_hash, customer_name_snapshot, customer_nif_snapshot, issuer_name, issuer_tax_id, issuer_address, total_tax, tax_basis, prev_hash, signature, fiscal_status, fiscal_msg, aeat_submission_id, gen_timestamp, is_correction, correction_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String INSERT_RETURN_DETAIL = "INSERT INTO return_details (return_id, product_id, quantity, unit_price, subtotal, tax_amount, net_amount) VALUES (?, ?, ?, ?, ?, ?, ?)";

    @Override
    public int saveReturn(Return returnRecord) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return saveReturn(returnRecord, conn);
        }
    }

    @Override
    public int saveReturn(Return returnRecord, Connection conn) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_RETURN, Statement.RETURN_GENERATED_KEYS)) {
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
            pstmt.setDouble(7, returnRecord.getCashAmount());
            pstmt.setDouble(8, returnRecord.getCardAmount());
            pstmt.setString(9, returnRecord.getDocType() != null ? returnRecord.getDocType() : "RECTIFICATIVA");
            pstmt.setString(10, returnRecord.getDocSeries() != null ? returnRecord.getDocSeries() : "R");
            if (returnRecord.getDocNumber() != null) {
                pstmt.setInt(11, returnRecord.getDocNumber());
            } else {
                pstmt.setNull(11, Types.INTEGER);
            }
            pstmt.setString(12, returnRecord.getDocStatus() != null ? returnRecord.getDocStatus() : "EMITIDO");
            pstmt.setString(13, returnRecord.getControlHash());
            pstmt.setString(14, returnRecord.getCustomerNameSnapshot());
            pstmt.setString(15, returnRecord.getCustomerNifSnapshot());
            pstmt.setString(16, returnRecord.getIssuerName());
            pstmt.setString(17, returnRecord.getIssuerTaxId());
            pstmt.setString(18, returnRecord.getIssuerAddress());
            pstmt.setDouble(19, returnRecord.getTotalTax());
            pstmt.setDouble(20, returnRecord.getTaxBasis());

            // VeriFactu Fields
            pstmt.setString(21, returnRecord.getPrevHash());
            pstmt.setString(22, returnRecord.getSignature());
            pstmt.setString(23, returnRecord.getFiscalStatus() != null ? returnRecord.getFiscalStatus() : "PENDING");
            pstmt.setString(24, returnRecord.getFiscalMsg());
            pstmt.setString(25, returnRecord.getAeatSubmissionId());
            pstmt.setString(26, returnRecord.getGenTimestamp());
            pstmt.setBoolean(27, returnRecord.isCorrection());
            pstmt.setString(28, returnRecord.getCorrectionType());

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    returnRecord.setReturnId(id);
                    return id;
                } else {
                    throw new SQLException("ERROR_RETURN_ID_NOT_GENERATED");
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
        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_RETURN_DETAIL)) {
            for (ReturnDetail detail : details) {
                pstmt.setInt(1, returnId);
                pstmt.setInt(2, detail.getProductId());
                pstmt.setInt(3, detail.getQuantity());
                pstmt.setDouble(4, detail.getUnitPrice());
                pstmt.setDouble(5, detail.getSubtotal());
                pstmt.setDouble(6, detail.getTaxAmount());
                pstmt.setDouble(7, detail.getNetAmount());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    @Override
    public void updateSaleReturnStatus(int saleId, boolean isReturn, String reason, double returnedAmount) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            updateSaleReturnStatus(saleId, isReturn, reason, returnedAmount, conn);
        }
    }

    @Override
    public void updateSaleReturnStatus(int saleId, boolean isReturn, String reason, double returnedAmount, Connection conn) throws SQLException {
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
        String sql = "SELECT r.*, u.username FROM returns r LEFT JOIN users u ON r.user_id = u.user_id WHERE r.return_datetime >= ? AND r.return_datetime <= ? ORDER BY r.return_datetime DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(start.atStartOfDay()));
            pstmt.setTimestamp(2, Timestamp.valueOf(end.atTime(23, 59, 59)));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    returns.add(mapResultSetToReturn(rs));
                }
            }
        }
        return returns;
    }

    @Override
    public List<Return> getReturnsBySaleId(int saleId) throws SQLException {
        List<Return> returns = new ArrayList<>();
        String sql = "SELECT r.*, u.username FROM returns r LEFT JOIN users u ON r.user_id = u.user_id WHERE r.sale_id = ? ORDER BY r.return_datetime DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, saleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    returns.add(mapResultSetToReturn(rs));
                }
            }
        }
        return returns;
    }

    @Override
    public List<Return> getReturnsByClosureId(int closureId) throws SQLException {
        List<Return> returns = new ArrayList<>();
        String sql = "SELECT r.*, u.username FROM returns r LEFT JOIN users u ON r.user_id = u.user_id WHERE r.closure_id = ? ORDER BY r.return_datetime ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, closureId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    returns.add(mapResultSetToReturn(rs));
                }
            }
        }
        return returns;
    }

    @Override
    public List<Return> getReturnsByUserAndRange(int userId, LocalDateTime start, LocalDateTime end) throws SQLException {
        List<Return> returns = new ArrayList<>();
        String sql = "SELECT r.*, u.username FROM returns r LEFT JOIN users u ON r.user_id = u.user_id WHERE r.user_id = ? AND r.return_datetime >= ? AND r.return_datetime <= ? ORDER BY r.return_datetime ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setTimestamp(2, Timestamp.valueOf(start));
            pstmt.setTimestamp(3, Timestamp.valueOf(end));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    returns.add(mapResultSetToReturn(rs));
                }
            }
        }
        return returns;
    }

    @Override
    public List<ReturnDetail> getReturnDetailsByReturnId(int returnId) throws SQLException {
        List<ReturnDetail> details = new ArrayList<>();
        String sql = "SELECT rd.*, p.name as product_name FROM return_details rd LEFT JOIN products p ON rd.product_id = p.product_id WHERE rd.return_id = ?";
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
                    detail.setTaxAmount(rs.getDouble("tax_amount"));
                    detail.setNetAmount(rs.getDouble("net_amount"));
                    detail.setProductName(rs.getString("product_name"));
                    details.add(detail);
                }
            }
        }
        return details;
    }

    @Override
    public Return getById(int returnId) throws SQLException {
        String sql = "SELECT r.*, u.username FROM returns r LEFT JOIN users u ON r.user_id = u.user_id WHERE r.return_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, returnId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Return ret = mapResultSetToReturn(rs);
                    ret.setDetails(getReturnDetailsByReturnId(returnId));
                    return ret;
                }
            }
        }
        return null;
    }

    @Override
    public void updateCorrectionData(int returnId, String newName, String newNif, boolean isCorrection, String correctionType, Connection conn) throws SQLException {
        String sql = "UPDATE returns SET customer_name_snapshot = ?, customer_nif_snapshot = ?, is_correction = ?, correction_type = ?, fiscal_status = 'PENDING', fiscal_msg = 'Correcci\u00f3n manual de datos' WHERE return_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newName);
            pstmt.setString(2, newNif);
            pstmt.setBoolean(3, isCorrection);
            pstmt.setString(4, correctionType);
            pstmt.setInt(5, returnId);
            pstmt.executeUpdate();
        }
    }

    private Return mapResultSetToReturn(ResultSet rs) throws SQLException {
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
        ret.setPaymentMethod(rs.getString("payment_method"));
        ret.setCashAmount(rs.getDouble("cash_amount"));
        ret.setCardAmount(rs.getDouble("card_amount"));
        ret.setDocType(rs.getString("doc_type"));
        ret.setDocSeries(rs.getString("doc_series"));
        ret.setDocNumber((Integer) rs.getObject("doc_number"));
        ret.setDocStatus(rs.getString("doc_status"));
        ret.setControlHash(rs.getString("control_hash"));
        ret.setFiscalStatus(rs.getString("fiscal_status"));
        ret.setFiscalMsg(rs.getString("fiscal_msg"));
        ret.setAeatSubmissionId(rs.getString("aeat_submission_id"));
        ret.setGenTimestamp(rs.getString("gen_timestamp"));
        ret.setPrevHash(rs.getString("prev_hash"));
        ret.setCustomerNameSnapshot(rs.getString("customer_name_snapshot"));
        ret.setCustomerNifSnapshot(rs.getString("customer_nif_snapshot"));
        ret.setIssuerName(rs.getString("issuer_name"));
        ret.setIssuerTaxId(rs.getString("issuer_tax_id"));
        ret.setIssuerAddress(rs.getString("issuer_address"));
        ret.setTotalTax(rs.getDouble("total_tax"));
        ret.setTaxBasis(rs.getDouble("tax_basis"));
        ret.setCorrection(rs.getBoolean("is_correction"));
        ret.setCorrectionType(rs.getString("correction_type"));
        return ret;
    }
}
