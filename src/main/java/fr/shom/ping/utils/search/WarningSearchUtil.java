package fr.shom.ping.utils.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.shom.ping.model.enums.GlobalWarningType;
import fr.shom.ping.model.enums.SerieType;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
import fr.shom.ping.model.predicate.WarningSearchCriteria;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.stream.Collectors.toList;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;
import static org.springframework.util.ObjectUtils.isEmpty;

public final class WarningSearchUtil {
    public static final String START_OF_DAY_TIME = "00:00:00";
    public static final String START_TO_END_RANGE = "startToEndRange";
    public static final String END_OF_DAY_TIME = "23:59:59";
    public static final String INNER_JOIN = "INNER JOIN ";
    public static final String LEFT_JOIN = "LEFT JOIN ";
    public static final String WHERE_STATUS_EQUALS = "WHERE w.status = :warningStatus\n";
    public static final String ORDER_BY_PUBLICATION_TIME_DESC = "ORDER BY w.publication_time DESC\n";
    public static final String ORDER_BY_YEAR_DESC_AND_RESERVATION_NUMBER_DESC = "ORDER BY w.year DESC, w.warning_number DESC\n";

    private static final ObjectMapper WARNING_MAPPER = Jackson2ObjectMapperBuilder.json().build();

    private WarningSearchUtil() {
    }

    public static void addCriteriaSelectionToSql(StringBuilder queryStringBuilder, WarningSearchCriteria warningSearchCriteria) {
        addCriteriaSelectionNotEmptyToSql(queryStringBuilder, warningSearchCriteria);

        if (!isEmpty(warningSearchCriteria.getDateStart()) || !isEmpty(warningSearchCriteria.getDateEnd())) {
            // We don't need to check if date_start or date_end is null
            // "null + time '01:23:45'" always returns null, so there is no timestamp limit depending on which attribute is null
            queryStringBuilder.append("AND tsrange(fdr.date_start + time '" + START_OF_DAY_TIME + "', fdr.date_end + time '" + END_OF_DAY_TIME + "') && " +
                ":startToEndRange\\:\\:tsrange\n");
        }

        if (!isEmpty(warningSearchCriteria.getGlobalWarningType()) && warningSearchCriteria.getGlobalWarningType().contains(GlobalWarningType.NOTICE)
            && !isEmpty(warningSearchCriteria.getSerieType()) && warningSearchCriteria.getSerieType().containsAll(
            newHashSet(
                SerieType.NAVAREA_NAVIGATIONAL_WARNING,
                SerieType.COASTAL_NAVIGATIONAL_WARNING,
                SerieType.LOCAL_NAVIGATIONAL_WARNING))
        ) {
            return;
        }

        if (isNotEmpty(warningSearchCriteria.getGlobalWarningType()) || isNotEmpty(warningSearchCriteria.getSerieType())) {
            queryStringBuilder.append("AND s.type IN (:serieTypes)\n");
        }
    }

    private static void addCriteriaSelectionNotEmptyToSql(StringBuilder queryStringBuilder, WarningSearchCriteria warningSearchCriteria) {
        Map<String, String> selectedCriteria = Map.of(
            "publishedFromIncludingDate", "AND w.publication_time >= :publishedFromIncludingDate\n",
            "serieId", "AND s.id = :serieId\n",
            "warningHazardTypeGenerals", "AND w.warning_hazard_type_general IN (:warningHazardTypeGenerals)\n",
            "subAreaIds", "AND sa.id IN (:subAreaIds)\n",
            "concernedMaritimeEntityIds", "AND we.entity_id IN :concernedMaritimeEntityIds\n",
            "reservationNumberAndYear", "AND CONCAT(w.warning_number, '/', (w.year - 2000)) ILIKE :reservationNumberAndYear\n",
            "entityCoverIds", "AND mec.id IN (:entityCoverIds)\n AND (st_intersects(mec.geom, wfpg.geom) OR st_intersects(mec.geom, wfpaa.geom))\n");
        Map<String, Object> keyValue = WARNING_MAPPER.convertValue(warningSearchCriteria, Map.class);
        keyValue.forEach((attribut, value) -> {
            if (!isEmpty(value) && !isEmpty(selectedCriteria.get(attribut))) {
                queryStringBuilder.append(selectedCriteria.get(attribut));
            }
        });
    }

    public static void addSearchCriteriaParametersToQuery(Query query, WarningSearchCriteria warningSearchCriteria) {
        addSearchCriteriaParametersNotEmptyToQuery(query, warningSearchCriteria);

        if (!isEmpty(warningSearchCriteria.getDateStart()) && isEmpty(warningSearchCriteria.getDateEnd())) {
            query.setParameter(START_TO_END_RANGE, "[" + warningSearchCriteria.getDateStart() + " " + START_OF_DAY_TIME + ",]");
        } else if (isEmpty(warningSearchCriteria.getDateStart()) && !isEmpty(warningSearchCriteria.getDateEnd())) {
            query.setParameter(START_TO_END_RANGE, "[," + warningSearchCriteria.getDateEnd() + " " + END_OF_DAY_TIME + "]");
        } else if (!isEmpty(warningSearchCriteria.getDateStart()) && !isEmpty(warningSearchCriteria.getDateEnd())) {
            query.setParameter(START_TO_END_RANGE,
                "[" + warningSearchCriteria.getDateStart() + " " + START_OF_DAY_TIME + "," + warningSearchCriteria.getDateEnd() + " " + END_OF_DAY_TIME + "]");
        }

        if (!isEmpty(warningSearchCriteria.getGlobalWarningType()) && warningSearchCriteria.getGlobalWarningType().contains(GlobalWarningType.NOTICE)
            && !isEmpty(warningSearchCriteria.getSerieType()) && warningSearchCriteria.getSerieType().containsAll(
            newHashSet(
                SerieType.NAVAREA_NAVIGATIONAL_WARNING,
                SerieType.COASTAL_NAVIGATIONAL_WARNING,
                SerieType.LOCAL_NAVIGATIONAL_WARNING))
        ) {
            return;
        }

        if (!isEmpty(warningSearchCriteria.getGlobalWarningType()) || !isEmpty(warningSearchCriteria.getSerieType())) {
            final List<String> serieTypes = new ArrayList<>();

            if (!isEmpty(warningSearchCriteria.getSerieType())) {
                serieTypes.addAll(warningSearchCriteria.getSerieType().stream().map(SerieType::name).collect(toList()));
            }

            if (!isEmpty(warningSearchCriteria.getGlobalWarningType())) {
                serieTypes.addAll(SerieType.fromGlobalWarningType(warningSearchCriteria.getGlobalWarningType()).stream().map(SerieType::name).collect(toList()));
            }

            query.setParameter("serieTypes", serieTypes);
        }
    }

    public static void addSearchCriteriaParametersNotEmptyToQuery(Query query, WarningSearchCriteria warningSearchCriteria) {
        Map<String, Object> selectedCriteria = new HashMap<>();
        selectedCriteria.put("publishedFromIncludingDate", warningSearchCriteria.getPublishedFromIncludingDate());
        selectedCriteria.put("serieId", warningSearchCriteria.getSerieId());
        selectedCriteria.put("subAreaIds", warningSearchCriteria.getSubAreaIds());
        if (isNotEmpty(warningSearchCriteria.getWarningHazardTypeGenerals())) {
            selectedCriteria.put("warningHazardTypeGenerals",
                warningSearchCriteria.getWarningHazardTypeGenerals().stream().map(WarningHazardTypeGeneral::name).collect(toList()));
        }
        selectedCriteria.put("concernedMaritimeEntityIds", warningSearchCriteria.getConcernedMaritimeEntityIds());
        selectedCriteria.put("reservationNumberAndYear", "%" + warningSearchCriteria.getReservationNumberAndYear()+ "%");
        selectedCriteria.put("entityCoverIds", warningSearchCriteria.getEntityCoverIds());

        Map<String, Object> keyValue = WARNING_MAPPER.convertValue(warningSearchCriteria, Map.class);
        keyValue.forEach((attribut, value) -> {
            if (!isEmpty(value) && !isEmpty(selectedCriteria.get(attribut))) {
                query.setParameter(attribut, selectedCriteria.get(attribut));
            }
        });
    }
}
