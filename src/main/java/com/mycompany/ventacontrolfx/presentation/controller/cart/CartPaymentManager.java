package com.mycompany.ventacontrolfx.presentation.controller.cart;

import com.mycompany.ventacontrolfx.application.usecase.CartUseCase;
import com.mycompany.ventacontrolfx.application.usecase.SaleUseCase;
import com.mycompany.ventacontrolfx.domain.model.CartItem;
import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.PaymentController;
import com.mycompany.ventacontrolfx.presentation.controller.receipt.ReceiptController;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import com.mycompany.ventacontrolfx.presentation.navigation.ModalService;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestor del proceso de pago y facturación.
 * Coordina la validación, ejecución de venta y emisión fiscal.
 */
public class CartPaymentManager {

    private final ServiceContainer container;
    private final CartUseCase cartUseCase;

    public CartPaymentManager(ServiceContainer container, CartUseCase cartUseCase) {
        this.container = container;
        this.cartUseCase = cartUseCase;
    }

    public void processPayment() {
        if (cartUseCase.getItemCount() == 0) return;

        double grandTotal = cartUseCase.getGrandTotal();
        Client selectedClient = cartUseCase.getSelectedClient();

        // 1. Validación Legal (Anti-Blanqueo / Facturación)
        if (grandTotal > 1000) {
            if (selectedClient == null || selectedClient.getTaxId() == null || selectedClient.getTaxId().trim().isEmpty()) {
                AlertUtil.showError(container.getBundle().getString("payment.error.id_required.title"),
                        container.getBundle().getString("payment.error.id_required.msg"));
                return;
            }
        }

        // 2. Abrir Pasarela de Pago
        ModalService.showModal("/view/cart/payment.fxml", container.getBundle().getString("payment.title"),
                Modality.APPLICATION_MODAL, StageStyle.UNDECORATED,
                container, (PaymentController pc) -> {
                    pc.setTotalAmount(cartUseCase.getGrandTotal(), (paid, change, method, cashAmount, cardAmount) -> {
                        executeSaleTransaction(paid, change, method, cashAmount, cardAmount);
                    });
                });
    }

    private void executeSaleTransaction(double paid, double change, String method, double cashAmount, double cardAmount) {
        try {
            List<CartItem> items = new ArrayList<>(cartUseCase.getCartItems());
            double total = cartUseCase.getGrandTotal();
            Client client = cartUseCase.getSelectedClient();
            Integer clientId = client != null ? client.getId() : null;
            String observations = cartUseCase.getGeneralObservation();
            int userId = container.getUserSession().getCurrentUser().getUserId();
            String promoCode = cartUseCase.getAppliedPromoCode();

            container.getAsyncManager().runAsyncTask(() -> {
                // A. Procesar Venta Core
                SaleUseCase.ProcessSaleResult result = container.getSaleUseCase().processSale(
                        items, total, method, clientId, userId, 0.0, null, cashAmount, cardAmount, observations, promoCode);

                // B. Emisión Fiscal (Segundo Plano)
                emitFiscalDocument(result.saleId, client);
                
                return result;

            }, (SaleUseCase.ProcessSaleResult result) -> {
                // C. Éxito: Limpiar y Notificar
                finalizeSale(result, items, total, paid, change, method, client, observations);
                
            }, (Throwable e) -> {
                handleSaleError(e);
            });
        } catch (Exception e) {
            AlertUtil.showError(container.getBundle().getString("cart.payment.error.unexpected"), e.getMessage());
        }
    }

    private void emitFiscalDocument(int saleId, Client client) {
        try {
            if (client != null && client.getTaxId() != null && !client.getTaxId().trim().isEmpty()) {
                String address = formatClientAddress(client);
                container.getEmitFiscalDocumentUseCase().emitInvoice(saleId, client.getName(), client.getTaxId(), address);
            } else {
                container.getEmitFiscalDocumentUseCase().emitTicket(saleId);
            }
        } catch (Exception e) {
            System.err.println("[CartPaymentManager] Error fiscal: " + e.getMessage());
        }
    }

    private String formatClientAddress(Client client) {
        StringBuilder sb = new StringBuilder(client.getAddress() != null ? client.getAddress() : "");
        if (client.getPostalCode() != null && !client.getPostalCode().isEmpty()) sb.append(", ").append(client.getPostalCode());
        if (client.getCity() != null && !client.getCity().isEmpty()) sb.append(" ").append(client.getCity());
        if (client.getProvince() != null && !client.getProvince().isEmpty()) sb.append(" (").append(client.getProvince()).append(")");
        return sb.toString();
    }

    private void finalizeSale(SaleUseCase.ProcessSaleResult result, List<CartItem> items, double total, 
                              double paid, double change, String method, Client client, String obs) {
        
        cartUseCase.clear();
        container.getEventBus().publishDataChange();

        ModalService.showStandardModal("/view/receipt/receipt.fxml",
                client != null ? container.getBundle().getString("receipt.title.invoice") : container.getBundle().getString("receipt.title.simplified"),
                container, (ReceiptController rc) -> {
                    if (client != null) rc.setClientInfo(client);
                    rc.setReceiptData(items, total, paid, change, method, result.saleId, null, null, 
                            obs, result.rewardPromoCode, result.rewardAmount, result.rewardExpiryDate);
                });
    }

    private void handleSaleError(Throwable e) {
        if (e.getMessage() != null && e.getMessage().contains("OPERACION_BLOQUEADA")) {
            // El controlador principal mostrará la alerta visual de caja cerrada
            AlertUtil.showError(container.getBundle().getString("cart.cash_closed.title"), 
                    e.getMessage().replace("OPERACION_BLOQUEADA: ", ""));
        } else {
            AlertUtil.showError(container.getBundle().getString("cart.payment.error.process"), e.getMessage());
        }
    }
}




