package fr.shom.ping.service;

import fr.shom.ping.exception.BadRequestException;
import fr.shom.ping.exception.ForbiddenException;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Georegulation;
import fr.shom.ping.model.dto.GeoregulationAdminTableDto;
import fr.shom.ping.model.dto.GeoregulationMapViewDto;
import fr.shom.ping.model.dto.GeoregulationModalDetailsDto;
import fr.shom.ping.model.enums.GeoregulationActivity;
import fr.shom.ping.model.enums.GeoregulationStatus;
import fr.shom.ping.model.mapper.GeoregulationMapViewTupleToGeoregulationMapViewDto;
import fr.shom.ping.model.predicate.GeoregulationMapSearchCriteria;
import fr.shom.ping.model.tuple.GeoregulationMapViewAggregatedTuple;
import fr.shom.ping.model.tuple.GeoregulationMapViewTuple;
import fr.shom.ping.properties.PingProperties;
import fr.shom.ping.repository.GeoregulationRepository;
import fr.shom.ping.utils.GeoregulationGpxUtil;
import fr.shom.ping.utils.GeoregulationKmlUtil;
import fr.shom.ping.utils.GeoregulationShpUtil;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static fr.shom.ping.model.enums.GeoregulationStatus.PUBLISHED;
import static fr.shom.ping.model.enums.GeoregulationType.NAVIGATION;
import static fr.shom.ping.utils.SchemaUtil.FIXED_DATE_RANGE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.GEOREGULATION_MAP_VIEW_TUPLE_MAPPING;
import static fr.shom.ping.utils.SchemaUtil.GEOREGULATION_NUMBER_AUTHORITY_CODE_TABLE;
import static fr.shom.ping.utils.search.WarningSearchUtil.END_OF_DAY_TIME;
import static fr.shom.ping.utils.search.WarningSearchUtil.LEFT_JOIN;
import static fr.shom.ping.utils.search.WarningSearchUtil.START_OF_DAY_TIME;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.ObjectUtils.isEmpty;

@Service
@RequiredArgsConstructor
public class GeoregulationService {

    private final GeoregulationRepository repository;
    private final EntityManager entityManager;
    private final GeoregulationMapViewTupleToGeoregulationMapViewDto georegulationMapViewTupleToGeoregulationMapViewDto =
        new GeoregulationMapViewTupleToGeoregulationMapViewDto();
    private final PingProperties pingProperties;
    private final GeoregulationGpxUtil georegulationGpxUtil;

    public GeoregulationModalDetailsDto getModalDetails(UUID id) {
        Georegulation georegulation = repository.findById(id).orElseThrow(NotFoundException::new);

        if (georegulation.getStatus() == GeoregulationStatus.DRAFT) {
            throw new ForbiddenException("error.api.georegulation-current-status-invalid");
        }

        return new GeoregulationModalDetailsDto(georegulation, false);
    }

    /**
     * Allow to check if the user has entered a criteria as false (therefore checking different from null and false)
     *
     * @param searchCriteria
     * @return true/false
     */
    private static boolean isSearchCriteriaFalse(Boolean searchCriteria) {
        return Boolean.FALSE.equals(searchCriteria);
    }

    /**
     * Allow to check if the user has entered a criteria as true (therefore checking different from null and true)
     *
     * @param searchCriteria
     * @return true/false
     */
    private static boolean isSearchCriteriaTrue(Boolean searchCriteria) {
        return Boolean.TRUE.equals(searchCriteria);
    }

    public List<GeoregulationMapViewDto> findAllBySearchCriteriaForMapView(GeoregulationMapSearchCriteria searchCriteria) {
        if (isSearchCriteriaFalse(searchCriteria.getTemporary()) && isSearchCriteriaFalse(searchCriteria.getPermanent())
            || isSearchCriteriaFalse(searchCriteria.getAllTerritorialSea()) && isSearchCriteriaFalse(searchCriteria.getNotAllTerritorialSea())
            || isSearchCriteriaFalse(searchCriteria.getSeasonalPeriod()) && isSearchCriteriaFalse(searchCriteria.getNotSeasonalPeriod())) {
            return emptyList();
        }

        final StringBuilder queryStringBuilder = new StringBuilder();
        queryStringBuilder.append("SELECT g.id, g.number_year, g.number_text, nac.authority_code as number_authority_code, g.number_date, g.is_temporary, " +
            "g.is_new, g.publication_date, gpf.properties as geometry_properties, gpf.geom as geometry\n");
        queryStringBuilder.append("FROM georegulation g\n");
        queryStringBuilder.append("LEFT JOIN georegulation_part gp ON gp.georegulation_id = g.id\n");
        queryStringBuilder.append("LEFT JOIN georegulation_part_feature gpf ON gpf.part_id = gp.id\n");
        queryStringBuilder.append("LEFT JOIN georegulation_seasonal_period gsp ON gsp.georegulation_id = g.id\n");
        queryStringBuilder.append(LEFT_JOIN + GEOREGULATION_NUMBER_AUTHORITY_CODE_TABLE + " nac ON g.id = nac.georegulation_id\n");

        if (!isEmpty(searchCriteria.getConcernedActivities())) {
            queryStringBuilder.append("LEFT JOIN georegulation_activity ga on ga.georegulation_id = g.id\n");
        }

        if (!isEmpty(searchCriteria.getDateStart()) || !isEmpty(searchCriteria.getDateEnd())) {
            queryStringBuilder.append(LEFT_JOIN + FIXED_DATE_RANGE_TABLE + " fdr_gr ON fdr_gr.parent_id = g.id\n");
            queryStringBuilder.append(LEFT_JOIN + FIXED_DATE_RANGE_TABLE + " fdr_gr_parts ON fdr_gr_parts.parent_id = gp.id\n");
            queryStringBuilder.append("LEFT JOIN georegulation_part_seasonal_period gpsp ON gpsp.part_id = gp.id\n");
        }

        queryStringBuilder.append("WHERE g.status = :georegulationStatus\n");
        queryStringBuilder.append("AND g.type = :georegulationType\n");
        queryStringBuilder.append("AND g.is_public = :georegulationIsPublic\n");
        queryStringBuilder.append("AND g.is_expired = :georegulationIsExpired\n");
        addCriteriaSelectionToSql(queryStringBuilder, searchCriteria);
        queryStringBuilder.append("ORDER BY g.publication_date DESC\n");

        final Query query = entityManager.createNativeQuery(queryStringBuilder.toString(), GEOREGULATION_MAP_VIEW_TUPLE_MAPPING);
        query.setParameter("georegulationStatus", PUBLISHED.name());
        query.setParameter("georegulationType", NAVIGATION.name());
        query.setParameter("georegulationIsPublic", true);
        query.setParameter("georegulationIsExpired", false);
        addSearchCriteriaParametersToQuery(query, searchCriteria);

        final Map<UUID, GeoregulationMapViewAggregatedTuple> georegulations = newHashMap();
        query.getResultList().forEach(tuple -> aggregateGeoregulationMapViewTuple(georegulations, (GeoregulationMapViewTuple) tuple));

        return georegulations.values()
            .stream()
            .map(georegulationMapViewTupleToGeoregulationMapViewDto)
            .collect(toList());
    }

    private static void aggregateGeoregulationMapViewTuple(Map<UUID, GeoregulationMapViewAggregatedTuple> georegulations, GeoregulationMapViewTuple tuple) {
        final UUID georegulationId = tuple.getId();
        if (georegulations.containsKey(georegulationId)) {
            if (tuple.getFeature() != null) {
                georegulations.get(georegulationId).getFeatures().add(tuple.getFeature());
            }

            georegulations.get(georegulationId).getMainNumber().getAuthorityCodes().add(tuple.getMainNumberAuthorityCode());
        } else {
            final GeoregulationMapViewAggregatedTuple aggregatedTuple = GeoregulationMapViewAggregatedTuple.builder()
                .id(georegulationId)
                .isNew(tuple.isNew())
                .isTemporary(tuple.isTemporary())
                .mainNumber(GeoregulationAdminTableDto.GeoregulationAdminTableNumberDto.builder()
                    .year(tuple.getMainNumberYear())
                    .text(tuple.getMainNumberText())
                    .authorityCodes(newHashSet(tuple.getMainNumberAuthorityCode()))
                    .date(tuple.getMainNumberDate())
                    .build())
                .build();

            if (tuple.getFeature() != null) {
                aggregatedTuple.getFeatures().add(tuple.getFeature());
            }
            georegulations.put(georegulationId, aggregatedTuple);
        }
    }

    private static void addCriteriaSelectionToSql(StringBuilder queryStringBuilder, GeoregulationMapSearchCriteria searchCriteria) {
        if (!isEmpty(searchCriteria.getPublishedFromIncludingDate())) {
            queryStringBuilder.append("AND g.publication_date >= :publicationDate\n");
        }

        if (!isEmpty(searchCriteria.getConcernedActivities())) {
            queryStringBuilder.append("AND ga.activity IN (:concernedActivities)\n");
        }

        if (!isEmpty(searchCriteria.getDateStart()) || !isEmpty(searchCriteria.getDateEnd())) {
            // We don't need to check if date_start or date_end is null
            // "null + time '01:23:45'" always returns null, so there is no timestamp limit depending on which attribute is null
            queryStringBuilder.append("AND (\n");

            // check global calendar period
            queryStringBuilder.append("(tsrange(fdr_gr.date_start + time '" + START_OF_DAY_TIME + "', fdr_gr.date_end + time '" + END_OF_DAY_TIME + "') " +
                "&& :startToEndRange\\:\\:tsrange AND (fdr_gr.date_start IS NOT NULL OR fdr_gr.date_end IS NOT NULL))\n");

            // check parts calendar period
            queryStringBuilder.append("OR (tsrange(fdr_gr_parts.date_start + time '" + START_OF_DAY_TIME + "', fdr_gr_parts.date_end + time '" +
                END_OF_DAY_TIME + "') && :startToEndRange\\:\\:tsrange AND (fdr_gr_parts.date_start IS NOT NULL OR fdr_gr_parts.date_end IS NOT NULL))\n");

            // check parts seasonal period
            final String partsSeasonalStart = "CONCAT((SELECT EXTRACT('Year' FROM CURRENT_DATE)),'-',LPAD(cast(gpsp.month_start as VARCHAR), 2, '0'),'-'," +
                "LPAD(cast(gpsp.day_start as VARCHAR), 2, '0'))\\:\\:date";
            final String partsSeasonalEnd = "CONCAT(case when (gpsp.month_start > gpsp.month_end) or (gpsp.month_start = gpsp.month_end and gpsp.day_start > " +
                "gpsp.day_end) then (SELECT EXTRACT('Year' FROM CURRENT_DATE) + 1) else (SELECT EXTRACT('Year' FROM CURRENT_DATE)) end,'-',LPAD(cast(gpsp" +
                ".month_end as VARCHAR), 2, '0'),'-'," +
                "LPAD(cast(gpsp.day_end as VARCHAR), 2, '0'))\\:\\:date";
            queryStringBuilder.append("OR ((gpsp.month_start IS NOT NULL AND gpsp.day_start IS NOT NULL OR gpsp.month_end IS NOT NULL AND gpsp.day_end IS NOT" +
                " NULL) AND tsrange(" + partsSeasonalStart + " + time '" + START_OF_DAY_TIME + "', " + partsSeasonalEnd + " + time '" +
                END_OF_DAY_TIME + "') && :startToEndRange\\:\\:tsrange)\n");

            queryStringBuilder.append("OR ");

            final String globalSeasonalStart = "CONCAT((SELECT EXTRACT('Year' FROM CURRENT_DATE)),'-',LPAD(cast(gsp.month_start as VARCHAR), 2, '0'),'-'," +
                "LPAD(cast(gsp.day_start as VARCHAR), 2, '0'))\\:\\:date";
            final String globalSeasonalEnd = "CONCAT(case when (gsp.month_start > gsp.month_end) or (gsp.month_start = gsp.month_end and gsp.day_start > gsp" +
                ".day_end) then (SELECT EXTRACT('Year' FROM CURRENT_DATE) + 1) else (SELECT EXTRACT('Year' FROM CURRENT_DATE)) end,'-',LPAD(cast(gsp" +
                ".month_end as VARCHAR), 2, '0'),'-'," +
                "LPAD(cast(gsp.day_end as VARCHAR), 2, '0'))\\:\\:date";
            queryStringBuilder.append("((gsp.month_start IS NOT NULL AND gsp.day_start IS NOT NULL OR gsp.month_end IS NOT NULL AND gsp.day_end IS NOT NULL) " +
                "AND tsrange(" + globalSeasonalStart + " + time '" + START_OF_DAY_TIME + "', " + globalSeasonalEnd + " + time '" +
                END_OF_DAY_TIME + "') && :startToEndRange\\:\\:tsrange)\n");

            // If "Non-seasonal" filter is also checked, non-seasonal GRs must also be displayed because they are still valid in period of interest.
            queryStringBuilder.append("OR (gsp.month_start IS NULL AND gsp.day_start IS NULL AND gsp.month_end IS NULL AND gsp.day_end IS NULL)\n");

            queryStringBuilder.append(")\n");

        }

        boolean filteredTerritorialAndNotTerritorialSea =
            isSearchCriteriaTrue(searchCriteria.getAllTerritorialSea()) && isSearchCriteriaTrue(searchCriteria.getNotAllTerritorialSea());
        boolean filteredPermanentAndTemporary =
            isSearchCriteriaTrue(searchCriteria.getPermanent()) && isSearchCriteriaTrue(searchCriteria.getTemporary());
        boolean filteredSeasonalAndNotSeasonal =
            isSearchCriteriaTrue(searchCriteria.getSeasonalPeriod()) && isSearchCriteriaTrue(searchCriteria.getNotSeasonalPeriod());

        if (filteredPermanentAndTemporary && filteredSeasonalAndNotSeasonal && filteredTerritorialAndNotTerritorialSea) {
            return;
        }

        if (!filteredTerritorialAndNotTerritorialSea && (searchCriteria.getAllTerritorialSea() != null || searchCriteria.getNotAllTerritorialSea() != null)) {
            queryStringBuilder.append("AND g.is_all_territorial_sea = :isAllTerritorialSea\n");
        }

        if (!filteredSeasonalAndNotSeasonal && (searchCriteria.getSeasonalPeriod() != null || searchCriteria.getNotSeasonalPeriod() != null)) {
            queryStringBuilder.append("AND (SELECT count(gsp.georegulation_id) > 0 FROM georegulation_seasonal_period gsp WHERE gsp.georegulation_id = g.id) " +
                "= :isSeasonalPeriod\n");
        }

        if (!filteredPermanentAndTemporary && (searchCriteria.getPermanent() != null || searchCriteria.getTemporary() != null)) {
            queryStringBuilder.append("AND g.is_temporary = :isTemporary\n");
        }
    }

    private static void addSearchCriteriaParametersToQuery(Query query, GeoregulationMapSearchCriteria searchCriteria) {
        if (!isEmpty(searchCriteria.getPublishedFromIncludingDate())) {
            query.setParameter("publicationDate", searchCriteria.getPublishedFromIncludingDate());
        }

        if (!isEmpty(searchCriteria.getConcernedActivities())) {
            query.setParameter("concernedActivities",
                searchCriteria.getConcernedActivities().stream().map(GeoregulationActivity::name).collect(toList()));
        }

        if (!isEmpty(searchCriteria.getDateStart()) && isEmpty(searchCriteria.getDateEnd())) {
            query.setParameter("startToEndRange", "[" + searchCriteria.getDateStart() + " " + START_OF_DAY_TIME + ",]");
        } else if (isEmpty(searchCriteria.getDateStart()) && !isEmpty(searchCriteria.getDateEnd())) {
            query.setParameter("startToEndRange", "[," + searchCriteria.getDateEnd() + " " + END_OF_DAY_TIME + "]");
        } else if (!isEmpty(searchCriteria.getDateStart()) && !isEmpty(searchCriteria.getDateEnd())) {
            query.setParameter("startToEndRange",
                "[" + searchCriteria.getDateStart() + " " + START_OF_DAY_TIME + "," + searchCriteria.getDateEnd() + " " + END_OF_DAY_TIME + "]");
        }

        boolean filteredTerritorialAndNotTerritorialSea =
            isSearchCriteriaTrue(searchCriteria.getAllTerritorialSea()) && isSearchCriteriaTrue(searchCriteria.getNotAllTerritorialSea());
        boolean filteredPermanentAndTemporary =
            isSearchCriteriaTrue(searchCriteria.getPermanent()) && isSearchCriteriaTrue(searchCriteria.getTemporary());
        boolean filteredSeasonalAndNotSeasonal =
            isSearchCriteriaTrue(searchCriteria.getSeasonalPeriod()) && isSearchCriteriaTrue(searchCriteria.getNotSeasonalPeriod());

        if (filteredPermanentAndTemporary && filteredSeasonalAndNotSeasonal && filteredTerritorialAndNotTerritorialSea) {
            return;
        }

        if (!filteredTerritorialAndNotTerritorialSea && (searchCriteria.getAllTerritorialSea() != null || searchCriteria.getNotAllTerritorialSea() != null)) {
            query.setParameter("isAllTerritorialSea", ofNullable(searchCriteria.getAllTerritorialSea()).orElse(false));
        }

        if (!filteredPermanentAndTemporary && (searchCriteria.getPermanent() != null || searchCriteria.getTemporary() != null)) {
            query.setParameter("isTemporary", searchCriteria.getTemporary() != null ? searchCriteria.getTemporary() : false);
        }

        if (!filteredSeasonalAndNotSeasonal && (searchCriteria.getSeasonalPeriod() != null || searchCriteria.getNotSeasonalPeriod() != null)) {
            query.setParameter("isSeasonalPeriod", searchCriteria.getSeasonalPeriod() != null ? searchCriteria.getSeasonalPeriod() : false);
        }
    }

    public byte[] downloadGeometriesAsKml(UUID id) throws ServiceException {
        return GeoregulationKmlUtil.generateKml(entityManager, id);
    }

    public void downloadGeometriesAsShp(UUID id, HttpServletResponse response) throws ServiceException {
        GeoregulationShpUtil.generateShp(entityManager, id, pingProperties.getGeneratedShapefileFolder(), response);
    }

    public void downloadGeometriesAsGpx(UUID id, String lang, HttpServletResponse response) throws ServiceException {
        if (!List.of(Locale.FRENCH.getLanguage(), Locale.ENGLISH.getLanguage()).contains(lang)) {
            throw new BadRequestException("Invalid language");
        }
        final Georegulation georegulation = repository.findById(id).orElseThrow(NotFoundException::new);
        if (georegulation.getStatus() != PUBLISHED) {
            throw new BadRequestException("Georegulation must be in PUBLISHED status");
        }
        georegulationGpxUtil.generateGpx(georegulation, lang, pingProperties.getGeneratedGpxFolder(), response);
    }
}
