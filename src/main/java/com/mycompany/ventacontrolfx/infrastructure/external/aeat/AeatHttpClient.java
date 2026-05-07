package com.mycompany.ventacontrolfx.infrastructure.external.aeat;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Cliente HTTP puramente nativo (Java 11+) configurado con un KeyStore (formato PKCS12 / .pfx)
 * para realizar la autenticación mutual TLS (mTLS) que exige la AEAT para los servicios VERI*FACTU.
 */
public class AeatHttpClient {
    
    private final String aeatUrl;
    private final HttpClient httpClient;

    public AeatHttpClient(String aeatUrl, String pfxResourcePath, String password) throws Exception {
        this.aeatUrl = aeatUrl;
        
        // Cargar el certificado pfx (PKCS12) desde el classpath
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream certStream = getClass().getResourceAsStream(pfxResourcePath)) {
            if (certStream == null) {
                // Alternativa por si se llama desde un contexto que no encuentra resource
                throw new RuntimeException("Certificado mTLS no encontrado en el classpath: " + pfxResourcePath);
            }
            keyStore.load(certStream, password.toCharArray());
        }

        // Inicializar el KeyManager con nuestro certificado para que Client auth funcione
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password.toCharArray());

        // Confía en las Autoridades de Certificación por defecto (necesario para https://prewww...)
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);

        // Crear contexto SSL de alta seguridad
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        // Inicializar el cliente HttpClient que reutilizará las conexiones y threads eficientemente
        this.httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .version(HttpClient.Version.HTTP_1_1) // SOAP 1.1 prefiere HTTP/1.1 usualmente
                .build();
    }

    /**
     * Envía la envolvente XML al servicio SOAP de facturación.
     * @param soapXml Envolvente XML que contiene el <env:Header> y <env:Body> (<SuministroLRFacturacionRecord>)
     * @return El string base parseado del response de la AEAT
     */
    public String sendSoapMessage(String soapXml) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aeatUrl))
                .timeout(java.time.Duration.ofSeconds(15))
                .header("Content-Type", "text/xml;charset=UTF-8")
                .header("SOAPAction", "\"\"")
                .POST(HttpRequest.BodyPublishers.ofString(soapXml.trim()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}


