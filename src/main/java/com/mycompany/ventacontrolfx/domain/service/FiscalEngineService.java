package com.mycompany.ventacontrolfx.domain.service;

import com.mycompany.ventacontrolfx.domain.model.Sale;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Motor fiscal para la gestión de VeriFactu.
 * Se encarga de generar la cadena de encadenamiento y calcular los hashes.
 */
public class FiscalEngineService {

    /**
     * Genera el hash de control para una venta basándose en sus datos y el hash anterior.
     * Según VeriFactu, el encadenamiento requiere datos del registro actual y el hash del anterior.
     */
    public String calculateChainingHash(Sale currentSale, String previousRecordHash) {
        try {
            // Estructura simplificada para el encadenamiento:
            // NIF_EMISOR|NUM_SERIE|NUM_FACTURA|FECHA_E|HORA_E|TOTAL|PREV_HASH
            
            StringBuilder sb = new StringBuilder();
            sb.append(currentSale.getDocSeries()).append("|");
            sb.append(currentSale.getDocNumber()).append("|");
            sb.append(currentSale.getSaleDateTime().toString()).append("|");
            sb.append(String.format("%.2f", currentSale.getTotal())).append("|");
            sb.append(previousRecordHash != null ? previousRecordHash : "");

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al calcular hash fiscal: SHA-256 no disponible", e);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
