package fr.shom.ping.utils.search;

import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.ServiceUserRole;
import fr.shom.ping.model.User;
import fr.shom.ping.model.dto.SubscriptionTableDto;
import fr.shom.ping.model.enums.SubscriptionTableSortProperty;
import fr.shom.ping.model.predicate.SubscriptionSearchCriteria;
import fr.shom.ping.model.tuple.SubscriptionTableTuple;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static fr.shom.ping.utils.SchemaUtil.AREA_TABLE;
import static fr.shom.ping.utils.SchemaUtil.MARITIME_SERVICE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.SUBSCRIPTION_TABLE_VIEW_TUPLE_MAPPING;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.util.ObjectUtils.isEmpty;

@Slf4j
public class SubscriptionTableUtil {

    public static final String LEFT_JOIN = "LEFT JOIN ";

    public static PageImpl<SubscriptionTableDto> find(EntityManager entityManager, SubscriptionSearchCriteria searchCriteria, Pageable pageable, User user) {
        String orderBy = orderBy(pageable);
        List<UUID> ids = selectSubscriptionIds(entityManager, searchCriteria, orderBy, user);
        if (ids.isEmpty()) {
            return new PageImpl<>(emptyList(), pageable, 0);
        }

        Set<UUID> paginatedIds = paginateIds(ids, pageable);
        List<SubscriptionTableDto> paginatedSubscriptions = new ArrayList<>(paginateSubscriptions(entityManager, paginatedIds, orderBy));

        return new PageImpl<>(paginatedSubscriptions, pageable, ids.size());
    }

    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    private static Collection<SubscriptionTableDto> paginateSubscriptions(EntityManager entityManager, Set<UUID> paginatedIds, String orderBy) {
        StringBuilder sql = from();
        sql.insert(0, "SELECT DISTINCT subscriptions.id, subscriptions.title, subscriptions.type, subscriptions.creation_date, subscriptions.sending_mail, " +
            "sa.name as area_name, sa.international_name as area_international_name, " +
            "ms.name as maritime_service_subscriber_name, ms.international_name as maritime_service_subscriber_international_name " +
            "\n");
        sql.append("WHERE subscriptions.id IN (:subscriptionIds)\n");
        sql.append(orderBy);

        Query query = entityManager.createNativeQuery(sql.toString(), SUBSCRIPTION_TABLE_VIEW_TUPLE_MAPPING);
        query.setParameter("subscriptionIds", paginatedIds);

        LinkedHashMap<UUID, SubscriptionTableDto> mapDto = new LinkedHashMap<>();
        query.getResultList().forEach(tuple -> add(mapDto, (SubscriptionTableTuple) tuple));

        return mapDto.values();
    }

    private static void add(LinkedHashMap<UUID, SubscriptionTableDto> mapDto, SubscriptionTableTuple tuple) {
        SubscriptionTableDto subscriptionTableDto = SubscriptionTableDto.builder()
            .id(tuple.getId())
            .title(tuple.getTitle())
            .creationDate(tuple.getCreationDate())
            .sendingMail(tuple.isSendingMail())
            .areaName(tuple.getAreaName())
            .areaInternationalName(tuple.getAreaInternationalName())
            .maritimeServiceSubscriberName(tuple.getMaritimeServiceSubscriberName())
            .maritimeServiceSubscriberInternationalName(tuple.getMaritimeServiceSubscriberInternationalName())
            .build();

        mapDto.put(tuple.getId(), subscriptionTableDto);
    }

    private static Set<UUID> paginateIds(List<UUID> ids, Pageable pageable) {
        return ids.stream()
            .skip(pageable.getPageNumber() * pageable.getPageSize())
            .limit(pageable.getPageSize())
            .collect(toSet());
    }

    // We ignore "findsecbugs:SQL_INJECTION_JPA" sonar warning as we use setParameter with our NativeQuery to prevent SQL Injection
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    private static List<UUID> selectSubscriptionIds(EntityManager entityManager, SubscriptionSearchCriteria searchCriteria, String orderBy, User user) {
        StringBuilder sql = from();
        sql.insert(0, "SELECT subscriptions.id, ms.name\n");
        addCriteriaSelectionToSql(sql, searchCriteria, user);
        sql.append("GROUP BY subscriptions.id, ms.name\n");
        sql.append(orderBy);

        Query query = entityManager.createNativeQuery(sql.toString());

        addSearchCriteriaParametersToQuery(query, searchCriteria, user);

        // Why we unwrap and addScalar : https://github.com/spring-projects/spring-data-jpa/issues/1796
        return query.unwrap(NativeQuery.class)
            .addScalar("id", StandardBasicTypes.UUID)
            .getResultList();
    }

    public static void addCriteriaSelectionToSql(StringBuilder queryStringBuilder, SubscriptionSearchCriteria searchCriteria, User user) {
        if (user instanceof ServiceUser) {
            queryStringBuilder.append("WHERE subscriptions.service_id IN :serviceIdsWithRoles\n");
        } else {
            queryStringBuilder.append("WHERE subscriptions.public_user_id = :publicUserId\n");
        }

        if (!isEmpty(searchCriteria.getType())) {
            queryStringBuilder.append("AND subscriptions.type = :subscriptionsType\n");
        }
    }

    public static void addSearchCriteriaParametersToQuery(Query query, SubscriptionSearchCriteria searchCriteria, User user) {
        if (user instanceof ServiceUser serviceUser) {
            List<UUID> serviceIdsWithRoles = serviceUser.getRoles().stream().map(ServiceUserRole::getServiceId).collect(Collectors.toList());
            query.setParameter("serviceIdsWithRoles", serviceIdsWithRoles);
        } else {
            query.setParameter("publicUserId", user.getId());
        }

        if (!isEmpty(searchCriteria.getType())) {
            query.setParameter("subscriptionsType", searchCriteria.getType().toString());
        }
    }

    private static StringBuilder from() {
        StringBuilder queryStringBuilder = new StringBuilder();
        queryStringBuilder.append("FROM subscription subscriptions\n");
        queryStringBuilder.append(LEFT_JOIN + AREA_TABLE + " sa ON subscriptions.area_id = sa.id\n");
        queryStringBuilder.append(LEFT_JOIN + MARITIME_SERVICE_TABLE + " ms ON subscriptions.service_id = ms.id\n");
        return queryStringBuilder;
    }

    private static String orderBy(Pageable pageable) {
        String orderProperty = "subscriptions.creation_date";
        Sort.Direction direction = DESC;

        Sort sortParams = pageable.getSort();
        if (!sortParams.isEmpty() && sortParams.stream().count() == 1) {
            Sort.Order sortParam = sortParams.stream().findFirst().get();
            SubscriptionTableSortProperty sortProperty = SubscriptionTableSortProperty.fromProperty(sortParam.getProperty());
            direction = sortParam.getDirection();
            if (sortProperty == SubscriptionTableSortProperty.TITLE) {
                orderProperty = "subscriptions.title";
            } else if (sortProperty == SubscriptionTableSortProperty.MARITIME_SERVICE_SUBSCRIBER) {
                orderProperty = "ms.name";
            } else {
                orderProperty = "subscriptions.creation_date";
            }
        }
        return "ORDER BY " + orderProperty + " " + direction.name();
    }
}
