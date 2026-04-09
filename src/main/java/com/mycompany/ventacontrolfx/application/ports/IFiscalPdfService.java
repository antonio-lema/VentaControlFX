package com.mycompany.ventacontrolfx.application.ports;

import com.mycompany.ventacontrolfx.application.usecase.QueryFiscalDocumentUseCase.PrintData;
import java.io.IOException;

/**
 * Puerto para la generaci\u00f3n de documentos fiscales en formato PDF.
 * Permite la automatizaci\u00f3n y exportaci\u00f3n masiva.
 */
public interface IFiscalPdfService {
    
    /**
     * Genera un PDF a partir de los datos de impresi\u00f3n y lo guarda en la ruta especificada.
     * @param data Datos agregados del documento fiscal.
     * @param outputPath Ruta absoluta donde se guardar\u00e1 el archivo.
     * @throws IOException Si hay errores de escritura.
     */
    void generateInvoicePdf(PrintData data, String outputPath) throws IOException;
    
    /**
     * Genera un PDF y devuelve los bytes (\u00fatil para previsualizaci\u00f3n o env\u00edo por email).
     */
    byte[] generateInvoicePdfBytes(PrintData data) throws IOException;
}
