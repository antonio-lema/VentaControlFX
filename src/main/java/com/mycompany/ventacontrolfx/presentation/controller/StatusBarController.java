package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.shared.bus.GlobalEventBus;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.shared.async.AsyncManager;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.util.Map;

public class StatusBarController implements Injectable, GlobalEventBus.DataChangeListener {

    @FXML
    private Label labelCountProducts, labelCountCategories, labelCountHistory, labelCountClosures, labelCountClients,
            labelCountUsers;

    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        container.getEventBus().subscribe(this);
        refreshCounts();
    }

    @Override
    public void onDataChanged() {
        refreshCounts();
    }

    public void refreshCounts() {
        container.getAsyncManager().execute(container.getDashboardUseCase().getAllCountsTask(),
                (Map<String, Integer> results) -> {
                    updateLabel(labelCountProducts, String.valueOf(results.getOrDefault("products", 0)));
                    updateLabel(labelCountCategories, String.valueOf(results.getOrDefault("categories", 0)));
                    updateLabel(labelCountHistory, String.valueOf(results.getOrDefault("sales", 0)));
                    updateLabel(labelCountClosures, String.valueOf(results.getOrDefault("closures", 0)));
                    updateLabel(labelCountClients, String.valueOf(results.getOrDefault("clients", 0)));
                    updateLabel(labelCountUsers, String.valueOf(results.getOrDefault("users", 0)));
                });
    }

    private void updateLabel(Label label, String text) {
        if (label != null)
            label.setText(text);
    }
}
