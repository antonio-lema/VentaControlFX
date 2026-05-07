package com.mycompany.ventacontrolfx.presentation.controller.receipt;

import com.mycompany.ventacontrolfx.application.usecase.SaleUseCase;
import com.mycompany.ventacontrolfx.domain.model.*;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.PrintPreviewController;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.ReturnDialogController;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.CorrectionDialogController;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import com.mycompany.ventacontrolfx.presentation.navigation.ModalService;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

/**
 * Gestiona las acciones complejas del historial (Devoluciones, Impresión, AEAT).
 */
public class HistoryActionManager {

    private final ServiceContainer container;
    private final SaleUseCase saleUseCase;

    public HistoryActionManager(ServiceContainer container, SaleUseCase saleUseCase) {
        this.container = container;
        this.saleUseCase = saleUseCase;
    }

    public void handlePrintTicket(Sale selected) {
        if (selected == null) {
            AlertUtil.showWarning(container.getBundle().getString("alert.warning"),
                    container.getBundle().getString("history.error.no_selection"));
            return;
        }

        try {
            // Garantizar detalles
            if (selected.getDetails() == null || selected.getDetails().isEmpty()) {
                Sale fullSale = saleUseCase.getSaleDetails(selected.getSaleId());
                if (fullSale != null) {
                    selected.setDetails(fullSale.getDetails());
                    if (selected.getClientId() == null) selected.setClientId(fullSale.getClientId());
                }
            }

            if (selected.getDetails() == null || selected.getDetails().isEmpty()) {
                AlertUtil.showWarning(container.getBundle().getString("alert.warning"),
                        container.getBundle().getString("history.error.no_details"));
                return;
            }

            ModalService.showStandardModal("/view/receipt/print_preview.fxml",
                    selected.getClientId() != null ? container.getBundle().getString("receipt.title.invoice")
                            : container.getBundle().getString("receipt.title.simplified"),
                    container,
                    (PrintPreviewController ppc) -> {
                        try {
                            List<CartItem> cartItems = new ArrayList<>();
                            for (SaleDetail detail : selected.getDetails()) {
                                Product p = new Product();
                                p.setName(detail.getProductName());
                                p.setPrice(detail.getUnitPrice());
                                p.setIva(detail.getIvaRate());
                                cartItems.add(new CartItem(p, detail.getQuantity()));
                            }

                            String fiscalNif = selected.getCustomerNifSnapshot();
                            if (fiscalNif != null && !fiscalNif.isEmpty()) {
                                Client virtualClient = new Client();
                                virtualClient.setName(selected.getCustomerNameSnapshot() != null ? selected.getCustomerNameSnapshot() : "Cliente");
                                virtualClient.setTaxId(fiscalNif);
                                if (selected.getClientId() != null) {
                                    Client realClient = container.getClientUseCase().getById(selected.getClientId());
                                    if (realClient != null) {
                                        virtualClient.setAddress(realClient.getAddress());
                                        virtualClient.setPostalCode(realClient.getPostalCode());
                                        virtualClient.setCity(realClient.getCity());
                                        virtualClient.setProvince(realClient.getProvince());
                                    }
                                }
                                ppc.setClientInfo(virtualClient);
                            } else if (selected.getClientId() != null) {
                                Client client = container.getClientUseCase().getById(selected.getClientId());
                                if (client != null) ppc.setClientInfo(client);
                            }

                            ppc.setReceiptData(cartItems, selected.getTotal(), selected.getTotal(), 0.0,
                                    selected.getPaymentMethod(), selected.getSaleId());

                        } catch (Exception e) {
                            AlertUtil.showError(container.getBundle().getString("alert.error"),
                                    container.getBundle().getString("history.error.load_preview"));
                        }
                    });
        } catch (SQLException e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("history.error.load_details") + ": " + e.getMessage());
        }
    }

    public void handleRegisterReturn(Sale selected, Runnable onSuccess) {
        if (selected == null) return;

        try {
            if (selected.getDetails() == null || selected.getDetails().isEmpty()) {
                Sale fullSale = saleUseCase.getSaleDetails(selected.getSaleId());
                if (fullSale != null) selected.setDetails(fullSale.getDetails());
            }

            if (selected.getDetails() == null || selected.getDetails().isEmpty()) {
                AlertUtil.showWarning(container.getBundle().getString("alert.warning"),
                        container.getBundle().getString("history.error.already_returned_all"));
                return;
            }

            // Validar efectivo en caja para la devolución
            double maxRefundable = selected.getDetails().stream()
                    .mapToDouble(d -> (d.getQuantity() - d.getReturnedQuantity()) * (d.getLineTotal() / d.getQuantity()))
                    .sum();

            if (maxRefundable > 0) {
                double cashInDrawer = container.getClosureUseCase().getCurrentCashInDrawer();
                double currentGrossTotal = selected.getTotal() + selected.getDiscountAmount();
                double cashRatio = (currentGrossTotal > 0) ? selected.getCashAmount() / currentGrossTotal : 1.0;
                double maxCashRefundNeeded = maxRefundable * cashRatio;

                if (maxCashRefundNeeded > cashInDrawer) {
                    String oldTicketHint = selected.getClosureId() != null ? "\n\n\ud83d\udca1 *Recuerda*: Este ticket es de una sesi\u00f3n antigua." : "";
                    boolean continuar = AlertUtil.showConfirmation(
                            "\u00e2\u0161\u00a0\u00ef\u00b8\u008f Efectivo limitado",
                            "Efectivo insuficiente para la parte de met\u00e1lico",
                            String.format("Efectivo en caja (%.2f \u20ac) < Devolución estimada (%.2f \u20ac).%s\n\ubfdeseas continuar?",
                                    cashInDrawer, maxCashRefundNeeded, oldTicketHint));
                    if (!continuar) return;
                }
            }

            ModalService.showModal("/view/dialog/return_dialog.fxml",
                    container.getBundle().getString("history.btn.register_return"), Modality.APPLICATION_MODAL,
                    StageStyle.TRANSPARENT, container, (ReturnDialogController controller) -> {
                        controller.init(selected, container);
                        controller.setOnSuccess((reason, items) -> {
                            try {
                                int userId = container.getUserSession().getCurrentUser().getUserId();
                                saleUseCase.registerPartialReturn(selected.getSaleId(), items, reason, userId);
                                AlertUtil.showInfo(container.getBundle().getString("alert.success"),
                                        container.getBundle().getString("history.success.return"));
                                onSuccess.run();
                            } catch (SQLException e) {
                                AlertUtil.showError("Error", "No se pudo procesar la devoluci\u00f3n: " + e.getMessage());
                            }
                        });
                    });
        } catch (SQLException e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"), "Error al cargar detalles: " + e.getMessage());
        }
    }

    public void handleCorrection(Sale selected, Runnable onSuccess) {
        if (selected == null) return;
        if (selected.getControlHash() == null) {
            AlertUtil.showWarning("Operación no permitida", "Solo se pueden subsanar tickets registrados fiscalmente.");
            return;
        }

        ModalService.showModal("/view/dialog/correction_dialog.fxml",
                "Subsanar Datos Fiscales", Modality.APPLICATION_MODAL,
                StageStyle.TRANSPARENT, container, (CorrectionDialogController controller) -> {
                    controller.init(selected);
                    controller.setOnSuccess((newName, newNif) -> {
                        try {
                            saleUseCase.registerCorrection(selected.getSaleId(), newName, newNif);
                            AlertUtil.showInfo("Corrección registrada", "Los datos han sido corregidos y se reenviarán.");
                            onSuccess.run();
                        } catch (SQLException e) {
                            AlertUtil.showError("Error", "No se pudo registrar la corrección: " + e.getMessage());
                        }
                    });
                });
    }

    public void handleResendAeat(Sale selected, Runnable onSuccess) {
        if (selected == null) return;

        container.getAsyncManager().runAsyncTask(() -> {
            try {
                return saleUseCase.resendToAeat(selected.getSaleId());
            } catch (Exception e) {
                return e.getMessage();
            }
        }, result -> {
            if ("OK".equals(result) || "SOLICITADO".equals(result)) {
                AlertUtil.showInfo("Éxito", "El ticket ha sido procesado o puesto en cola.");
                onSuccess.run();
            } else {
                AlertUtil.showError("Error AEAT", "No se pudo completar el envío: " + result);
                onSuccess.run();
            }
        }, null);
    }
}



