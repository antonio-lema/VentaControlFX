package com.mycompany.ventacontrolfx.domain.exception;

/**
 * Excepci\u00f3n base para errores de l\u00f3gica de negocio.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

