package com.mycompany.ventacontrolfx.presentation.controller.reports;

import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestor de estadísticas y gráficas para el informe de clientes.
 */
public class ClientReportStatsManager {

    private final ServiceContainer container;

    public ClientReportStatsManager(ServiceContainer container) {
        this.container = container;
    }

    public void updateKpis(List<ClientReportDataManager.ClientRow> rows, Label active, Label total, Label avg, Label ltv, Label vActive, Label vTotal, Label vAvg, Label vLtv) {
        if (rows.isEmpty()) { reset(active, total, avg, ltv, vActive, vTotal, vAvg, vLtv); return; }

        long activeCount = rows.stream().filter(r -> r.isActive()).count();
        double totalRev = rows.stream().mapToDouble(r -> r.total()).sum();
        int totalOrders = rows.stream().mapToInt(r -> r.count()).sum();
        double avgOrder = totalOrders > 0 ? totalRev / totalOrders : 0;
        double ltvVal = totalRev / rows.size();

        active.setText(String.valueOf(activeCount));
        total.setText(String.format("\u20ac%.2f", totalRev));
        avg.setText(String.format("\u20ac%.2f", avgOrder));
        ltv.setText(String.format("\u20ac%.2f", ltvVal));

        setVariation(vActive, 5.2, true);
        setVariation(vTotal, 12.4, true);
        setVariation(vAvg, -1.2, false);
        setVariation(vLtv, 3.8, true);
    }

    private void reset(Label active, Label total, Label avg, Label ltv, Label vA, Label vT, Label vAv, Label vL) {
        active.setText("0"); total.setText("\u20ac0.00"); avg.setText("\u20ac0.00"); ltv.setText("\u20ac0.00");
        vA.setText("-"); vT.setText("-"); vAv.setText("-"); vL.setText("-");
    }

    private void setVariation(Label lbl, double amount, boolean positive) {
        if (lbl == null) return;
        lbl.setText((positive ? "\u2191 " : "\u2193 ") + Math.abs(amount) + "%");
        lbl.setStyle(positive 
            ? "-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-padding: 2 6; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;"
            : "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-padding: 2 6; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");
    }

    public void populateEvolutionChart(LineChart<String, Number> chart, List<Sale> sales, String filter) {
        chart.getData().clear();
        if (sales.isEmpty()) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        boolean groupByDay = filter.contains("d\u00eda") || filter.contains("Hoy") || filter.contains("mes");

        if (groupByDay) {
            Map<LocalDate, Double> byDay = new TreeMap<>();
            for (Sale s : sales) {
                if (s.getSaleDateTime() != null) {
                    LocalDate d = s.getSaleDateTime().toLocalDate();
                    byDay.put(d, byDay.getOrDefault(d, 0.0) + s.getTotal());
                }
            }
            fillEmptyDays(byDay, filter);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM");
            byDay.forEach((d, val) -> series.getData().add(new XYChart.Data<>(d.format(fmt), val)));
        } else {
            Map<YearMonth, Double> byMonth = new TreeMap<>();
            for (Sale s : sales) {
                if (s.getSaleDateTime() != null) {
                    YearMonth ym = YearMonth.from(s.getSaleDateTime());
                    byMonth.put(ym, byMonth.getOrDefault(ym, 0.0) + s.getTotal());
                }
            }
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yy");
            byMonth.forEach((ym, val) -> series.getData().add(new XYChart.Data<>(ym.format(fmt), val)));
        }
        chart.getData().add(series);
    }

    private void fillEmptyDays(Map<LocalDate, Double> byDay, String filter) {
        LocalDate end = LocalDate.now();
        LocalDate start = filter.contains("7") ? end.minusDays(7) : end.minusDays(30);
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (!byDay.containsKey(d)) byDay.put(d, 0.0);
        }
    }
}

