package com.mycompany.ventacontrolfx.infrastructure.external.aeat;

import java.util.List;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

/**
 * Constructor de las peticiones XML (SOAP Envelope) para Alta y Anulación de
 * registros VERI*FACTU.
 * Trabaja nativamente usando StringBuilder para un rendimiento ultra-rápido de
 * forma thread-safe.
 */
public class VerifactuXmlBuilder {

    private static final String NAMESPACE_SF = "https://www2.agenciatributaria.gob.es/static_files/common/internet/dep/aplicaciones/es/aeat/tike/cont/ws/SuministroInformacion.xsd";
    private static final String NAMESPACE_SFLR = "https://www2.agenciatributaria.gob.es/static_files/common/internet/dep/aplicaciones/es/aeat/tike/cont/ws/SuministroLR.xsd";

    private String nifObligado;
    private String razonSocialObligado;

    public VerifactuXmlBuilder(String nifObligado, String razonSocialObligado) {
        this.nifObligado = nifObligado;
        this.razonSocialObligado = razonSocialObligado;
    }

    public void setCredentials(String nif, String razonSocial) {
        this.nifObligado = nif;
        this.razonSocialObligado = razonSocial;
    }

    public String getNifObligado() {
        return nifObligado;
    }

    public String getRazonSocialObligado() {
        return razonSocialObligado;
    }

    /**
     * Construye un envoltorio de ALTA (AltaRegistroFacturacion) para el listado de
     * ventas.
     */
    public String buildAltaSoapMessage(List<VerifactuPayload> payloads) {
        return buildEnvelope("RegFactuSistemaFacturacion", payloads);
    }

    /**
     * Construye un envoltorio de BAJA/ANULACION
     * (AnulacionRegFactuSistemaFacturacion) para listado de cancelaciones.
     */
    public String buildAnulacionSoapMessage(List<VerifactuPayload> payloads) {
        return buildEnvelope("AnulacionRegFactuSistemaFacturacion", payloads);
    }

    private String buildEnvelope(String rootNode, List<VerifactuPayload> payloads) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
        xml.append("xmlns:sf=\"").append(NAMESPACE_SF).append("\" ");
        xml.append("xmlns:sfLR=\"").append(NAMESPACE_SFLR).append("\">\n");
        xml.append("  <soapenv:Header/>\n");
        xml.append("  <soapenv:Body>\n");
        xml.append("    <sfLR:").append(rootNode).append(">\n");

        // --- Cabecera Común ---
        xml.append("      <sfLR:Cabecera>\n");
        xml.append("        <sf:ObligadoEmision>\n");
        xml.append("          <sf:NombreRazon>").append(escapeXml(razonSocialObligado)).append("</sf:NombreRazon>\n");
        xml.append("          <sf:NIF>").append(escapeXml(nifObligado)).append("</sf:NIF>\n");
        xml.append("        </sf:ObligadoEmision>\n");
        xml.append("      </sfLR:Cabecera>\n");

        // --- Iterar Registros ---
        for (VerifactuPayload payload : payloads) {
            boolean isAnulacion = payload.isAnulacion();
            xml.append("      <sfLR:RegistroFactura>\n");
            xml.append("        <sf:").append(isAnulacion ? "RegistroAnulacion" : "RegistroAlta").append(">\n");
            xml.append("          <sf:IDVersion>1.0</sf:IDVersion>\n");

            // IDFactura
            xml.append("          <sf:IDFactura>\n");
            xml.append("            <sf:").append(isAnulacion ? "IDEmisorFacturaAnulada" : "IDEmisorFactura")
                    .append(">").append(escapeXml(payload.getNifEmisor()))
                    .append("</sf:").append(isAnulacion ? "IDEmisorFacturaAnulada" : "IDEmisorFactura").append(">\n");
            xml.append("            <sf:").append(isAnulacion ? "NumSerieFacturaAnulada" : "NumSerieFactura")
                    .append(">").append(escapeXml(payload.getNumSerieFactura()))
                    .append("</sf:").append(isAnulacion ? "NumSerieFacturaAnulada" : "NumSerieFactura").append(">\n");
            xml.append("            <sf:")
                    .append(isAnulacion ? "FechaExpedicionFacturaAnulada" : "FechaExpedicionFactura").append(">")
                    .append(escapeXml(payload.getFechaExpedicion()))
                    .append("</sf:").append(isAnulacion ? "FechaExpedicionFacturaAnulada" : "FechaExpedicionFactura")
                    .append(">\n");
            xml.append("          </sf:IDFactura>\n");
            
            
            if (!isAnulacion) {
                xml.append("          <sf:NombreRazonEmisor>").append(escapeXml(razonSocialObligado))
                        .append("</sf:NombreRazonEmisor>\n");

                xml.append("          <sf:TipoFactura>").append(escapeXml(payload.getTipoFactura()))
                        .append("</sf:TipoFactura>\n");

                // Tipo de Rectificativa (Si aplica)
                if (payload.isRectificativa()) {
                    xml.append("          <sf:TipoRectificativa>I</sf:TipoRectificativa>\n");
                    xml.append("          <sf:FacturasRectificadas>\n");
                    xml.append("            <sf:IDFacturaRectificada>\n");
                    xml.append("              <sf:IDEmisorFactura>").append(escapeXml(payload.getNifEmisor()))
                            .append("</sf:IDEmisorFactura>\n");
                    xml.append("              <sf:NumSerieFactura>").append(escapeXml(payload.getOriginalNumSerie()))
                            .append("</sf:NumSerieFactura>\n");
                    xml.append("              <sf:FechaExpedicionFactura>")
                            .append(escapeXml(payload.getOriginalFechaExp()))
                            .append("</sf:FechaExpedicionFactura>\n");
                    xml.append("            </sf:IDFacturaRectificada>\n");
                    xml.append("          </sf:FacturasRectificadas>\n");
                }

                xml.append("          <sf:DescripcionOperacion>Ventas de mostrador TPV</sf:DescripcionOperacion>\n");
                xml.append("          <sf:FacturaSimplificadaArt7273>N</sf:FacturaSimplificadaArt7273>\n");

                boolean noCustomer = payload.getCustomerNif() == null || payload.getCustomerNif().isEmpty();
                xml.append("          <sf:FacturaSinIdentifDestinatarioArt61d>").append(noCustomer ? "S" : "N")
                        .append("</sf:FacturaSinIdentifDestinatarioArt61d>\n");

                if (!noCustomer) {
                    xml.append("          <sf:Destinatarios>\n");
                    xml.append("            <sf:IDDestinatario>\n");
                    xml.append("              <sf:NombreRazon>").append(escapeXml(payload.getCustomerName()))
                            .append("</sf:NombreRazon>\n");
                    xml.append("              <sf:NIF>").append(escapeXml(payload.getCustomerNif()))
                            .append("</sf:NIF>\n");
                    xml.append("            </sf:IDDestinatario>\n");
                    xml.append("          </sf:Destinatarios>\n");
                }

                xml.append("          <sf:Desglose>\n");
                java.util.Map<Double, Double[]> breakdown = payload.getVatBreakdown();
                if (breakdown != null && !breakdown.isEmpty()) {
                    for (java.util.Map.Entry<Double, Double[]> entry : breakdown.entrySet()) {
                        double rate = entry.getKey();
                        double lineBase = entry.getValue()[0];
                        double lineCuota = entry.getValue()[1];
                        xml.append("            <sf:DetalleDesglose>\n");
                        xml.append("              <sf:Impuesto>01</sf:Impuesto>\n");
                        xml.append("              <sf:ClaveRegimen>01</sf:ClaveRegimen>\n");
                        xml.append("              <sf:CalificacionOperacion>S1</sf:CalificacionOperacion>\n");
                        xml.append("              <sf:TipoImpositivo>")
                                .append(String.format(java.util.Locale.US, "%.2f", rate))
                                .append("</sf:TipoImpositivo>\n");
                        xml.append("              <sf:BaseImponibleOimporteNoSujeto>")
                                .append(String.format(java.util.Locale.US, "%.2f", Math.abs(lineBase)))
                                .append("</sf:BaseImponibleOimporteNoSujeto>\n");
                        xml.append("              <sf:CuotaRepercutida>")
                                .append(String.format(java.util.Locale.US, "%.2f", Math.abs(lineCuota)))
                                .append("</sf:CuotaRepercutida>\n");
                        xml.append("            </sf:DetalleDesglose>\n");
                    }
                } else {
                    double base = payload.getTotalNet();
                    double cuota = payload.getTotalTax();
                    double rate = (base != 0) ? (Math.abs(cuota) / Math.abs(base)) * 100 : 21.0;
                    xml.append("            <sf:DetalleDesglose>\n");
                    xml.append("              <sf:Impuesto>01</sf:Impuesto>\n");
                    xml.append("              <sf:ClaveRegimen>01</sf:ClaveRegimen>\n");
                    xml.append("              <sf:CalificacionOperacion>S1</sf:CalificacionOperacion>\n");
                    xml.append("              <sf:TipoImpositivo>")
                            .append(String.format(java.util.Locale.US, "%.2f", rate)).append("</sf:TipoImpositivo>\n");
                    xml.append("              <sf:BaseImponibleOimporteNoSujeto>")
                            .append(String.format(java.util.Locale.US, "%.2f", Math.abs(base)))
                            .append("</sf:BaseImponibleOimporteNoSujeto>\n");
                    xml.append("              <sf:CuotaRepercutida>")
                            .append(String.format(java.util.Locale.US, "%.2f", Math.abs(cuota)))
                            .append("</sf:CuotaRepercutida>\n");
                    xml.append("            </sf:DetalleDesglose>\n");
                }
                xml.append("          </sf:Desglose>\n");
                xml.append("          <sf:CuotaTotal>")
                        .append(String.format(java.util.Locale.US, "%.2f", Math.abs(payload.getTotalTax())))
                        .append("</sf:CuotaTotal>\n");
                xml.append("          <sf:ImporteTotal>")
                        .append(String.format(java.util.Locale.US, "%.2f", Math.abs(payload.getImporteTotal())))
                        .append("</sf:ImporteTotal>\n");
                
            }



            // Encadenamiento (OBLIGATORIO para todos los registros)
            xml.append("          <sf:Encadenamiento>\n");
            if (payload.getPrevHash() != null && !payload.getPrevHash().isEmpty()) {
                xml.append("            <sf:RegistroAnterior>\n");
                xml.append("              <sf:IDEmisorFactura>").append(escapeXml(payload.getNifEmisor()))
                        .append("</sf:IDEmisorFactura>\n");
                xml.append("              <sf:NumSerieFactura>").append(escapeXml(payload.getPrevNumSerie()))
                        .append("</sf:NumSerieFactura>\n");
                xml.append("              <sf:FechaExpedicionFactura>")
                        .append(escapeXml(payload.getPrevFechaExpedicion())).append("</sf:FechaExpedicionFactura>\n");
                xml.append("              <sf:Huella>").append(escapeXml(payload.getPrevHash()).toUpperCase())
                        .append("</sf:Huella>\n");
                xml.append("            </sf:RegistroAnterior>\n");
            } else {
                xml.append("            <sf:PrimerRegistro>S</sf:PrimerRegistro>\n");
            }
            xml.append("          </sf:Encadenamiento>\n");


            // Sistema Informático
            xml.append("          <sf:SistemaInformatico>\n");
            xml.append("            <sf:NombreRazon>").append(escapeXml(razonSocialObligado))
                    .append("</sf:NombreRazon>\n");
            xml.append("            <sf:NIF>").append(escapeXml(nifObligado)).append("</sf:NIF>\n");
            xml.append("            <sf:NombreSistemaInformatico>VentaControlFX</sf:NombreSistemaInformatico>\n");
            xml.append("            <sf:IdSistemaInformatico>01</sf:IdSistemaInformatico>\n");
            xml.append("            <sf:Version>1.0</sf:Version>\n");
            xml.append("            <sf:NumeroInstalacion>01</sf:NumeroInstalacion>\n");
            xml.append("            <sf:TipoUsoPosibleSoloVerifactu>S</sf:TipoUsoPosibleSoloVerifactu>\n");
            xml.append("            <sf:TipoUsoPosibleMultiOT>S</sf:TipoUsoPosibleMultiOT>\n");
            xml.append("            <sf:IndicadorMultiplesOT>N</sf:IndicadorMultiplesOT>\n");
            xml.append("          </sf:SistemaInformatico>\n");

            String ahoraIso = payload.getGenTimestamp() != null && !payload.getGenTimestamp().isEmpty()
                    ? payload.getGenTimestamp()
                    : java.time.ZonedDateTime.now(java.time.ZoneId.of("Europe/Madrid")).minusMinutes(1)
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx"));

            xml.append("          <sf:FechaHoraHusoGenRegistro>").append(ahoraIso)
                    .append("</sf:FechaHoraHusoGenRegistro>\n");

            // Huella
            String cuotaStr = String.format(java.util.Locale.US, "%.2f", Math.abs(payload.getTotalTax()));
            String totalStr = String.format(java.util.Locale.US, "%.2f", Math.abs(payload.getImporteTotal()));
            StringBuilder sb = new StringBuilder();
            sb.append(isAnulacion ? "IDEmisorFacturaAnulada=" : "IDEmisorFactura=").append(payload.getNifEmisor());
            sb.append(isAnulacion ? "&NumSerieFacturaAnulada=" : "&NumSerieFactura=")
                    .append(payload.getNumSerieFactura());
            sb.append(isAnulacion ? "&FechaExpedicionFacturaAnulada=" : "&FechaExpedicionFactura=")
                    .append(payload.getFechaExpedicion());
            if (!isAnulacion) {
                sb.append("&TipoFactura=").append(payload.getTipoFactura());
                sb.append("&CuotaTotal=").append(cuotaStr);
                sb.append("&ImporteTotal=").append(totalStr);
            }
            sb.append("&Huella=").append(payload.getPrevHash() != null ? payload.getPrevHash().toUpperCase() : "");
            sb.append("&FechaHoraHusoGenRegistro=").append(ahoraIso);
            String huellaCalculada = sha256(sb.toString()).toUpperCase();

            xml.append("          <sf:TipoHuella>01</sf:TipoHuella>\n");
            xml.append("          <sf:Huella>").append(huellaCalculada).append("</sf:Huella>\n");

            xml.append("        </sf:").append(isAnulacion ? "RegistroAnulacion" : "RegistroAlta").append(">\n");
            xml.append("      </sfLR:RegistroFactura>\n");
        }

        xml.append("    </sfLR:").append(rootNode).append(">\n");
        xml.append("  </soapenv:Body>\n");
        xml.append("</soapenv:Envelope>");
        return xml.toString();
    }

    private String escapeXml(String input) {
        if (input == null)
            return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String sha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
