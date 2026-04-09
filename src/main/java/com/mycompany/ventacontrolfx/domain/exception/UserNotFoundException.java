package com.mycompany.ventacontrolfx.domain.exception;

/**
 * ExcepciÃ³n lanzada cuando no se encuentra un usuario por su nombre de usuario.
 */
public class UserNotFoundException extends BusinessException {
    public UserNotFoundException(String username) {
        super("El usuario '" + username + "' no existe en el sistema.");
    }
}
