package com.mycompany.ventacontrolfx.presentation.controller.receipt;

import com.mycompany.ventacontrolfx.application.usecase.QueryFiscalDocumentUseCase;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.shared.util.QrGenerator;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.TextAlignment;
import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Gestor de la sección fiscal (VeriFactu) del recibo.
 */
public class ReceiptFiscalManager {

    private final ServiceContainer container;

    public ReceiptFiscalManager(ServiceContainer container) {
        this.container = container;
    }

    public void renderVerifactuSection(VBox receiptContent, int saleId, SaleConfig cfg) {
        try {
            QueryFiscalDocumentUseCase queryUseCase = container.getQueryFiscalDocumentUseCase();
            QueryFiscalDocumentUseCase.PrintData data = queryUseCase.getDataForReprint(saleId);
            
            if (data == null || data.document == null || data.document.getControlHash() == null) return;

            VBox fiscalBox = new VBox(5);
            fiscalBox.setAlignment(Pos.CENTER);
            fiscalBox.setStyle("-fx-padding: 25 0 10 0; -fx-background-color: white;");
            
            Line separator = new Line(0, 0, 180, 0);
            separator.setStroke(Color.web("#DDDDDD"));
            separator.getStrokeDashArray().addAll(2.0, 2.0);
            
            Label lblBrand = new Label("SISTEMA VERI*FACTU");
            lblBrand.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #333333; -fx-padding: 5 0 0 0;");
            
            Label lblLegal = new Label("Factura verificable en la sede electr\u00f3nica de la AEAT");
            lblLegal.setStyle("-fx-font-size: 9px; -fx-text-fill: #666666; -fx-alignment: center;");
            lblLegal.setWrapText(true); lblLegal.setTextAlignment(TextAlignment.CENTER); lblLegal.setMaxWidth(220);
            
            String aeatUrl = buildAeatUrl(data, cfg);
            byte[] qrBytes = QrGenerator.generateQrCode(aeatUrl, 300, 300);
            
            if (qrBytes != null) {
                ImageView imgView = new ImageView(new Image(new ByteArrayInputStream(qrBytes)));
                imgView.setFitWidth(140); imgView.setFitHeight(140);
                VBox qrFrame = new VBox(imgView);
                qrFrame.setAlignment(Pos.CENTER);
                qrFrame.setStyle("-fx-padding: 8; -fx-border-color: #EEEEEE; -fx-border-width: 1; -fx-background-color: white;");
                fiscalBox.getChildren().addAll(separator, lblBrand, lblLegal, qrFrame);
            }
            
            String h = data.document.getControlHash();
            String formattedHash = h.length() > 32 ? h.substring(0, 32) + "\n" + h.substring(32) : h;
            Label lblHash = new Label("Huella: " + formattedHash);
            lblHash.setStyle("-fx-font-size: 7px; -fx-text-fill: #999999; -fx-alignment: center; -fx-font-family: monospace;");
            lblHash.setWrapText(true); lblHash.setTextAlignment(TextAlignment.CENTER); lblHash.setMaxWidth(260);
            
            fiscalBox.getChildren().add(lblHash);
            receiptContent.getChildren().add(fiscalBox);
            
        } catch (Exception ex) {
            System.err.println("Error renderizando Verifactu: " + ex.getMessage());
        }
    }

    private String buildAeatUrl(QueryFiscalDocumentUseCase.PrintData data, SaleConfig cfg) {
        String nif = (data.document.getIssuerTaxId() != null) ? data.document.getIssuerTaxId() : cfg.getCif();
        return "https://prewww1.aeat.es/wlpl/TIKE-CONT/ValidarQR?nif=" 
               + (nif != null ? nif.toUpperCase() : "")
               + "&numserie=" + data.document.getFullReference()
               + "&fecha=" + data.document.getIssuedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
               + "&importe=" + String.format(Locale.US, "%.2f", Math.abs(data.document.getTotalAmount()));
    }
}


