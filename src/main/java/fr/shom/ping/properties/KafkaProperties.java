package fr.shom.ping.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "kafka")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KafkaProperties {
    private String bootstrapAddress;
    private String topicName;
    private String sissyUpdateTopicName;
    private String groupId;
    private String individualGroupId;
    private String invalidGeoregulationsLinksTopicName;
    private String subscriptionMailNotificationTopicName;
    private String infosourceReceivedNotificationTopicName;
    private String surveyExternalAnalysisNotificationTopicName;
    private String sseNotificationTopicName;
    private String sseNotificationTransactionalId;
}
