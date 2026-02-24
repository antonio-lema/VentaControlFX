package com.mycompany.ventacontrolfx.service;

import com.mycompany.ventacontrolfx.dao.ConfigDAO;
import javafx.application.Platform;
import com.mycompany.ventacontrolfx.util.AlertUtil;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Servicio para envio de correos REAL usando JavaMail.
 * Las credenciales se obtienen de la base de datos (tabla system_config).
 */
public class EmailService {

    private final ConfigDAO configDAO = new ConfigDAO();

    public void sendEmail(String to, String subject, String body) {
        // Ejecutar en un hilo separado para no bloquear la interfaz gráfica
        new Thread(() -> {
            // 1. Obtener configuración de la BD
            String smtpHost = configDAO.getValue("smtp_host");
            String smtpPort = configDAO.getValue("smtp_port");
            String emailFrom = configDAO.getValue("email_from");
            String emailPassword = configDAO.getValue("email_password");

            // Validar que exista la configuración
            if (smtpHost == null || smtpPort == null || emailFrom == null || emailPassword == null) {
                Platform.runLater(() -> {
                    AlertUtil.showError("Error de Configuración",
                            "No se pudo enviar el correo porque faltan configuraciones en la base de datos (system_config).");
                });
                return;
            }

            try {
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
                message.setText(body);

                Transport.send(message);

                // Notificar éxito en la UI
                Platform.runLater(() -> {
                    AlertUtil.showInfo("Correo Enviado",
                            "Se ha enviado un correo a: " + to + "\nRevisa tu bandeja de entrada.");
                });

            } catch (MessagingException e) {
                // Notificar error en la UI
                Platform.runLater(() -> {
                    AlertUtil.showError("Error de Envío", "No se pudo enviar el correo: " + e.getMessage());
                });
            }
        }).start();
    }

    public void sendRecoveryCode(String email, String code) {
        String subject = "Código de Recuperación de Contraseña - TPV";
        String body = "Tu código de recuperación es: " + code + "\n\n"
                + "Usa este código en la aplicación para restablecer tu contraseña.\n"
                + "Si no solicitaste esto, ignora este mensaje.";
        sendEmail(email, subject, body);
    }

    public void sendUsernameReminder(String email, String username) {
        String subject = "Recordatorio de Usuario - TPV";
        String body = "Hola,\n\n"
                + "El nombre de usuario asociado a esta cuenta es: " + username + "\n\n"
                + "Conéctate usando este nombre de usuario.";
        sendEmail(email, subject, body);
    }
}
