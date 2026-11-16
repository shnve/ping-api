package fr.shom.ping.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * PISAC: serie distribuida en S-124, para la página pública «Servicios de datos» del portal. Con el nombre y los
 * idiomas el portal arma las direcciones de la API REST ({@code /nw}) y los identificadores de las instancias SECOM
 * ({@code <nombre en minúsculas con _>_<idioma>}).
 */
@Schema(name = "Data Service Serie DTO")
public record DataServiceSerieDto(
    @Schema(description = "Serie name (value of the nameOfSeries parameter)") String name,
    @Schema(description = "International serie name") String internationalName,
    @Schema(description = "S-124 identifier of the serie") String s124Identifier,
    @Schema(description = "Mandatory languages of the serie (es first)") List<String> languages) {
}
