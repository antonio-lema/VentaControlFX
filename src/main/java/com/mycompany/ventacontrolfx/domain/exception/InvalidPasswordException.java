package com.mycompany.ventacontrolfx.domain.exception;

/**
 * Excepción lanzada cuando la contraseña proporcionada no coincide con la
 * almacenada.
 */
public class InvalidPasswordException extends BusinessException {
    public InvalidPasswordException() {
        super("La contraseña proporcionada es incorrecta.");
    }
}
