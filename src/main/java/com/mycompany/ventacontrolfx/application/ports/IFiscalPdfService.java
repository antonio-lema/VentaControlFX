package com.mycompany.ventacontrolfx.application.ports;

import com.mycompany.ventacontrolfx.application.usecase.QueryFiscalDocumentUseCase.PrintData;
import java.io.IOException;

/**
 * Puerto para la generaciÃ³n de documentos fiscales en formato PDF.
 * Permite la automatizaciÃ³n y exportaciÃ³n masiva.
 */
public interface IFiscalPdfService {
    
    /**
     * Genera un PDF a partir de los datos de impresiÃ³n y lo guarda en la ruta especificada.
     * @param data Datos agregados del documento fiscal.
     * @param outputPath Ruta absoluta donde se guardarÃ¡ el archivo.
     * @throws IOException Si hay errores de escritura.
     */
    void generateInvoicePdf(PrintData data, String outputPath) throws IOException;
    
    /**
     * Genera un PDF y devuelve los bytes (Ãºtil para previsualizaciÃ³n o envÃ­o por email).
     */
    byte[] generateInvoicePdfBytes(PrintData data) throws IOException;
}
