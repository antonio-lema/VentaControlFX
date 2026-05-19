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
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FiscalRenderer {
    private final ServiceContainer container;
    private final SaleConfig cfg;

    public FiscalRenderer(ServiceContainer container, SaleConfig cfg) {
        this.container = container;
        this.cfg = cfg;
    }

    public void renderFiscalBlock(VBox vBox, int saleId) {
        try {
            QueryFiscalDocumentUseCase query = this.container.getQueryFiscalDocumentUseCase();
            QueryFiscalDocumentUseCase.PrintData data = query.getDataForReprint(saleId);
            if (data == null || data.document == null) return;
            renderFiscalBlock(vBox, cfg.getCif(), data.document.getFullReference(), data.document.getIssuedAt(), data.document.getTotalAmount(), data.document.getControlHash());
        } catch (Exception e) {
            System.err.println("Error rendering fiscal block: " + e.getMessage());
        }
    }

    public void renderFiscalBlock(VBox vBox, String cif, String ref, LocalDateTime date, double total, String hash) {
        if (hash == null) return;
        VBox box = new VBox(5); box.setAlignment(Pos.CENTER); box.setStyle("-fx-padding: 5 0 10 0;");
        
        Label brand = new Label("SISTEMA VERI*FACTU"); brand.setStyle("-fx-font-weight: bold; -fx-font-size: 10px;");
        
        String nif = (cif != null) ? cif : cfg.getCif();
        String url = "https://prewww1.aeat.es/wlpl/TIKE-CONT/ValidarQR?nif=" + nif + "&numserie=" + ref + "&fecha=" + date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "&importe=" + String.format(Locale.US, "%.2f", Math.abs(total));
        
        byte[] qr = QrGenerator.generateQrCode(url, 200, 200);
        if (qr != null) {
            ImageView img = new ImageView(new Image(new ByteArrayInputStream(qr)));
            img.setFitWidth(100); img.setFitHeight(100);
            img.setCursor(javafx.scene.Cursor.HAND);
            img.setOnMouseClicked(event -> {
                try {
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                    }
                } catch (Exception ex) {
                    System.err.println("Error al abrir URL de AEAT: " + ex.getMessage());
                }
            });
            box.getChildren().addAll(brand, img);
        }
        
        Label lblHash = new Label("Huella: " + hash); lblHash.setStyle("-fx-font-size: 7px;"); lblHash.setWrapText(true);
        box.getChildren().add(lblHash);
        
        // Separador para dividir la sección Verifactu del resto de la cabecera
        javafx.scene.shape.Line separator = new javafx.scene.shape.Line(0, 0, 180, 0);
        separator.setStroke(javafx.scene.paint.Color.web("#DDDDDD"));
        separator.getStrokeDashArray().addAll(2.0, 2.0);
        box.getChildren().add(separator);
        
        // Encontrar la sección de información del ticket para posicionar el bloque Verifactu justo arriba de ella
        // (quedando de forma elegante debajo del logotipo, datos de empresa y datos del cliente si existen)
        javafx.scene.Node ticketInfo = vBox.lookup("#ticketInfoSection");
        int index = -1;
        if (ticketInfo != null) {
            index = vBox.getChildren().indexOf(ticketInfo);
        }
        if (index >= 0) {
            vBox.getChildren().add(index, box);
        } else {
            vBox.getChildren().add(box);
        }
    }
}


