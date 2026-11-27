package fr.shom.ping;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    public static final String DATA_SERVICES_ES_GROUP = "datos";
    public static final String DATA_SERVICES_EN_GROUP = "datos-en";
    public static final String DATA_SERVICES_ES_DISPLAY_NAME = "PISAC - Servicios de datos (S-124 / SECOM)";
    public static final String DATA_SERVICES_EN_DISPLAY_NAME = "PISAC - Data Services (S-124 / SECOM)";

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
            .info(new Info()
                .title("PISAC — Servicios de datos (SHN Venezuela)")
                .description("Servicios de datos de PISAC: advertencias náuticas en OHI S-124 (REST /nw y SECOM IEC 63173-2).")
                .contact(new Contact().name("SHN Venezuela").email("shnayunav@shn.gob.ve"))
                .version("1.0"));
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

    /**
     * PISAC: especificación pública en español (/v3/api-docs/datos).
     */
    @Bean
    public GroupedOpenApi dataServicesAPI() {
        return GroupedOpenApi.builder()
            .group(DATA_SERVICES_ES_GROUP)
            .displayName(DATA_SERVICES_ES_DISPLAY_NAME)
            .pathsToMatch("/nw/**", "/secom/**")
            .addOpenApiCustomizer(spanishDataServicesCustomizer())
            .build();
    }

    /**
     * PISAC: especificación pública en inglés (/v3/api-docs/datos-en) para cambio dinámico ES/EN en Swagger UI.
     */
    @Bean
    public GroupedOpenApi dataServicesEnglishAPI() {
        return GroupedOpenApi.builder()
            .group(DATA_SERVICES_EN_GROUP)
            .displayName(DATA_SERVICES_EN_DISPLAY_NAME)
            .pathsToMatch("/nw/**", "/secom/**")
            .addOpenApiCustomizer(englishDataServicesCustomizer())
            .build();
    }

    @Bean
    public SwaggerIndexTransformer swaggerIndexTransformer(
        SwaggerUiConfigProperties swaggerUiConfig,
        SwaggerUiOAuthProperties swaggerUiOAuthProperties,
        SwaggerWelcomeCommon swaggerWelcomeCommon,
        ObjectMapperProvider objectMapperProvider
    ) {
        return new PisacSwaggerIndexTransformer(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
    }

    public static OpenApiCustomizer spanishDataServicesCustomizer() {
        return openApi -> localizeDataServicesOpenApi(openApi, false);
    }

    public static OpenApiCustomizer englishDataServicesCustomizer() {
        return openApi -> localizeDataServicesOpenApi(openApi, true);
    }

    @SuppressWarnings("rawtypes")
    private static void localizeDataServicesOpenApi(OpenAPI openApi, boolean english) {
        if (openApi.getInfo() == null) {
            openApi.setInfo(new Info());
        }
        openApi.getInfo()
            .title(english
                ? "PISAC — Data Services (SHN Venezuela)"
                : "PISAC — Servicios de datos (SHN Venezuela)")
            .description(english
                ? "Public PISAC data services for navigational warnings compliant with IHO S-124 Edition 2.0.0: XML/GML REST API (/nw) and IEC 63173-2 SECOM service (/secom). Technical support: shnayunav@shn.gob.ve."
                : "Servicios públicos de datos de PISAC para advertencias náuticas conforme a la norma OHI S-124 edición 2.0.0: API REST XML/GML (/nw) y servicio SECOM conforme a IEC 63173-2 (/secom). Soporte técnico: shnayunav@shn.gob.ve.")
            .version("1.0")
            .contact(new Contact().name("SHN Venezuela").email("shnayunav@shn.gob.ve"));

        if (openApi.getTags() != null) {
            for (Tag tag : openApi.getTags()) {
                if ("S-124".equalsIgnoreCase(tag.getName())) {
                    tag.setDescription(english
                        ? "S-124 Navigational Warnings REST API (XML/GML)"
                        : "API REST S-124 de advertencias náuticas (XML/GML)");
                } else if ("Secom".equalsIgnoreCase(tag.getName())) {
                    tag.setDescription(english
                        ? "SECOM API (IEC 63173-2) for S-124 data exchange"
                        : "API SECOM (IEC 63173-2) de intercambio de datos S-124");
                }
            }
        }

        if (openApi.getPaths() != null) {
            for (Map.Entry<String, PathItem> entry : openApi.getPaths().entrySet()) {
                String path = entry.getKey();
                Operation getOp = entry.getValue() != null ? entry.getValue().getGet() : null;
                if (getOp == null) {
                    continue;
                }
                localizeOperation(path, getOp, english);
            }
        }

        if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
            for (Map.Entry<String, Schema> schemaEntry : openApi.getComponents().getSchemas().entrySet()) {
                Schema schema = schemaEntry.getValue();
                if (schema == null || schema.getProperties() == null) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Schema> properties = schema.getProperties();
                for (Map.Entry<String, Schema> propEntry : properties.entrySet()) {
                    localizeSchemaProperty(propEntry.getKey(), propEntry.getValue(), english);
                }
            }
        }
    }

    private static void localizeOperation(String path, Operation op, boolean english) {
        boolean singleNwMessage = path.endsWith("/Get_NW_Message");
        if (path.endsWith("/Get_NW_Messages")) {
            op.setSummary(english
                ? "Get in-force navigational warnings in S-124 format (XML)"
                : "Obtener advertencias náuticas vigentes en formato S-124 (XML)");
            op.setDescription(english
                ? "Returns an S-124 Exchange Set (XML/GML) with in-force navigational warnings. Omit nameOfSeries to retrieve all S-124 series."
                : "Devuelve un conjunto de intercambio S-124 (XML/GML) con las advertencias náuticas vigentes. Si se omite nameOfSeries, se devuelven todas las series S-124.");
        } else if (singleNwMessage) {
            op.setSummary(english
                ? "Get a specific navigational warning in S-124 format (XML)"
                : "Obtener una advertencia náutica específica en formato S-124 (XML)");
            op.setDescription(english
                ? "Returns the S-124 dataset (XML/GML) for a specific in-force navigational warning identified by series, warning number, and year."
                : "Devuelve el dataset S-124 (XML/GML) de una advertencia náutica vigente identificada por serie, número y año.");
        } else if (path.endsWith("/Get_in-force_NW_MessageIdentifiers")) {
            op.setSummary(english
                ? "List in-force S-124 navigational warning identifiers (XML)"
                : "Listar identificadores de advertencias náuticas vigentes en S-124 (XML)");
            op.setDescription(english
                ? "Returns MRN references and identifiers of currently in-force navigational warnings. The nameOfSeries parameter is optional; omit it to list all S-124 series."
                : "Devuelve las referencias MRN e identificadores de las advertencias náuticas actualmente en vigencia. El parámetro nameOfSeries es opcional; si se omite, lista todas las series S-124.");
        } else if (path.endsWith("/v1/ping")) {
            op.setSummary(english
                ? "Check if the SECOM service instance is available"
                : "Verificar si la instancia del servicio SECOM está disponible");
            op.setDescription(english
                ? "Health check operation defined by IEC 63173-2 (SECOM) for a specific series and language instance."
                : "Operación de verificación de disponibilidad definida por IEC 63173-2 (SECOM) para una instancia de serie e idioma.");
        } else if (path.endsWith("/v1/capability")) {
            op.setSummary(english
                ? "Get capabilities of the SECOM service instance"
                : "Obtener las capacidades de la instancia del servicio SECOM");
            op.setDescription(english
                ? "Returns the supported S-100 data product types, container types, and protocol capabilities of the SECOM service instance."
                : "Devuelve los tipos de productos S-100, contenedores soportados y capacidades de la instancia SECOM.");
        } else if (path.endsWith("/v1/object")) {
            op.setSummary(english
                ? "Obtain signed S-124 navigational warning packages from SECOM instance"
                : "Obtener paquetes S-124 de advertencias náuticas desde la instancia SECOM");
            op.setDescription(english
                ? "All query parameters are optional. This service instance offers data with containerType=1 and dataProductType=S124. Leave dataReference empty to retrieve all published navigational warnings in the series. If used, dataReference is the UUID of a navigational warning (e.g. 45c70257-318a-49e6-b789-3218e1316b59 in https://pisac.shn.gob.ve/avurnav-notice/45c70257-318a-49e6-b789-3218e1316b59)."
                : "Todos los parámetros de consulta son opcionales. Esta instancia ofrece datos con containerType=1 y dataProductType=S124. Deje dataReference vacío para recuperar todas las advertencias náuticas publicadas de la serie. Si se utiliza, dataReference corresponde al UUID de una advertencia náutica (p. ej. 45c70257-318a-49e6-b789-3218e1316b59 en https://pisac.shn.gob.ve/avurnav-notice/45c70257-318a-49e6-b789-3218e1316b59).");
        }

        if (op.getParameters() != null) {
            for (Parameter param : op.getParameters()) {
                localizeParameter(param, singleNwMessage, english);
            }
        }

        if (op.getResponses() != null) {
            for (Map.Entry<String, ApiResponse> respEntry : op.getResponses().entrySet()) {
                localizeResponse(path, respEntry.getKey(), respEntry.getValue(), english);
            }
        }
    }

    private static void localizeParameter(Parameter param, boolean singleNwMessage, boolean english) {
        if (param == null || param.getName() == null) {
            return;
        }
        switch (param.getName()) {
            case "version" -> param.setDescription(english
                ? "S-124 API version (IHO S-124 2.0.0)"
                : "Versión de la API S-124 (OHI S-124 2.0.0)");
            case "nameOfSeries" -> {
                if (singleNwMessage) {
                    param.setDescription(english ? "Series name" : "Nombre de la serie");
                } else {
                    param.setDescription(english
                        ? "Series name (optional; leave empty to return all series)"
                        : "Nombre de la serie (opcional; vacío devuelve todas las series)");
                }
            }
            case "warningNumbers" -> param.setDescription(english
                ? "Optional list of warning numbers (e.g. 20/26). Leave empty to return all in-force warnings."
                : "Lista opcional de números de advertencia (p. ej. 20/26). Vacío devuelve todas las vigentes.");
            case "warningNumber" -> param.setDescription(english
                ? "Warning number within the series (e.g. 20)"
                : "Número de la advertencia dentro de la serie (p. ej. 20)");
            case "year" -> param.setDescription(english
                ? "4-digit year of the warning (e.g. 2026)"
                : "Año de 4 dígitos de la advertencia (p. ej. 2026)");
            case "lang" -> param.setDescription(english
                ? "S-124 dataset language (es or en)"
                : "Idioma del dataset S-124 (es o en)");
            case "serviceInstanceId" -> {
                param.setDescription(english
                    ? "ID of the SECOM service instance (<series>_<lang>)"
                    : "Identificador de la instancia del servicio SECOM (<serie>_<idioma>)");
                param.setExample(english ? "advertencia_local_ve_en" : "advertencia_local_ve_es");
            }
            case "dataReference" -> param.setDescription(english
                ? "Optional object ID (UUID). Leave empty to retrieve all published objects in the series."
                : "Identificador opcional del objeto (UUID). Déjelo vacío para recuperar todos los objetos publicados de la serie.");
            case "containerType" -> param.setDescription(english
                ? "SECOM container type (1 = S100_ExchangeSet)"
                : "Tipo de contenedor SECOM (1 = S100_ExchangeSet)");
            case "dataProductType" -> param.setDescription(english
                ? "S-100 data product type"
                : "Tipo de producto de datos S-100");
            case "productVersion" -> param.setDescription(english
                ? "Product specification version based on S-100"
                : "Versión de la especificación de producto basada en S-100");
            case "page" -> param.setDescription(english
                ? "Requested pagination page"
                : "Número de página solicitado");
            case "pageSize" -> param.setDescription(english
                ? "Requested page size"
                : "Cantidad de elementos por página");
            default -> {
            }
        }
    }

    private static void localizeResponse(String path, String code, ApiResponse resp, boolean english) {
        if (resp == null) {
            return;
        }
        if (path.startsWith("/nw/")) {
            if ("200".equals(code)) {
                resp.setDescription(english ? "Successful response (S-124 XML)" : "Respuesta exitosa (XML S-124)");
            }
            return;
        }
        if (path.endsWith("/v1/ping")) {
            if ("200".equals(code)) {
                resp.setDescription(english ? "Service instance available" : "Instancia del servicio disponible");
            } else if ("404".equals(code)) {
                resp.setDescription(english
                    ? "Service instance {serviceInstanceId} not available"
                    : "Instancia del servicio {serviceInstanceId} no disponible");
            }
        } else if (path.endsWith("/v1/capability")) {
            if ("200".equals(code)) {
                resp.setDescription(english ? "Service instance capabilities" : "Capacidades de la instancia del servicio");
            } else if ("404".equals(code)) {
                resp.setDescription(english
                    ? "Cannot get capability for serviceInstanceId: {serviceInstanceId}"
                    : "No se pudieron obtener las capacidades para serviceInstanceId: {serviceInstanceId}");
            }
        } else if (path.endsWith("/v1/object")) {
            switch (code) {
                case "200" -> resp.setDescription(english
                    ? "totalItem indicates the number of in-force navigational warnings that the service instance returns. A responseText is included when warning data is missing in the response."
                    : "totalItem indica la cantidad de advertencias náuticas vigentes que devuelve la instancia del servicio. Se incluye responseText cuando faltan datos en la respuesta.");
                case "400" -> resp.setDescription(english ? "Incorrect request" : "Solicitud incorrecta");
                case "403" -> resp.setDescription(english
                    ? "Not authorized to access requested information"
                    : "No autorizado para acceder a la información solicitada");
                case "404" -> resp.setDescription(english ? "Information not found" : "Información no encontrada");
                default -> {
                }
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static void localizeSchemaProperty(String propName, Schema propSchema, boolean english) {
        if (propSchema == null || propName == null) {
            return;
        }
        switch (propName) {
            case "dataReference" -> propSchema.setDescription(english
                ? "Optional object ID (UUID). Leave empty to retrieve all published objects in the series."
                : "Identificador opcional del objeto (UUID). Déjelo vacío para recuperar todos los objetos publicados de la serie.");
            case "containerType" -> propSchema.setDescription(english ? "Type of container" : "Tipo de contenedor");
            case "dataProductType" -> propSchema.setDescription(english ? "Data product type" : "Tipo de producto de datos");
            case "productVersion" -> propSchema.setDescription(english
                ? "Product version based on S-100"
                : "Versión del producto basada en S-100");
            case "page" -> propSchema.setDescription(english
                ? "Pagination page requested"
                : "Página solicitada");
            case "pageSize" -> propSchema.setDescription(english
                ? "Pagination size requested"
                : "Tamaño de página solicitado");
            case "id" -> propSchema.setDescription(english ? "Unique ID" : "Identificador único");
            default -> {
            }
        }
    }
}
