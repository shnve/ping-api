package fr.shom.ping.utils.search;

import fr.shom.ping.model.dto.SurveyTableDto;
import fr.shom.ping.model.enums.SurveyTableSortProperty;
import fr.shom.ping.model.predicate.SurveySearchCriteria;
import fr.shom.ping.model.tuple.SurveyTableTuple;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static fr.shom.ping.model.enums.SurveyState.DRAFT;
import static fr.shom.ping.utils.SchemaUtil.SURVEY_ADMIN_TABLE_VIEW_TUPLE_MAPPING;
import static fr.shom.ping.utils.search.WarningSearchUtil.END_OF_DAY_TIME;
import static fr.shom.ping.utils.search.WarningSearchUtil.START_OF_DAY_TIME;
import static java.util.stream.Collectors.toSet;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.util.ObjectUtils.isEmpty;

public abstract class AbstractSurveyTableUtil {

    protected Set<UUID> paginateIds(List<UUID> ids, Pageable pageable) {
        return ids.stream()
            .skip(pageable.getPageNumber() * pageable.getPageSize())
            .limit(pageable.getPageSize())
            .collect(toSet());
    }

    protected void map(LinkedHashMap<UUID, SurveyTableDto> mapDto, SurveyTableTuple tuple) {
        add(mapDto, tuple);
    }

    protected void add(LinkedHashMap<UUID, SurveyTableDto> mapDto, SurveyTableTuple tuple) {
        final SurveyTableDto surveyTableDto = SurveyTableDto.builder()
            .id(tuple.getId())
            .identifier(tuple.getIdentifier())
            .nameAndLocation(tuple.getNameAndLocation())
            .lastUpdate(tuple.getLastUpdate())
            .startDate(tuple.getStartDate())
            .endDate(tuple.getEndDate())
            .compliant(tuple.getCompliant())
            .expectedDeletionDate(tuple.getExpectedDeletionDate())
            .testProcessingStatus(tuple.getTestProcessingStatus())
            .testProcessingDate(tuple.getTestProcessingDate())
            .transmissionStatus(tuple.getTransmissionStatus())
            .lastUpdateTransmissionDate(tuple.getLastUpdateTransmissionDate())
            .build();
        mapDto.put(tuple.getId(), surveyTableDto);
    }

    protected String orderBy(SurveySearchCriteria searchCriteria, Pageable pageable) {
        return orderBy(searchCriteria, pageable, false);
    }

    protected String orderBy(SurveySearchCriteria searchCriteria, Pageable pageable, boolean useForFinalSelect) {
        String orderProperty;
        Sort.Direction direction;
        switch (searchCriteria.getState()) {
            case TRANSMITTED:
                orderProperty = "survey.last_update_transmission_date";
                direction = DESC;
                break;
            case DRAFT:
            default:
                orderProperty = "survey.last_update";
                direction = DESC;
        }

        final Sort sortParams = pageable.getSort();
        // We only support one sort property
        if (!sortParams.isEmpty() && sortParams.stream().findFirst().isPresent()) {
            final Sort.Order sortParam = sortParams.stream().findFirst().get();
            final SurveyTableSortProperty sortProperty = SurveyTableSortProperty.fromProperty(sortParam.getProperty());
            direction = sortParam.getDirection();
            switch (sortProperty) {
                case PING_ID:
                    orderProperty = "survey.identifier";
                    break;
                case SHOM_ID:
                    orderProperty = "survey.shom_id";
                    break;
                case PRODUCER_ORGANIZATION:
                    orderProperty = "survey.producer_organization";
                    break;
                case OWNER_ORGANIZATION:
                    orderProperty = "survey.owner_organization";
                    break;
                case PERIOD_END_DATE:
                    orderProperty = "survey.end_date";
                    break;
                case COMPLIANT:
                    orderProperty = useForFinalSelect ? "compliant" : "survey.last_update";
                    break;
                case NAME_AND_LOCATION:
                    orderProperty = "survey.name_and_location";
                    break;
                case EXPECTED_DELETION_DATE:
                    orderProperty = useForFinalSelect ? "expected_deletion_date" : "survey.last_update";
                    break;
                case TRANSMISSION_STATUS_WITH_DATE:
                    orderProperty = "survey.last_update_transmission_date";
                    break;
                case LAST_UPDATE:
                default:
                    orderProperty = "survey.last_update";
            }
        }

        return "ORDER BY " + orderProperty + " " + direction.name();
    }

    public void addBaseCriteriaSelectionToSql(StringBuilder queryStringBuilder, SurveySearchCriteria searchCriteria) {
        if (!isEmpty(searchCriteria.getState())) {
            queryStringBuilder.append("AND survey.state = :state\n");
        }

        if (StringUtils.isNotBlank(searchCriteria.getNameAndLocation())) {
            queryStringBuilder.append("AND LOWER(survey.name_and_location) LIKE :nameAndLocation\n");
        }

        if (!isEmpty(searchCriteria.getStartDate())) {
            // We don't need to check if date_start or date_end is null
            // "null + time '01:23:45'" always returns null, so there is no timestamp limit depending on which attribute is null
            queryStringBuilder.append("AND tsrange(survey.start_date + time '" + START_OF_DAY_TIME + "', survey.start_date + time '" + END_OF_DAY_TIME + "') " +
                "&& :periodStartRange\\:\\:tsrange\n");
        }

        if (!isEmpty(searchCriteria.getEndDate())) {
            // We don't need to check if date_start or date_end is null
            // "null + time '01:23:45'" always returns null, so there is no timestamp limit depending on which attribute is null
            queryStringBuilder.append("AND tsrange(survey.end_date + time '" + START_OF_DAY_TIME + "', survey.end_date + time '" + END_OF_DAY_TIME + "') " +
                "&& :periodEndRange\\:\\:tsrange\n");
        }

        if (!DRAFT.equals(searchCriteria.getState())) {
            if (StringUtils.isNotBlank(searchCriteria.getIdentifier())) {
                queryStringBuilder.append("AND LOWER(survey.identifier) LIKE :identifier\n");
            }

            if (!isEmpty(searchCriteria.getTransmissionStartDate()) || !isEmpty(searchCriteria.getTransmissionEndDate())) {
                // We don't need to check if date_start or date_end is null
                // "null + time '01:23:45'" always returns null, so there is no timestamp limit depending on which attribute is null
                queryStringBuilder.append("AND tsrange(survey.last_update_transmission_date + time '" + START_OF_DAY_TIME
                    + "', survey.last_update_transmission_date + time '" + END_OF_DAY_TIME
                    + "') && :transmissionDatePeriodStartToEndRange\\:\\:tsrange\n");
            }
        }
    }

    public void addBaseSearchCriteriaParametersToQuery(Query query, SurveySearchCriteria searchCriteria) {
        if (!isEmpty(searchCriteria.getState())) {
            query.setParameter("state", searchCriteria.getState().name());
        }

        if (StringUtils.isNotBlank(searchCriteria.getNameAndLocation())) {
            query.setParameter("nameAndLocation", "%" + searchCriteria.getNameAndLocation().toLowerCase() + "%");
        }

        if (!isEmpty(searchCriteria.getStartDate())) {
            query.setParameter("periodStartRange", "[" + searchCriteria.getStartDate() + " " + START_OF_DAY_TIME + ",]");
        }

        if (!isEmpty(searchCriteria.getEndDate())) {
            query.setParameter("periodEndRange", "[," + searchCriteria.getEndDate() + " " + END_OF_DAY_TIME + "]");
        }

        if (!DRAFT.equals(searchCriteria.getState())) {
            if (StringUtils.isNotBlank(searchCriteria.getIdentifier())) {
                query.setParameter("identifier", "%" + searchCriteria.getIdentifier().toLowerCase() + "%");
            }

            if (!isEmpty(searchCriteria.getTransmissionStartDate()) && isEmpty(searchCriteria.getTransmissionEndDate())) {
                query.setParameter("transmissionDatePeriodStartToEndRange", "[" + searchCriteria.getTransmissionStartDate() + " " + START_OF_DAY_TIME + ",]");
            } else if (isEmpty(searchCriteria.getTransmissionStartDate()) && !isEmpty(searchCriteria.getTransmissionEndDate())) {
                query.setParameter("transmissionDatePeriodStartToEndRange", "[," + searchCriteria.getTransmissionEndDate() + " " + END_OF_DAY_TIME + "]");
            } else if (!isEmpty(searchCriteria.getTransmissionStartDate()) && !isEmpty(searchCriteria.getTransmissionEndDate())) {
                query.setParameter("transmissionDatePeriodStartToEndRange",
                    "[" + searchCriteria.getTransmissionStartDate() + " " + START_OF_DAY_TIME + "," + searchCriteria.getTransmissionEndDate() + " "
                        + END_OF_DAY_TIME + "]");
            }
        }
    }

    public Collection<SurveyTableDto> getSurveyTableDtos(EntityManager entityManager, Set<UUID> paginatedIds, String orderBy, StringBuilder sql) {
        sql.insert(0, "SELECT DISTINCT survey.id, survey.identifier, survey.name_and_location, survey.last_update, survey.start_date, " +
            "survey.end_date, survey.compliant, survey.last_update as expected_deletion_date, spr.status as test_processing_status, " +
            "spr.processing_date as test_processing_date, survey.transmission_status, survey.last_update_transmission_date\n"
        );

        sql.append("LEFT JOIN survey_processing_result_list sprl ON sprl.survey_id = survey.id\n");
        sql.append("LEFT JOIN survey_processing_result spr ON spr.id = sprl.processing_result_id\n");

        sql.append("WHERE survey.id IN (:surveyIds)\n");
        sql.append("AND spr.processing_date IN (" +
            "select max(processing_date) from survey_processing_result spr join survey_processing_result_list sprl on sprl.processing_result_id = spr.id " +
            "group by sprl.survey_id)\n");

        sql.append(orderBy);

        final Query query = entityManager.createNativeQuery(sql.toString(), SURVEY_ADMIN_TABLE_VIEW_TUPLE_MAPPING);
        query.setParameter("surveyIds", paginatedIds);

        final LinkedHashMap<UUID, SurveyTableDto> mapDto = new LinkedHashMap<>();
        query.getResultList().forEach(tuple -> map(mapDto, (SurveyTableTuple) tuple));

        return mapDto.values();
    }
}
