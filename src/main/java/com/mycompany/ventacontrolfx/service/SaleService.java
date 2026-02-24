package com.mycompany.ventacontrolfx.service;

import com.mycompany.ventacontrolfx.dao.DBConnection;
import com.mycompany.ventacontrolfx.dao.SaleDAO;
import com.mycompany.ventacontrolfx.dao.SaleDetailDAO;
import com.mycompany.ventacontrolfx.dao.ReturnDAO;
import com.mycompany.ventacontrolfx.dao.DatabaseInitializer;
import com.mycompany.ventacontrolfx.model.CartItem;
import com.mycompany.ventacontrolfx.model.Sale;
import com.mycompany.ventacontrolfx.model.SaleDetail;
import com.mycompany.ventacontrolfx.model.Return;
import com.mycompany.ventacontrolfx.model.ReturnDetail;
import com.mycompany.ventacontrolfx.util.UserSession;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class SaleService {

    public void saveSale(List<CartItem> items, double total, String paymentMethod) throws SQLException {
        saveSale(items, total, paymentMethod, null);
    }

    public void saveSale(List<CartItem> items, double total, String paymentMethod, Integer clientId)
            throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            DatabaseInitializer.initialize(conn);
            conn.setAutoCommit(false);
            try {
                // 1. Save Sale
                String saleSql = "INSERT INTO sales (sale_datetime, user_id, client_id, total, payment_method, iva, is_return, return_reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                int saleId = -1;

                int userId = 1; // Default
                if (UserSession.getInstance().getCurrentUser() != null) {
                    userId = UserSession.getInstance().getCurrentUser().getUserId();
                }

                // Calculate IVA (e.g. 21% included in total)
                double iva = total - (total / 1.21);

                try (PreparedStatement pstmt = conn.prepareStatement(saleSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                    pstmt.setInt(2, userId);
                    if (clientId != null && clientId > 0) {
                        pstmt.setInt(3, clientId);
                    } else {
                        pstmt.setNull(3, Types.INTEGER);
                    }
                    pstmt.setDouble(4, total);
                    pstmt.setString(5, paymentMethod);
                    pstmt.setDouble(6, iva);
                    pstmt.setBoolean(7, false); // is_return
                    pstmt.setNull(8, Types.VARCHAR); // return_reason

                    int affectedRows = pstmt.executeUpdate();
                    if (affectedRows == 0) {
                        throw new SQLException("Error al crear la venta, no se afectaron filas.");
                    }

                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            saleId = rs.getInt(1);
                        } else {
                            throw new SQLException("Error al crear la venta, no se obtuvo el ID generado.");
                        }
                    }
                }

                // 2. Save Details
                String detailSql = "INSERT INTO sale_details (sale_id, product_id, quantity, unit_price, line_total) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(detailSql)) {
                    for (CartItem item : items) {
                        pstmt.setInt(1, saleId);
                        pstmt.setInt(2, item.getProduct().getId());
                        pstmt.setInt(3, item.getQuantity());
                        pstmt.setDouble(4, item.getProduct().getPrice());
                        pstmt.setDouble(5, item.getTotal()); // product price * quantity
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<Sale> getSalesHistory(LocalDate startDate, LocalDate endDate) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            SaleDAO saleDAO = new SaleDAO(conn);
            SaleDetailDAO detailDAO = new SaleDetailDAO(conn);

            List<Sale> sales = saleDAO.getSalesByRange(startDate, endDate);
            for (Sale sale : sales) {
                sale.setDetails(detailDAO.getDetailsBySaleId(sale.getSaleId()));
            }
            return sales;
        }
    }

    public List<Sale> getSalesHistory(LocalDate date) throws SQLException {
        return getSalesHistory(date, date);
    }

    public Sale getSaleById(int saleId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            SaleDAO saleDAO = new SaleDAO(conn);
            SaleDetailDAO detailDAO = new SaleDetailDAO(conn);

            Sale sale = saleDAO.getSaleById(saleId);
            if (sale != null) {
                sale.setDetails(detailDAO.getDetailsBySaleId(saleId));
            }
            return sale;
        }
    }

    public void registerReturn(int saleId, String reason) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            DatabaseInitializer.initialize(conn);
            conn.setAutoCommit(false);
            try {
                SaleDAO saleDAO = new SaleDAO(conn);
                SaleDetailDAO detailDAO = new SaleDetailDAO(conn);
                ReturnDAO returnDAO = new ReturnDAO(conn);

                // Get original details
                List<SaleDetail> details = detailDAO.getDetailsBySaleId(saleId);

                double totalRefunded = 0;
                List<ReturnDetail> newReturnDetails = new ArrayList<>();

                for (SaleDetail d : details) {
                    // Update returned qty in sale_details
                    int quantityToReturn = d.getQuantity() - d.getReturnedQuantity();
                    if (quantityToReturn > 0) {
                        detailDAO.updateReturnedQuantity(d.getDetailId(), d.getQuantity()); // Mark all as returned

                        double lineRefund = quantityToReturn * d.getUnitPrice();
                        totalRefunded += lineRefund;

                        // Create return detail
                        ReturnDetail rd = new ReturnDetail();
                        rd.setProductId(d.getProductId());
                        rd.setQuantity(quantityToReturn);
                        rd.setUnitPrice(d.getUnitPrice());
                        rd.setSubtotal(lineRefund);
                        newReturnDetails.add(rd);
                    }
                }

                // 1. Create Return Record
                int userId = 1;
                if (UserSession.getInstance().getCurrentUser() != null) {
                    userId = UserSession.getInstance().getCurrentUser().getUserId();
                }

                Return newReturn = new Return();
                newReturn.setSaleId(saleId);
                newReturn.setUserId(userId);
                newReturn.setTotalRefunded(totalRefunded);
                newReturn.setReason(reason);
                newReturn.setReturnDatetime(LocalDateTime.now());

                int returnId = returnDAO.createReturn(newReturn);

                // 2. Create Return Details
                // The DAO method creates details AND sets return_id in the DB.
                // We should pass the details list.
                // NOTE: Prior implementation looped to set returnId on object, which is good
                // practice but DAO handles the INSERT with returnId param.
                returnDAO.createReturnDetails(newReturnDetails, returnId);

                // 3. Update Sales Table (Legacy + Total Returned Amount)
                Sale sale = saleDAO.getSaleById(saleId);
                double currentReturnedAmount = sale.getReturnedAmount();
                saleDAO.updateReturnedAmount(saleId, currentReturnedAmount + totalRefunded);

                // Always mark as returned if full return (which this method assumes, based on
                // loop logic)
                saleDAO.updateReturnStatus(saleId, true, reason);

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void registerPartialReturn(int saleId, Map<Integer, Integer> returnItems, String reason)
            throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            DatabaseInitializer.initialize(conn);
            conn.setAutoCommit(false);
            try {
                SaleDAO saleDAO = new SaleDAO(conn);
                SaleDetailDAO saleDetailDAO = new SaleDetailDAO(conn);
                ReturnDAO returnDAO = new ReturnDAO(conn);

                List<SaleDetail> details = saleDetailDAO.getDetailsBySaleId(saleId);

                double refundAmountForThisTransaction = 0;
                boolean allReturned = true;
                List<ReturnDetail> newReturnDetails = new ArrayList<>();

                if (!details.isEmpty()) {
                    for (SaleDetail d : details) {
                        // Check if this item is being returned now
                        if (returnItems.containsKey(d.getDetailId())) {
                            int qtyToReturnNow = returnItems.get(d.getDetailId());

                            // Validation (ensure we don't return more than bought)
                            int alreadyReturned = d.getReturnedQuantity();
                            int maxReturnable = d.getQuantity() - alreadyReturned;

                            if (qtyToReturnNow > maxReturnable) {
                                qtyToReturnNow = maxReturnable;
                            }

                            if (qtyToReturnNow > 0) {
                                // Update sale_details
                                int newTotalReturn = alreadyReturned + qtyToReturnNow;
                                saleDetailDAO.updateReturnedQuantity(d.getDetailId(), newTotalReturn);
                                d.setReturnedQuantity(newTotalReturn);

                                // Calculate Money
                                double lineRefund = qtyToReturnNow * d.getUnitPrice();
                                refundAmountForThisTransaction += lineRefund;

                                // Create Return Detail Object
                                ReturnDetail rd = new ReturnDetail();
                                rd.setProductId(d.getProductId());
                                rd.setQuantity(qtyToReturnNow);
                                rd.setUnitPrice(d.getUnitPrice());
                                rd.setSubtotal(lineRefund);
                                newReturnDetails.add(rd);
                            }
                        }

                        // Check global status
                        if (d.getReturnedQuantity() < d.getQuantity()) {
                            allReturned = false;
                        }
                    }
                } else {
                    allReturned = false;
                }

                // Only proceed if there is something to return
                if (refundAmountForThisTransaction > 0) {
                    // 1. Create Return Record
                    int userId = 1;
                    if (UserSession.getInstance().getCurrentUser() != null) {
                        userId = UserSession.getInstance().getCurrentUser().getUserId();
                    }

                    Return newReturn = new Return();
                    newReturn.setSaleId(saleId);
                    newReturn.setUserId(userId);
                    newReturn.setTotalRefunded(refundAmountForThisTransaction);
                    newReturn.setReason(reason);
                    newReturn.setReturnDatetime(LocalDateTime.now());

                    int returnId = returnDAO.createReturn(newReturn);

                    // 2. Insert Details
                    returnDAO.createReturnDetails(newReturnDetails, returnId);

                    // 3. Update Sale Total Returned Amount
                    Sale sale = saleDAO.getSaleById(saleId);
                    double currentReturned = sale.getReturnedAmount();
                    saleDAO.updateReturnedAmount(saleId, currentReturned + refundAmountForThisTransaction);
                }

                // 4. Update Status
                if (allReturned) {
                    saleDAO.updateReturnStatus(saleId, true, reason);
                } else {
                    saleDAO.updateReturnStatus(saleId, false, reason + " (Parcial)");
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public int getTotalCount() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            SaleDAO saleDAO = new SaleDAO(conn);
            return saleDAO.getTotalCount();
        }
    }
}
