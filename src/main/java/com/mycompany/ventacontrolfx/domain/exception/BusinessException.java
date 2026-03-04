package com.mycompany.ventacontrolfx.domain.exception;

/**
 * Excepción base para errores de lógica de negocio.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
