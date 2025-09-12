package fr.shom.ping.service;

import fr.shom.ping.model.EmailToSend;
import fr.shom.ping.model.dto.SissyUpdateMessageDto;
import fr.shom.ping.model.dto.SseNotificationDto;
import fr.shom.ping.properties.KafkaProperties;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaService {

    private final KafkaTemplate<String, SissyUpdateMessageDto> updateMessageKafkaTemplate;

    private final KafkaTemplate<String, EmailToSend> emailKafkaTemplate;

    private final KafkaTemplate<String, SseNotificationDto> sseNotificationKafkaTemplate;

    private final KafkaProperties properties;

    public void sendSissyUpdateMessage(SissyUpdateMessageDto message) {
        log.info("Send message: {}", message.toString());
        CompletableFuture<SendResult<String, SissyUpdateMessageDto>> future =
            updateMessageKafkaTemplate.send(properties.getSissyUpdateTopicName(), message);

        future.whenComplete((SendResult<String, SissyUpdateMessageDto> result, Throwable ex) -> {
            if (ex == null) {
                log.info("Sent message=[{}] with offset=[{}]", message, result.getRecordMetadata().offset());
            } else {
                log.error("Unable to send message=[{}] due to: {}", message, ex.getMessage());
            }
        });
    }

    public void sendInvalidGeoregulationsLinksMessage(EmailToSend payload) {
        log.info("Send message: {}", payload.toString());
        CompletableFuture<SendResult<String, EmailToSend>> future =
            emailKafkaTemplate.send(properties.getInvalidGeoregulationsLinksTopicName(), payload);
        future.whenComplete((SendResult<String, EmailToSend> result, Throwable ex) -> {
            if (ex == null) {
                log.info("Sent message=[{}] with offset=[{}]", payload, result.getRecordMetadata().offset());
            } else {
                log.error("Unable to send message=[{}] due to: {}", payload, ex.getMessage());
            }
        });
    }

    public void sendSubscriptionNotificationMessage(EmailToSend payload) {
        log.info("Send message: {}", payload.toString());
        CompletableFuture<SendResult<String, EmailToSend>> future =
            emailKafkaTemplate.send(properties.getSubscriptionMailNotificationTopicName(), payload);
        future.whenComplete((SendResult<String, EmailToSend> result, Throwable ex) -> {
            if (ex == null) {
                log.info("Sent message=[{}] with offset=[{}]", payload, result.getRecordMetadata().offset());
            } else {
                log.error("Unable to send message=[{}] due to: {}", payload, ex.getMessage());
            }
        });
    }

    public void sendInfosourceReceivedNotificationMessage(EmailToSend payload) {
        log.info("Send message: {}", payload.toString());
        CompletableFuture<SendResult<String, EmailToSend>> future =
            emailKafkaTemplate.send(properties.getInfosourceReceivedNotificationTopicName(), payload);
        future.whenComplete((SendResult<String, EmailToSend> result, Throwable ex) -> {
            if (ex == null) {
                log.info("Sent message=[{}] with offset=[{}]", payload, result.getRecordMetadata().offset());
            } else {
                log.error("Unable to send message=[{}] due to: {}", payload, ex.getMessage());
            }
        });
    }

    @Transactional
    public void sendSseNotification(SseNotificationDto payload) {
        log.info("Send message: {}", payload.toString());
        CompletableFuture<SendResult<String, SseNotificationDto>> future =
            sseNotificationKafkaTemplate.send(properties.getSseNotificationTopicName(), payload);
        future.whenComplete((SendResult<String, SseNotificationDto> result, Throwable ex) -> {
            if (ex == null) {
                log.info("Sent message=[{}] with offset=[{}]", payload, result.getRecordMetadata().offset());
            } else {
                log.error("Unable to send message=[{}] due to: {}", payload, ex.getMessage());
            }
        });
    }


    public void sendSurveyExternalAnalysisNotificationMessage(EmailToSend payload) {
        log.info("Send message: {}", payload.toString());
        CompletableFuture<SendResult<String, EmailToSend>> future =
            emailKafkaTemplate.send(properties.getSurveyExternalAnalysisNotificationTopicName(), payload);
        future.whenComplete((SendResult<String, EmailToSend> result, Throwable ex) -> {
            if (ex == null) {
                log.info("Sent message=[{}] with offset=[{}]", payload, result.getRecordMetadata().offset());
            } else {
                log.error("Unable to send message=[{}] due to: {}", payload, ex.getMessage());
            }
        });
    }
}
