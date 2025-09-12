package fr.shom.ping;

import fr.shom.ping.model.dto.LighthouseAndBeaconInfosourceMessageDto;
import fr.shom.ping.model.dto.SseNotificationDto;
import fr.shom.ping.properties.KafkaProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@EnableKafka
@Configuration
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerConfig {

    private final KafkaProperties properties;

    public ConsumerFactory<String, LighthouseAndBeaconInfosourceMessageDto> lighthouseAndBeaconInfosourceMessageDtoConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapAddress());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getGroupId());
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(LighthouseAndBeaconInfosourceMessageDto.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LighthouseAndBeaconInfosourceMessageDto>
    lighthouseAndBeaconInfosourceMessageDtoListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, LighthouseAndBeaconInfosourceMessageDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(lighthouseAndBeaconInfosourceMessageDtoConsumerFactory());
        factory.setCommonErrorHandler(new CommonLoggingErrorHandler());
        return factory;
    }

    // https://medium.com/@aviksaha2007/exactly-once-processing-with-apache-kafka-4ae243ced107
    // https://www.linkedin.com/pulse/kafka-transactions-part-1-exactly-once-messaging-rob-golder/
    // https://www.linkedin.com/pulse/kafka-transactions-part-2-spring-boot-demo-rob-golder/
    // https://github.com/lydtechconsulting/kafka-transactions/tree/main?tab=readme-ov-file
    @Bean
    public ConsumerFactory<String, SseNotificationDto> sseNotificationConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapAddress());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getGroupId());

        // ensures that the consumer reads only committed messages and ignores any aborted or duplicated messages
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        // disable auto-commit of consumer offsets as it is producer's responsibility to send consumer offsets
        // to be committed to the consumer coordinator during the transation
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(SseNotificationDto.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SseNotificationDto> sseNotificationListenerContainerFactory(
        final ConsumerFactory<String, SseNotificationDto> sseNotificationConsumerFactory) {
        final ConcurrentKafkaListenerContainerFactory<String, SseNotificationDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(sseNotificationConsumerFactory);

        // Commit the offset after each record is processed by the listener
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        factory.setCommonErrorHandler(new CommonLoggingErrorHandler());
        return factory;
    }
}
