package fr.shom.ping;

import fr.shom.ping.properties.KeycloakProperties;
import fr.shom.ping.repository.ServiceUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
@Import(value = { KeycloakProperties.class })
public class PingApiMethodSecurityConfig {

    private final ServiceUserRepository serviceUserRepository;
    private final ApplicationContext applicationContext;
    private final KeycloakProperties keycloakProperties;

    @Bean
    protected MethodSecurityExpressionHandler createExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler
                .setPermissionEvaluator(new PingApiPermissionEvaluator(serviceUserRepository, keycloakProperties));
        expressionHandler.setApplicationContext(applicationContext);
        return expressionHandler;
    }
}
