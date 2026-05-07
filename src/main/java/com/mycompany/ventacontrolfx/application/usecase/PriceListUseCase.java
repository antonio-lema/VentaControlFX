package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Price;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.domain.repository.IMassivePriceUpdateRepository;
import com.mycompany.ventacontrolfx.domain.repository.IPriceHistoryRepository;
import com.mycompany.ventacontrolfx.domain.repository.IPriceListRepository;
import com.mycompany.ventacontrolfx.domain.repository.IPriceRepository;
import java.sql.SQLException;
import java.util.List;

public class PriceListUseCase {
    private final IPriceListRepository repository;
    private final IPriceRepository priceRepository;
    private final IPriceHistoryRepository historyRepository;
    private final IMassivePriceUpdateRepository massiveUpdateRepository;

    public PriceListUseCase(IPriceListRepository repository, 
                          IPriceRepository priceRepository,
                          IPriceHistoryRepository historyRepository,
                          IMassivePriceUpdateRepository massiveUpdateRepository) {
        this.repository = repository;
        this.priceRepository = priceRepository;
        this.historyRepository = historyRepository;
        this.massiveUpdateRepository = massiveUpdateRepository;
    }

    public List<PriceList> getAll() throws SQLException {
        return repository.getAll();
    }

    public PriceList getById(int id) throws SQLException {
        return repository.getById(id);
    }

    public String getAveragePercentageDifference(int priceListId) throws SQLException {
        return historyRepository.getAveragePercentageDifference(priceListId);
    }

    public PriceList getDefault() throws SQLException {
        return repository.getDefault();
    }

    public void save(PriceList priceList) throws SQLException {
        repository.save(priceList);
    }

    public void update(PriceList priceList) throws SQLException {
        repository.update(priceList);
    }

    public void delete(int id) throws SQLException {
        if (id == 1) {
            throw new SQLException("No se puede eliminar la tarifa principal (ID: 1).");
        }
        repository.delete(id);
    }

    public void setAsDefault(int id) throws SQLException {
        repository.setAsDefault(id);
    }

    public PriceList clone(int sourceId, String newName, double percentage) throws SQLException {
        // 1. Obtener la fuente para copiar metadatos
        PriceList source = repository.getById(sourceId);
        String description = source != null
                ? "Clon de " + source.getName() + " (" + percentage + "%). " + source.getDescription()
                : "";

        // 2. Crear la nueva lista
        PriceList newList = new PriceList(0, newName, description, false, true, 0);
        int targetId = repository.save(newList);

        // 3. Clonar los precios con el multiplicador
        double multiplier = 1.0 + (percentage / 100.0);
        massiveUpdateRepository.cloneAndAdjustPriceList(sourceId, targetId, multiplier,
                "Clonaci\u00f3n con ajuste del " + percentage + "%", java.time.LocalDateTime.now(), null);

        return new PriceList(targetId, newName, description, false, true, 0);
    }

    public void updateProductPrice(int productId, int priceListId, double newPriceValue, String reason)
            throws SQLException {
        Price newPrice = new Price(productId, priceListId, newPriceValue, reason);
        priceRepository.updateCurrentAndSave(newPrice);
    }
}

