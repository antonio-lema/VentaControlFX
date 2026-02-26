package com.mycompany.ventacontrolfx.service;

import com.mycompany.ventacontrolfx.dao.CashClosureDAO;
import com.mycompany.ventacontrolfx.dao.DBConnection;
import com.mycompany.ventacontrolfx.dao.SaleDAO;
import com.mycompany.ventacontrolfx.model.CashClosure;
import com.mycompany.ventacontrolfx.util.UserSession;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import com.mycompany.ventacontrolfx.dao.DatabaseInitializer;
import java.util.List;
import java.util.Map;

public class CashClosureService {
    private final UserSession userSession;

    public CashClosureService(UserSession userSession) {
        this.userSession = userSession;
    }

    public Map<String, Double> getTodaySalesTotals() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            DatabaseInitializer.initialize(conn);
            SaleDAO saleDAO = new SaleDAO(conn);
            com.mycompany.ventacontrolfx.dao.ReturnDAO returnDAO = new com.mycompany.ventacontrolfx.dao.ReturnDAO(conn);

            // Fetch only pending sales and returns
            Map<String, Double> salesTotals = saleDAO.getPendingTotals();
            Map<String, Double> returnTotals = returnDAO.getPendingReturnTotals();

            // Subtract returns from sales totals
            for (Map.Entry<String, Double> entry : returnTotals.entrySet()) {
                String method = entry.getKey();
                double returnedAmount = entry.getValue();

                if (salesTotals.containsKey(method)) {
                    salesTotals.put(method, salesTotals.get(method) - returnedAmount);
                } else {
                    salesTotals.put(method, -returnedAmount);
                }
            }

            return salesTotals;
        }
    }

    public int getTodaySalesCount() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            SaleDAO saleDAO = new SaleDAO(conn);
            return saleDAO.getPendingTransactionCount();
        }
    }

    public boolean isClosureDoneToday() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            CashClosureDAO closureDAO = new CashClosureDAO(conn);
            return closureDAO.isClosureDone(LocalDate.now());
        }
    }

    public void performClosure(double totalCash, double totalCard) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            CashClosureDAO closureDAO = new CashClosureDAO(conn);

            CashClosure closure = new CashClosure();
            closure.setClosureDate(LocalDate.now());

            int userId = 1; // Default fallback
            if (userSession.getCurrentUser() != null) {
                userId = userSession.getCurrentUser().getUserId();
            }

            closure.setUserId(userId);
            closure.setTotalCash(totalCash);
            closure.setTotalCard(totalCard);
            closure.setTotalAll(totalCash + totalCard);

            closureDAO.save(closure);
        }
    }

    public List<CashClosure> getClosureHistory(LocalDate start, LocalDate end) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            CashClosureDAO closureDAO = new CashClosureDAO(conn);
            return closureDAO.getAllClosures(start, end);
        }
    }

    public List<com.mycompany.ventacontrolfx.model.ProductSummary> getProductSummary(int closureId)
            throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return new CashClosureDAO(conn).getProductSummary(closureId);
        }
    }

    public List<com.mycompany.ventacontrolfx.model.ProductSummary> getPendingProductSummary() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return new CashClosureDAO(conn).getPendingProductSummary();
        }
    }

    public int getCount() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            CashClosureDAO closureDAO = new CashClosureDAO(conn);
            return closureDAO.getCount();
        }
    }
}
