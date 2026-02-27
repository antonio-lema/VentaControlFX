package com.mycompany.ventacontrolfx.domain.repository;

public interface IEmailSender {
    void send(String to, String subject, String body) throws Exception;
}
