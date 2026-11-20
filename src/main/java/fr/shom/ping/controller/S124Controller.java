package fr.shom.ping.controller;

import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.dto.AbstractS124ExchangeSetBaseDto;
import fr.shom.ping.service.S124Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static fr.shom.ping.service.S124Service.VERSION_1;
import static fr.shom.ping.service.S124Service.VERSION_2;
import static fr.shom.ping.utils.S124Util.DEFAULT_LANGUAGE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.util.ObjectUtils.isEmpty;

@RestController
@RequestMapping("/nw")
@RequiredArgsConstructor
@Tag(name = "S-124", description = "API REST S-124 de advertencias náuticas (XML)")
public class S124Controller {
    private final S124Service service;

    @GetMapping(value = "/{version}/Get_NW_Messages", produces = APPLICATION_XML_VALUE)
    @Operation(summary = "Obtener advertencias náuticas vigentes en formato S-124 (XML)")
    public AbstractS124ExchangeSetBaseDto getNwMessages(
        @Parameter(description = "Versión de la API S-124", schema = @Schema(allowableValues = {"v1", "v2"}, example = "v2"))
        @PathVariable String version,
        @Parameter(description = "Nombre de la serie (opcional; vacío devuelve todas las series)", example = "ADVERTENCIA LOCAL VE")
        @RequestParam(required = false) String nameOfSeries,
        @Parameter(description = "Lista opcional de números de advertencia (p. ej. 20/26). Vacío devuelve todas las vigentes.")
        @RequestParam(required = false) List<String> warningNumbers,
        @Parameter(description = "Idioma del dataset S-124", schema = @Schema(allowableValues = {"es", "en"}, defaultValue = "es"))
        @RequestParam(defaultValue = DEFAULT_LANGUAGE) String lang) {
        checkIfversionIsSupported(version);

        if (isEmpty(warningNumbers)) {
            return service.findByNameOfSeries(version, nameOfSeries, lang);
        }
        return service.findByNameOfSeriesAndWarningNumberAndYearList(version, nameOfSeries, warningNumbers, lang);
    }

    @GetMapping(value = "{version}/Get_NW_Message", produces = APPLICATION_XML_VALUE)
    @Operation(summary = "Obtener una advertencia náutica específica en formato S-124 (XML)")
    public AbstractS124ExchangeSetBaseDto getNwMessage(
        @Parameter(description = "Versión de la API S-124", schema = @Schema(allowableValues = {"v1", "v2"}, example = "v2"))
        @PathVariable String version,
        @Parameter(description = "Nombre de la serie", example = "ADVERTENCIA LOCAL VE")
        @RequestParam(required = false) String nameOfSeries,
        @Parameter(description = "Número de la advertencia dentro de la serie (p. ej. 20)")
        @RequestParam(required = false) Integer warningNumber,
        @Parameter(description = "Año de 4 dígitos de la advertencia (p. ej. 2026)")
        @RequestParam(required = false) Integer year,
        @Parameter(description = "Idioma del dataset S-124", schema = @Schema(allowableValues = {"es", "en"}, defaultValue = "es"))
        @RequestParam(defaultValue = DEFAULT_LANGUAGE) String lang) {
        checkIfversionIsSupported(version);
        return service.findByNameOfSeriesAndWarningNumberAndYear(version, nameOfSeries, warningNumber, year, lang);
    }

    @GetMapping(value = "{version}/Get_in-force_NW_MessageIdentifiers", produces = APPLICATION_XML_VALUE)
    @Operation(summary = "Listar identificadores de advertencias náuticas vigentes en S-124 (XML)")
    public AbstractS124ExchangeSetBaseDto getInForceNwMessageIdentifiers(
        @Parameter(description = "Versión de la API S-124", schema = @Schema(allowableValues = {"v1", "v2"}, example = "v2"))
        @PathVariable String version,
        @Parameter(description = "Nombre de la serie (opcional; vacío devuelve todas las series)", example = "ADVERTENCIA LOCAL VE")
        @RequestParam(required = false) String nameOfSeries) {
        checkIfversionIsSupported(version);
        return service.getInForceNwMessageIdentifiers(version, nameOfSeries);
    }

    public static void checkIfversionIsSupported(String version) {
        if (!List.of(VERSION_1, VERSION_2).contains(version)) {
            throw new NotFoundException("Unknown version : " + version);
        }
    }
}
