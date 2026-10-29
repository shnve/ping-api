package fr.shom.ping.service;

import fr.shom.ping.PingApiTranslationsRegistry;
import fr.shom.ping.model.EmailToSend;
import fr.shom.ping.model.FeaturePart;
import fr.shom.ping.model.Georegulation;
import fr.shom.ping.model.GeoregulationSubscription;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.Subscription;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.WarningSubscription;
import fr.shom.ping.model.enums.NotificationType;
import fr.shom.ping.model.enums.SurveyState;
import fr.shom.ping.properties.EmailNotificationProperties;
import fr.shom.ping.properties.SubscriptionNotificationProperties;
import fr.shom.ping.properties.SurveyExternalAnalysisMailProperties;
import fr.shom.ping.repository.ServiceUserRepository;
import fr.shom.ping.utils.GeoregulationUtil;
import fr.shom.ping.utils.TelexUtil;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.stereotype.Service;

import static fr.shom.ping.PingApiTranslationsRegistry.isInternationalLanguage;
import static fr.shom.ping.service.SurveyExternalAnalysisService.getLastProcessingResult;
import static fr.shom.ping.utils.WarningUtil.generateUsualNumber;

@Service
@AllArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final static String DEFAULT_LANGUAGE = "fr";

    private static final String EMAIL_LINE_SEPARATOR = "<br/>";
    public static final String A_HREF = "<a href=\"";

    public static final String INSTITUTIONAL_SIGNATURE_HTML =
        "<br/><br/><div style=\"margin-top: 24px; padding-top: 16px; border-top: 2px solid #e2e8f0; font-size: 12px; color: #475569; line-height: 1.5; font-family: 'Segoe UI', Arial, sans-serif;\">" +
        "<p style=\"margin: 0 0 10px 0; font-size: 13px; font-weight: bold; color: #0b3152;\">" +
        "Atentamente,<br/>" +
        "PISAC - Plataforma de Informaci&oacute;n de Seguridad Acu&aacute;tica<br/>" +
        "<span style=\"font-size: 12px; font-weight: normal; color: #334155;\">Servicio de Hidrograf&iacute;a, Oceanograf&iacute;a, Meteorolog&iacute;a y Cartografiado N&aacute;utico (SHN - Venezuela)</span>" +
        "</p>" +
        "<p style=\"margin: 0 0 8px 0; font-size: 11px; color: #64748b; line-height: 1.45;\">" +
        "La plataforma PISAC est&aacute; implementada &uacute;nicamente para las aguas jurisdiccionales de la Rep&uacute;blica Bolivariana de Venezuela." +
        "</p>" +
        "<p style=\"margin: 0 0 12px 0; font-size: 11px; color: #64748b; line-height: 1.45;\">" +
        "La disponibilidad de las advertencias de navegaci&oacute;n en esta plataforma no exime a los capitanes de la obligaci&oacute;n de recibir en el mar las advertencias de navegaci&oacute;n a trav&eacute;s de los sistemas de difusi&oacute;n oficiales, de conformidad con las disposiciones del Convenio SOLAS." +
        "</p>" +
        "<p style=\"margin: 0 0 12px 0; font-size: 11px; color: #64748b; line-height: 1.45;\">" +
        "La plataforma PISAC presenta una selecci&oacute;n de decretos mar&iacute;timos. Para mayor informaci&oacute;n consultar las publicaciones de las autoridades competentes." +
        "</p>" +
        "<div style=\"margin-top: 10px; padding-top: 8px; border-top: 1px dashed #cbd5e1; font-size: 11px; color: #94a3b8;\">" +
        "No responda o reenv&iacute;e correos a esta cuenta. Esta es una cuenta no monitoreada.<br/>" +
        "Para cualquier consulta adicional, por favor enviar un correo electr&oacute;nico a <a href=\"mailto:shnser@shn.gob.ve\" style=\"color: #0066cc; text-decoration: none;\">shnser@shn.gob.ve</a>" +
        "</div>" +
        "</div>";

    private final KafkaService kafkaService;

    private final SubscriptionNotificationProperties subscriptionNotificationProperties;

    private final EmailNotificationProperties emailNotificationProperties;

    private PingApiTranslationsRegistry translationsRegistry;

    private final TelexUtil telexUtil;

    private final XssSanitizerService xssSanitizerService;

    private final ServiceUserRepository serviceUserRepository;

    private final SurveyExternalAnalysisMailProperties surveyExternalAnalysisMailProperties;

    public void sendNotification(WarningSubscription subscription, Warning warning, String email) {
        if (!subscription.isSendingMail()) {
            return;
        }
        final String lang = subscription.getLang();
        final String defaultLanguage = StringUtils.isNotBlank(lang) ? lang :
            warning.getSerie().getMandatoryLanguages().stream().findFirst().orElse(DEFAULT_LANGUAGE);
        sendEmailNotification(email, getSubject(warning, defaultLanguage), getBody(subscription, warning, defaultLanguage));
    }

    public void sendNotification(GeoregulationSubscription subscription, Georegulation georegulation, String email, NotificationType notificationType) {
        if (!subscription.isSendingMail()) {
            return;
        }
        sendEmailNotification(email, getSubject(georegulation), getBody(subscription, georegulation, notificationType));
    }

    private void sendEmailNotification(String email, String subject, String body) {
        kafkaService.sendSubscriptionNotificationMessage(
            EmailToSend.builder()
                .recipients(email)
                .subject(xssSanitizerService.sanitize(subject))
                .body(xssSanitizerService.sanitize(body + INSTITUTIONAL_SIGNATURE_HTML))
                .build()
        );
    }

    public void sendSurveyExternalAnalysisNotificationEmail(Survey survey, NotificationType notificationType) {
        final String recipients = getSurveyExternalAnalysisNotificationEmailRecipients(survey);
        if (StringUtils.isBlank(recipients)) {
            return;
        }
        // Mail notification
        kafkaService.sendSurveyExternalAnalysisNotificationMessage(
            EmailToSend.builder()
                .recipients(recipients)
                .subject(surveyExternalAnalysisMailProperties.getEmailSubject())
                .body(getSurveyExternalAnalysisNotificationEmailBody(survey, notificationType) + INSTITUTIONAL_SIGNATURE_HTML)
                .build()
        );
    }

    public String getSurveyExternalAnalysisNotificationEmailBody(Survey survey, NotificationType notificationType) {
        String body = "";
        String accessLink = subscriptionNotificationProperties.getEmailFrontLinkRoot() + surveyExternalAnalysisMailProperties.getEmailAccessSurveyLink()
            + survey.getId();
        switch (notificationType) {
            case SURVEY_EXTERNAL_ANALYSIS_END_WITH_CRITICAL_PROBES:
                body = surveyExternalAnalysisMailProperties.getEmailBodyEndWithCriticalProbes();
                break;
            case SURVEY_EXTERNAL_ANALYSIS_END_WITHOUT_CRITICAL_PROBE:
                body = surveyExternalAnalysisMailProperties.getEmailBodyEndWithoutCriticalProbe();
                break;
            case SURVEY_EXTERNAL_ANALYSIS_END_WITH_FAILURE:
                body = surveyExternalAnalysisMailProperties.getEmailBodyEndWithFailure();
                accessLink = subscriptionNotificationProperties.getEmailFrontLinkRoot() +
                    surveyExternalAnalysisMailProperties.getEmailAccessNewContributionLink();
                break;
        }
        final Instant processingDate = getLastProcessingResult(survey).getProcessingDate();
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        final String dateTime = formatter.format(processingDate);
        return body.replace("{{surveyType}}", survey.getState() == SurveyState.TEST ? "test" : "versement")
            .replace("{{dateTime}}", dateTime)
            .replace("{{link}}", accessLink);
    }

    public String getSurveyExternalAnalysisNotificationEmailRecipients(Survey survey) {
        if (survey.getIssuingPublicUser() != null) {
            if (survey.getIssuingPublicUser().hasEmailNotification()) {
                return survey.getIssuingPublicUser().getEmail();
            }
            return null;
        }
        return serviceUserRepository.findAllWithRoleOnService(survey.getIssuingService().getId()).stream()
            .map(ServiceUser::getEmail)
            .collect(Collectors.joining(", "));
    }

    private String getSubject(Warning warning, String lang) {
        final boolean internationalLanguage = isInternationalLanguage(lang);
        final String warningHazardTypeDetailsLine = warning.getFeatureParts().stream().map(FeaturePart::getWarningHazardTypeDetails)
            .flatMap(Collection::stream)
            .map(Enum::name)
            .sorted()
            .map(enumName -> translationsRegistry.getWarningHazardTypeDetailsByEnumNameAndLang(enumName, lang))
            .collect(Collectors.joining(" - "));
        return generateUsualNumber(warning, internationalLanguage) + " - " + warningHazardTypeDetailsLine;
    }

    private String getBody(Subscription subscription, Warning warning, String lang) {
        final boolean internationalLanguage = isInternationalLanguage(lang);
        String title = "";

        if (subscription.getService() != null) {
            title = (internationalLanguage ?
                "Maritime service subscriber: " + subscription.getService().getInternationalName() :
                "Service maritime abonné : " + subscription.getService().getName())
                + EMAIL_LINE_SEPARATOR;
        }

        final String accessLink =
            subscriptionNotificationProperties.getEmailFrontLinkRoot() + subscriptionNotificationProperties.getEmailAccessWarningLink() + warning.getId();
        final String deleteSubscriptionLink =
            subscriptionNotificationProperties.getEmailFrontLinkRoot() + subscriptionNotificationProperties.getEmailDeleteSubscriptionLink() + subscription.getId();
        final String upperAccessLinkStart = "<A HREF=\"" + accessLink.toUpperCase() + "\">";
        final String upperAccessLinkEnd = "</A>";
        return title +
            (internationalLanguage ? "Subscription title: " : "Titre de l'abonnement : ") +
            subscription.getTitle() +
            EMAIL_LINE_SEPARATOR +
            telexUtil.generateTelex(warning, accessLink).replace(upperAccessLinkStart, upperAccessLinkStart.toLowerCase()).replace(upperAccessLinkEnd
                , upperAccessLinkEnd.toLowerCase()).replace("\n", EMAIL_LINE_SEPARATOR) +
            EMAIL_LINE_SEPARATOR +
            A_HREF + deleteSubscriptionLink + "\">" +
            (internationalLanguage ? "Delete subscription" : "Supprimer l'abonnement") +
            "</a>";
    }

    private String getSubject(Georegulation georegulation) {
        return new StringBuilder("Géo-règlement ")
            .append(GeoregulationUtil.getNumber(georegulation))
            .toString();
    }

    private String getBody(Subscription subscription, Georegulation georegulation, NotificationType notificationType) {
        final String accessLink =
            subscriptionNotificationProperties.getEmailFrontLinkRoot() + subscriptionNotificationProperties.getEmailAccessGeoregulationLink() + georegulation.getId();
        final String deleteSubscriptionLink =
            subscriptionNotificationProperties.getEmailFrontLinkRoot() + subscriptionNotificationProperties.getEmailDeleteSubscriptionLink() + subscription.getId();
        final StringBuilder result = new StringBuilder("Service maritime abonné : " + subscription.getService().getName())
            .append(EMAIL_LINE_SEPARATOR)
            .append("Titre de l'abonnement : ")
            .append(subscription.getTitle())
            .append(EMAIL_LINE_SEPARATOR)
            .append("Géo-règlement ")
            .append(A_HREF + accessLink + "\">")
            .append(GeoregulationUtil.getNumber(georegulation))
            .append("</a>")
            .append(EMAIL_LINE_SEPARATOR);

        if (StringUtils.isNotBlank(georegulation.getTitle())) {
            result.append("Objet : ")
                .append(georegulation.getTitle());
        }

        return result.append(EMAIL_LINE_SEPARATOR)
            .append(getStatusLine(georegulation, notificationType))
            .append(EMAIL_LINE_SEPARATOR)
            .append(A_HREF + deleteSubscriptionLink + "\">")
            .append("Supprimer l'abonnement")
            .append("</a>")
            .toString();
    }

    private String getStatusLine(Georegulation georegulation, NotificationType notificationType) {
        String prefix = "";
        Instant eventDate = Instant.now();
        switch (notificationType) {
            case GEOREGULATION_PUBLISHED:
                prefix = "Publié";
                eventDate = georegulation.getPublicationDate();
                break;
            case GEOREGULATION_UPDATED:
                prefix = "Modifié";
                eventDate = georegulation.getLastUpdate();
                break;
            case GEOREGULATION_REPEALED:
                prefix = "Abrogé";
                eventDate = georegulation.getRepealDate();
                break;
            default:
                break;
        }
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());
        return prefix + " dans PING le " + formatter.format(eventDate);
    }

    public void sendInfosourceReceivedNotification(Infosource infosource, String email) {
        if (StringUtils.isBlank(email) || !EmailValidator.getInstance().isValid(email)) {
            return;
        }

        final String accessLink =
            emailNotificationProperties.getEmailFrontLinkRoot() + emailNotificationProperties.getEmailAccessInfosourceLink() + infosource.getId();

        final String subject = "Infosource " + infosource.getNumber();
        final String body = new StringBuilder("Infosource reçue :")
            .append(EMAIL_LINE_SEPARATOR)
            .append("Infosource ")
            .append(A_HREF + accessLink + "\">")
            .append(infosource.getNumber())
            .append("</a>")
            .append(EMAIL_LINE_SEPARATOR)
            .toString();

        kafkaService.sendInfosourceReceivedNotificationMessage(
            EmailToSend.builder()
                .recipients(email)
                .subject(xssSanitizerService.sanitize(subject))
                .body(xssSanitizerService.sanitize(body + INSTITUTIONAL_SIGNATURE_HTML))
                .build()
        );
    }
}
