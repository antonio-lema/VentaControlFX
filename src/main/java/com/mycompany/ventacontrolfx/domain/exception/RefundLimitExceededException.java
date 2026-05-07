package com.mycompany.ventacontrolfx.domain.exception;

public class RefundLimitExceededException extends Exception {
    public RefundLimitExceededException(String message) {
        super(message);
    }
}

