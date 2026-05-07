package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.PriceUpdateLog;
import java.sql.SQLException;
import java.util.List;

/**
 * Interface for the price update log repository.
 */
public interface IPriceUpdateLogRepository {
    /**
     * Saves a new price update log entry.
     */
    void save(PriceUpdateLog log) throws SQLException;

    /**
     * Retrieves all price update log entries.
     */
    List<PriceUpdateLog> getAll() throws SQLException;
    /**
     * Updates the number of products affected by a specific log entry.
     */
    void updateProductsUpdatedCount(int logId, int count) throws SQLException;
}

