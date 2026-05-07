package com.mycompany.ventacontrolfx.shared.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class QrGenerator {

    /**
     * Genera un código QR a partir de un texto y lo devuelve como arreglo de bytes (PNG).
     * 
     * @param text El texto a codificar en el QR.
     * @param width El ancho de la imagen.
     * @param height El alto de la imagen.
     * @return El ByteArray con la imagen PNG generada.
     */
    public static byte[] generateQrCode(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

            try (ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
                return pngOutputStream.toByteArray();
            }
        } catch (WriterException | IOException e) {
            System.err.println("Error al generar el Código QR: " + e.getMessage());
            return null;
        }
    }
}

