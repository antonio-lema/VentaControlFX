package com.mycompany.ventacontrolfx.presentation.controller.closure;

import com.mycompany.ventacontrolfx.domain.model.CashClosure;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import java.util.List;

/**
 * Gestor de Acciones para Cierres de Caja.
 * Maneja auditorías, revisiones y ediciones.
 */
public class ClosureActionManager {

    private final ServiceContainer container;

    public ClosureActionManager(ServiceContainer container) {
        this.container = container;
    }

    public void markAsReviewed(CashClosure closure, Runnable onComplete) {
        if (closure == null) return;
        if (AlertUtil.showConfirmation("Revisar Arqueo", "¿Marcar el arqueo #" + closure.getClosureId() + " como revisado?", "")) {
            container.getAsyncManager().runAsyncTask(() -> {
                int reviewerId = container.getUserSession().getCurrentUser().getUserId();
                container.getClosureUseCase().markAsReviewed(closure.getClosureId(), reviewerId);
                return null;
            }, (res) -> onComplete.run(), (e) -> AlertUtil.showError("Error", e.getMessage()));
        }
    }

    public void editClosure(CashClosure closure, Runnable onComplete) {
        if (closure == null) return;
        
        com.mycompany.ventacontrolfx.presentation.controller.dialog.EditClosureDialogController ctrl = 
            com.mycompany.ventacontrolfx.presentation.navigation.ModalService.showTransparentModal(
                "/view/dialog/edit_closure_dialog.fxml",
                "Modificar Arqueo",
                container,
                (com.mycompany.ventacontrolfx.presentation.controller.dialog.EditClosureDialogController controller) -> {
                    controller.init(
                        container.getClosureUseCase(), 
                        container.getUserSession(), 
                        closure.getClosureId(), 
                        closure.getActualCash()
                    );
                }
            );

        if (ctrl != null && ctrl.isConfirmed()) {
            AlertUtil.showToast("El arqueo #" + closure.getClosureId() + " ha sido modificado.");
            if (onComplete != null) onComplete.run();
        }
    }

    public void markAllAsReviewed(List<CashClosure> closures, String filter, Runnable onComplete) {
        if (closures == null || closures.isEmpty()) return;
        if (AlertUtil.showConfirmation("Revisión Masiva", "¿Desea marcar todos los cierres visibles como revisados?", "Esta acción no se puede deshacer.")) {
            container.getAsyncManager().runAsyncTask(() -> {
                int reviewerId = container.getUserSession().getCurrentUser().getUserId();
                for (CashClosure c : closures) {
                    if (c.getReviewedBy() == null) container.getClosureUseCase().markAsReviewed(c.getClosureId(), reviewerId);
                }
                return null;
            }, (res) -> onComplete.run(), (e) -> AlertUtil.showError("Error", e.getMessage()));
        }
    }

    public void printAudit(CashClosure closure) {
        if (closure == null) return;
        
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Guardar Auditoría en PDF");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Archivos PDF (*.pdf)", "*.pdf"));
        fileChooser.setInitialFileName("Auditoria_Cierre_" + closure.getClosureId() + ".pdf");
        
        java.io.File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            container.getAsyncManager().runAsyncTask(() -> {
                try {
                    // Fetch movements for the audit
                    List<com.mycompany.ventacontrolfx.domain.repository.ICashClosureRepository.CashMovement> movements = 
                        container.getClosureUseCase().getMovementsByClosure(closure.getClosureId());

                    com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4);
                    com.lowagie.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(file));
                    document.open();
                    
                    com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 18, java.awt.Color.BLACK);
                    com.lowagie.text.Font subtitleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 14, java.awt.Color.DARK_GRAY);
                    com.lowagie.text.Font normalFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 10, java.awt.Color.BLACK);
                    com.lowagie.text.Font boldFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 10, java.awt.Color.BLACK);

                    com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("AUDITORÍA DE CIERRE #" + closure.getClosureId(), titleFont);
                    title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                    document.add(title);
                    document.add(new com.lowagie.text.Paragraph("\n"));
                    
                    // Resumen
                    document.add(new com.lowagie.text.Paragraph("Fecha de Creación: " + closure.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), normalFont));
                    document.add(new com.lowagie.text.Paragraph("Estado: " + closure.getStatus(), normalFont));
                    document.add(new com.lowagie.text.Paragraph("\nRESUMEN FINANCIERO", subtitleFont));
                    document.add(new com.lowagie.text.Paragraph("Fondo Inicial: " + String.format("%.2f \u20ac", closure.getInitialFund()), normalFont));
                    document.add(new com.lowagie.text.Paragraph("Efectivo Esperado: " + String.format("%.2f \u20ac", closure.getExpectedCash()), normalFont));
                    document.add(new com.lowagie.text.Paragraph("Efectivo Contado: " + String.format("%.2f \u20ac", closure.getActualCash()), boldFont));
                    document.add(new com.lowagie.text.Paragraph("Descuadre: " + String.format("%.2f \u20ac", closure.getDifference()), normalFont));
                    document.add(new com.lowagie.text.Paragraph("\n"));

                    // Tabla Movimientos
                    document.add(new com.lowagie.text.Paragraph("DETALLE DE MOVIMIENTOS", subtitleFont));
                    document.add(new com.lowagie.text.Paragraph("\n"));

                    if (movements != null && !movements.isEmpty()) {
                        com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(4);
                        table.setWidthPercentage(100);
                        table.setWidths(new float[]{1.5f, 1f, 3f, 1f});

                        String[] headers = {"Hora", "Tipo", "Motivo", "Importe"};
                        for (String h : headers) {
                            com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(h, boldFont));
                            cell.setBackgroundColor(new java.awt.Color(230, 230, 230));
                            cell.setPadding(5);
                            table.addCell(cell);
                        }

                        for (com.mycompany.ventacontrolfx.domain.repository.ICashClosureRepository.CashMovement mov : movements) {
                            table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(mov.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")), normalFont)));
                            table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(mov.getType(), normalFont)));
                            table.addCell(new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(mov.getReason() != null ? mov.getReason() : "", normalFont)));
                            
                            com.lowagie.text.pdf.PdfPCell amountCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(String.format("%.2f \u20ac", mov.getAmount()), normalFont));
                            amountCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
                            table.addCell(amountCell);
                        }
                        document.add(table);
                    } else {
                        document.add(new com.lowagie.text.Paragraph("No hay movimientos registrados para este cierre.", normalFont));
                    }
                    
                    document.close();
                    return true;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, (res) -> AlertUtil.showToast("Auditoría guardada exitosamente en PDF."), 
               (e) -> AlertUtil.showError("Error", "No se pudo generar el PDF: " + e.getMessage()));
        }
    }
}

