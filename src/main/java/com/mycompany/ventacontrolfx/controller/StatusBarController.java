package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.service.GlobalEventBus;
import com.mycompany.ventacontrolfx.service.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AsyncManager;
import com.mycompany.ventacontrolfx.util.Injectable;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.util.Map;

/**
 * Enterprise Status Bar.
 * Clean integration with AsyncManager and DashboardUseCase.
 */
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
        // Uniform Async Execution using UseCase aggregation
        AsyncManager.execute(container.getDashboardUseCase().getAllCountsTask(), results -> {
            updateLabel(labelCountProducts, String.valueOf(results.get("products")));
            updateLabel(labelCountCategories, String.valueOf(results.get("categories")));
            updateLabel(labelCountHistory, String.valueOf(results.get("sales")));
            updateLabel(labelCountClosures, String.valueOf(results.get("closures")));
            updateLabel(labelCountClients, String.valueOf(results.get("clients")));
            updateLabel(labelCountUsers, String.valueOf(results.get("users")));
        });
    }

    private void updateLabel(Label label, String text) {
        if (label != null)
            label.setText(text);
    }
}
