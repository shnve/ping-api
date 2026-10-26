package fr.shom.ping;

import fr.shom.ping.properties.CorsProperties;
import fr.shom.ping.properties.KeycloakProperties;
import fr.shom.ping.properties.ProxyProperties;
import fr.shom.ping.properties.SecurityProperties;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static java.util.Arrays.asList;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Import(value = { KeycloakProperties.class, SecurityProperties.class, CorsProperties.class })
@Slf4j
public class PingApiKeycloakSecurityConfig {

    @Value("${keycloak.principal-attribute}")
    private String principalNameAttribute;

    @Value("${ping.keycloak.pro-issuer-uri}")
    private String issuerPingPro;

    @Value("${ping.keycloak.public-issuer-uri}")
    private String issuerPingPublic;

    private final SecurityProperties securityProperties;

    private final CorsProperties corsProperties;

    private final ProxyProperties proxyProperties;

    private final Map<String, AuthenticationManager> authenticationManagers = new HashMap<>();

    public PingApiKeycloakSecurityConfig(SecurityProperties securityProperties, CorsProperties corsProperties,
            ProxyProperties proxyProperties) {
        this.securityProperties = securityProperties;
        this.corsProperties = corsProperties;
        this.proxyProperties = proxyProperties;
    }

    private void addManager(Map<String, AuthenticationManager> authenticationManagers, String issuer) {
        JwtAuthenticationProvider authenticationProvider = new JwtAuthenticationProvider(jwtDecoder(issuer));
        authenticationProvider.setJwtAuthenticationConverter(jwtAuthenticationConverter());
        authenticationManagers.put(issuer, authenticationProvider::authenticate);
    }

    /**
     * @Bean
     *       public SecurityFilterChain resourceServerFilterChain(HttpSecurity http)
     *       throws Exception {
     *       List.of(issuerPingPro, issuerPingPublic).forEach(issuer ->
     *       addManager(authenticationManagers, issuer));
     *       JwtIssuerAuthenticationManagerResolver authenticationManagerResolver =
     *       new
     *       JwtIssuerAuthenticationManagerResolver(authenticationManagers::get);
     * 
     *       http.authorizeHttpRequests(authorize ->
     *       authorize.anyRequest().permitAll())
     *       .sessionManagement(sessions -> sessions
     *       .requireExplicitAuthenticationStrategy(false)
     *       .sessionCreationPolicy(STATELESS).sessionAuthenticationStrategy(sessionAuthenticationStrategy())
     *       )
     *       .exceptionHandling(exceptionHandling ->
     *       exceptionHandling.authenticationEntryPoint(new
     *       Http403ForbiddenEntryPoint()))
     *       .csrf(csrf -> csrf
     *       .csrfTokenRepository(csrfTokenRepository())
     *       .ignoringRequestMatchers(
     *       Arrays.stream(securityProperties.getCsrfAllowedRequestUris()).map(AntPathRequestMatcher::antMatcher).toArray(AntPathRequestMatcher[]::new)
     *       )
     *       )
     *       .cors(cors -> cors
     *       .configurationSource(corsConfigurationSource())
     *       )
     *       .headers(headers -> headers.referrerPolicy(referrer ->
     *       referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
     *       /*.httpStrictTransportSecurity(hstsConfig ->
     *       hstsConfig.includeSubDomains(true)
     *       .preload(true)
     *       .maxAgeInSeconds(Duration.ofDays(365).toSeconds()))
     */
    /**
     * );
     * http.oauth2ResourceServer(oauth2 ->
     * oauth2.authenticationManagerResolver(authenticationManagerResolver));
     * return http.build();
     * }
     **/

    @Bean
    public SecurityFilterChain resourceServerFilterChain(HttpSecurity http) throws Exception {
        // 1. REACTIVAMOS LA CONEXION A KEYCLOAK
        List.of(issuerPingPro, issuerPingPublic).forEach(issuer -> addManager(authenticationManagers, issuer));

        JwtIssuerAuthenticationManagerResolver authenticationManagerResolver = new JwtIssuerAuthenticationManagerResolver(
                authenticationManagers::get);

        http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .sessionManagement(sessions -> sessions
                        .requireExplicitAuthenticationStrategy(false)
                        .sessionCreationPolicy(STATELESS)
                        .sessionAuthenticationStrategy(sessionAuthenticationStrategy()))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(new Http403ForbiddenEntryPoint()))
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource()))
                .headers(headers -> headers.referrerPolicy(
                        referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)))
                // Servidor OAuth2 activo
                .oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(authenticationManagerResolver));

        return http.build();
    }

    private CookieCsrfTokenRepository csrfTokenRepository() {
        final CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.secure(true));
        return repository;
    }

    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(sessionRegistry());
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(asList(corsProperties.getAllowedOrigins()));
        configuration.setAllowedMethods(asList(corsProperties.getAllowedMethods()));
        configuration.setAllowedHeaders(asList(corsProperties.getAllowedHeaders()));
        configuration.setAllowCredentials(corsProperties.getAllowCredentials());
        configuration.setExposedHeaders(asList(corsProperties.getExposedHeaders()));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(corsProperties.getRegisterOnPath(), configuration);
        return source;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(
                Arrays.stream(securityProperties.getIgnoredPaths()).map(AntPathRequestMatcher::antMatcher)
                        .toArray(AntPathRequestMatcher[]::new));
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    interface AuthoritiesConverter extends Converter<Map<String, Object>, Collection<GrantedAuthority>> {
    }

    @Bean
    AuthoritiesConverter realmRolesAuthoritiesConverter() {
        return claims -> {

            Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();

            final Map<String, Object> resourceAccess = (Map<String, Object>) claims.get("resource_access");

            if (resourceAccess != null) {
                final Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("ping-api");

                if (clientAccess != null) {
                    grantedAuthorities = ((List<String>) clientAccess.get("roles")).stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                }
            }
            return grantedAuthorities;
        };
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        final JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter
                .setJwtGrantedAuthoritiesConverter(jwt -> realmRolesAuthoritiesConverter().convert(jwt.getClaims()));
        jwtAuthenticationConverter.setPrincipalClaimName(principalNameAttribute);
        return jwtAuthenticationConverter;
    }

    public JwtDecoder jwtDecoder(String issuer) {
        NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder builder = NimbusJwtDecoder.withIssuerLocation(issuer);
        if (proxyProperties.isUseProxy()) {
            final Proxy proxy = new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(proxyProperties.getProxyHost(), proxyProperties.getProxyPort()));
            final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setProxy(proxy);
            builder.restOperations(new RestTemplate(requestFactory));
        }
        final NimbusJwtDecoder jwtDecoder = builder.build();
        final OAuth2TokenValidator<Jwt> jwtValidator = JwtValidators.createDefaultWithIssuer(issuer);
        jwtDecoder.setJwtValidator(jwtValidator);
        return jwtDecoder;
    }
}
