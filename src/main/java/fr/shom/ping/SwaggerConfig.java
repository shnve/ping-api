package fr.shom.ping;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    public static class Property {
        private Property() {
        }

        public static final String ID_DESCRIPTION = "Unique ID";
        public static final String URN_DESCRIPTION = "Uniform resource name (RFC 8141 compliant)";
        public static final String NAME_DESCRIPTION = "Name";
        public static final String TECHNICAL_IDENTIFIER = "Technical identifier";
        public static final String TITLE_DESCRIPTION = "Title";
        public static final String INTERNATIONAL_NAME_DESCRIPTION = "International name";
        public static final String INTERNATIONAL_TITLE_DESCRIPTION = "International title";
        public static final String PATH_DESCRIPTION = "Path";
        public static final String INFORMATION_DESCRIPTION = "Information";
        public static final String LOCALIZED_INFORMATION_DESCRIPTION = "More information";
        public static final String INTERNATIONAL_DISCLAIMER = "International disclaimer";
        public static final String DISCLAIMER = "Disclaimer";
        public static final String COUNTRY_DESCRIPTION = "Country (ISO 3166-1 compliant)";
        public static final String GEOMETRY_DESCRIPTION = "Geometry (EPSG:3857)";
        public static final String GEOMETRY_EXAMPLE = "{ \"type\": \"MultiPolygon\", \"coordinates\": [ [ [ [ -6410445.25813015, 6026834.45716739 ], [ " +
            "-6117596.47229202, 6039411.99301505 ], [ -6100619.73108402, 5797367.13173827 ], [ -6410445.25813015, 5797367.13173827 ], [ -6410445.25813015, " +
            "6026834.45716739 ] ] ] ] }";
        public static final String LOCALIZED_TEXT_EXAMPLE = "{ \"fr\" : \"Traduction en français\", \"en\" : \"English translation\" }";
        public static final String EMPTY_LIST_EXAMPLE = "[]";
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info().title("Ping API").version("1.0"));
    }

    @Bean
    public GroupedOpenApi allApis() {
        return GroupedOpenApi.builder()
            .group("all")
            .pathsToMatch("/**")
            .build();
    }

    @Bean
    public GroupedOpenApi secomAPI() {
        return GroupedOpenApi.builder()
            .group("secom")
            .pathsToMatch("/secom/**")
            .build();
    }

}
