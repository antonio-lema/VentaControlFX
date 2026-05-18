package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.model.Return;
import com.mycompany.ventacontrolfx.domain.model.ReturnDetail;
import com.mycompany.ventacontrolfx.domain.model.SaleDetail;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ISaleRepository {
        int saveSale(Sale sale) throws SQLException;

        int saveSale(Sale sale, Connection conn) throws SQLException;

        void saveSaleDetails(List<SaleDetail> details, int saleId) throws SQLException;

        void saveSaleDetails(List<SaleDetail> details, int saleId, Connection conn) throws SQLException;

        void saveSaleTaxSummaries(List<com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary> summaries, int saleId)
                        throws SQLException;

        void saveSaleTaxSummaries(List<com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary> summaries, int saleId,
                        Connection conn) throws SQLException;

        List<SaleDetail> getDetailsBySaleId(int saleId) throws SQLException;

        List<com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary> getTaxSummariesBySaleId(int saleId)
                        throws SQLException;

        Sale getById(int saleId) throws SQLException;

        List<Sale> getByRange(LocalDate start, LocalDate end) throws SQLException;

        List<Sale> getByRange(LocalDate start, LocalDate end, int limit) throws SQLException;
        
        List<Sale> getByClosureId(int closureId) throws SQLException;
        
        List<Sale> getByUserAndRange(int userId, LocalDateTime start, LocalDateTime end) throws SQLException;

        com.mycompany.ventacontrolfx.domain.model.HistoryStats getStatsByRange(LocalDate start, LocalDate end)
                        throws SQLException;

        int count() throws SQLException;

        List<com.mycompany.ventacontrolfx.domain.model.ProductSummary> getTopProductsByClient(int clientId, int limit)
                        throws SQLException;

        List<com.mycompany.ventacontrolfx.domain.model.ClientSaleSummary> getClientSalesSummary(LocalDate start,
                        LocalDate end) throws SQLException;

        List<Sale> getByClient(int clientId) throws SQLException;

        // Analytics
        java.util.Map<String, Double> getCategoryDistribution(LocalDate start, LocalDate end) throws SQLException;

        java.util.Map<Integer, Integer> getHourlyDistribution(LocalDate start, LocalDate end) throws SQLException;

        String getLastControlHash(String docSeries) throws SQLException;

        void updateCorrectionData(int saleId, String newName, String newNif, boolean isCorrection, String correctionType, Connection conn) throws SQLException;
        
        void updateFiscalStatus(int saleId, String status, String message, Connection conn) throws SQLException;
}

