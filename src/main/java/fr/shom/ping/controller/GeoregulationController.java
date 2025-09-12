package fr.shom.ping.controller;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.dto.EnumTypeWithTranslationDto;
import fr.shom.ping.model.dto.GeoregulationMapViewDto;
import fr.shom.ping.model.dto.GeoregulationModalDetailsDto;
import fr.shom.ping.model.predicate.GeoregulationMapSearchCriteria;
import fr.shom.ping.repository.GeoregulationActivitiesRepository;
import fr.shom.ping.repository.GeoregulationPartTypeRepository;
import fr.shom.ping.repository.GeoregulationTypesRepository;
import fr.shom.ping.service.GeoregulationService;
import fr.shom.ping.utils.EnumUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


import static fr.shom.ping.utils.GpxUtil.DEFAULT_LANGUAGE;

@Tag(name = "Georegulation", description = "Georegulation API")
@RestController
@RequestMapping("/api/georegulations")
@RequiredArgsConstructor
public class GeoregulationController {
    private final GeoregulationService service;
    private final GeoregulationPartTypeRepository georegulationPartTypeRepository;
    private final GeoregulationTypesRepository georegulationTypeRepository;
    private final GeoregulationActivitiesRepository activitiesRepository;

    @Operation(summary = "Get georegulation (modal details)", tags = "Georegulation")
    @GetMapping("/{id}/modal-details")
    public GeoregulationModalDetailsDto getModalDetails(@PathVariable UUID id) {
        return service.getModalDetails(id);
    }

    @GetMapping("/map")
    @Operation(summary = "Get georegulations by search criteria (Map View)", tags = "Georegulation")
    public List<GeoregulationMapViewDto> getAllBySearchCriteriaForMapView(GeoregulationMapSearchCriteria searchCriteria) {
        return service.findAllBySearchCriteriaForMapView(searchCriteria);
    }

    @GetMapping(value = "/{id}/geometries/kml")
    @Operation(summary = "Get geometries of georegulation by id (KML)", tags = "Georegulation")
    public @ResponseBody
    byte[] downloadGeometriesAsKml(@PathVariable UUID id, @Parameter(hidden = true) HttpServletResponse response) {
        try {
            response.addHeader("Content-Disposition", "attachment;filename=export.kml");
            response.setContentType("application/xml");
            return service.downloadGeometriesAsKml(id);
        } catch (ServiceException exception) {
            throw new BadRequestException(exception.getMessage());
        }
    }

    @GetMapping(value = "/{id}/geometries/shp")
    @Operation(summary = "Get geometries of georegulation by id (SHP)", tags = "Georegulation")
    public void downloadGeometriesAsShp(@PathVariable UUID id, @Parameter(hidden = true) HttpServletResponse response) {
        try {
            response.addHeader("Content-Disposition", "attachment;filename=shapefiles.zip");
            response.setContentType("application/zip");
            service.downloadGeometriesAsShp(id, response);
        } catch (ServiceException exception) {
            throw new BadRequestException(exception.getMessage());
        }
    }

    @GetMapping(value = "/{id}/geometries/gpx")
    @Operation(summary = "Get geometries of georegulation by id (GPX)", tags = "Georegulation")
    public void downloadGeometriesAsGpx(@PathVariable UUID id, @RequestParam(defaultValue = DEFAULT_LANGUAGE) String lang,
        @Parameter(hidden = true) HttpServletResponse response) {
        try {
            response.setContentType("application/xml");
            service.downloadGeometriesAsGpx(id, lang, response);
        } catch (ServiceException exception) {
            throw new BadRequestException(exception.getMessage());
        }
    }

    @GetMapping(value = "/types")
    @Operation(summary = "Get the list of georegulation types", tags = "Georegulation")
    public List<EnumTypeWithTranslationDto> getTypes() {
        return EnumUtil.convertToEnumTypeWithTranslationDto(georegulationTypeRepository.findAll(Sort.by("enumName")));
    }

    @GetMapping(value = "/activities")
    @Operation(summary = "Get the list of georegulation activities", tags = "Georegulation")
    public List<EnumTypeWithTranslationDto> getActivities() {
        return EnumUtil.convertToEnumTypeWithTranslationDto(activitiesRepository.findAll(Sort.by("enumName")));
    }

    @GetMapping(value = "/restriction-categories")
    @Operation(summary = "Get list of georegulation categories restrictions", tags = "Georegulation")
    public List<EnumTypeWithTranslationDto> getRestrictionCategories() {
        return EnumUtil.convertToEnumTypeWithTranslationDto(georegulationPartTypeRepository.findAll(Sort.by("enumName")));
    }

}
