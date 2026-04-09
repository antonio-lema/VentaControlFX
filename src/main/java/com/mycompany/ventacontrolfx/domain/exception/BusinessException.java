package com.mycompany.ventacontrolfx.domain.exception;

/**
 * Excepci\u00c3\u00b3n base para errores de l\u00c3\u00b3gica de negocio.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
