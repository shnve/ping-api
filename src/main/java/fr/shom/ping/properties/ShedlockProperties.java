package fr.shom.ping.properties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ping.shedlock")
@NoArgsConstructor
@AllArgsConstructor
public class ShedlockProperties {
    private String defaultLockTimeAtMostFor;
    private String lockTimeAtLeastFor;
    private String lockTimeAtMostFor;
}
