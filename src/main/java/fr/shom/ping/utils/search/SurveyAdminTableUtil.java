package fr.shom.ping.utils.search;

import fr.shom.ping.model.dto.SurveyTableDto;
import fr.shom.ping.model.predicate.SurveyAdminSearchCriteria;
import fr.shom.ping.model.predicate.SurveySearchCriteria;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static java.util.Collections.emptyList;
import static org.springframework.util.ObjectUtils.isEmpty;

public final class SurveyAdminTableUtil extends AbstractAdminSurveyTableUtil {
    private static SurveyAdminTableUtil INSTANCE;

    private SurveyAdminTableUtil() {
    }

    public synchronized static SurveyAdminTableUtil getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SurveyAdminTableUtil();
        }
        return INSTANCE;
    }

    public PageImpl<SurveyTableDto> find(EntityManager entityManager, SurveyAdminSearchCriteria searchCriteria, Pageable pageable) {
        final String orderBy = orderBy(searchCriteria, pageable);
        final List<UUID> ids = selectSurveyIds(entityManager, searchCriteria, orderBy);
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
    private Collection<SurveyTableDto> paginateSurveys(EntityManager entityManager, Set<UUID> paginatedIds, String orderBy) {
        final StringBuilder sql = from();
        return getSurveyTableDtos(entityManager, paginatedIds, orderBy, sql);
    }

    @Override
    public void addCriteriaSelectionToSql(StringBuilder queryStringBuilder, SurveySearchCriteria searchCriteria) {
        addCriteriaSelectionToSql(queryStringBuilder, (SurveyAdminSearchCriteria) searchCriteria);
    }

    public void addCriteriaSelectionToSql(StringBuilder queryStringBuilder, SurveyAdminSearchCriteria searchCriteria) {
        queryStringBuilder.append("WHERE 1=1\n");

        if (!isEmpty(searchCriteria.getIssuingServiceId())) {
            queryStringBuilder.append("AND ms_survey.id = :issuingServiceId\n");
        }

        addBaseCriteriaSelectionToSql(queryStringBuilder, searchCriteria);
    }

    @Override
    public void addSearchCriteriaParametersToQuery(Query query, SurveySearchCriteria searchCriteria) {
        addSearchCriteriaParametersToQuery(query, (SurveyAdminSearchCriteria) searchCriteria);
    }

    public void addSearchCriteriaParametersToQuery(Query query, SurveyAdminSearchCriteria searchCriteria) {

        if (!isEmpty(searchCriteria.getIssuingServiceId())) {
            query.setParameter("issuingServiceId", searchCriteria.getIssuingServiceId());
        }

        addBaseSearchCriteriaParametersToQuery(query, searchCriteria);
    }
}
