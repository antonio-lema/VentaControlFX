package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Promotion;
import com.mycompany.ventacontrolfx.domain.model.PromotionScope;
import com.mycompany.ventacontrolfx.domain.model.PromotionType;
import com.mycompany.ventacontrolfx.domain.repository.IPromotionRepository;
import java.sql.SQLException;
import java.util.List;

/**
 * Caso de uso para la gesti\u00c3\u00b3n administrativa de promociones.
 */
public class PromotionUseCase {
    private final IPromotionRepository repository;

    public PromotionUseCase(IPromotionRepository repository) {
        this.repository = repository;
    }

    public List<Promotion> getAllPromotions() throws SQLException {
        return repository.getAll();
    }

    public List<Promotion> getActivePromotions() throws SQLException {
        return repository.getActive();
    }

    public void savePromotion(Promotion promotion) throws SQLException {
        if (promotion.getId() == null) {
            repository.save(promotion);
        } else {
            repository.update(promotion);
        }
    }

    public void deletePromotion(Integer id) throws SQLException {
        repository.delete(id);
    }

    public void toggleActive(Integer id, boolean active) throws SQLException {
        repository.toggleActive(id, active);
    }
}
