package com.mycompany.ventacontrolfx.application.ports;

import com.mycompany.ventacontrolfx.application.usecase.QueryFiscalDocumentUseCase.PrintData;
import java.io.IOException;

/**
 * Puerto para la generaci\u00c3\u00b3n de documentos fiscales en formato PDF.
 * Permite la automatizaci\u00c3\u00b3n y exportaci\u00c3\u00b3n masiva.
 */
public interface IFiscalPdfService {
    
    /**
     * Genera un PDF a partir de los datos de impresi\u00c3\u00b3n y lo guarda en la ruta especificada.
     * @param data Datos agregados del documento fiscal.
     * @param outputPath Ruta absoluta donde se guardar\u00c3\u00a1 el archivo.
     * @throws IOException Si hay errores de escritura.
     */
    void generateInvoicePdf(PrintData data, String outputPath) throws IOException;
    
    /**
     * Genera un PDF y devuelve los bytes (\u00c3\u00batil para previsualizaci\u00c3\u00b3n o env\u00c3\u00ado por email).
     */
    byte[] generateInvoicePdfBytes(PrintData data) throws IOException;
}
