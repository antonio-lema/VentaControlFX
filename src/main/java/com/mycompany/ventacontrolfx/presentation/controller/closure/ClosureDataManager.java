package com.mycompany.ventacontrolfx.presentation.controller.closure;

import com.mycompany.ventacontrolfx.application.usecase.CashClosureUseCase;
import com.mycompany.ventacontrolfx.domain.model.CashClosure;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import com.mycompany.ventacontrolfx.shared.util.PaginationHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestor de datos y filtrado para el historial de cierres.
 */
public class ClosureDataManager {

    private final ServiceContainer container;
    private final CashClosureUseCase closureUseCase;
    private final PaginationHelper<CashClosure> paginationHelper;
    private final ObservableList<CashClosure> allClosures = FXCollections.observableArrayList();

    public ClosureDataManager(ServiceContainer container, PaginationHelper<CashClosure> paginationHelper) {
        this.container = container;
        this.closureUseCase = container.getClosureUseCase();
        this.paginationHelper = paginationHelper;
    }

    public void load(LocalDate start, LocalDate end, Runnable onFinish) {
        try {
            LocalDate finalStart = (start == null) ? LocalDate.of(2000, 1, 1) : start;
            LocalDate finalEnd = (end == null) ? LocalDate.of(2100, 1, 1) : end;
            
            List<CashClosure> closures = closureUseCase.getHistory(finalStart, finalEnd);
            allClosures.setAll(closures);
            onFinish.run();
        } catch (Exception e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"), container.getBundle().getString("closure.history.error.load"));
        }
    }

    public void applyFilter(String status) {
        String allText = container.getBundle().getString("closure.history.status.all");
        String squaredText = container.getBundle().getString("closure.history.status.squared");
        String offsetText = container.getBundle().getString("closure.history.status.offset");
        String reviewedText = container.getBundle().getString("closure.history.status.reviewed");
        String excludedText = container.getBundle().getString("closure.history.status.excluded");

        List<CashClosure> filtered = allClosures.stream()
                .filter(c -> {
                    if (allText.equals(status)) return true;
                    if (squaredText.equals(status)) return "CUADRADO".equals(c.getStatus());
                    if (offsetText.equals(status)) return "DESCUADRE".equals(c.getStatus());
                    if (reviewedText.equals(status)) return "REVISADO".equals(c.getStatus());
                    if (excludedText.equals(status)) return "EXCLUIDO".equals(c.getStatus());
                    return false;
                })
                .collect(Collectors.toList());
        paginationHelper.setData(filtered);
    }

    public void updateKPIs(Label lblTotalCount, Label lblTotalDiff, Label lblPending, Label lblCurrentCash) {
        double totalDiff = allClosures.stream()
                .filter(c -> !"EXCLUIDO".equals(c.getStatus()))
                .mapToDouble(CashClosure::getDifference)
                .sum();
        long pending = allClosures.stream()
                .filter(c -> !"REVISADO".equals(c.getStatus()))
                .count();

        if (lblTotalCount != null) lblTotalCount.setText(String.valueOf(allClosures.size()));
        if (lblTotalDiff != null) lblTotalDiff.setText(String.format("%+.2f \u20ac", totalDiff));
        if (lblPending != null) lblPending.setText(String.valueOf(pending));

        try {
            if (lblCurrentCash != null) lblCurrentCash.setText(String.format("%.2f \u20ac", closureUseCase.getCurrentCashInDrawer()));
        } catch (Exception e) {
            if (lblCurrentCash != null) lblCurrentCash.setText("---");
        }
    }

    public List<CashClosure> getAll() { return allClosures; }
}


