package fr.shom.ping.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ping.email-notification")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailNotificationProperties {
    private String emailFrontLinkRoot;
    private String emailAccessInfosourceLink;
}
