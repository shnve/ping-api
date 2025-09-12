package fr.shom.ping.utils.search;

import fr.shom.ping.model.dto.GeneralAreaDto;
import fr.shom.ping.model.dto.WarningLocalityNamesDto;
import fr.shom.ping.model.dto.WarningTableViewDto;
import fr.shom.ping.model.predicate.WarningSearchCriteria;
import fr.shom.ping.model.tuple.WarningTableViewTuple;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static fr.shom.ping.model.enums.WarningStatus.PUBLISHED;
import static fr.shom.ping.utils.SchemaUtil.AFFECTED_AREA_TABLE;
import static fr.shom.ping.utils.SchemaUtil.FEATURE_PART_TABLE;
import static fr.shom.ping.utils.SchemaUtil.FIXED_DATE_RANGE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.GENERAL_AREA_TABLE;
import static fr.shom.ping.utils.SchemaUtil.MARITIME_ENTITY_TABLE;
import static fr.shom.ping.utils.SchemaUtil.SERIE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.SUB_AREA_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_ENTITY_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_FEATURE_PART_GEOMETRY;
import static fr.shom.ping.utils.SchemaUtil.WARNING_HAZARD_TYPE_DETAILS_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_LOCALITY_NAMES_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_TABLE_VIEW_TUPLE_MAPPING;
import static fr.shom.ping.utils.WarningUtil.convertGeneralAreaInformationToLocationNames;
import static fr.shom.ping.utils.search.WarningSearchUtil.INNER_JOIN;
import static fr.shom.ping.utils.search.WarningSearchUtil.LEFT_JOIN;
import static fr.shom.ping.utils.search.WarningSearchUtil.ORDER_BY_YEAR_DESC_AND_RESERVATION_NUMBER_DESC;
import static fr.shom.ping.utils.search.WarningSearchUtil.WHERE_STATUS_EQUALS;
import static fr.shom.ping.utils.search.WarningSearchUtil.addCriteriaSelectionToSql;
import static fr.shom.ping.utils.search.WarningSearchUtil.addSearchCriteriaParametersToQuery;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.ObjectUtils.isEmpty;

public final class WarningSimpleTableUtil {

    private WarningSimpleTableUtil() {
    }

    private static StringBuilder buildSql(WarningSearchCriteria warningSearchCriteria) {
        StringBuilder queryStringBuilder = new StringBuilder();

        queryStringBuilder.append("FROM warning w\n");
        queryStringBuilder.append(INNER_JOIN + GENERAL_AREA_TABLE + " g ON w.id = g.warning_id\n");
        queryStringBuilder.append(LEFT_JOIN + WARNING_LOCALITY_NAMES_TABLE + " l ON w.id = l.warning_id\n");
        queryStringBuilder.append(LEFT_JOIN + FEATURE_PART_TABLE + " fp ON w.id = fp.warning_id\n");
        queryStringBuilder.append(LEFT_JOIN + WARNING_HAZARD_TYPE_DETAILS_TABLE + " fd ON fd.feature_part_id = fp.id\n");
        queryStringBuilder.append(LEFT_JOIN + WARNING_ENTITY_TABLE + " we ON w.id = we.warning_id\n");
        queryStringBuilder.append(LEFT_JOIN + MARITIME_ENTITY_TABLE + " me ON we.entity_id = me.id\n");
        queryStringBuilder.append(LEFT_JOIN + WARNING_FEATURE_PART_GEOMETRY + " wfpg on fp.id = wfpg.feature_part_id\n");
        queryStringBuilder.append(LEFT_JOIN + AFFECTED_AREA_TABLE + " wfpaa on fp.id = wfpaa.feature_part_id\n");
        queryStringBuilder.append(LEFT_JOIN + MARITIME_ENTITY_TABLE + " mec ON mec.type = 'SERVICE_AREA'\n");

        if (!isEmpty(warningSearchCriteria.getSubAreaIds())) {
            queryStringBuilder.append(INNER_JOIN + SUB_AREA_TABLE + " sa ON g.sub_area_id = sa.id\n");
        }

        if (!isEmpty(warningSearchCriteria.getSerieType()) || !isEmpty(warningSearchCriteria.getSerieId()) || !isEmpty(warningSearchCriteria.getGlobalWarningType())) {
            queryStringBuilder.append(INNER_JOIN + SERIE_TABLE + " s ON w.serie_id = s.id\n");
        }

        if (!isEmpty(warningSearchCriteria.getDateStart()) || !isEmpty(warningSearchCriteria.getDateEnd())) {
            queryStringBuilder.append(LEFT_JOIN + FIXED_DATE_RANGE_TABLE + " fdr ON fp.id = fdr.parent_id\n");
        }

        queryStringBuilder.append(WHERE_STATUS_EQUALS);
        addCriteriaSelectionToSql(queryStringBuilder, warningSearchCriteria);

        return queryStringBuilder;
    }

    public static PageImpl<WarningTableViewDto> find(EntityManager entityManager, WarningSearchCriteria warningSearchCriteria, Pageable pageable) {
        StringBuilder sql = buildSql(warningSearchCriteria);
        sql.insert(0, "select ww.id from (\n" +
            "SELECT w.id, w.warning_number, w.year\n" +
            "FROM warning w\n" +
            "WHERE w.id IN (\n" +
            "SELECT w.id\n");
        sql.append("GROUP BY w.id\n" +
            ")\n" +
            ORDER_BY_YEAR_DESC_AND_RESERVATION_NUMBER_DESC +
            ") as ww\n");

        Query query = entityManager.createNativeQuery(sql.toString());

        query.setParameter("warningStatus", PUBLISHED.name());
        addSearchCriteriaParametersToQuery(query, warningSearchCriteria);

        // Why we unwrap and addScalar : https://github.com/spring-projects/spring-data-jpa/issues/1796
        List<UUID> resultList = query.unwrap(NativeQuery.class)
            .addScalar("id", StandardBasicTypes.UUID)
            .getResultList();

        long totalCount = resultList.size();

        Set<UUID> warningIds = resultList.stream()
            .skip(pageable.getPageNumber() * pageable.getPageSize())
            .limit(pageable.getPageSize())
            .collect(toSet());

        sql = buildSql(warningSearchCriteria);
        sql.insert(0, "SELECT w.id, w.name_of_series, w.warning_number, w.year, g.id as general_area_id, g.sub_area_id as sub_area_id" +
            ", g.position as general_area_position, g.name as general_area_name, g.international_name as general_area_international_name," +
            " l.id as locality_id, l.parent_id as locality_parent_id, l.position as locality_position , l.name as locality_name, l.international_name as locality_international_name," +
            " w.warning_hazard_type_general, fd.warning_hazard_type_detail\n");
        sql.append("AND w.id IN (:warningIds)\n");
        sql.append(ORDER_BY_YEAR_DESC_AND_RESERVATION_NUMBER_DESC);

        query = entityManager.createNativeQuery(sql.toString(), WARNING_TABLE_VIEW_TUPLE_MAPPING);
        query.setParameter("warningStatus", PUBLISHED.name());
        query.setParameter("warningIds", warningIds);
        addSearchCriteriaParametersToQuery(query, warningSearchCriteria);

        LinkedHashMap<UUID, WarningTableViewDto> warnings = new LinkedHashMap<>();
        query.getResultList().forEach(tuple -> map(warnings, (WarningTableViewTuple) tuple));

        return new PageImpl<>(new ArrayList<>(warnings.values()), pageable, totalCount);
    }

    private static void map(LinkedHashMap<UUID, WarningTableViewDto> warnings, WarningTableViewTuple tuple) {
        if (warnings.containsKey(tuple.getId())) {
            update(warnings, tuple);
        } else {
            add(warnings, tuple);
        }
    }

    private static void update(LinkedHashMap<UUID, WarningTableViewDto> warnings, WarningTableViewTuple tuple) {
        WarningTableViewDto currentWarning = warnings.get(tuple.getId());

        GeneralAreaDto generalArea = currentWarning.getGeneralAreaSortedSet().stream()
            .filter(generalAreaDto -> generalAreaDto.getId().equals(tuple.getGeneralAreaId()))
            .findFirst()
            .orElse(GeneralAreaDto.builder()
                .id(tuple.getGeneralAreaId())
                .subAreaId(tuple.getSubAreaId())
                .position(tuple.getGeneralAreaPosition())
                .build());

        updateGeneralAreas(currentWarning, generalArea, tuple);

        if (!isEmpty(tuple.getLocalityId())) {
            WarningLocalityNamesDto locality = currentWarning.getLocalitySortedList().stream()
                .filter(localityDto -> localityDto.getId().equals(tuple.getLocalityId()))
                .findFirst()
                .orElse(WarningLocalityNamesDto.builder()
                    .id(tuple.getLocalityId())
                    .parentId(tuple.getLocalityParentId())
                    .position(tuple.getLocalityPosition())
                    .name(tuple.getLocalityName())
                    .internationalName(tuple.getLocalityInternationalName())
                    .build());

            updateLocalities(currentWarning, locality, tuple);
        }

        updateWarningHazardTypeDetails(currentWarning, tuple);
    }

    private static void add(LinkedHashMap<UUID, WarningTableViewDto> warnings, WarningTableViewTuple tuple) {
        WarningTableViewDto warningTableViewDto = WarningTableViewDto.builder()
            .id(tuple.getId())
            .nameOfSeries(tuple.getNameOfSeries())
            .warningNumber(tuple.getWarningNumber())
            .year(tuple.getYear())
            .warningHazardTypeGeneral(tuple.getWarningHazardTypeGeneral())
            .build();

        GeneralAreaDto generalArea = GeneralAreaDto.builder()
            .id(tuple.getGeneralAreaId())
            .id(tuple.getGeneralAreaId())
            .position(tuple.getGeneralAreaPosition())
            .build();

        updateGeneralAreas(warningTableViewDto, generalArea, tuple);

        if (!isEmpty(tuple.getLocalityId())) {
            WarningLocalityNamesDto locality = WarningLocalityNamesDto.builder()
                .id(tuple.getLocalityId())
                .parentId(tuple.getLocalityParentId())
                .position(tuple.getLocalityPosition())
                .name(tuple.getLocalityName())
                .internationalName(tuple.getLocalityInternationalName())
                .build();

            updateLocalities(warningTableViewDto, locality, tuple);
        }

        updateWarningHazardTypeDetails(warningTableViewDto, tuple);

        warnings.put(tuple.getId(), warningTableViewDto);
    }

    private static void updateGeneralAreas(WarningTableViewDto warning, GeneralAreaDto generalArea, WarningTableViewTuple tuple) {
        generalArea.setLocationNames(convertGeneralAreaInformationToLocationNames(tuple.getGeneralAreaName(), tuple.getGeneralAreaInternationalName()));
        warning.getGeneralAreaSortedSet().add(generalArea);
    }

    private static void updateLocalities(WarningTableViewDto warning, WarningLocalityNamesDto locality, WarningTableViewTuple tuple) {
        warning.getLocalitySortedList().add(locality);
    }

    private static void updateWarningHazardTypeDetails(WarningTableViewDto warning, WarningTableViewTuple tuple) {
        if (tuple.getWarningHazardTypeDetail() != null) {
            warning.getWarningHazardTypeDetails().add(tuple.getWarningHazardTypeDetail());
        }
    }
}
