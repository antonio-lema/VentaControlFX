package com.mycompany.ventacontrolfx.domain.exception;

/**
 * ExcepciÃ³n lanzada cuando la contraseÃ±a proporcionada no coincide con la
 * almacenada.
 */
public class InvalidPasswordException extends BusinessException {
    public InvalidPasswordException() {
        super("La contraseÃ±a proporcionada es incorrecta.");
    }
}
