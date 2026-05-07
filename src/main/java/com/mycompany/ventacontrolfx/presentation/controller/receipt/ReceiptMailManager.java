package com.mycompany.ventacontrolfx.presentation.controller.receipt;

import com.mycompany.ventacontrolfx.application.ports.IFiscalPdfService;
import com.mycompany.ventacontrolfx.application.usecase.QueryFiscalDocumentUseCase;
import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.domain.repository.IEmailSender;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import java.util.function.Consumer;

/**
 * Gestor de envío de facturas por correo electrónico.
 */
public class ReceiptMailManager {

    private final ServiceContainer container;

    public ReceiptMailManager(ServiceContainer container) {
        this.container = container;
    }

    public void sendInvoiceEmail(Client client, int saleId, SaleConfig cfg, Runnable onSuccess, Consumer<Throwable> onError) {
        if (client == null || client.getEmail() == null || client.getEmail().isEmpty()) {
            AlertUtil.showWarning(container.getBundle().getString("alert.warning"), container.getBundle().getString("receipt.error.no_email"));
            return;
        }

        container.getAsyncManager().runAsyncTask(() -> {
            QueryFiscalDocumentUseCase queryUseCase = container.getQueryFiscalDocumentUseCase();
            QueryFiscalDocumentUseCase.PrintData data = queryUseCase.getDataForReprint(saleId);
            if (cfg != null) data.logoPath = cfg.getLogoPath();

            IFiscalPdfService pdfService = container.getPdfService();
            byte[] pdfBytes = pdfService.generateInvoicePdfBytes(data);

            IEmailSender emailSender = container.getEmailSender();
            String subject = "Su factura de " + (cfg != null ? cfg.getCompanyName() : "GestionTPV");
            String body = "Estimado/a " + client.getName() + ",\n\nAdjuntamos la factura correspondiente a su compra #" + saleId + ".\n\nGracias por su confianza.";
            
            emailSender.sendWithAttachment(client.getEmail(), subject, body, pdfBytes, "Factura_" + saleId + ".pdf");
            return true;
        }, result -> {
            AlertUtil.showToast(container.getBundle().getString("receipt.success.email_sent") + ": " + client.getEmail());
            onSuccess.run();
        }, onError);
    }
}

