package com.mycompany.ventacontrolfx.infrastructure.external.pdf;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.mycompany.ventacontrolfx.application.ports.IFiscalPdfService;
import com.mycompany.ventacontrolfx.application.usecase.QueryFiscalDocumentUseCase.PrintData;
import com.mycompany.ventacontrolfx.domain.model.FiscalDocument;
import com.mycompany.ventacontrolfx.domain.model.SaleDetail;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class OpenPdfFiscalService implements IFiscalPdfService {

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
    private static final Font FONT_TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
    private static final Font FONT_HASH = FontFactory.getFont(FontFactory.COURIER, 7, Color.GRAY);

    @Override
    public void generateInvoicePdf(PrintData data, String outputPath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, fos);
            document.open();
            buildDocument(document, data);
            document.close();
        } catch (DocumentException e) {
            throw new IOException("Error building PDF document", e);
        }
    }

    @Override
    public byte[] generateInvoicePdfBytes(PrintData data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();
            buildDocument(document, data);
            document.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new IOException("Error building PDF document", e);
        }
    }

    private void buildDocument(Document document, PrintData data) throws DocumentException {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        boolean isInvoice = data.document.getDocType() == FiscalDocument.Type.FACTURA;

        // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac LOGOTIPO (Opcional)
        // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
        if (data.logoPath != null && !data.logoPath.trim().isEmpty()) {
            try {
                Image logo = Image.getInstance(data.logoPath);
                logo.scaleToFit(120, 60);
                logo.setAlignment(Element.ALIGN_LEFT);
                document.add(logo);
            } catch (Exception e) {
                // Si falla la carga del logo, simplemente no se a\u00f1ade
                System.err.println("No se pudo cargar el logotipo desde: " + data.logoPath);
            }
        }

        // ── CABECERA DE DOCUMENTO ──
        PdfPTable typeTable = new PdfPTable(1);
        typeTable.setWidthPercentage(100);

        String label;
        if (data.document.getDocType() == FiscalDocument.Type.FACTURA) {
            label = "FACTURA ORDINARIA";
        } else if (data.document.getDocType() == FiscalDocument.Type.RECTIFICATIVA) {
            label = "FACTURA RECTIFICATIVA (ABONO)";
        } else {
            label = "FACTURA SIMPLIFICADA (TICKET)";
        }

        PdfPCell typeCell = new PdfPCell(new Phrase(label, FONT_TITLE));
        typeCell.setBorder(com.lowagie.text.Rectangle.BOTTOM);
        typeCell.setBorderWidth(2f);
        typeCell.setPaddingBottom(10);
        typeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        typeTable.addCell(typeCell);
        document.add(typeTable);
        document.add(new Paragraph("\n"));

        // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac INFO EMISOR Y RECEPTOR
        // (DOS COLUMNAS) \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[] { 1f, 1f });

        // Columna Izquierda: Emisor
        PdfPCell issuerCol = new PdfPCell();
        issuerCol.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        issuerCol.addElement(new Paragraph("DATOS DEL EMISOR", FONT_BOLD));
        issuerCol.addElement(new Paragraph(data.document.getIssuerName(), FONT_SUBTITLE));
        issuerCol.addElement(new Paragraph("NIF/CIF: " + data.document.getIssuerTaxId(), FONT_BODY));
        issuerCol.addElement(new Paragraph(data.document.getIssuerAddress(), FONT_BODY));
        infoTable.addCell(issuerCol);

        // Columna Derecha: Datos de Facturaci\u00f3n
        PdfPCell metaCol = new PdfPCell();
        metaCol.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        metaCol.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPTable nestedMeta = new PdfPTable(2);
        nestedMeta.setWidthPercentage(100);
        addMetaLine(nestedMeta, "N\u00ba Documento:", data.document.getFullReference());
        addMetaLine(nestedMeta, "Fecha Emisi\u00f3n:", data.document.getIssuedAt().format(dtf));
        metaCol.addElement(nestedMeta);
        infoTable.addCell(metaCol);
        document.add(infoTable);
        document.add(new Paragraph("\n"));

        // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac DATOS DEL RECEPTOR
        // (S\u00f3lo si existe) \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
        if (data.document.getReceiverName() != null && !data.document.getReceiverName().trim().isEmpty()) {
            PdfPTable clientBox = new PdfPTable(1);
            clientBox.setWidthPercentage(100);
            PdfPCell cCell = new PdfPCell();
            cCell.setPadding(15);
            cCell.setBackgroundColor(new Color(245, 247, 250));
            cCell.setBorderColor(new Color(200, 210, 220));
            cCell.addElement(new Paragraph("DATOS DEL RECEPTOR (CLIENTE)", FONT_BOLD));
            cCell.addElement(new Paragraph(data.document.getReceiverName(), FONT_SUBTITLE));
            cCell.addElement(new Paragraph("NIF/CIF: " + data.document.getReceiverTaxId(), FONT_BODY));
            cCell.addElement(new Paragraph(data.document.getReceiverAddress(), FONT_BODY));
            clientBox.addCell(cCell);
            document.add(clientBox);
            document.add(new Paragraph("\n"));
        } else if (isInvoice) {
            // Si es factura pero no hay cliente, mostramos un aviso
            document.add(new Paragraph("RECEPTOR: CLIENTE GEN\u00c9RICO / CONTADO", FONT_BODY));
            document.add(new Paragraph("\n"));
        }

        document.add(new LineSeparator());
        document.add(new Paragraph("\n"));

        // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac L\u00cdNEAS DE DETALLE
        // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 4f, 1f, 1.5f, 1f, 1.5f });
        table.setHeaderRows(1);

        addTableHeader(table, "Producto / Descripci\u00f3n");
        addTableHeader(table, "Cant.");
        addTableHeader(table, "Precio Ud.");
        addTableHeader(table, "IVA %");
        addTableHeader(table, "Total");

        for (SaleDetail detail : data.lines) {
            table.addCell(createCell(detail.getProductName(), Element.ALIGN_LEFT));
            table.addCell(createCell(String.valueOf(detail.getQuantity()), Element.ALIGN_CENTER));
            table.addCell(createCell(String.format("%.2f \u20ac", detail.getUnitPrice()), Element.ALIGN_RIGHT));
            table.addCell(createCell(String.format("%.0f%%", detail.getIvaRate()), Element.ALIGN_CENTER));
            table.addCell(createCell(String.format("%.2f \u20ac", detail.getLineTotal()), Element.ALIGN_RIGHT));
        }
        document.add(table);

        // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac RESUMEN DE IMPORTES
        // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingBefore(20);
        summaryTable.setWidths(new float[] { 3f, 1f });

        // Columna Izquierda: Espacio o Notas
        PdfPCell notesCell = new PdfPCell(new Phrase("Observaciones: Gracias por su confianza.", FONT_SMALL));
        notesCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        notesCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        summaryTable.addCell(notesCell);

        // Columna Derecha: Totales
        PdfPCell totalsCol = new PdfPCell();
        totalsCol.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        PdfPTable nestedTotals = new PdfPTable(2);
        nestedTotals.setWidthPercentage(100);
        addTotalLine(nestedTotals, "Base Imponible:", String.format("%.2f \u20ac", data.document.getBaseAmount()));

        // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac DESGLOSE DE IVA DESDE
        // EL SNAPSHOT FISCAL (V2.0)
        // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
        if (data.sale.getTaxSummaries() != null && !data.sale.getTaxSummaries().isEmpty()) {
            for (com.mycompany.ventacontrolfx.domain.model.SaleTaxSummary summary : data.sale.getTaxSummaries()) {
                addTotalLine(nestedTotals, String.format("%s (%.1f%%):", summary.getTaxName(), summary.getTaxRate()),
                        String.format("%.2f \u20ac", summary.getTaxAmount()));
            }
        } else {
            // Fallback para ventas hist\u00f3ricas sin snapshot V2.0 (C\u00e1lculo
            // simplificado)
            java.util.Map<Double, Double[]> breakdown = new java.util.TreeMap<>();
            for (SaleDetail line : data.lines) {
                double rate = line.getIvaRate();
                Double[] vals = breakdown.getOrDefault(rate, new Double[] { 0.0, 0.0 });
                vals[0] += (line.getLineTotal() - line.getIvaAmount()); // Base
                vals[1] += line.getIvaAmount(); // Cuota
                breakdown.put(rate, vals);
            }
            for (java.util.Map.Entry<Double, Double[]> entry : breakdown.entrySet()) {
                addTotalLine(nestedTotals, String.format("IVA %.0f%%:", entry.getKey()),
                        String.format("%.2f \u20ac", entry.getValue()[1]));
            }
        }

        addTotalLine(nestedTotals, "Total Impuestos:", String.format("%.2f \u20ac", data.document.getVatAmount()));

        PdfPCell grandTotalLabel = new PdfPCell(new Phrase("TOTAL A PAGAR:", FONT_TOTAL));
        grandTotalLabel.setBorder(com.lowagie.text.Rectangle.TOP);
        grandTotalLabel.setPaddingTop(10);
        nestedTotals.addCell(grandTotalLabel);

        PdfPCell grandTotalValue = new PdfPCell(
                new Phrase(String.format("%.2f \u20ac", data.document.getTotalAmount()), FONT_TOTAL));
        grandTotalValue.setBorder(com.lowagie.text.Rectangle.TOP);
        grandTotalValue.setPaddingTop(10);
        grandTotalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        nestedTotals.addCell(grandTotalValue);

        totalsCol.addElement(nestedTotals);
        summaryTable.addCell(totalsCol);
        document.add(summaryTable);

        // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac CUP\u00d3N DE REGALO /
        // RECOMPENSA (V1.13) \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
        // System.out.println("[DEBUG] PDF Generation - Reward code: "
        //        + (data.sale != null ? data.sale.getRewardPromoCode() : "SALE_IS_NULL"));
        if (data.sale != null && data.sale.getRewardPromoCode() != null && !data.sale.getRewardPromoCode().isEmpty()) {
            document.add(new Paragraph("\n"));
            PdfPTable rewardBox = new PdfPTable(1);
            rewardBox.setWidthPercentage(100);
            PdfPCell rCell = new PdfPCell();
            rCell.setPadding(10);
            rCell.setBorderWidth(1.5f);
            rCell.setBorderColor(new Color(40, 167, 69)); // Verde éxito
            rCell.setBackgroundColor(new Color(232, 245, 233));

            Paragraph pTitle = new Paragraph("\u2728 \u00a1REGALO POR TU COMPRA! \u2728", FONT_BOLD);
            pTitle.setAlignment(Element.ALIGN_CENTER);
            rCell.addElement(pTitle);

            String expiry = "";
            if (data.sale.getSaleDateTime() != null) {
                java.time.format.DateTimeFormatter dtfReward = java.time.format.DateTimeFormatter
                        .ofPattern("dd/MM/yyyy");
                expiry = data.sale.getSaleDateTime().plusMonths(3).format(dtfReward);
            }

            Paragraph pMsg = new Paragraph(
                    "Disfruta de 10,00 \u20ac de descuento en tu pr\u00f3xima compra.\nV\u00e1lido hasta: " + expiry,
                    FONT_BODY);
            pMsg.setAlignment(Element.ALIGN_CENTER);
            rCell.addElement(pMsg);

            Paragraph pCode = new Paragraph("C\u00d3DIGO: " + data.sale.getRewardPromoCode(), FONT_TOTAL);
            pCode.setAlignment(Element.ALIGN_CENTER);
            rCell.addElement(pCode);

            rewardBox.addCell(rCell);
            document.add(rewardBox);
        }

        // ── PIE DE PÁGINA FISCAL PREMIUM (VERIFACTU) ──
        document.add(new Paragraph("\n\n"));
        
        PdfPTable fiscalTable = new PdfPTable(1);
        fiscalTable.setWidthPercentage(100);
        fiscalTable.setSpacingBefore(10);
        
        // Título de sistema
        PdfPCell titleCell = new PdfPCell(new Phrase("SISTEMA VERI*FACTU", FONT_BOLD));
        titleCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setPaddingBottom(2);
        fiscalTable.addCell(titleCell);

        // Texto obligatorio
        PdfPCell legalCell = new PdfPCell(new Phrase("Factura verificable en la sede electrónica de la AEAT", FONT_SMALL));
        legalCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        legalCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        legalCell.setPaddingBottom(10);
        fiscalTable.addCell(legalCell);

        // Generar QR (URL de AEAT Verifactu)
        String nif = (data.document.getIssuerTaxId() != null ? data.document.getIssuerTaxId().toUpperCase() : "");
        String fullRef = data.document.getFullReference();
        String fecha = data.document.getIssuedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String importe = String.format(java.util.Locale.US, "%.2f", Math.abs(data.document.getTotalAmount()));

        String aeatUrl = "https://prewww1.aeat.es/wlpl/TIKE-CONT/ValidarQR?nif=" + nif
                         + "&numserie=" + fullRef
                         + "&fecha=" + fecha
                         + "&importe=" + importe;
        
        byte[] qrBytes = null;
        try {
            qrBytes = com.mycompany.ventacontrolfx.shared.util.QrGenerator.generateQrCode(aeatUrl, 350, 350);
        } catch (Exception e) {
            System.err.println("Error generador QR PDF: " + e.getMessage());
        }

        if (qrBytes != null) {
            try {
                Image qrImage = Image.getInstance(qrBytes);
                qrImage.setAlignment(Element.ALIGN_CENTER);
                qrImage.scaleToFit(120, 120);
                
                PdfPCell qrCell = new PdfPCell(qrImage);
                qrCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                qrCell.setPadding(5);
                fiscalTable.addCell(qrCell);
            } catch (Exception e) {
                System.err.println("Error al incrustar QR en PDF: " + e.getMessage());
            }
        }

        // Huella / Hash
        String h = data.document.getControlHash();
        PdfPCell hashCell = new PdfPCell(new Phrase("Huella: " + h, FONT_HASH));
        hashCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        hashCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        hashCell.setPaddingTop(10);
        fiscalTable.addCell(hashCell);

        document.add(fiscalTable);
    }

    private void addMetaLine(PdfPTable table, String label, String value) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, FONT_SMALL));
        cellLabel.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        table.addCell(cellLabel);

        PdfPCell cellValue = new PdfPCell(new Phrase(value, FONT_BOLD));
        cellValue.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        cellValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cellValue);
    }

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BOLD));
        cell.setBackgroundColor(new Color(230, 230, 230));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        cell.setBorderWidth(1f);
        table.addCell(cell);
    }

    private PdfPCell createCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BODY));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(6);
        cell.setBorder(com.lowagie.text.Rectangle.BOTTOM);
        cell.setBorderColor(new Color(220, 220, 220));
        return cell;
    }

    private void addTotalLine(PdfPTable table, String label, String value) {
        PdfPCell cLabel = new PdfPCell(new Phrase(label, FONT_BODY));
        cLabel.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        cLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cLabel.setPadding(4);
        table.addCell(cLabel);

        PdfPCell cValue = new PdfPCell(new Phrase(value, FONT_BOLD));
        cValue.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        cValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cValue.setPadding(4);
        table.addCell(cValue);
    }
}



