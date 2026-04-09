package com.mycompany.ventacontrolfx.infrastructure.email;

import com.mycompany.ventacontrolfx.domain.repository.ICompanyConfigRepository;
import com.mycompany.ventacontrolfx.domain.repository.IEmailSender;
import com.mycompany.ventacontrolfx.infrastructure.persistence.JdbcCompanyConfigRepository;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.util.Properties;

public class SmtpEmailAdapter implements IEmailSender {

    private final ICompanyConfigRepository configRepository = new JdbcCompanyConfigRepository();

    @Override
    public void send(String to, String subject, String body) throws Exception {
        sendWithAttachment(to, subject, body, null, null);
    }

    @Override
    public void sendWithAttachment(String to, String subject, String body, byte[] attachment, String fileName)
            throws Exception {
        String smtpHost = configRepository.getValue("smtp_host");
        String smtpPort = configRepository.getValue("smtp_port");
        String emailFrom = configRepository.getValue("email_from");
        String emailPassword = configRepository.getValue("email_password");

        if (smtpHost == null || smtpPort == null || emailFrom == null || emailPassword == null) {
            throw new Exception("Configuraci\u00c3\u00b3n SMTP incompleta en la base de datos.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailFrom, emailPassword);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(emailFrom));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);

        if (attachment != null && fileName != null) {
            // Multipart message
            Multipart multipart = new MimeMultipart();

            // Text part
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(body);
            multipart.addBodyPart(textPart);

            // Attachment part
            MimeBodyPart attachmentPart = new MimeBodyPart();
            ByteArrayDataSource bds = new ByteArrayDataSource(attachment, "application/pdf");
            attachmentPart.setDataHandler(new javax.activation.DataHandler(bds));
            attachmentPart.setFileName(fileName);
            multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);
        } else {
            // Simple text message
            message.setText(body);
        }

        Transport.send(message);
    }
}
