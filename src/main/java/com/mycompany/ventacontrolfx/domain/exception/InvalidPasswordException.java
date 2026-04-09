package com.mycompany.ventacontrolfx.domain.exception;

/**
 * Excepci\u00c3\u00b3n lanzada cuando la contrase\u00c3\u00b1a proporcionada no coincide con la
 * almacenada.
 */
public class InvalidPasswordException extends BusinessException {
    public InvalidPasswordException() {
        super("La contrase\u00c3\u00b1a proporcionada es incorrecta.");
    }
}
