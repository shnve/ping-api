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
import fr.shom.ping.service.email.PisacEmailTemplate;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.validator.routines.EmailValidator;

import static fr.shom.ping.service.EmailNotificationService.INSTITUTIONAL_HEADER_HTML;
import static fr.shom.ping.service.EmailNotificationService.INSTITUTIONAL_SIGNATURE_HTML;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmtpEmailService {

    private final PisacEmailTemplate emailTemplate;

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
            log.warn("No se puede enviar correo: destinatario vacío");
            return;
        }

        // PISAC (F5.3.7): solo direcciones válidas (evita inyección de cabeceras o destinatarios malformados)
        final String[] recipients = Arrays.stream(emailToSend.getRecipients().split("[,;]"))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .filter(address -> {
                final boolean valid = EmailValidator.getInstance().isValid(address);
                if (!valid) {
                    log.warn("Destinatario de correo descartado por formato inválido: '{}'", address);
                }
                return valid;
            })
            .toArray(String[]::new);

        if (recipients.length == 0) {
            log.warn("No se encontraron destinatarios válidos en '{}'", emailToSend.getRecipients());
            return;
        }

        // Asunto: texto plano en una sola línea (los servicios lo pasan por el saneador HTML, que codifica entidades)
        final String subject = StringUtils.normalizeSpace(StringEscapeUtils.unescapeHtml4(
            StringUtils.defaultIfBlank(emailToSend.getSubject(), "Notificación PISAC")));

        try {
            JavaMailSenderImpl mailSender = buildMailSender();
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name());

            if (StringUtils.isNotBlank(smtpFromDisplayName)) {
                helper.setFrom(new InternetAddress(smtpFrom, smtpFromDisplayName, StandardCharsets.UTF_8.name()));
            } else {
                helper.setFrom(smtpFrom);
            }

            helper.setTo(recipients);
            helper.setSubject(subject);
            // Correo automático: evita respuestas automáticas (fuera de oficina, etc.) hacia la cuenta no monitoreada
            mimeMessage.setHeader("Auto-Submitted", "auto-generated");
            mimeMessage.setHeader("X-Auto-Response-Suppress", "All");

            // PISAC (F5.3.7): plantilla común con logo en línea (cid:) y contenido saneado con la política de correo
            final String body = StringUtils.defaultString(emailToSend.getBody());
            if (emailTemplate.isAlreadyRendered(body)) {
                helper.setText(body, true);
            } else {
                final String safeContent = emailTemplate.sanitizeContent(stripLegacyLayout(body));
                final boolean withLogo = emailTemplate.logo().exists();
                helper.setText(emailTemplate.renderText(subject, safeContent), emailTemplate.renderHtml(subject, safeContent, withLogo));
                if (withLogo) {
                    helper.addInline(PisacEmailTemplate.LOGO_CID, emailTemplate.logo(), PisacEmailTemplate.LOGO_CONTENT_TYPE);
                }
            }

            mailSender.send(mimeMessage);
            log.info("Correo SMTP enviado exitosamente a {} con asunto '{}'", Arrays.toString(recipients), subject);
        } catch (Exception ex) {
            log.error("Error al enviar correo SMTP a '{}' con asunto '{}': {}",
                emailToSend.getRecipients(), subject, ex.getMessage(), ex);
        }
    }

    /**
     * Mensajes antiguos (encolados antes de F5.3.7) traen la cabecera y la firma HTML anteriores: se quitan
     * para no duplicarlas dentro de la plantilla nueva.
     */
    private String stripLegacyLayout(String body) {
        return body.replace(INSTITUTIONAL_HEADER_HTML, "").replace(INSTITUTIONAL_SIGNATURE_HTML, "");
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
