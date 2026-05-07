package com.mycompany.ventacontrolfx.presentation.controller.receipt;

import javafx.print.*;
import javafx.scene.Node;
import javafx.scene.transform.Scale;

public class PrinterManager {

    public boolean print(Node node, Printer printer, boolean isA4) {
        if (printer == null) return false;

        PrinterJob job = PrinterJob.createPrinterJob(printer);
        if (job != null) {
            PageLayout layout;
            if (isA4) {
                layout = printer.createPageLayout(Paper.A4, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
            } else {
                // Formato ticket (80mm aprox)
                layout = printer.createPageLayout(Paper.NA_LETTER, PageOrientation.PORTRAIT, 0, 0, 0, 0);
            }

            double scaleX = layout.getPrintableWidth() / node.getBoundsInParent().getWidth();
            double scaleY = scaleX;
            node.getTransforms().add(new Scale(scaleX, scaleY));

            boolean success = job.printPage(layout, node);
            if (success) {
                job.endJob();
            }
            node.getTransforms().clear();
            return success;
        }
        return false;
    }
}

