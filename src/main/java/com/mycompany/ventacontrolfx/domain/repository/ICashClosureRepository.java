package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.CashClosure;
import com.mycompany.ventacontrolfx.domain.model.ProductSummary;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface ICashClosureRepository {
    void save(CashClosure closure) throws SQLException;

    List<CashClosure> getByRange(LocalDate start, LocalDate end) throws SQLException;

    List<ProductSummary> getProductSummary(int closureId) throws SQLException;

    List<ProductSummary> getPendingProductSummary() throws SQLException;

    boolean isClosureDone(LocalDate date) throws SQLException;

    int count() throws SQLException;

    java.util.Map<String, Double> getPendingTotals() throws SQLException;

    int getPendingTransactionCount() throws SQLException;
}
