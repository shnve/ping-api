package fr.shom.ping.utils.search;


import fr.shom.ping.model.dto.GeoregulationAdminTableDto;
import fr.shom.ping.model.enums.GeoregulationAdminTableSortProperty;
import fr.shom.ping.model.predicate.GeoregulationAdminSearchCriteria;
import fr.shom.ping.model.tuple.GeoregulationAdminTableTuple;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import static com.google.common.collect.Sets.newHashSet;
import static fr.shom.ping.utils.SchemaUtil.GEOREGULATION_ACTIVITY_TABLE;
import static fr.shom.ping.utils.SchemaUtil.GEOREGULATION_ADMIN_TABLE_TUPLE_MAPPING;
import static fr.shom.ping.utils.SchemaUtil.GEOREGULATION_ENTITY_TABLE;
import static fr.shom.ping.utils.SchemaUtil.GEOREGULATION_NUMBER_AUTHORITY_CODE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.GEOREGULATION_NUMBER_AUTHORITY_TABLE;
import static fr.shom.ping.utils.SchemaUtil.GEOREGULATION_TABLE;
import static fr.shom.ping.utils.SchemaUtil.MARITIME_ENTITY_TABLE;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.data.domain.Sort.Direction.DESC;

public final class GeoregulationAdminTableUtil {

    public static final String LEFT_JOIN = "LEFT JOIN ";

    private GeoregulationAdminTableUtil() {
    }

    public static PageImpl<GeoregulationAdminTableDto> find(EntityManager entityManager, GeoregulationAdminSearchCriteria searchCriteria, Pageable pageable) {
        String orderBy = orderBy(pageable.getSort());
        List<UUID> georegulationIds = selectGeoregulationIds(entityManager, searchCriteria, orderBy);
        if (georegulationIds.isEmpty()) {
            return new PageImpl<>(emptyList(), pageable, 0);
        }

        Set<UUID> paginatedGeoregulationIds = paginateGeoregulationIds(georegulationIds, pageable);
        List<GeoregulationAdminTableDto> paginatedGeoregulations = new ArrayList<>(paginateGeoregulations(entityManager, paginatedGeoregulationIds, orderBy));

        return new PageImpl<>(paginatedGeoregulations, pageable, georegulationIds.size());
    }

    private static StringBuilder from() {
        StringBuilder queryStringBuilder = new StringBuilder();

        queryStringBuilder.append("FROM " + GEOREGULATION_TABLE + " g\n");
        queryStringBuilder.append(LEFT_JOIN + GEOREGULATION_ACTIVITY_TABLE + " ga ON g.id = ga.georegulation_id\n");
        queryStringBuilder.append(LEFT_JOIN + GEOREGULATION_ENTITY_TABLE + " ge ON g.id = ge.georegulation_id\n");
        queryStringBuilder.append(LEFT_JOIN + MARITIME_ENTITY_TABLE + " me ON ge.entity_id = me.id\n");
        queryStringBuilder.append(LEFT_JOIN + GEOREGULATION_NUMBER_AUTHORITY_TABLE + " na ON g.id = na.georegulation_id\n");
        queryStringBuilder.append(LEFT_JOIN + GEOREGULATION_NUMBER_AUTHORITY_CODE_TABLE + " nac ON g.id = nac.georegulation_id\n");

        return queryStringBuilder;
    }

    private static String orderBy(Sort sortParams) {
        Direction direction = DESC;
        String orderProperty = "g.number_year " + direction.name() + ", g.number_text";

        // We only support one sort property
        if (!sortParams.isEmpty() && sortParams.stream().count() == 1) {
            Sort.Order sortParam = sortParams.stream().findFirst().get();
            GeoregulationAdminTableSortProperty sortProperty = GeoregulationAdminTableSortProperty.fromProperty(sortParam.getProperty());
            direction = sortParam.getDirection();
            switch (sortProperty) {
                case PUBLICATION_DATE:
                    orderProperty = "g.publication_date";
                    break;
                case LAST_UPDATE:
                    orderProperty = "g.last_update";
                    break;
                case REPEAL_DATE:
                    orderProperty = "g.repeal_date";
                    break;
                case NUMBER:
                default:
                    orderProperty = "g.number_year " + direction.name() + ", g.number_text";
            }
        }

        return "ORDER BY " + orderProperty + " " + direction.name();
    }

    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    private static List<UUID> selectGeoregulationIds(EntityManager entityManager, GeoregulationAdminSearchCriteria searchCriteria, String orderBy) {
        StringBuilder sql = from();
        sql.insert(0, "SELECT g.id\n");
        addCriteriaSelectionToSql(sql, searchCriteria);
        sql.append("GROUP BY g.id\n");
        sql.append(orderBy);

        Query query = entityManager.createNativeQuery(sql.toString());

        addSearchCriteriaParametersToQuery(query, searchCriteria);

        // Why we unwrap and addScalar : https://github.com/spring-projects/spring-data-jpa/issues/1796
        return query.unwrap(NativeQuery.class)
            .addScalar("id", StandardBasicTypes.UUID)
            .getResultList();
    }

    private static Set<UUID> paginateGeoregulationIds(List<UUID> georegulationIds, Pageable pageable) {
        return georegulationIds.stream()
            .skip(pageable.getPageNumber() * pageable.getPageSize())
            .limit(pageable.getPageSize())
            .collect(toSet());
    }

    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    private static Collection<GeoregulationAdminTableDto> paginateGeoregulations(EntityManager entityManager, Set<UUID> paginatedGeoregulationIds,
        String orderBy) {
        StringBuilder sql = from();
        sql.insert(0, "SELECT g.id, g.status, g.number_year AS main_number_year, g.number_text AS main_number_text, nac.authority_code as " +
            "main_number_authority_code," +
            "g.number_date AS main_number_date, g.type, ga.activity, " +
            "g.title, g.international_title, me.id AS maritime_entity_id, me.name AS maritime_entity_name, " +
            "me.international_name AS maritime_entity_international_name, g.internal_comment, g.last_update, g.publication_date, g.repeal_date, " +
            "g.is_public, g.is_new, g.is_expired\n");
        sql.append("WHERE g.id IN (:ids)\n");
        sql.append(orderBy);

        Query query = entityManager.createNativeQuery(sql.toString(), GEOREGULATION_ADMIN_TABLE_TUPLE_MAPPING);
        query.setParameter("ids", paginatedGeoregulationIds);

        LinkedHashMap<UUID, GeoregulationAdminTableDto> georegulations = new LinkedHashMap<>();
        query.getResultList().forEach(tuple -> map(georegulations, (GeoregulationAdminTableTuple) tuple));

        return georegulations.values();
    }

    private static void map(LinkedHashMap<UUID, GeoregulationAdminTableDto> georegulations, GeoregulationAdminTableTuple tuple) {
        if (georegulations.containsKey(tuple.getId())) {
            update(georegulations, tuple);
        } else {
            add(georegulations, tuple);
        }
    }

    private static void update(LinkedHashMap<UUID, GeoregulationAdminTableDto> georegulations, GeoregulationAdminTableTuple tuple) {
        GeoregulationAdminTableDto currentGeoregulation = georegulations.get(tuple.getId());

        currentGeoregulation.getMainNumber().getAuthorityCodes().add(tuple.getMainNumberAuthorityCode());
        ofNullable(tuple.getActivity()).ifPresent(activity -> currentGeoregulation.getActivities().add(activity));
        ofNullable(tuple.getMaritimeEntityId()).ifPresent(entityId -> currentGeoregulation.getEntities().add(
            GeoregulationAdminTableDto.GeoregulationAdminTableMaritimeEntityDto.builder()
                .id(entityId)
                .name(tuple.getMaritimeEntityName())
                .internationalName(tuple.getMaritimeEntityInternationalName())
                .build()
        ));
    }

    private static void add(LinkedHashMap<UUID, GeoregulationAdminTableDto> georegulations, GeoregulationAdminTableTuple tuple) {
        GeoregulationAdminTableDto dto = GeoregulationAdminTableDto.builder()
            .id(tuple.getId())
            .status(tuple.getStatus())
            .mainNumber(GeoregulationAdminTableDto.GeoregulationAdminTableNumberDto.builder()
                .year(tuple.getMainNumberYear())
                .text(tuple.getMainNumberText())
                .authorityCodes(newHashSet(tuple.getMainNumberAuthorityCode()))
                .date(tuple.getMainNumberDate())
                .build())
            .type(tuple.getType())
            .title(tuple.getTitle())
            .internationalTitle(tuple.getInternationalTitle())
            .internalComment(tuple.getInternalComment())
            .lastUpdate(tuple.getLastUpdate())
            .publicationDate(tuple.getPublicationDate())
            .repealDate(tuple.getRepealDate())
            .isPublic(tuple.isPublic())
            .isNew(tuple.isNew())
            .isExpired(tuple.isExpired())
            .build();

        georegulations.put(tuple.getId(), dto);
        update(georegulations, tuple);
    }

    public static void addCriteriaSelectionToSql(StringBuilder queryStringBuilder, GeoregulationAdminSearchCriteria searchCriteria) {
        queryStringBuilder.append("WHERE g.status = :status\n");
        queryStringBuilder.append("AND g.issuing_service_id = :issuingServiceId\n");

        if (searchCriteria.getMainNumberYear() != null) {
            queryStringBuilder.append("AND g.number_year = :mainNumberYear\n");
        }

        if (isNotBlank(searchCriteria.getMainNumberText())) {
            queryStringBuilder.append("AND g.number_text ILIKE :mainNumberText\n");
        }

        if (isNotEmpty(searchCriteria.getMainNumberAuthorityIds())) {
            queryStringBuilder.append("AND na.authority_id IN :mainNumberAuthorityIds\n");
        }

        if (searchCriteria.getMainNumberDate() != null) {
            queryStringBuilder.append("AND g.number_date = :mainNumberDate\n");
        }

        if (searchCriteria.getType() != null) {
            queryStringBuilder.append("AND g.type = :type\n");
        }

        if (isNotEmpty(searchCriteria.getMaritimeEntityIds())) {
            queryStringBuilder.append("AND ge.entity_id IN :maritimeEntityIds\n");
        }

        if (isNotEmpty(searchCriteria.getActivities())) {
            queryStringBuilder.append("AND ga.activity IN :activities\n");
        }

        if (searchCriteria.getIsPublic() != null) {
            queryStringBuilder.append("AND g.is_public = :isPublic\n");
        }

        if (searchCriteria.getIsNew() != null) {
            queryStringBuilder.append("AND g.is_new = :isNew\n");
        }

        if (searchCriteria.getIsExpired() != null) {
            queryStringBuilder.append("AND g.is_expired = :isExpired\n");
        }
    }

    public static void addSearchCriteriaParametersToQuery(Query query, GeoregulationAdminSearchCriteria searchCriteria) {
        query.setParameter("status", searchCriteria.getStatus().name());
        query.setParameter("issuingServiceId", searchCriteria.getIssuingServiceId());

        if (searchCriteria.getMainNumberYear() != null) {
            query.setParameter("mainNumberYear", searchCriteria.getMainNumberYear());
        }

        if (isNotBlank(searchCriteria.getMainNumberText())) {
            query.setParameter("mainNumberText", "%" + searchCriteria.getMainNumberText() + "%");
        }

        if (isNotEmpty(searchCriteria.getMainNumberAuthorityIds())) {
            query.setParameter("mainNumberAuthorityIds", searchCriteria.getMainNumberAuthorityIds());
        }

        if (searchCriteria.getMainNumberDate() != null) {
            query.setParameter("mainNumberDate", searchCriteria.getMainNumberDate());
        }

        if (searchCriteria.getType() != null) {
            query.setParameter("type", searchCriteria.getType().name());
        }

        if (isNotEmpty(searchCriteria.getMaritimeEntityIds())) {
            query.setParameter("maritimeEntityIds", searchCriteria.getMaritimeEntityIds());
        }

        if (isNotEmpty(searchCriteria.getActivities())) {
            query.setParameter("activities", searchCriteria.getActivities().stream().map(Enum::name).collect(toList()));
        }

        if (searchCriteria.getIsPublic() != null) {
            query.setParameter("isPublic", searchCriteria.getIsPublic());
        }

        if (searchCriteria.getIsNew() != null) {
            query.setParameter("isNew", searchCriteria.getIsNew());
        }

        if (searchCriteria.getIsExpired() != null) {
            query.setParameter("isExpired", searchCriteria.getIsExpired());
        }
    }
}
