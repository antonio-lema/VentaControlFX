package com.mycompany.ventacontrolfx.application.ports;

import com.mycompany.ventacontrolfx.application.usecase.QueryFiscalDocumentUseCase.PrintData;
import java.io.IOException;

/**
 * Puerto para la generación de documentos fiscales en formato PDF.
 * Permite la automatización y exportación masiva.
 */
public interface IFiscalPdfService {
    
    /**
     * Genera un PDF a partir de los datos de impresión y lo guarda en la ruta especificada.
     * @param data Datos agregados del documento fiscal.
     * @param outputPath Ruta absoluta donde se guardará el archivo.
     * @throws IOException Si hay errores de escritura.
     */
    void generateInvoicePdf(PrintData data, String outputPath) throws IOException;
    
    /**
     * Genera un PDF y devuelve los bytes (útil para previsualización o envío por email).
     */
    byte[] generateInvoicePdfBytes(PrintData data) throws IOException;
}
