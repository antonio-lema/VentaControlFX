package com.mycompany.ventacontrolfx.domain.repository;

import com.mycompany.ventacontrolfx.domain.model.Promotion;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz para la persistencia de promociones.
 */
public interface IPromotionRepository {
    List<Promotion> getAll() throws SQLException;

    List<Promotion> getActive() throws SQLException;

    Optional<Promotion> getById(int id) throws SQLException;

    Promotion save(Promotion promotion) throws SQLException;

    void update(Promotion promotion) throws SQLException;

    void delete(int id) throws SQLException;

    void toggleActive(int id, boolean active) throws SQLException;
}
