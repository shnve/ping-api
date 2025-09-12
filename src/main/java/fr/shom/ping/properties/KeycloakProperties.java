package fr.shom.ping.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Data
@Configuration
@ConfigurationProperties(prefix = "ping.keycloak")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeycloakProperties {
    private String serverUrl;
    private int poolSize;
    private Realm publicRealm;
    private Realm proRealm;
    private KeycloakRoles roles;
    private String principalAttribute;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Realm {
        private String realm;
        private Client adminClient;
        private Client apiClient;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Client {
        private String clientId;
        private String clientSecret;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeycloakRoles {
        private String functionalAdmin;
        private String serviceUser;
    }
}
