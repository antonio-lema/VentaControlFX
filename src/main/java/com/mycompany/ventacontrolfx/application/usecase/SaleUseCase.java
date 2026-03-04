package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.*;
import com.mycompany.ventacontrolfx.domain.repository.ISaleRepository;
import com.mycompany.ventacontrolfx.domain.repository.ICompanyConfigRepository;
import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SaleUseCase {
    private final ISaleRepository saleRepository;
    private final ICompanyConfigRepository configRepository;
    private CashClosureUseCase cashClosureUseCase;

    public SaleUseCase(ISaleRepository saleRepository, ICompanyConfigRepository configRepository) {
        this.saleRepository = saleRepository;
        this.configRepository = configRepository;
    }

    /** Inyecta el use case de caja para registrar movimientos de devolución. */
    public void setCashClosureUseCase(CashClosureUseCase cashClosureUseCase) {
        this.cashClosureUseCase = cashClosureUseCase;
    }

    public int processSale(List<CartItem> cartItems, double total, String paymentMethod, Integer clientId, int userId)
            throws SQLException {
        SaleConfig config = configRepository.load();
        double globalTaxRate = config.getTaxRate();

        double calculatedTotalIva = 0.0;
        List<SaleDetail> details = new ArrayList<>();

        for (CartItem item : cartItems) {
            double itemTotal = item.getTotal();
            double effectiveRate = item.getProduct().resolveEffectiveIva(globalTaxRate);

            double itemBase = itemTotal / (1.0 + (effectiveRate / 100.0));
            double itemIva = itemTotal - itemBase;
            calculatedTotalIva += itemIva;

            SaleDetail d = new SaleDetail();
            d.setProductId(item.getProduct().getId());
            d.setQuantity(item.getQuantity());
            d.setUnitPrice(item.getProduct().getPrice());
            d.setLineTotal(itemTotal);
            d.setIvaRate(effectiveRate);
            d.setIvaAmount(itemIva);
            details.add(d);
        }

        Sale sale = new Sale();
        sale.setSaleDateTime(LocalDateTime.now());
        sale.setUserId(userId);
        sale.setClientId(clientId);
        sale.setTotal(total); 
        sale.setPaymentMethod(paymentMethod);
        sale.setIva(calculatedTotalIva);
        sale.setReturn(false);
        sale.setDetails(details);

        int saleId = saleRepository.saveSale(sale);
        saleRepository.saveSaleDetails(details, saleId);

        return saleId;
    }

    public List<Sale> getHistory(LocalDate start, LocalDate end) throws SQLException {
        return saleRepository.getByRange(start, end);
    }

    public Sale getSaleDetails(int saleId) throws SQLException {
        return saleRepository.getById(saleId);
    }

    /**
     * Registra una devolución parcial o total de una venta.
     * Implementa consistencia transaccional: si el registro en caja falla, se revierte todo.
     */
    public void registerPartialReturn(int saleId, Map<Integer, Integer> returnItems, String reason,
            int userId) throws SQLException {
        Sale sale = saleRepository.getById(saleId);
        if (sale == null)
            return;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                double refundAmountForThisTransaction = 0;
                boolean allReturned = true;
                List<ReturnDetail> newReturnDetails = new ArrayList<>();

                for (SaleDetail d : sale.getDetails()) {
                    if (returnItems.containsKey(d.getDetailId())) {
                        int qtyToReturnNow = returnItems.get(d.getDetailId());
                        int maxReturnable = d.getQuantity() - d.getReturnedQuantity();

                        if (qtyToReturnNow > maxReturnable)
                            qtyToReturnNow = maxReturnable;

                        if (qtyToReturnNow > 0) {
                            int newTotalReturn = d.getReturnedQuantity() + qtyToReturnNow;
                            saleRepository.updateDetailReturnedQuantity(d.getDetailId(), newTotalReturn, conn);
                            d.setReturnedQuantity(newTotalReturn);

                            double lineRefund = qtyToReturnNow * d.getUnitPrice();
                            refundAmountForThisTransaction += lineRefund;

                            ReturnDetail rd = new ReturnDetail();
                            rd.setProductId(d.getProductId());
                            rd.setQuantity(qtyToReturnNow);
                            rd.setUnitPrice(d.getUnitPrice());
                            rd.setSubtotal(lineRefund);
                            newReturnDetails.add(rd);
                        }
                    }
                    if (d.getReturnedQuantity() < d.getQuantity())
                        allReturned = false;
                }

                if (refundAmountForThisTransaction > 0) {
                    Return newReturn = new Return();
                    newReturn.setSaleId(saleId);
                    newReturn.setUserId(userId);
                    newReturn.setTotalRefunded(refundAmountForThisTransaction);
                    newReturn.setReason(reason);
                    newReturn.setReturnDatetime(LocalDateTime.now());
                    newReturn.setPaymentMethod(sale.getPaymentMethod());

                    int returnId = saleRepository.saveReturn(newReturn, conn);
                    saleRepository.saveReturnDetails(newReturnDetails, returnId, conn);

                    double newTotalReturned = sale.getReturnedAmount() + refundAmountForThisTransaction;
                    saleRepository.updateSaleReturnStatus(saleId, allReturned, allReturned ? reason : reason + " (Parcial)",
                            newTotalReturned, conn);

                    // Registrar en el libro mayor de caja si la venta original fue en efectivo
                    if (cashClosureUseCase != null && "Efectivo".equalsIgnoreCase(sale.getPaymentMethod())) {
                        String cashReason = String.format("[Devolución Ticket #%d] %s", saleId, reason);
                        cashClosureUseCase.registerCashReturn(refundAmountForThisTransaction, cashReason, userId, conn);
                    }
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof SQLException) throw (SQLException) e;
                throw new SQLException("Error durante la transacción de devolución: " + e.getMessage(), e);
            }
        }
    }

    public int getTotalSalesCount() throws SQLException {
        return saleRepository.count();
    }

    public List<Sale> getSalesByRange(LocalDate from, LocalDate to) throws SQLException {
        return saleRepository.getByRange(from, to);
    }

    public List<Sale> getSalesByUser(int userId, LocalDate from, LocalDate to) throws SQLException {
        return saleRepository.getByRange(from, to).stream()
                .filter(s -> s.getUserId() == userId && !s.isReturn())
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Sale> getSalesByClient(int clientId, LocalDate from, LocalDate to) throws SQLException {
        return saleRepository.getByRange(from, to).stream()
                .filter(s -> s.getClientId() != null && s.getClientId() == clientId && !s.isReturn())
                .collect(java.util.stream.Collectors.toList());
    }

    public List<ReturnDetail> getReturnDetails(int returnId) throws SQLException {
        return saleRepository.getReturnDetailsByReturnId(returnId);
    }

    public List<Return> getReturnsHistory(LocalDate start, LocalDate end) throws SQLException {
        return saleRepository.getReturnsByRange(start, end);
    }
}
