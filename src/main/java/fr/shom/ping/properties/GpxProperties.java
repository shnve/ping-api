package fr.shom.ping.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ping.gpx-disclaimer")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GpxProperties {
    private String fr;
    private String en;
}
