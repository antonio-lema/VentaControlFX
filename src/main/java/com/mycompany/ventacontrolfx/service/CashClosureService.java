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
import java.util.Map;

public class CashClosureService {

    public Map<String, Double> getTodaySalesTotals() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            DatabaseInitializer.initialize(conn);
            SaleDAO saleDAO = new SaleDAO(conn);
            com.mycompany.ventacontrolfx.dao.ReturnDAO returnDAO = new com.mycompany.ventacontrolfx.dao.ReturnDAO(conn);

            Map<String, Double> salesTotals = saleDAO.getTotalsByDate(LocalDate.now());
            Map<String, Double> returnTotals = returnDAO.getReturnTotalsByDate(LocalDate.now());

            // Subtract returns from sales totals
            for (Map.Entry<String, Double> entry : returnTotals.entrySet()) {
                String method = entry.getKey();
                double returnedAmount = entry.getValue();

                if (salesTotals.containsKey(method)) {
                    salesTotals.put(method, salesTotals.get(method) - returnedAmount);
                } else {
                    // Should theoretically exist if initialized properly or if sale exists,
                    // but if not, we record negative (money out)
                    salesTotals.put(method, -returnedAmount);
                }
            }

            return salesTotals;
        }
    }

    public int getTodaySalesCount() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            SaleDAO saleDAO = new SaleDAO(conn);
            return saleDAO.getTransactionCountByDate(LocalDate.now());
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
            if (UserSession.getInstance().getCurrentUser() != null) {
                userId = UserSession.getInstance().getCurrentUser().getUserId();
            }

            closure.setUserId(userId);
            closure.setTotalCash(totalCash);
            closure.setTotalCard(totalCard);
            closure.setTotalAll(totalCash + totalCard);

            closureDAO.save(closure);
        }
    }
}
