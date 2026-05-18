package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.Return;
import com.mycompany.ventacontrolfx.domain.model.ReturnDetail;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface IReturnRepository {
    int saveReturn(Return returnRecord) throws SQLException;

    int saveReturn(Return returnRecord, Connection conn) throws SQLException;

    void saveReturnDetails(List<ReturnDetail> details, int returnId) throws SQLException;

    void saveReturnDetails(List<ReturnDetail> details, int returnId, Connection conn) throws SQLException;

    void updateSaleReturnStatus(int saleId, boolean isReturn, String reason, double returnedAmount)
            throws SQLException;

    void updateSaleReturnStatus(int saleId, boolean isReturn, String reason, double returnedAmount, Connection conn)
            throws SQLException;

    void updateDetailReturnedQuantity(int detailId, int quantity) throws SQLException;

    void updateDetailReturnedQuantity(int detailId, int quantity, Connection conn) throws SQLException;

    List<Return> getReturnsByRange(LocalDate start, LocalDate end) throws SQLException;

    List<Return> getReturnsBySaleId(int saleId) throws SQLException;

    List<Return> getReturnsByClosureId(int closureId) throws SQLException;

    List<Return> getReturnsByUserAndRange(int userId, LocalDateTime start, LocalDateTime end) throws SQLException;

    List<ReturnDetail> getReturnDetailsByReturnId(int returnId) throws SQLException;
    
    Return getById(int returnId) throws SQLException;
    
    void updateCorrectionData(int returnId, String newName, String newNif, boolean isCorrection, String correctionType, Connection conn) throws SQLException;
}
