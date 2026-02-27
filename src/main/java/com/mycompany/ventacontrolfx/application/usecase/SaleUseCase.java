package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.*;
import com.mycompany.ventacontrolfx.domain.repository.ISaleRepository;
import com.mycompany.ventacontrolfx.domain.repository.ICompanyConfigRepository;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SaleUseCase {
    private final ISaleRepository saleRepository;
    private final ICompanyConfigRepository configRepository;

    public SaleUseCase(ISaleRepository saleRepository, ICompanyConfigRepository configRepository) {
        this.saleRepository = saleRepository;
        this.configRepository = configRepository;
    }

    public int processSale(List<CartItem> cartItems, double total, String paymentMethod, Integer clientId, int userId)
            throws SQLException {
        SaleConfig config = configRepository.load();

        // Regla de Negocio: Cálculo de IVA
        double taxRate = config.getTaxRate();
        double iva = total - (total / (1 + (taxRate / 100)));

        Sale sale = new Sale();
        sale.setSaleDateTime(LocalDateTime.now());
        sale.setUserId(userId);
        sale.setClientId(clientId);
        sale.setTotal(total);
        sale.setPaymentMethod(paymentMethod);
        sale.setIva(iva);
        sale.setReturn(false);

        List<SaleDetail> details = new ArrayList<>();
        for (CartItem item : cartItems) {
            SaleDetail d = new SaleDetail();
            d.setProductId(item.getProduct().getId());
            d.setQuantity(item.getQuantity());
            d.setUnitPrice(item.getProduct().getPrice());
            d.setLineTotal(item.getTotal());
            details.add(d);
        }
        sale.setDetails(details);

        return saleRepository.saveSale(sale);
    }

    public List<Sale> getHistory(LocalDate start, LocalDate end) throws SQLException {
        return saleRepository.getByRange(start, end);
    }

    public Sale getSaleDetails(int saleId) throws SQLException {
        return saleRepository.getById(saleId);
    }

    public void registerPartialReturn(int saleId, java.util.Map<Integer, Integer> returnItems, String reason,
            int userId) throws SQLException {
        Sale sale = saleRepository.getById(saleId);
        if (sale == null)
            return;

        double refundAmountForThisTransaction = 0;
        boolean allReturned = true;
        java.util.List<ReturnDetail> newReturnDetails = new java.util.ArrayList<>();

        for (SaleDetail d : sale.getDetails()) {
            if (returnItems.containsKey(d.getDetailId())) {
                int qtyToReturnNow = returnItems.get(d.getDetailId());
                int maxReturnable = d.getQuantity() - d.getReturnedQuantity();

                if (qtyToReturnNow > maxReturnable)
                    qtyToReturnNow = maxReturnable;

                if (qtyToReturnNow > 0) {
                    int newTotalReturn = d.getReturnedQuantity() + qtyToReturnNow;
                    saleRepository.updateDetailReturnedQuantity(d.getDetailId(), newTotalReturn);
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

            int returnId = saleRepository.saveReturn(newReturn);
            saleRepository.saveReturnDetails(newReturnDetails, returnId);

            double newTotalReturned = sale.getReturnedAmount() + refundAmountForThisTransaction;
            saleRepository.updateSaleReturnStatus(saleId, allReturned, allReturned ? reason : reason + " (Parcial)",
                    newTotalReturned);
        }
    }

    public int getTotalSalesCount() throws SQLException {
        return saleRepository.count();
    }
}
