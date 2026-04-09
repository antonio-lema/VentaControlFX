package com.mycompany.ventacontrolfx.domain.service;

import com.mycompany.ventacontrolfx.domain.model.Price;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.domain.repository.IPriceRepository;
import java.sql.SQLException;
import java.util.Optional;

public class PriceResolutionService {
    private final IPriceRepository priceRepository;
    private final com.mycompany.ventacontrolfx.domain.repository.IClientRepository clientRepository;

    public PriceResolutionService(IPriceRepository priceRepository,
            com.mycompany.ventacontrolfx.domain.repository.IClientRepository clientRepository) {
        this.priceRepository = priceRepository;
        this.clientRepository = clientRepository;
    }

    /**
     * Resuelve el precio de un producto considerando la tarifa solicitada y cayendo
     * a la tarifa por defecto si no existe una especÃ­fica, cumpliendo con las
     * prioridades.
     */
    public Optional<Price> resolvePrice(int productId, int priceListId) throws SQLException {
        // 1. Intentar obtener el precio en la tarifa especÃ­fica (viva actualmente)
        Optional<Price> specificPrice = priceRepository.getActivePrice(productId, priceListId);

        if (specificPrice.isPresent()) {
            return specificPrice;
        }

        // 2. Fallback a la tarifa por defecto si no hay precio especÃ­fico activo
        PriceList defaultList = priceRepository.getDefaultPriceList();
        if (priceListId != defaultList.getId()) {
            return priceRepository.getActivePrice(productId, defaultList.getId());
        }

        return Optional.empty();
    }

    /**
     * Calcula el precio para un cliente especÃ­fico considerando su lista de precios
     * asignada.
     */
    public Optional<Price> resolvePriceForClient(int productId, Integer clientId) throws SQLException {
        int listId = -1;

        if (clientId != null && clientId > 0) {
            com.mycompany.ventacontrolfx.domain.model.Client client = clientRepository.getById(clientId);
            if (client != null && client.getPriceListId() > 0) {
                listId = client.getPriceListId();
            }
        }

        if (listId <= 0) {
            PriceList defaultList = priceRepository.getDefaultPriceList();
            listId = defaultList.getId();
        }

        return resolvePrice(productId, listId);
    }
}
