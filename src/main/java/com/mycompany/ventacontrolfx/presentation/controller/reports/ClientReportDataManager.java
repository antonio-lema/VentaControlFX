package com.mycompany.ventacontrolfx.presentation.controller.reports;


import com.mycompany.ventacontrolfx.application.usecase.ClientUseCase;
import com.mycompany.ventacontrolfx.application.usecase.SaleUseCase;
import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.domain.model.ClientSaleSummary;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestor de carga y clasificación de datos para el informe de clientes.
 */
public class ClientReportDataManager {

    private final ServiceContainer container;
    private final SaleUseCase saleUseCase;
    private final ClientUseCase clientUseCase;

    public ClientReportDataManager(ServiceContainer container) {
        this.container = container;
        this.saleUseCase = container.getSaleUseCase();
        this.clientUseCase = container.getClientUseCase();
    }

    public void fetchData(LocalDate start, LocalDate end, java.util.function.Consumer<List<ClientRow>> onSuccess, java.util.function.Consumer<Throwable> onError) {
        container.getAsyncManager().runAsyncTask(() -> {
            try {
                List<ClientSaleSummary> summaries = saleUseCase.getClientSalesSummary(start, end);
                List<Client> clients = clientUseCase.getAllClients();
                Map<Integer, Client> clientMap = clients.stream().collect(Collectors.toMap(Client::getId, c -> c, (a, b) -> a));

                List<ClientRow> rows = new ArrayList<>();
                for (ClientSaleSummary s : summaries) {
                    Client c = clientMap.get(s.getClientId());
                    rows.add(mapToRow(s, c));
                }
                rows.sort((a, b) -> Double.compare(b.total, a.total));
                return rows;
            } catch (Exception e) { throw new RuntimeException(e); }
        }, onSuccess, onError);
    }

    private ClientRow mapToRow(ClientSaleSummary s, Client c) {
        String name = c != null ? c.getName() : container.getBundle().getString("sidebar.item.directory") + " #" + s.getClientId();
        String info = (c != null && c.getTaxId() != null && !c.getTaxId().isEmpty()) ? "CIF: " + c.getTaxId() : container.getBundle().getString("report.client.info.particular");
        boolean active = s.getLastPurchase() != null && s.getLastPurchase().toLocalDate().isAfter(LocalDate.now().minusDays(30));

        // Lógica de Tiering (Negocio)
        String tier = container.getBundle().getString("report.client.tier.bronze");
        String color = "#d1d5db";
        if (s.getTotalSpent() > 1000) { tier = container.getBundle().getString("report.client.tier.diamond"); color = "#e2e8f0"; }
        else if (s.getTotalSpent() > 500) { tier = container.getBundle().getString("report.client.tier.gold"); color = "#fbbf24"; }
        else if (s.getTotalSpent() > 200) { tier = container.getBundle().getString("report.client.tier.silver"); color = "#60a5fa"; }

        return new ClientRow(s.getClientId(), name, info, s.getTotalSpent(), s.getTotalOrders(), active, tier, color, s.getLastPurchase());
    }

    /**
     * ViewModel para las filas del informe.
     */
    public static record ClientRow(
        int clientId, String clientName, String info, double total, 
        int count, boolean isActive, String tier, String color, LocalDateTime lastPurchase
    ) {}
}

