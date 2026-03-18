package com.mycompany.ventacontrolfx.domain.repository;

public interface IEmailSender {
    void send(String to, String subject, String body) throws Exception;

    void sendWithAttachment(String to, String subject, String body, byte[] attachment, String fileName)
            throws Exception;
}
