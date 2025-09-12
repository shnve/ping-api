package fr.shom.ping.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ping.georegulations-cron-check-links")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeoregulationsCronCheckLinksProperties {

    private String cronValue;
    private String emailRecipient;
    private String emailSubject;
    private String emailBodyStart;
}
