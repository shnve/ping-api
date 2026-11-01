package fr.shom.ping.service;

import fr.shom.ping.model.EmailToSend;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import static fr.shom.ping.service.EmailNotificationService.INSTITUTIONAL_HEADER_HTML;
import static fr.shom.ping.service.EmailNotificationService.INSTITUTIONAL_SIGNATURE_HTML;

@Service
@Slf4j
public class SmtpEmailService {

    @Value("${SMTP_HOST:mail.shn.gob.ve}")
    private String smtpHost;

    @Value("${SMTP_PORT:465}")
    private int smtpPort;

    @Value("${SMTP_SECURITY:ssl}")
    private String smtpSecurity;

    @Value("${SMTP_USER:}")
    private String smtpUser;

    @Value("${SMTP_PASSWORD:}")
    private String smtpPassword;

    @Value("${SMTP_FROM:shn_pisac@shn.gob.ve}")
    private String smtpFrom;

    @Value("${SMTP_FROM_DISPLAY_NAME:PISAC - SHN Venezuela}")
    private String smtpFromDisplayName;

    public void sendEmail(EmailToSend emailToSend) {
        if (emailToSend == null || StringUtils.isBlank(emailToSend.getRecipients())) {
            log.warn("No se puede enviar correo: destinatario vacío en payload {}", emailToSend);
            return;
        }

        final String[] recipients = Arrays.stream(emailToSend.getRecipients().split("[,;]"))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .toArray(String[]::new);

        if (recipients.length == 0) {
            log.warn("No se encontraron destinatarios válidos en '{}'", emailToSend.getRecipients());
            return;
        }

        try {
            JavaMailSenderImpl mailSender = buildMailSender();
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            if (StringUtils.isNotBlank(smtpFromDisplayName)) {
                helper.setFrom(new InternetAddress(smtpFrom, smtpFromDisplayName, StandardCharsets.UTF_8.name()));
            } else {
                helper.setFrom(smtpFrom);
            }

            helper.setTo(recipients);
            helper.setSubject(StringUtils.defaultString(emailToSend.getSubject(), "Notificación PISAC"));

            String bodyHtml = StringUtils.defaultString(emailToSend.getBody(), "");
            if (!bodyHtml.contains("Plataforma de Informaci&oacute;n de Seguridad Acu&aacute;tica")) {
                bodyHtml = INSTITUTIONAL_HEADER_HTML + bodyHtml + INSTITUTIONAL_SIGNATURE_HTML;
            }
            helper.setText(bodyHtml, true);

            mailSender.send(mimeMessage);
            log.info("Correo SMTP enviado exitosamente a {} con asunto '{}'", Arrays.toString(recipients), emailToSend.getSubject());
        } catch (Exception ex) {
            log.error("Error al enviar correo SMTP a '{}' con asunto '{}': {}",
                emailToSend.getRecipients(), emailToSend.getSubject(), ex.getMessage(), ex);
        }
    }

    private JavaMailSenderImpl buildMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtpHost);
        mailSender.setPort(smtpPort);
        mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.connectiontimeout", "15000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");

        if (StringUtils.isNotBlank(smtpUser)) {
            mailSender.setUsername(smtpUser);
            mailSender.setPassword(smtpPassword);
            props.put("mail.smtp.auth", "true");
        } else {
            props.put("mail.smtp.auth", "false");
        }

        if ("ssl".equalsIgnoreCase(smtpSecurity) || smtpPort == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust", "*");
        } else if ("tls".equalsIgnoreCase(smtpSecurity) || "starttls".equalsIgnoreCase(smtpSecurity) || smtpPort == 587) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.trust", "*");
        }

        return mailSender;
    }
}
