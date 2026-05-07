package com.mycompany.ventacontrolfx.infrastructure.external.aeat;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestor Asíncrono del "Buzón de Salida" (Outbox Pattern) para VERI*FACTU.
 */
public class VerifactuOutboxManager {

    private static final Logger LOGGER = Logger.getLogger(VerifactuOutboxManager.class.getName());

    private final AeatHttpClient httpClient;
    private final VerifactuXmlBuilder xmlBuilder;
    private final com.mycompany.ventacontrolfx.shared.bus.GlobalEventBus eventBus;
    private final ScheduledExecutorService scheduler;

    private int reqMinRegistros = 1;      
    private int reqSegundosEspera = 60;     
    private LocalDateTime ultimoEnvio = LocalDateTime.now().minusDays(1);
    
    private int consecutiveFailures = 0;
    private boolean isInternetDownDetected = false;

    private final JdbcVerifactuRepository outboxRepository = new JdbcVerifactuRepository();

    public VerifactuOutboxManager(AeatHttpClient httpClient, VerifactuXmlBuilder xmlBuilder, com.mycompany.ventacontrolfx.shared.bus.GlobalEventBus eventBus) {
        this.httpClient = httpClient;
        this.xmlBuilder = xmlBuilder;
        this.eventBus = eventBus;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "VerifactuOutbox-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        // Lanzamos la primera ejecución INMEDIATAMENTE (1s) para no esperar
        scheduler.schedule(this::procesarCola, 1, TimeUnit.SECONDS);
        LOGGER.fine("[VeriFactu] Outbox Manager arrancado (Modo Telemetría)");
    }

    public void updateCredentials(String nif, String razonSocial) {
        LOGGER.fine("[VeriFactu] Credenciales configuradas: " + nif + " / " + razonSocial);
        this.xmlBuilder.setCredentials(nif, razonSocial);
    }

    private void procesarCola() {
        int delaySiguienteVuelta = reqSegundosEspera; 
        
        try {
            // System.out.println(">>> [VeriFactu] INICIANDO CICLO DE SINCRONIZACIÓN...");
            
            String nifActual = xmlBuilder.getNifObligado();
            String razonActual = xmlBuilder.getRazonSocialObligado();
            
            // System.out.println("[VeriFactu] Usando NIF: " + nifActual);
            
            if (nifActual == null || nifActual.isEmpty()) {
                // LOGGER.fine("[VeriFactu] ADVERTENCIA: No hay NIF configurado aún. Saltando esta vuelta...");
                return;
            }

            List<VerifactuPayload> pendientes = outboxRepository.getPendingRecordsOrderByIdAsc(nifActual, razonActual);
            
            if (pendientes == null || pendientes.isEmpty()) {
                // System.out.println("[VeriFactu] No hay nada pendiente en la base de datos. Zzz...");
            } else {
                List<VerifactuPayload> batch = pendientes.size() > 1000 ? pendientes.subList(0, 1000) : pendientes;
                String soapRequest = xmlBuilder.buildAltaSoapMessage(batch);

                try {
                    if (eventBus != null) {
                        javafx.application.Platform.runLater(eventBus::publishVerifactuSyncStarted);
                    }
                    
                    String soapResponse = httpClient.sendSoapMessage(soapRequest);
                    
                    if (eventBus != null) {
                        javafx.application.Platform.runLater(() -> eventBus.publishVerifactuSyncFinished("OK"));
                    }
                    
                    if (isInternetDownDetected) {
                        notifyIncident(batch);
                        isInternetDownDetected = false;
                    }
                    consecutiveFailures = 0;

                    actualizarPoliticasDeFlujo(soapResponse);
                    procesarEstadosResponse(batch, soapResponse);
                    this.ultimoEnvio = LocalDateTime.now();
                    
                    // Hacienda ha podido actualizar reqSegundosEspera en actualizarPoliticasDeFlujo
                    delaySiguienteVuelta = reqSegundosEspera;
                } catch (Exception ex) {
                    if (eventBus != null) {
                        String errMsg = ex.getMessage();
                        javafx.application.Platform.runLater(() -> eventBus.publishVerifactuSyncFinished(errMsg));
                    }
                    consecutiveFailures++;
                    LOGGER.log(Level.WARNING, "[VeriFactu] Error de comunicación: {0}", ex.getMessage());
                    if (consecutiveFailures >= 3 && !isInternetDownDetected) {
                        isInternetDownDetected = true;
                    }
                    delaySiguienteVuelta = 30; 
                }
            }
        } catch (Throwable t) {
            System.err.println("[VeriFactu] ERROR CRÍTICO EN EL MOTOR: " + t.getMessage());
            t.printStackTrace();
        } finally {
            // System.out.println("<<< [VeriFactu] FIN DEL CICLO. Próxima vuelta en " + delaySiguienteVuelta + " segundos.");
            scheduler.schedule(this::procesarCola, delaySiguienteVuelta, TimeUnit.SECONDS);
        }
    }

    private void notifyIncident(List<VerifactuPayload> batch) {
        List<Integer> saleIds = new ArrayList<>();
        List<Integer> returnIds = new ArrayList<>();
        for (VerifactuPayload p : batch) {
            String[] parts = p.getIdRegistro().split("-");
            int id = Integer.parseInt(parts[1]);
            if ("ALTA".equals(parts[0])) saleIds.add(id);
            else returnIds.add(id);
        }
        if (eventBus != null) {
            javafx.application.Platform.runLater(() -> {
                eventBus.publishVerifactuIncident(saleIds, returnIds);
            });
        }
    }

    private void actualizarPoliticasDeFlujo(String response) {
        if (response.contains("MinimoRegistrosEnvio")) {
            String n = extractTag(response, "MinimoRegistrosEnvio");
            if (n != null) {
                try { 
                    int nuevoMin = Integer.parseInt(n);
                    if (nuevoMin != reqMinRegistros) {
                        System.out.println("[VeriFactu] AEAT solicita nuevo mínimo de registros: " + nuevoMin);
                        reqMinRegistros = nuevoMin; 
                    }
                } catch (Exception ignored) {}
            }
        }
        if (response.contains("TiempoEsperaEnvio")) {
            String t = extractTag(response, "TiempoEsperaEnvio");
            if (t != null) {
                try { 
                    int nuevoTiempo = Integer.parseInt(t);
                    if (nuevoTiempo != reqSegundosEspera) {
                        System.out.println("[VeriFactu] AEAT solicita cambiar intervalo de envío a: " + nuevoTiempo + " segundos (" + (nuevoTiempo / 60) + " min)");
                        reqSegundosEspera = nuevoTiempo; 
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void procesarEstadosResponse(List<VerifactuPayload> batchEnviado, String response) {
        // Splitting robusto: buscamos la etiqueta RespuestaLinea ignorando el prefijo
        // Usamos una expresión regular para que funcione con <tik:RespuestaLinea>, <tikR:RespuestaLinea>, etc.
        String[] lineas = response.split("<[^>]*RespuestaLinea>");
        
        // System.out.println("[VeriFactu] Analizando " + (lineas.length - 1) + " líneas de respuesta de Hacienda...");
        
        if (lineas.length <= 1) {
            System.err.println("[VeriFactu] ADVERTENCIA: No se han encontrado líneas de respuesta formateadas. Respuesta RAW: " + response);
        }

        for (VerifactuPayload record : batchEnviado) {
            String fullNum = record.getNumSerieFactura();
            boolean encontrado = false;
            
            for (int i = 1; i < lineas.length; i++) {
                String lineXml = lineas[i];
                // Buscamos el número de factura en la línea de respuesta
                if (lineXml.contains(">" + fullNum + "<")) {
                    encontrado = true;
                    String estado = extractTag(lineXml, "EstadoRegistro");
                    if ("Correcto".equals(estado) || "Aceptación Completa".equals(estado)) {
                        String csv = extractTag(lineXml, "CSV");
                        outboxRepository.updateStatus(record.getIdRegistro(), "ACCEPTED", csv != null ? csv : "OK");
                        // System.out.println("[VeriFactu] Ticket " + fullNum + " -> ACEPTADO");
                    } else {
                        String cod = extractTag(lineXml, "CodigoErrorRegistro");
                        String desc = extractTag(lineXml, "DescripcionErrorRegistro");
                        if ("3000".equals(cod)) {
                            outboxRepository.updateStatus(record.getIdRegistro(), "ACCEPTED", "Duplicado");
                            // System.out.println("[VeriFactu] Ticket " + fullNum + " -> ACEPTADO (Duplicado)");
                        } else {
                            outboxRepository.updateStatus(record.getIdRegistro(), "REJECTED", "[" + cod + "] " + desc);
                            System.err.println("[VeriFactu] Ticket " + fullNum + " -> RECHAZADO: [" + cod + "] " + desc);
                        }
                    }
                    break;
                }
            }
            
            if (!encontrado) {
                System.err.println("[VeriFactu] ERROR: No se encontró respuesta para el ticket " + fullNum + " en el mensaje de Hacienda.");
            }
        }
    }

    private String extractTag(String xml, String tag) {
        try {
            // Buscamos el tag ignorando posibles prefijos (ej: <sflr:Tag> o <Tag>)
            int tagStart = xml.indexOf(":" + tag + ">");
            if (tagStart == -1) tagStart = xml.indexOf("<" + tag + ">");
            if (tagStart == -1) return null;

            int valStart = xml.indexOf(">", tagStart) + 1;
            int valEnd = xml.indexOf("</", valStart);
            if (valEnd == -1) return null;

            return xml.substring(valStart, valEnd).trim();
        } catch (Exception e) {
            return null;
        }
    }
}


