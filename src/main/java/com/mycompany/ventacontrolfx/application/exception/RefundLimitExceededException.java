package com.mycompany.ventacontrolfx.application.exception;

public class RefundLimitExceededException extends Exception {
    public RefundLimitExceededException(String message) {
        super(message);
    }
}
