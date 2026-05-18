package com.mycompany.ventacontrolfx.shared.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class QrGenerator {

    /**
     * Genera un código QR a partir de un texto y lo devuelve como arreglo de bytes (PNG).
     * También guarda una copia física en formato PNG en la carpeta 'qr_codes' del ordenador
     * para permitir el acceso directo del usuario.
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
                byte[] qrBytes = pngOutputStream.toByteArray();
                
                // Guardar copia física en el ordenador para acceso fácil
                saveQrToLocalDisk(qrBytes, text);
                
                return qrBytes;
            }
        } catch (WriterException | IOException e) {
            System.err.println("Error al generar el Código QR: " + e.getMessage());
            return null;
        }
    }

    /**
     * Guarda la imagen del código QR como un archivo físico en la raíz del proyecto.
     */
    private static void saveQrToLocalDisk(byte[] qrBytes, String text) {
        try {
            // Intentar extraer el número de serie/referencia del ticket si es un QR de la AEAT
            String filename = "qr_code_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            if (text != null && text.contains("numserie=")) {
                String[] parts = text.split("numserie=");
                if (parts.length > 1) {
                    String ref = parts[1].split("&")[0];
                    // Sanitizar caracteres no válidos para el nombre de archivo
                    ref = ref.replaceAll("[^a-zA-Z0-9\\-_]", "_");
                    filename = "qr_ticket_" + ref;
                }
            }

            // Ruta de guardado en el ordenador (raíz del proyecto)
            String projectDir = System.getProperty("user.dir");
            String qrDir = projectDir + File.separator + "qr_codes";
            File dir = new File(qrDir);
            
            // Crear el directorio si no existe
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File qrFile = new File(dir, filename + ".png");
            try (FileOutputStream fos = new FileOutputStream(qrFile)) {
                fos.write(qrBytes);
            }

            // Log en consola para indicar la ubicación exacta al usuario
            System.out.println("\n--- [NUEVO CÓDIGO QR GUARDADO FÍSIGAMENTE] ---");
            System.out.println("Contenido/URL: " + text);
            System.out.println("Ubicación en disco: " + qrFile.getAbsolutePath());
            System.out.println("----------------------------------------------\n");
        } catch (Exception e) {
            System.err.println("Error al guardar copia local del código QR: " + e.getMessage());
        }
    }
}

