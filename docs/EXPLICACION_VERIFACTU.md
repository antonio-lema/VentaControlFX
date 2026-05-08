# Implementación de Veri*Factu en VentaControlFX

Veri*Factu es el sistema de facturación certificada de la AEAT (Agencia Tributaria) que garantiza la integridad, conservación, accesibilidad, legibilidad, trazabilidad e inalterabilidad de los registros de facturación.

## 1. Estructura de Base de Datos
La información fiscal reside principalmente en las tablas `sales` (ventas) y `returns` (devoluciones). Los campos clave para Veri*Factu son:

*   **`control_hash`**: El hash único generado para el registro actual.
*   **`prev_hash`**: El hash del registro anterior. Esto crea una **cadena inalterable** (si borras un ticket, la cadena se rompe).
*   **`signature`**: Firma digital del registro utilizando el certificado del cliente.
*   **`fiscal_status`**: Estado del envío (`PENDING`, `ACCEPTED`, `REJECTED`).
*   **`fiscal_msg`**: Mensaje de respuesta del servidor de la AEAT.
*   **`aeat_submission_id`**: El ID de registro oficial que nos devuelve Hacienda tras recibir la factura.
*   **`gen_timestamp`**: Fecha y hora exacta de generación del registro (incluyendo milisegundos).

## 2. Lógica de Trazabilidad (El Encadenamiento)
Para cumplir con la normativa, cada venta está vinculada a la anterior mediante hashes:

1.  Antes de guardar una venta, el sistema busca el `control_hash` del último ticket emitido.
2.  Se genera un nuevo hash que combina los datos del ticket actual + el hash del anterior.
3.  Esto asegura que nadie pueda inyectar o modificar tickets antiguos sin que se detecte.

## 3. Flujo de Envío (VerifactuOutboxManager)
El proceso no ocurre \"en tiempo real\" durante la venta para no hacer esperar al cliente, sino que se gestiona en segundo plano:

*   **Outbox Pattern**: Las ventas se guardan como `PENDING`.
*   **VerifactuOutboxManager**: Es el encargado de recoger los registros pendientes.
*   **VerifactuXmlBuilder**: Transforma los datos de la venta al formato XML requerido por la AEAT (basado en el esquema XSD oficial).
*   **AeatHttpClient**: Realiza la conexión segura mediante **Certificado Digital (PFX)** al servidor SOAP de Hacienda.

## 4. Representación Gráfica (QR y URL)
Cumpliendo con la ley, cada ticket generado incluye:
*   **URL de Verificación**: Una URL que apunta a `prewww1.aeat.es` (en pruebas) o `www1.aeat.es` (en producción).
*   **Código QR**: Generado por `QrGenerator`, permite al consumidor final escanear su ticket y verificar en la web de la AEAT que la factura ha sido declarada correctamente.

## 5. Configuración
Los parámetros de conexión se gestionan en `ServiceContainer` y se guardan en la configuración de la empresa:
*   `verifactu.url`: El punto de acceso al servicio web de Hacienda.
*   Certificado: Ruta al archivo `.pfx` y su contraseña.

> [!IMPORTANT]
> El sistema está diseñado para manejar incidencias. Si el envío falla (por falta de internet, por ejemplo), el ticket se queda como `PENDING` y el `VerifactuIncidentHandler` reintentará el envío automáticamente cuando sea posible, manteniendo siempre el orden cronológico.
