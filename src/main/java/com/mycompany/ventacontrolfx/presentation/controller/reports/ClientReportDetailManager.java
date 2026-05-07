package com.mycompany.ventacontrolfx.presentation.controller.reports;


import com.mycompany.ventacontrolfx.application.usecase.SaleUseCase;
import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gestor del panel de detalles de cliente en el informe.
 */
public class ClientReportDetailManager {

    private final ServiceContainer container;
    private final SaleUseCase saleUseCase;
    private final Region pnlEmpty, pnlContent;
    private final Label lblInitials, lblName, lblTier, lblInfo;
    private final StackPane spAvatar;
    private final TableView<Sale> tableSales;

    public ClientReportDetailManager(ServiceContainer container, Region pnlEmpty, Region pnlContent, Label lblInitials, Label lblName, Label lblTier, Label lblInfo, StackPane spAvatar, TableView<Sale> tableSales) {
        this.container = container;
        this.saleUseCase = container.getSaleUseCase();
        this.pnlEmpty = pnlEmpty;
        this.pnlContent = pnlContent;
        this.lblInitials = lblInitials;
        this.lblName = lblName;
        this.lblTier = lblTier;
        this.lblInfo = lblInfo;
        this.spAvatar = spAvatar;
        this.tableSales = tableSales;
    }

    public void show(ClientReportDataManager.ClientRow row) {
        pnlEmpty.setVisible(false); pnlEmpty.setManaged(false);
        pnlContent.setVisible(true); pnlContent.setManaged(true);
        pnlContent.setStyle("-fx-opacity: 1;");

        lblName.setText(row.clientName());
        lblInitials.setText(row.clientName().substring(0, Math.min(row.clientName().length(), 2)).toUpperCase());

        String[] colors = { "#e0e7ff", "#dcfce7", "#fef3c7", "#f3e8ff", "#fee2e2" };
        spAvatar.setStyle("-fx-background-color: " + colors[Math.abs(row.clientName().hashCode()) % colors.length] + "; -fx-background-radius: 50; -fx-min-width: 64; -fx-min-height: 64;");

        applyTierStyle(row.tier());
        String lastDate = row.lastPurchase() == null ? container.getBundle().getString("report.client.never") : row.lastPurchase().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        lblInfo.setText(row.info() + " | " + String.format(container.getBundle().getString("report.client.last_purchase"), lastDate));

        loadSales(row.clientId());
    }

    private void applyTierStyle(String tier) {
        lblTier.setText(tier);
        if (tier.equals(container.getBundle().getString("report.client.tier.gold")))
            lblTier.setStyle("-fx-background-color: #fef08a; -fx-text-fill: #854d0e; -fx-padding: 2 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
        else if (tier.equals(container.getBundle().getString("report.client.tier.silver")))
            lblTier.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-padding: 2 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
        else
            lblTier.setStyle("-fx-background-color: #fed7aa; -fx-text-fill: #9a3412; -fx-padding: 2 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
    }

    private void loadSales(int clientId) {
        tableSales.setItems(FXCollections.observableArrayList());
        tableSales.setPlaceholder(new Label("Cargando ventas..."));
        new Thread(() -> {
            try {
                List<Sale> sales = saleUseCase.getSalesByClient(clientId);
                List<Sale> limited = sales.size() > 200 ? sales.subList(0, 200) : sales;
                Platform.runLater(() -> {
                    tableSales.setItems(FXCollections.observableArrayList(limited));
                    tableSales.setPlaceholder(new Label("No hay ventas registradas"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> tableSales.setPlaceholder(new Label("Error al cargar ventas")));
            }
        }, "ClientDetail-Loader").start();
    }
}

