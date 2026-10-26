package fr.shom.ping.service;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.QWarning;
import fr.shom.ping.model.Serie;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.dto.WarningDetailDto;
import fr.shom.ping.model.dto.WarningHazardTypeGeneralDto;
import fr.shom.ping.model.dto.WarningMapViewDto;
import fr.shom.ping.model.dto.WarningTableViewDto;
import fr.shom.ping.model.enums.WarningStatus;
import fr.shom.ping.model.mapper.WarningMapViewTupleToWarningMapViewDto;
import fr.shom.ping.model.mapper.WarningToWarningDetailDtoMapper;
import fr.shom.ping.model.predicate.WarningSearchCriteria;
import fr.shom.ping.model.tuple.WarningMapViewAggregatedTuple;
import fr.shom.ping.model.tuple.WarningMapViewTuple;
import fr.shom.ping.properties.PingProperties;
import fr.shom.ping.repository.WarningHazardTypeGeneralRepository;
import fr.shom.ping.repository.WarningRepository;
import fr.shom.ping.utils.WarningGpxUtil;
import fr.shom.ping.utils.WarningKmlUtil;
import fr.shom.ping.utils.WarningShpUtil;
import fr.shom.ping.utils.search.WarningDetailedTableUtil;
import fr.shom.ping.utils.search.WarningSimpleTableUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import static fr.shom.ping.model.enums.WarningStatus.PUBLISHED;
import static fr.shom.ping.utils.SchemaUtil.AFFECTED_AREA_TABLE;
import static fr.shom.ping.utils.SchemaUtil.FEATURE_PART_TABLE;
import static fr.shom.ping.utils.SchemaUtil.FIXED_DATE_RANGE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.GENERAL_AREA_TABLE;
import static fr.shom.ping.utils.SchemaUtil.SERIE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.SUB_AREA_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_FEATURE_PART_GEOMETRY;
import static fr.shom.ping.utils.SchemaUtil.WARNING_HAZARD_TYPE_DETAILS_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_MAP_VIEW_TUPLE_MAPPING;
import static fr.shom.ping.utils.search.WarningSearchUtil.INNER_JOIN;
import static fr.shom.ping.utils.search.WarningSearchUtil.LEFT_JOIN;
import static fr.shom.ping.utils.search.WarningSearchUtil.ORDER_BY_PUBLICATION_TIME_DESC;
import static fr.shom.ping.utils.search.WarningSearchUtil.WHERE_STATUS_EQUALS;
import static fr.shom.ping.utils.search.WarningSearchUtil.addCriteriaSelectionToSql;
import static fr.shom.ping.utils.search.WarningSearchUtil.addSearchCriteriaParametersToQuery;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.ObjectUtils.isEmpty;

@RequiredArgsConstructor
@Service
public class WarningService {
    private final EntityManager entityManager;

    private final WarningRepository repository;

    private final WarningHazardTypeGeneralRepository warningHazardTypeGeneralRepository;

    private final WarningMapViewTupleToWarningMapViewDto warningMapViewTupleToWarningMapViewDto = new WarningMapViewTupleToWarningMapViewDto();

    private final WarningToWarningDetailDtoMapper warningToWarningDetailDtoMapper = new WarningToWarningDetailDtoMapper();

    private final PingProperties pingProperties;

    private final WarningGpxUtil warningGpxUtil;

    public WarningDetailDto findOne(UUID id) {
        final Warning warning = getWarningById(id);

        if (asList(WarningStatus.DRAFT, WarningStatus.MODEL).contains(warning.getStatus())) {
            throw new ForbiddenException("error.api.warning-current-status-invalid");
        }

        return warningToWarningDetailDtoMapper.apply(warning);
    }

    public Warning getWarningById(UUID id) {
        return repository.findById(id).orElseThrow(NotFoundException::new);
    }

    /**
     * Comply to DCG_RG_027
     *
     * @return list of warning summaries
     */
    public List<WarningMapViewDto> findAllBySearchCriteriaAndApplyMapViewDto(
            WarningSearchCriteria warningSearchCriteria) {
        if (isEmpty(warningSearchCriteria.getGlobalWarningType()) && isEmpty(warningSearchCriteria.getSerieType())) {
            return emptyList();
        }

        StringBuilder queryStringBuilder = new StringBuilder();

        queryStringBuilder
                .append("SELECT w.id, w.name_of_series, w.warning_number, w.year, w.warning_hazard_type_general, " +
                        "d.warning_hazard_type_detail as warning_hazard_type_details, s.type as serie_type, w.publication_time, "
                        +
                        "wfpg.properties as geometry_properties, wfpg.geom as geometry, wfpaa.properties as affected_area_properties, wfpaa.geom as affected_area\n");
        queryStringBuilder.append("FROM warning w\n");
        queryStringBuilder.append(LEFT_JOIN + FEATURE_PART_TABLE + " fp ON w.id = fp.warning_id\n");
        queryStringBuilder.append(LEFT_JOIN + WARNING_HAZARD_TYPE_DETAILS_TABLE + " d ON fp.id = d.feature_part_id\n");
        queryStringBuilder
                .append(LEFT_JOIN + WARNING_FEATURE_PART_GEOMETRY + " wfpg ON fp.id = wfpg.feature_part_id\n");
        queryStringBuilder.append(LEFT_JOIN + AFFECTED_AREA_TABLE + " wfpaa ON fp.id = wfpaa.feature_part_id\n");
        queryStringBuilder.append(INNER_JOIN + SERIE_TABLE + " s ON w.serie_id = s.id\n");

        if (!isEmpty(warningSearchCriteria.getSubAreaIds())) {
            queryStringBuilder.append(INNER_JOIN + GENERAL_AREA_TABLE + " ga ON w.id = ga.warning_id\n");
            queryStringBuilder.append(INNER_JOIN + SUB_AREA_TABLE + " sa ON ga.sub_area_id = sa.id\n");
        }

        if (!isEmpty(warningSearchCriteria.getDateStart()) || !isEmpty(warningSearchCriteria.getDateEnd())) {
            queryStringBuilder.append(LEFT_JOIN + FIXED_DATE_RANGE_TABLE + " fdr ON fp.id = fdr.parent_id\n");
        }

        queryStringBuilder.append(WHERE_STATUS_EQUALS);
        addCriteriaSelectionToSql(queryStringBuilder, warningSearchCriteria);
        queryStringBuilder.append(ORDER_BY_PUBLICATION_TIME_DESC);

        Query query = entityManager.createNativeQuery(queryStringBuilder.toString(), WARNING_MAP_VIEW_TUPLE_MAPPING);
        query.setParameter("warningStatus", PUBLISHED.name());
        addSearchCriteriaParametersToQuery(query, warningSearchCriteria);

        Map<UUID, WarningMapViewAggregatedTuple> warnings = new HashMap<>();
        query.getResultList().forEach(tuple -> aggregateWarningMapViewTuple(warnings, (WarningMapViewTuple) tuple));

        return warnings.values().stream().map(warningMapViewTupleToWarningMapViewDto).collect(toList());
    }

    private void aggregateWarningMapViewTuple(Map<UUID, WarningMapViewAggregatedTuple> warnings,
            WarningMapViewTuple tuple) {
        UUID warningId = tuple.getId();
        if (warnings.containsKey(warningId)) {
            if (tuple.getWarningHazardTypeDetails() != null) {
                warnings.get(warningId).getWarningHazardTypeDetails().add(tuple.getWarningHazardTypeDetails());
            }

            if (tuple.getGeometry() != null) {
                warnings.get(warningId).getGeometries().add(tuple.getGeometry());
            }

            if (tuple.getAffectedArea() != null) {
                warnings.get(warningId).getAffectedAreas().add(tuple.getAffectedArea());
            }
        } else {
            WarningMapViewAggregatedTuple aggregatedTuple = WarningMapViewAggregatedTuple.builder().id(warningId)
                    .nameOfSeries(tuple.getNameOfSeries())
                    .warningNumber(tuple.getWarningNumber())
                    .year(tuple.getYear())
                    .warningHazardTypeGeneral(tuple.getWarningHazardTypeGeneral())
                    .serieType(tuple.getSerieType())
                    .publicationTime(tuple.getPublicationTime())
                    .build();
            if (tuple.getWarningHazardTypeDetails() != null) {
                aggregatedTuple.getWarningHazardTypeDetails().add(tuple.getWarningHazardTypeDetails());
            }

            if (tuple.getGeometry() != null) {
                aggregatedTuple.getGeometries().add(tuple.getGeometry());
            }

            if (tuple.getAffectedArea() != null) {
                aggregatedTuple.getAffectedAreas().add(tuple.getAffectedArea());
            }
            warnings.put(warningId, aggregatedTuple);
        }
    }

    public PageImpl<WarningTableViewDto> findAllBySearchCriteriaAndApplyTableViewDto(
            WarningSearchCriteria warningSearchCriteria, Pageable pageable) {
        return WarningSimpleTableUtil.find(this.entityManager, warningSearchCriteria, pageable);
    }

    public PageImpl<WarningDetailDto> findAllBySearchCriteriaAndApplyDetailedTableViewDto(
            WarningSearchCriteria warningSearchCriteria, Pageable pageable) {
        return WarningDetailedTableUtil.find(entityManager, warningSearchCriteria, pageable);
    }

    public byte[] downloadGeometriesAsKml(UUID id) throws ServiceException {
        return WarningKmlUtil.generateKml(entityManager, id);
    }

    public void downloadGeometriesAsShp(UUID id, HttpServletResponse response) throws ServiceException {
        WarningShpUtil.generateShp(entityManager, id, pingProperties.getGeneratedShapefileFolder(), response);
    }

    public void downloadGeometriesAsGpx(UUID warningId, String lang, HttpServletResponse response)
            throws ServiceException {
        if (!List.of("fr", "en", "es").contains(lang)) {
            throw new BadRequestException("Invalid language");
        }
        final Warning warning = repository.findById(warningId).orElseThrow(NotFoundException::new);
        if (warning.getStatus() != PUBLISHED) {
            throw new BadRequestException("Warning must be in PUBLISHED status");
        }
        warningGpxUtil.generateGpx(warning, lang, pingProperties.getGeneratedGpxFolder(), response);
    }

    public void downloadGeometriesAsGpxZip(List<UUID> warningIds, String lang, HttpServletResponse response)
            throws ServiceException {
        if (!List.of("es", Locale.ENGLISH.getLanguage()).contains(lang)) {
            throw new BadRequestException("Invalid language");
        }
        final List<Warning> warnings = repository.findAllById(warningIds);
        if (warnings.isEmpty()) {
            throw new NotFoundException();
        }
        if (warnings.stream()
                .anyMatch(warning -> warning.getStatus() != PUBLISHED)) {
            throw new BadRequestException("Warnings must be in PUBLISHED status");
        }
        warningGpxUtil.generateGpx(warnings, lang, pingProperties.getGeneratedGpxFolder(), response);
    }

    public List<WarningHazardTypeGeneralDto> getHazardTypeGenerals() {
        return warningHazardTypeGeneralRepository.findAll(Sort.by("enumName")).stream()
                .map(WarningHazardTypeGeneralDto::from)
                .collect(toList());
    }

    public Page<Warning> getPublishedWarnings(Serie serie, int pageNumber, int pageSize) {
        return repository.findAll(QWarning.warning.serie.eq(serie).and(QWarning.warning.status.eq(PUBLISHED)),
                PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "publicationTime")));
    }
}
