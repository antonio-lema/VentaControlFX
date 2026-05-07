package com.mycompany.ventacontrolfx.presentation.controller.cart;

import com.mycompany.ventacontrolfx.application.usecase.PriceListUseCase;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

/**
 * Gestiona la sincronización del selector de tarifas con el estado del carrito.
 */
public class SellPriceListManager {

    private final ServiceContainer container;
    private final PriceListUseCase priceListUseCase;
    private final ComboBox<PriceList> comboPriceList;

    public SellPriceListManager(ServiceContainer container, PriceListUseCase priceListUseCase, ComboBox<PriceList> comboPriceList) {
        this.container = container;
        this.priceListUseCase = priceListUseCase;
        this.comboPriceList = comboPriceList;
    }

    public void setup(java.util.function.Consumer<Integer> onPriceListChanged) {
        if (comboPriceList == null) return;

        comboPriceList.setConverter(new StringConverter<>() {
            @Override public String toString(PriceList object) { return object == null ? "" : object.getName(); }
            @Override public PriceList fromString(String string) { return null; }
        });

        container.getAsyncManager().runAsyncTask(() -> priceListUseCase.getAll(), priceLists -> {
            comboPriceList.getItems().setAll(priceLists);
            
            int currentId = container.getCartUseCase().getPriceListId();
            PriceList toSelect = priceLists.stream().filter(pl -> pl.getId() == currentId).findFirst().orElse(null);
            if (toSelect == null) {
                toSelect = priceLists.stream().filter(PriceList::isDefault).findFirst()
                        .orElse(!priceLists.isEmpty() ? priceLists.get(0) : null);
            }

            if (toSelect != null) {
                comboPriceList.getSelectionModel().select(toSelect);
                if (container.getCartUseCase().getPriceListId() != toSelect.getId()) {
                    container.getCartUseCase().setPriceListId(toSelect.getId());
                }
                onPriceListChanged.accept(toSelect.getId());
            }

            comboPriceList.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    if (newVal.getId() != container.getCartUseCase().getPriceListId()) {
                        container.getCartUseCase().setPriceListId(newVal.getId());
                    }
                    onPriceListChanged.accept(newVal.getId());
                }
            });
        }, null);
    }

    public void syncSelection(int priceListId) {
        PriceList selected = comboPriceList.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getId() != priceListId) {
            for (PriceList pl : comboPriceList.getItems()) {
                if (pl.getId() == priceListId) {
                    comboPriceList.getSelectionModel().select(pl);
                    break;
                }
            }
        }
    }
}

