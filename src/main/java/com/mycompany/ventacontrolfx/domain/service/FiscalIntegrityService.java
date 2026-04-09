package com.mycompany.ventacontrolfx.domain.service;

import com.mycompany.ventacontrolfx.domain.model.FiscalDocument;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Servicio de Dominio para la integridad fiscal de documentos.
 * Calcula y verifica el hash de control (SHA-256) que garantiza
 * que los datos del documento no han sido manipulados externamente.
 *
 * Clean Architecture â€” Capa de Dominio (no tiene dependencias de
 * infraestructura).
 */
public class FiscalIntegrityService {

    /**
     * Clave secreta del sistema. En producciÃ³n, debe cargarse desde
     * system_config encriptado o una variable de entorno.
     */
    private static final String SECRET = "VENTA_CTRL_FX_2026";

    /**
     * Calcula el hash SHA-256 para el documento y lo asigna al campo controlHash.
     * Debe llamarse justo antes de persistir el documento.
     */
    public void stampHash(FiscalDocument doc) {
        String raw = buildRaw(doc);
        doc.setControlHash(sha256(raw));
    }

    /**
     * Verifica que el hash del documento no ha sido alterado.
     * 
     * @return true si el documento es Ã­ntegro, false si fue manipulado.
     */
    public boolean verify(FiscalDocument doc) {
        if (doc.getControlHash() == null || doc.getControlHash().isBlank()) {
            return false;
        }
        String expected = sha256(buildRaw(doc));
        return expected.equals(doc.getControlHash());
    }

    // â”€â”€ private helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private String buildRaw(FiscalDocument doc) {
        return doc.getSaleId()
                + "|" + doc.getDocSeries()
                + "|" + doc.getDocNumber()
                + "|" + doc.getTotalAmount()
                + "|" + (doc.getIssuedAt() != null ? doc.getIssuedAt().toString() : "")
                + "|" + SECRET;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
