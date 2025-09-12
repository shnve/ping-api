package fr.shom.ping.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ping.clamav")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClamAvProperties {
    private String host;
    private Integer port;
    private Integer syncAnalysisThreshold;
    private String clamavReplacedPath;
    private String clamavReplacementPath;
}
