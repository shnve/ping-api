package fr.shom.ping.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ping.cors")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorsProperties {
    private String[] allowedOrigins;
    private String[] allowedMethods;
    private String[] allowedHeaders;
    private String[] exposedHeaders;
    private Boolean allowCredentials;
    private String registerOnPath;
}
