package com.mycompany.ventacontrolfx.presentation.controller.receipt;

import com.mycompany.ventacontrolfx.application.usecase.SaleUseCase;
import com.mycompany.ventacontrolfx.domain.model.HistoryStats;
import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import com.mycompany.ventacontrolfx.presentation.component.SkeletonHistoryRow;
import com.mycompany.ventacontrolfx.presentation.component.SkeletonStatCard;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Gestor de carga de datos y estados de UI para el Historial.
 * Maneja la asincronía y los componentes de carga (Skeletons).
 */
public class HistoryDataManager {

    private final ServiceContainer container;
    private final SaleUseCase saleUseCase;
    private final VBox skeletonContainer;
    private final TableView<Sale> table;
    private final List<Label> kpiLabels;

    public HistoryDataManager(ServiceContainer container, VBox skeletonContainer, TableView<Sale> table, List<Label> kpiLabels) {
        this.container = container;
        this.saleUseCase = container.getSaleUseCase();
        this.skeletonContainer = skeletonContainer;
        this.table = table;
        this.kpiLabels = kpiLabels;
    }

    /**
     * Carga las ventas en un rango de fechas de forma asíncrona.
     */
    public void loadSales(LocalDate start, LocalDate end, Consumer<Object[]> onSuccess) {
        showLoading(true);

        // Cargamos las ventas primero para que la tabla se pinte rápido
        container.getAsyncManager().runAsyncTask(() -> {
            try {
                // Ejecutamos ambas tareas en paralelo si el asyncManager lo permite, 
                // o al menos las separamos para que una no bloquee a la otra
                List<Sale> sales = saleUseCase.getSalesByRange(start, end, 100);
                HistoryStats stats = saleUseCase.getHistoryStats(start, end);
                return new Object[] { stats, sales };
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, result -> {
            showLoading(false);
            onSuccess.accept((Object[]) result);
        }, e -> {
            showLoading(false);
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("history.error.load") + ": " + e.getMessage());
        });
    }

    /**
     * Busca una venta específica por su ID.
     */
    public void searchById(int id, Consumer<Object[]> onSuccess) {
        showLoading(true);
        container.getAsyncManager().runAsyncTask(() -> {
            try {
                Sale s = saleUseCase.getSaleDetails(id);
                if (s == null) return null;
                HistoryStats stats = new HistoryStats(1, s.getTotal(), s.getCashAmount(), s.getCardAmount());
                return new Object[] { stats, Collections.singletonList(s) };
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, result -> {
            showLoading(false);
            if (result == null) {
                // Silently return empty results for real-time search UX
                onSuccess.accept(new Object[] { new HistoryStats(0, 0, 0, 0), Collections.emptyList() });
            } else {
                onSuccess.accept((Object[]) result);
            }
        }, e -> {
            showLoading(false);
            AlertUtil.showError(container.getBundle().getString("alert.error"), e.getMessage());
        });
    }

    private void showLoading(boolean show) {
        Platform.runLater(() -> {
            toggleTableSkeletons(show);
            toggleKpiSkeletons(show);
        });
    }

    private void toggleTableSkeletons(boolean show) {
        if (skeletonContainer == null) return;
        skeletonContainer.setVisible(show);
        skeletonContainer.setManaged(show);
        if (show) {
            skeletonContainer.getChildren().clear();
            for (int i = 0; i < 6; i++) {
                skeletonContainer.getChildren().add(new SkeletonHistoryRow());
                Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
                skeletonContainer.getChildren().add(spacer);
            }
            skeletonContainer.prefHeightProperty().bind(table.heightProperty());
            skeletonContainer.prefWidthProperty().bind(table.widthProperty());
        }
    }

    private void toggleKpiSkeletons(boolean show) {
        for (Label lbl : kpiLabels) {
            if (lbl == null) continue;
            lbl.setGraphic(show ? new SkeletonStatCard() : null);
            if (show) lbl.setText("");
        }
    }
}


