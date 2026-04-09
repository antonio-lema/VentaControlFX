package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.model.SaleDetail;
import com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary;
import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.application.usecase.ClientUseCase;
import com.mycompany.ventacontrolfx.application.usecase.GetSaleTicketUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.ModalService;
import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.model.CartItem;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

/**
 * Controlador para la visualizaci\u00c3\u00b3n detallada de un ticket de venta.
 * Redise\u00c3\u00b1ado para ofrecer una est\u00c3\u00a9tica de ticket t\u00c3\u00a9rmico profesional
 * y coherente con la UI de la aplicaci\u00c3\u00b3n.
 */
public class TicketDetailController implements Injectable {

    @FXML
    private Label lblTicketId, lblDate, lblClient, lblUser, lblPayment, lblSubtotal, lblIva, lblTotal, lblReturnBadge;
    @FXML
    private VBox itemsContainer;
    @FXML
    private VBox taxDetailsContainer;

    private ClientUseCase clientUseCase;
    private GetSaleTicketUseCase getSaleTicketUseCase;
    private ServiceContainer container;
    private Sale currentSale;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.clientUseCase = container.getClientUseCase();
        this.getSaleTicketUseCase = container.getGetSaleTicketUseCase();
    }

    /**
     * Configura los datos de la venta en la interfaz.
     * Carga los detalles faltantes si es necesario.
     * 
     * @param sale Objeto de venta a mostrar
     */
    public void setSale(Sale sale) {
        try {
            // Asegurar que tenemos todos los detalles del ticket (Clean Architecture)
            if (sale.getDetails() == null || sale.getDetails().isEmpty()) {
                sale = getSaleTicketUseCase.execute(sale.getSaleId());
            }
            this.currentSale = sale;

            // Datos de cabecera
            lblTicketId.setText("Ticket #" + sale.getSaleId());
            lblDate.setText(sale.getSaleDateTime() != null ? sale.getSaleDateTime().format(FMT) : "-");

            // Estado de devoluci\u00c3\u00b3n
            if (lblReturnBadge != null) {
                lblReturnBadge.setVisible(sale.isReturn());
                lblReturnBadge.setManaged(sale.isReturn());
            }

            // Datos del Cliente
            if (sale.getClientId() != null && sale.getClientId() > 0) {
                try {
                    Client client = clientUseCase.getById(sale.getClientId());
                    lblClient.setText(client != null ? client.getName() : "Consumidor Final");
                } catch (SQLException e) {
                    lblClient.setText("Particular");
                }
            } else {
                lblClient.setText("Consumidor Final");
            }

            // Datos de personal y pago
            lblUser.setText(sale.getUserName() != null ? sale.getUserName() : "ID " + sale.getUserId());
            lblPayment.setText(sale.getPaymentMethod() != null ? sale.getPaymentMethod().toUpperCase() : "N/D");

            // Totales
            lblTotal.setText(String.format("%.2f \u20AC", sale.getTotal()));
            lblIva.setText(String.format("%.2f \u20AC", sale.getIva()));
            lblSubtotal.setText(String.format("%.2f \u20AC", sale.getTotal() - sale.getIva()));

            // Construcci\u00c3\u00b3n din\u00c3\u00a1mica de la lista de productos ( Receipt Look )
            renderItems(sale);

            // Desglose de impuestos detallado (V2)
            renderTaxDetails(sale);

        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "No se pudo renderizar el ticket: " + e.getMessage());
        }
    }

    /**
     * Renderiza las filas de productos en el contenedor VBox.
     */
    private void renderItems(Sale sale) {
        itemsContainer.getChildren().clear();

        for (SaleDetail detail : sale.getDetails()) {
            VBox itemRow = new VBox(2);
            itemRow.getStyleClass().add("item-row");

            HBox mainRow = new HBox();
            mainRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label nameLabel = new Label(detail.getProductName());
            nameLabel.getStyleClass().add("item-name");
            nameLabel.setWrapText(true);
            nameLabel.setMaxWidth(220);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label qtyLabel = new Label(String.valueOf(detail.getQuantity()));
            qtyLabel.getStyleClass().add("item-qty");
            qtyLabel.setPrefWidth(40);
            qtyLabel.setAlignment(javafx.geometry.Pos.CENTER);

            Label totalLabel = new Label(String.format("%.2f \u20AC", detail.getLineTotal()));
            totalLabel.getStyleClass().add("item-total");
            totalLabel.setPrefWidth(80);
            totalLabel.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            mainRow.getChildren().addAll(nameLabel, spacer, qtyLabel, totalLabel);

            // Sub-fila para precio unitario e informaci\u00c3\u00b3n de devoluci\u00c3\u00b3n si aplica
            if (detail.getReturnedQuantity() > 0) {
                Label returnInfo = new Label(String.format("  \u00e2\u0161\u00a0 Devuelto: %d uds", detail.getReturnedQuantity()));
                returnInfo.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-font-weight: bold;");
                itemRow.getChildren().addAll(mainRow, returnInfo);
            } else {
                itemRow.getChildren().add(mainRow);
            }

            itemsContainer.getChildren().add(itemRow);
        }
    }

    /**
     * Renderiza el desglose de impuestos en el ticket.
     */
    private void renderTaxDetails(Sale sale) {
        if (taxDetailsContainer == null)
            return;
        taxDetailsContainer.getChildren().clear();

        if (sale.getTaxSummaries() == null || sale.getTaxSummaries().isEmpty()) {
            taxDetailsContainer.setManaged(false);
            taxDetailsContainer.setVisible(false);
            return;
        }

        taxDetailsContainer.setManaged(true);
        taxDetailsContainer.setVisible(true);

        for (SaleTaxSummary summary : sale.getTaxSummaries()) {
            HBox row = new HBox();
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setSpacing(10);

            Label nameLabel = new Label(summary.getTaxName());
            nameLabel.getStyleClass().add("tax-detail-name");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label basisLabel = new Label(String.format("Base: %.2f\u20AC", summary.getTaxBasis()));
            basisLabel.getStyleClass().add("tax-detail-basis");
            basisLabel.setMinWidth(90);
            basisLabel.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            Label amountLabel = new Label(String.format("Cuota: %.2f\u20AC", summary.getTaxAmount()));
            amountLabel.getStyleClass().add("tax-detail-amount");
            amountLabel.setMinWidth(90);
            amountLabel.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            row.getChildren().addAll(nameLabel, spacer, basisLabel, amountLabel);
            taxDetailsContainer.getChildren().add(row);
        }
    }

    @FXML
    private void handlePrint() {
        if (currentSale == null)
            return;

        ModalService.showStandardModal("/view/print_preview.fxml",
                currentSale.getClientId() != null && currentSale.getClientId() > 0 ? "Factura" : "Factura Simplificada",
                container,
                (PrintPreviewController ppc) -> {
                    try {
                        List<CartItem> cartItems = new ArrayList<>();
                        for (SaleDetail detail : currentSale.getDetails()) {
                            Product p = new Product();
                            p.setName(detail.getProductName());
                            p.setPrice(detail.getUnitPrice());
                            p.setIva(detail.getIvaRate()); // Usar IVA hist\u00c3\u00b3rico
                            cartItems.add(new CartItem(p, detail.getQuantity()));
                        }

                        if (currentSale.getClientId() != null && currentSale.getClientId() > 0) {
                            Client client = clientUseCase.getById(currentSale.getClientId());
                            if (client != null) {
                                ppc.setClientInfo(client);
                            }
                        }

                        ppc.setReceiptData(cartItems, currentSale.getTotal(), currentSale.getTotal(), 0.0,
                                currentSale.getPaymentMethod(), currentSale.getSaleId());

                    } catch (Exception e) {
                        e.printStackTrace();
                        AlertUtil.showError("Error", "Error al preparar la vista previa de impresi\u00c3\u00b3n.");
                    }
                });
    }

    @FXML
    private void handleClose() {
        ((Stage) lblTicketId.getScene().getWindow()).close();
    }
}
