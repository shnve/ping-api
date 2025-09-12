package fr.shom.ping.controller;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.dto.EnumTypeWithTranslationDto;
import fr.shom.ping.model.dto.WarningDetailDto;
import fr.shom.ping.model.dto.WarningHazardTypeGeneralDto;
import fr.shom.ping.model.dto.WarningMapViewDto;
import fr.shom.ping.model.dto.WarningTableViewDto;
import fr.shom.ping.model.predicate.WarningSearchCriteria;
import fr.shom.ping.repository.WarningHazardTypeDetailsRepository;
import fr.shom.ping.repository.WarningReferenceCategoryRepository;
import fr.shom.ping.repository.WarningTypeRepository;
import fr.shom.ping.service.WarningService;
import fr.shom.ping.utils.EnumUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static fr.shom.ping.utils.GpxUtil.DEFAULT_LANGUAGE;

@RestController
@RequestMapping("/api/warnings")
@RequiredArgsConstructor
@Tag(name = "Warning", description = "Warning API")
public class WarningController {

    private final WarningService service;

    private final WarningTypeRepository warningTypeRepository;

    private final WarningHazardTypeDetailsRepository warningHazardTypeDetailsRepository;

    private final WarningReferenceCategoryRepository referenceCategoryRepository;

    @GetMapping("/{id}")
    @Operation(summary = "Get warning by id", tags = "Warning")
    public WarningDetailDto getOne(@PathVariable UUID id) {
        return service.findOne(id);
    }

    @GetMapping("/map")
    @Operation(summary = "Get warnings by search criteria (Map View)", tags = "Warning")
    public List<WarningMapViewDto> getAllBySearchCriteriaForMapView(WarningSearchCriteria warningSearchCriteria) {
        return service.findAllBySearchCriteriaAndApplyMapViewDto(warningSearchCriteria);
    }

    @GetMapping("/table")
    @Operation(summary = "Get paginated warnings by search criteria (Table View)", tags = "Warning")
    public ResponseEntity<PagedModel<EntityModel<WarningTableViewDto>>> getAllBySearchCriteriaForTableView(WarningSearchCriteria warningSearchCriteria,
        Pageable pageable,
        PagedResourcesAssembler<WarningTableViewDto> pagedResourcesAssembler) {

        Page<WarningTableViewDto> warnings = service.findAllBySearchCriteriaAndApplyTableViewDto(warningSearchCriteria, pageable);
        PagedModel<EntityModel<WarningTableViewDto>> paginatedWarnings;

        if (warnings.hasContent()) {
            paginatedWarnings = pagedResourcesAssembler.toModel(warnings);
        } else {
            paginatedWarnings = (PagedModel<EntityModel<WarningTableViewDto>>) pagedResourcesAssembler.toEmptyModel(warnings, WarningTableViewDto.class);
        }

        return ResponseEntity.ok(paginatedWarnings);
    }

    @GetMapping("/detailed-table")
    @Operation(summary = "Get paginated warnings by search criteria (Detailed Table View)", tags = "Warning")
    public ResponseEntity<PagedModel<EntityModel<WarningDetailDto>>> getAllBySearchCriteriaForDetailedTableView(WarningSearchCriteria warningSearchCriteria,
        Pageable pageable,
        PagedResourcesAssembler<WarningDetailDto> pagedResourcesAssembler) {

        Page<WarningDetailDto> warnings = service.findAllBySearchCriteriaAndApplyDetailedTableViewDto(warningSearchCriteria, pageable);
        PagedModel<EntityModel<WarningDetailDto>> paginatedWarnings;

        if (warnings.hasContent()) {
            paginatedWarnings = pagedResourcesAssembler.toModel(warnings);
        } else {
            paginatedWarnings = (PagedModel<EntityModel<WarningDetailDto>>) pagedResourcesAssembler.toEmptyModel(warnings, WarningDetailDto.class);
        }

        return ResponseEntity.ok(paginatedWarnings);
    }

    @GetMapping(value = "/{id}/geometries/kml")
    @Operation(summary = "Get warning geometries by id as kml", tags = "Warning")
    public @ResponseBody
    byte[] downloadGeometriesAsKml(@PathVariable UUID id, HttpServletResponse response) {
        try {
            response.addHeader("Content-Disposition", "attachment;filename=export.kml");
            response.setContentType("application/xml");
            return service.downloadGeometriesAsKml(id);
        } catch (ServiceException exception) {
            throw new BadRequestException(exception.getMessage());
        }
    }

    @GetMapping(value = "/{id}/geometries/shp")
    @Operation(summary = "Get warning geometries by id as shp files in zip", tags = "Warning")
    public void downloadGeometriesAsShp(@PathVariable UUID id, HttpServletResponse response) {
        try {
            response.addHeader("Content-Disposition", "attachment;filename=shapefiles.zip");
            response.setContentType("application/zip");
            service.downloadGeometriesAsShp(id, response);
        } catch (ServiceException exception) {
            throw new BadRequestException(exception.getMessage());
        }
    }

    @GetMapping(value = "/{id}/geometries/gpx")
    @Operation(summary = "Get warning geometries by id as single gpx file", tags = "Warning")
    public void downloadGeometriesAsGpx(
        @PathVariable UUID id,
        @RequestParam(defaultValue = DEFAULT_LANGUAGE) String lang,
        HttpServletResponse response) {
        try {
            response.setContentType("application/xml");
            service.downloadGeometriesAsGpx(id, lang, response);
        } catch (ServiceException exception) {
            throw new BadRequestException(exception.getMessage());
        }
    }

    @GetMapping(value = "/download-gpx-zip")
    @Operation(summary = "Get warnings geometries by ids as gpx files (one per warning) in zip", tags = "Warning")
    public void downloadGeometriesAsGpxZip(
        @RequestParam List<UUID> warningIds,
        @RequestParam(defaultValue = DEFAULT_LANGUAGE) String lang,
        HttpServletResponse response) {
        try {
            response.addHeader("Content-Disposition", "attachment;filename=gpx.zip");
            response.setContentType("application/zip");
            service.downloadGeometriesAsGpxZip(warningIds, lang, response);
        } catch (ServiceException exception) {
            throw new BadRequestException(exception.getMessage());
        }
    }

    @GetMapping("/types")
    @Operation(summary = "Get the list of Warning types", tags = "Warning")
    public List<EnumTypeWithTranslationDto> getTypes() {
        return EnumUtil.convertToEnumTypeWithTranslationDto(warningTypeRepository.findAll(Sort.by("enumName")));
    }

    @GetMapping("/reference-categories")
    @Operation(summary = "Get the list of Warning Reference Categories", tags = "Warning")
    public List<EnumTypeWithTranslationDto> getReferenceCategories() {
        return EnumUtil.convertToEnumTypeWithTranslationDto(referenceCategoryRepository.findAll(Sort.by("enumName")));
    }

    @GetMapping("/hazard-type-generals")
    @Operation(summary = "Get the list of Warning Hazard Type Generals", tags = "Warning")
    public List<WarningHazardTypeGeneralDto> getHazardTypeGenerals() {
        return service.getHazardTypeGenerals();
    }

    @GetMapping("/hazard-type-details")
    @Operation(summary = "Get the list of Warning Hazard Type Details", tags = "Warning")
    public List<EnumTypeWithTranslationDto> getHazardTypeDetails() {
        return EnumUtil.convertToEnumTypeWithTranslationDto(warningHazardTypeDetailsRepository.findAll(Sort.by("enumName")));
    }

}
