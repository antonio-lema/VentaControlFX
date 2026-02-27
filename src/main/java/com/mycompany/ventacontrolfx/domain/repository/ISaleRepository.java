package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.model.Return;
import com.mycompany.ventacontrolfx.domain.model.ReturnDetail;
import com.mycompany.ventacontrolfx.domain.model.SaleDetail;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface ISaleRepository {
    int saveSale(Sale sale) throws SQLException;

    void saveSaleDetails(List<SaleDetail> details, int saleId) throws SQLException;

    List<SaleDetail> getDetailsBySaleId(int saleId) throws SQLException;

    Sale getById(int saleId) throws SQLException;

    List<Sale> getByRange(LocalDate start, LocalDate end) throws SQLException;

    int saveReturn(Return returnRecord) throws SQLException;

    void saveReturnDetails(List<ReturnDetail> details, int returnId) throws SQLException;

    void updateSaleReturnStatus(int saleId, boolean isReturn, String reason, double returnedAmount) throws SQLException;

    void updateDetailReturnedQuantity(int detailId, int quantity) throws SQLException;

    int count() throws SQLException;
}
