package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.shared.bus.GlobalEventBus;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.shared.async.AsyncManager;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import java.util.Map;

public class StatusBarController implements Injectable, GlobalEventBus.DataChangeListener, GlobalEventBus.VerifactuSyncListener {

    @FXML
    private Label labelCountProducts, labelCountCategories, labelCountHistory, labelCountClosures, labelCountClients,
            labelCountUsers, lblVerifactuStatus;
    @FXML
    private HBox badgeVerifactu;

    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        container.getEventBus().subscribe(this);
        container.getEventBus().subscribeVerifactuSync(this);
        refreshCounts();
        updateLabel(lblVerifactuStatus, "");
        if (badgeVerifactu != null) {
            badgeVerifactu.setVisible(false);
            badgeVerifactu.setManaged(false);
        }
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

    @Override
    public void onSyncStarted() {
        if (badgeVerifactu != null) {
            badgeVerifactu.setVisible(true);
            badgeVerifactu.setManaged(true);
            badgeVerifactu.setStyle("-fx-background-color: #34495e; -fx-background-radius: 20; -fx-padding: 2 8;");
        }
        updateLabel(lblVerifactuStatus, "Procesando lote en Hacienda (AEAT)...");
    }

    @Override
    public void onSyncFinished(String result) {
        if ("OK".equals(result)) {
            updateLabel(lblVerifactuStatus, "Hacienda: Lote procesado con éxito");
            if (badgeVerifactu != null) {
                badgeVerifactu.setStyle("-fx-background-color: #27ae60; -fx-background-radius: 20; -fx-padding: 2 8;");
            }
            // Ocultar después de unos segundos
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(5));
            pause.setOnFinished(e -> {
                if (badgeVerifactu != null) {
                    badgeVerifactu.setVisible(false);
                    badgeVerifactu.setManaged(false);
                }
            });
            pause.play();
        } else {
            updateLabel(lblVerifactuStatus, "Hacienda: Error en el lote (" + result + ")");
            if (badgeVerifactu != null) {
                badgeVerifactu.setStyle("-fx-background-color: #c0392b; -fx-background-radius: 20; -fx-padding: 2 8;");
            }
        }
    }

    @FXML
    private void handleTestIncident() {
        if (container != null) {
            container.getEventBus().publishVerifactuIncident(
                java.util.Collections.singletonList(1), 
                java.util.Collections.emptyList()
            );
        }
    }
}
