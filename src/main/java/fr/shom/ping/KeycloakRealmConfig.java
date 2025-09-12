package fr.shom.ping;

import fr.shom.ping.properties.KeycloakProperties;
import fr.shom.ping.properties.ProxyProperties;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.keycloak.OAuth2Constants.CLIENT_CREDENTIALS;

@Configuration
public class KeycloakRealmConfig {

    @Bean
    @Primary
    public RealmResource publicRealmResource(KeycloakProperties keycloakProperties, ProxyProperties proxyProperties) {
        ResteasyClientBuilder rcb = ((ResteasyClientBuilder) ClientBuilder.newBuilder())
            .connectionPoolSize(keycloakProperties.getPoolSize());
        if (proxyProperties.isUseProxy()) {
            rcb = rcb.defaultProxy(proxyProperties.getProxyHost(), proxyProperties.getProxyPort());
        }
        return KeycloakBuilder.builder()
            .realm(keycloakProperties.getPublicRealm().getRealm())
            .serverUrl(keycloakProperties.getServerUrl())
            .grantType(CLIENT_CREDENTIALS)
            .clientId(keycloakProperties.getPublicRealm().getAdminClient().getClientId())
            .clientSecret(keycloakProperties.getPublicRealm().getAdminClient().getClientSecret())
            .resteasyClient(rcb.build())
            .build().realm(keycloakProperties.getPublicRealm().getRealm());
    }

    @Bean("proRealmResource")
    public RealmResource proRealmResource(KeycloakProperties keycloakProperties, ProxyProperties proxyProperties) {
        ResteasyClientBuilder rcb = ((ResteasyClientBuilder) ClientBuilder.newBuilder())
            .connectionPoolSize(keycloakProperties.getPoolSize());
        if (proxyProperties.isUseProxy()) {
            rcb = rcb.defaultProxy(proxyProperties.getProxyHost(), proxyProperties.getProxyPort());
        }
        return KeycloakBuilder.builder()
            .realm(keycloakProperties.getProRealm().getRealm())
            .serverUrl(keycloakProperties.getServerUrl())
            .grantType(CLIENT_CREDENTIALS)
            .clientId(keycloakProperties.getProRealm().getAdminClient().getClientId())
            .clientSecret(keycloakProperties.getProRealm().getAdminClient().getClientSecret())
            .resteasyClient(rcb.build())
            .build().realm(keycloakProperties.getProRealm().getRealm());
    }
}
