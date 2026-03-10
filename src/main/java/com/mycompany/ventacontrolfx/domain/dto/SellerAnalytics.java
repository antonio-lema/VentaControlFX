package com.mycompany.ventacontrolfx.domain.dto;

import com.mycompany.ventacontrolfx.domain.model.Sale;
import java.util.List;

public class SellerAnalytics {
    private int userId;
    private String sellerName;
    private double totalSales;
    private int transactionCount;
    private double participationPercentage;
    private double averageTicket;
    
    // Growth metrics
    private double previousTotalSales;
    private double growthPercentage;
    
    // Payment method breakdown
    private double cashTotal;
    private double cardTotal;
    
    private List<Sale> sales;

    public SellerAnalytics(int userId, String sellerName) {
        this.userId = userId;
        this.sellerName = sellerName;
    }

    public int getUserId() { return userId; }
    public String getSellerName() { return sellerName; }
    public double getTotalSales() { return totalSales; }
    public void setTotalSales(double totalSales) { this.totalSales = totalSales; }
    public int getTransactionCount() { return transactionCount; }
    public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }
    public double getParticipationPercentage() { return participationPercentage; }
    public void setParticipationPercentage(double participationPercentage) { this.participationPercentage = participationPercentage; }
    public double getAverageTicket() { return averageTicket; }
    public void setAverageTicket(double averageTicket) { this.averageTicket = averageTicket; }
    public double getPreviousTotalSales() { return previousTotalSales; }
    public void setPreviousTotalSales(double previousTotalSales) { this.previousTotalSales = previousTotalSales; }
    public double getGrowthPercentage() { return growthPercentage; }
    public void setGrowthPercentage(double growthPercentage) { this.growthPercentage = growthPercentage; }
    public double getCashTotal() { return cashTotal; }
    public void setCashTotal(double cashTotal) { this.cashTotal = cashTotal; }
    public double getCardTotal() { return cardTotal; }
    public void setCardTotal(double cardTotal) { this.cardTotal = cardTotal; }
    public List<Sale> getSales() { return sales; }
    public void setSales(List<Sale> sales) { this.sales = sales; }

    // Nuevas métricas para el Dashboard SaaS Avanzado
    private double margin;
    private double goalReachedPercentage;
    private double returnsTotal;

    public double getMargin() { return margin; }
    public void setMargin(double margin) { this.margin = margin; }
    public double getGoalReachedPercentage() { return goalReachedPercentage; }
    public void setGoalReachedPercentage(double goalReachedPercentage) { this.goalReachedPercentage = goalReachedPercentage; }
    public double getReturnsTotal() { return returnsTotal; }
    public void setReturnsTotal(double returnsTotal) { this.returnsTotal = returnsTotal; }
}
