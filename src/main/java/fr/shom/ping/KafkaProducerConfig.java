package fr.shom.ping;

import fr.shom.ping.model.EmailToSend;
import fr.shom.ping.model.dto.SissyUpdateMessageDto;
import fr.shom.ping.model.dto.SseNotificationDto;
import fr.shom.ping.properties.KafkaProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
@RequiredArgsConstructor
public class KafkaProducerConfig {

    private final KafkaProperties properties;

    @Bean
    public ProducerFactory<String, SissyUpdateMessageDto> updateMessageProducerFactory() {
        return new DefaultKafkaProducerFactory<>(getConfigProps());
    }

    @Bean
    public KafkaTemplate<String, SissyUpdateMessageDto> updateMessageKafkaTemplate() {
        return new KafkaTemplate<>(updateMessageProducerFactory());
    }

    @Bean
    public ProducerFactory<String, EmailToSend> emailProducerFactory() {
        return new DefaultKafkaProducerFactory<>(getConfigProps());
    }

    @Bean
    public KafkaTemplate<String, EmailToSend> emailKafkaTemplate() {
        return new KafkaTemplate<>(emailProducerFactory());
    }

    @Bean
    public ProducerFactory<String, SseNotificationDto> sseNotificationProducerFactory() {
        return new DefaultKafkaProducerFactory<>(getOnceProcessingConfigProps());
    }

    @Bean
    public KafkaTemplate<String, SseNotificationDto> sseNotificationKafkaTemplate() {
        return new KafkaTemplate<>(sseNotificationProducerFactory());
    }

    private Map<String, Object> getConfigProps() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapAddress());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return configProps;
    }

    private Map<String, Object> getOnceProcessingConfigProps() {
        final Map<String, Object> configProps = new HashMap<>(getConfigProps());
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        return configProps;
    }
}
