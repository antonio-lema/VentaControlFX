package com.mycompany.ventacontrolfx.domain.exception;

/**
 * Excepci\u00f3n lanzada cuando la contrase\u00f1a proporcionada no coincide con la
 * almacenada.
 */
public class InvalidPasswordException extends BusinessException {
    public InvalidPasswordException() {
        super("La contrase\u00f1a proporcionada es incorrecta.");
    }
}

