package fr.shom.ping;

import fr.shom.ping.properties.ClamAvProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.capybara.clamav.ClamavClient;
import xyz.capybara.clamav.Platform;

@Configuration
@RequiredArgsConstructor
public class ClamAvConfig {

    private final ClamAvProperties properties;

    @Bean
    public ClamavClient clamavClient() {
        return new ClamavClient(properties.getHost(), properties.getPort(), Platform.UNIX);
    }
}
