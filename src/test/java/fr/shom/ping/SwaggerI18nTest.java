package fr.shom.ping;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerI18nTest {

    private static OpenAPI sampleOpenApi() {
        Operation nwIdentifiers = new Operation()
            .addParametersItem(new Parameter().name("version"))
            .addParametersItem(new Parameter().name("nameOfSeries"))
            .addParametersItem(new Parameter().name("lang"))
            .responses(new ApiResponses().addApiResponse("200", new ApiResponse()));

        Operation secomObject = new Operation()
            .addParametersItem(new Parameter().name("serviceInstanceId"))
            .addParametersItem(new Parameter().name("dataReference"))
            .responses(new ApiResponses()
                .addApiResponse("200", new ApiResponse())
                .addApiResponse("400", new ApiResponse()));

        return new OpenAPI()
            .tags(List.of(new Tag().name("S-124"), new Tag().name("Secom")))
            .paths(new Paths()
                .addPathItem("/nw/{version}/Get_in-force_NW_MessageIdentifiers", new PathItem().get(nwIdentifiers))
                .addPathItem("/secom/{serviceInstanceId}/v1/object", new PathItem().get(secomObject)));
    }

    @Test
    void spanishAndEnglishCustomizersLocalizeAllMetadataParametersAndResponses() {
        OpenAPI esApi = sampleOpenApi();
        SwaggerConfig.spanishDataServicesCustomizer().customise(esApi);

        assertThat(esApi.getInfo().getTitle()).contains("Servicios de datos");
        assertThat(esApi.getInfo().getDescription()).contains("shnayunav@shn.gob.ve");
        assertThat(esApi.getPaths().get("/nw/{version}/Get_in-force_NW_MessageIdentifiers").getGet().getSummary())
            .contains("Listar identificadores");
        assertThat(esApi.getPaths().get("/secom/{serviceInstanceId}/v1/object").getGet().getResponses().get("400").getDescription())
            .isEqualTo("Solicitud incorrecta");

        OpenAPI enApi = sampleOpenApi();
        SwaggerConfig.englishDataServicesCustomizer().customise(enApi);

        assertThat(enApi.getInfo().getTitle()).contains("Data Services");
        assertThat(enApi.getInfo().getDescription()).contains("shnayunav@shn.gob.ve");
        assertThat(enApi.getPaths().get("/nw/{version}/Get_in-force_NW_MessageIdentifiers").getGet().getSummary())
            .contains("List in-force");
        assertThat(enApi.getPaths().get("/nw/{version}/Get_in-force_NW_MessageIdentifiers").getGet().getParameters().get(1).getDescription())
            .contains("optional; leave empty to return all series");
        assertThat(enApi.getPaths().get("/secom/{serviceInstanceId}/v1/object").getGet().getResponses().get("400").getDescription())
            .isEqualTo("Incorrect request");
    }

    @Test
    void indexTransformerInjectsLanguageSelectorAndBothOpenApiEndpoints() {
        String sampleHtml = "<!DOCTYPE html><html><head><title>Swagger UI</title></head><body><div id=\"swagger-ui\"></div></body></html>";
        String transformed = PisacSwaggerIndexTransformer.injectLanguageSelector(sampleHtml);

        assertThat(transformed)
            .contains("id=\"pisac-swagger-bar\"")
            .contains("id=\"pisac-lang-es\"")
            .contains("id=\"pisac-lang-en\"")
            .contains("/v3/api-docs/datos")
            .contains("/v3/api-docs/datos-en");
    }
}
