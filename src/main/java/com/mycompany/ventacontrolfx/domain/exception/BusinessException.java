package com.mycompany.ventacontrolfx.domain.exception;

/**
 * ExcepciÃ³n base para errores de lÃ³gica de negocio.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
