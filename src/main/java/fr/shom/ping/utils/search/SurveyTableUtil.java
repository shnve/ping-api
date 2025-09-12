package fr.shom.ping.utils.search;

import fr.shom.ping.model.dto.SurveyTableDto;
import fr.shom.ping.model.predicate.SurveySearchCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static fr.shom.ping.utils.SchemaUtil.PUBLIC_USER_TABLE;
import static fr.shom.ping.utils.search.WarningSearchUtil.LEFT_JOIN;
import static java.util.Collections.emptyList;

public final class SurveyTableUtil extends AbstractSurveyTableUtil {
    private static SurveyTableUtil INSTANCE;

    private SurveyTableUtil() {
    }

    public synchronized static SurveyTableUtil getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SurveyTableUtil();
        }
        return INSTANCE;
    }

    public PageImpl<SurveyTableDto> find(EntityManager entityManager, SurveySearchCriteria searchCriteria, Pageable pageable, UUID publicUserId) {
        final String orderBy = orderBy(searchCriteria, pageable);
        final List<UUID> ids = selectSurveyIds(entityManager, searchCriteria, orderBy, publicUserId);
        if (ids.isEmpty()) {
            return new PageImpl<>(emptyList(), pageable, 0);
        }

        final Set<UUID> paginatedIds = paginateIds(ids, pageable);
        final List<SurveyTableDto> paginatedSurveys = new ArrayList<>(paginateSurveys(entityManager, paginatedIds, orderBy(searchCriteria, pageable,
            true)));

        return new PageImpl<>(paginatedSurveys, pageable, ids.size());
    }

    // We ignore "findsecbugs:SQL_INJECTION_JPA" sonar warning as we use setParameter with our NativeQuery to prevent SQL Injection
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    private List<UUID> selectSurveyIds(EntityManager entityManager, SurveySearchCriteria searchCriteria, String orderBy, UUID publicUserId) {
        final StringBuilder sql = from();

        sql.insert(0, "SELECT survey.id\n");
        addCriteriaSelectionToSql(sql, searchCriteria);
        sql.append("GROUP BY survey.id\n");
        sql.append(orderBy);

        final Query query = entityManager.createNativeQuery(sql.toString());

        addSearchCriteriaParametersToQuery(query, searchCriteria, publicUserId);

        // Why we unwrap and addScalar : https://github.com/spring-projects/spring-data-jpa/issues/1796
        return query.unwrap(NativeQuery.class)
            .addScalar("id", StandardBasicTypes.UUID)
            .getResultList();
    }

    // We ignore "findsecbugs:SQL_INJECTION_JPA" sonar warning as we use setParameter with our NativeQuery to prevent SQL Injection
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    private Collection<SurveyTableDto> paginateSurveys(EntityManager entityManager, Set<UUID> paginatedIds, String orderBy) {
        final StringBuilder sql = from();
        return getSurveyTableDtos(entityManager, paginatedIds, orderBy, sql);
    }

    private StringBuilder from() {
        StringBuilder queryStringBuilder = new StringBuilder();

        queryStringBuilder.append("FROM survey survey\n");
        queryStringBuilder.append(LEFT_JOIN + PUBLIC_USER_TABLE + " pub ON survey.issuing_public_user_id = pub.id\n");

        return queryStringBuilder;
    }

    public void addCriteriaSelectionToSql(StringBuilder queryStringBuilder, SurveySearchCriteria searchCriteria) {
        queryStringBuilder.append(LEFT_JOIN + PUBLIC_USER_TABLE + " pu ON survey.issuing_public_user_id = pu.id\n");
        queryStringBuilder.append("WHERE 1=1\n");
        queryStringBuilder.append("AND pu.id = :issuingPublicUserId\n");
        addBaseCriteriaSelectionToSql(queryStringBuilder, searchCriteria);
    }

    public void addSearchCriteriaParametersToQuery(Query query, SurveySearchCriteria searchCriteria, UUID publicUserId) {
        query.setParameter("issuingPublicUserId", publicUserId);
        addBaseSearchCriteriaParametersToQuery(query, searchCriteria);
    }
}
