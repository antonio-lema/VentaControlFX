package com.mycompany.ventacontrolfx.presentation.controller.receipt;

import com.mycompany.ventacontrolfx.application.usecase.SaleUseCase;
import com.mycompany.ventacontrolfx.application.usecase.GetSaleTicketUseCase;
import com.mycompany.ventacontrolfx.domain.model.Return;
import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.PrintPreviewController;
import com.mycompany.ventacontrolfx.presentation.controller.receipt.TicketDetailController;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import com.mycompany.ventacontrolfx.presentation.navigation.ModalService;
import java.sql.SQLException;

/**
 * Gestiona las acciones de la lista de devoluciones (Reimpresión, Ver Ticket Original).
 */
public class ReturnActionManager {

    private final ServiceContainer container;
    private final SaleUseCase saleUseCase;
    private final GetSaleTicketUseCase getSaleTicketUseCase;

    public ReturnActionManager(ServiceContainer container, SaleUseCase saleUseCase, GetSaleTicketUseCase getSaleTicketUseCase) {
        this.container = container;
        this.saleUseCase = saleUseCase;
        this.getSaleTicketUseCase = getSaleTicketUseCase;
    }

    public void handleReprint(Return current) {
        if (current == null) {
            AlertUtil.showWarning(container.getBundle().getString("alert.warning"), container.getBundle().getString("returns.warning.select"));
            return;
        }

        try {
            if (current.getDetails() == null || current.getDetails().isEmpty()) {
                current.setDetails(saleUseCase.getReturnDetails(current.getReturnId()));
            }

            Sale originalSale = saleUseCase.getSaleDetails(current.getSaleId());

            ModalService.showStandardModal("/view/receipt/print_preview.fxml",
                    container.getBundle().getString("returns.modal.rectificativa_title"), container,
                    (PrintPreviewController ppc) -> ppc.setReturnData(current, originalSale, current.getDetails()));

        } catch (SQLException e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("returns.error.details") + ": " + e.getMessage());
        }
    }

    public void handleViewOriginalTicket(int saleId) {
        try {
            Sale sale = getSaleTicketUseCase.execute(saleId);
            if (sale == null) return;

            ModalService.showTransparentModal("/view/receipt/view_ticket_modal.fxml",
                    container.getBundle().getString("returns.modal.ticket_title") + saleId, container,
                    (TicketDetailController controller) -> controller.setSale(sale));
        } catch (SQLException e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("returns.error.ticket"));
        }
    }
}




