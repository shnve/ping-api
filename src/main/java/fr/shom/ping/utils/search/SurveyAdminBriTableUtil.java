package fr.shom.ping.utils.search;

import fr.shom.ping.model.dto.SurveyTableBriDto;
import fr.shom.ping.model.predicate.SurveyAdminBriSearchCriteria;
import fr.shom.ping.model.predicate.SurveySearchCriteria;
import fr.shom.ping.model.tuple.SurveyTableBriTuple;
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

import static fr.shom.ping.utils.SchemaUtil.SURVEY_ADMIN_TABLE_BRI_VIEW_TUPLE_MAPPING;
import static fr.shom.ping.utils.SurveyUtil.EXPECTED_DELETION_DATE_OFFSET_DAYS_FOR_TRANSMITTED_DEPOSIT;
import static java.util.Collections.emptyList;
import static org.springframework.util.ObjectUtils.isEmpty;

public final class SurveyAdminBriTableUtil extends AbstractAdminSurveyTableUtil {
    private static SurveyAdminBriTableUtil INSTANCE;

    private SurveyAdminBriTableUtil() {
    }

    public synchronized static SurveyAdminBriTableUtil getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SurveyAdminBriTableUtil();
        }
        return INSTANCE;
    }

    public PageImpl<SurveyTableBriDto> find(EntityManager entityManager, SurveyAdminBriSearchCriteria searchCriteria, Pageable pageable) {
        final String orderBy = orderBy(searchCriteria, pageable);
        final List<UUID> ids = selectSurveyIds(entityManager, searchCriteria, orderBy);
        if (ids.isEmpty()) {
            return new PageImpl<>(emptyList(), pageable, 0);
        }

        final Set<UUID> paginatedIds = paginateIds(ids, pageable);
        final List<SurveyTableBriDto> paginatedSurveys = new ArrayList<>(paginateSurveys(entityManager, paginatedIds, orderBy(searchCriteria, pageable,
            true)));

        return new PageImpl<>(paginatedSurveys, pageable, ids.size());
    }

    // We ignore "findsecbugs:SQL_INJECTION_JPA" sonar warning as we use setParameter with our NativeQuery to prevent SQL Injection
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    private Collection<SurveyTableBriDto> paginateSurveys(EntityManager entityManager, Set<UUID> paginatedIds, String orderBy) {
        final StringBuilder sql = from();
        return getSurveyTableBriDtos(entityManager, paginatedIds, orderBy, sql);
    }

    // We ignore "findsecbugs:SQL_INJECTION_JPA" sonar warning as we use setParameter with our NativeQuery to prevent SQL Injection
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    private List<UUID> selectSurveyIds(EntityManager entityManager, SurveyAdminBriSearchCriteria searchCriteria, String orderBy) {
        final StringBuilder sql = from();

        sql.insert(0, "SELECT survey.id\n");
        addCriteriaSelectionToSql(sql, searchCriteria);
        sql.append("GROUP BY survey.id\n");
        sql.append(orderBy);

        final Query query = entityManager.createNativeQuery(sql.toString());

        addSearchCriteriaParametersToQuery(query, searchCriteria);

        // Why we unwrap and addScalar : https://github.com/spring-projects/spring-data-jpa/issues/1796
        return query.unwrap(NativeQuery.class)
            .addScalar("id", StandardBasicTypes.UUID)
            .getResultList();
    }

    @Override
    public void addCriteriaSelectionToSql(StringBuilder queryStringBuilder, SurveySearchCriteria searchCriteria) {
        addCriteriaSelectionToSql(queryStringBuilder, (SurveyAdminBriSearchCriteria) searchCriteria);
    }

    public void addCriteriaSelectionToSql(StringBuilder queryStringBuilder, SurveyAdminBriSearchCriteria searchCriteria) {
        queryStringBuilder.append("WHERE 1=1\n");

        if (!isEmpty(searchCriteria.getShomId())) {
            queryStringBuilder.append("AND LOWER(survey.shom_id) LIKE :shomId\n");
        }

        if (!isEmpty(searchCriteria.getOwnerOrganization())) {
            queryStringBuilder.append("AND LOWER(survey.owner_organization) LIKE :ownerOrganization\n");
        }

        if (!isEmpty(searchCriteria.getProducerOrganization())) {
            queryStringBuilder.append("AND LOWER(survey.producer_organization) LIKE :producerOrganization\n");
        }

        if (!isEmpty(searchCriteria.getBriInternalComment())) {
            queryStringBuilder.append("AND LOWER(survey.bri_internal_comment) LIKE :briInternalComment\n");
        }

        if (searchCriteria.getNotCompliant() != null) {
            queryStringBuilder.append("AND survey.compliant = 'NOT_COMPLIANT'\n");
        }

        if (!isEmpty(searchCriteria.getTransmissionStatus())) {
            queryStringBuilder.append("AND survey.transmission_status = :transmissionStatus\n");
        }

        if (!isEmpty(searchCriteria.getExpectedDeletionDate())) {
            queryStringBuilder.append("AND survey.last_update >= :expectedDeletionDate\n");
        }

        addBaseCriteriaSelectionToSql(queryStringBuilder, searchCriteria);
    }

    @Override
    public void addSearchCriteriaParametersToQuery(Query query, SurveySearchCriteria searchCriteria) {
        addSearchCriteriaParametersToQuery(query, (SurveyAdminBriSearchCriteria) searchCriteria);
    }

    public void addSearchCriteriaParametersToQuery(Query query, SurveyAdminBriSearchCriteria searchCriteria) {

        if (!isEmpty(searchCriteria.getShomId())) {
            query.setParameter("shomId", "%" + searchCriteria.getShomId() + "%");
        }

        if (!isEmpty(searchCriteria.getOwnerOrganization())) {
            query.setParameter("ownerOrganization", "%" + searchCriteria.getOwnerOrganization() + "%");
        }

        if (!isEmpty(searchCriteria.getProducerOrganization())) {
            query.setParameter("producerOrganization", "%" + searchCriteria.getProducerOrganization() + "%");
        }

        if (!isEmpty(searchCriteria.getBriInternalComment())) {
            query.setParameter("briInternalComment", "%" + searchCriteria.getBriInternalComment() + "%");
        }

        if (!isEmpty(searchCriteria.getTransmissionStatus())) {
            query.setParameter("transmissionStatus", searchCriteria.getTransmissionStatus());
        }

        if (!isEmpty(searchCriteria.getExpectedDeletionDate())) {
            query.setParameter("expectedDeletionDate",
                searchCriteria.getExpectedDeletionDate().minusDays(EXPECTED_DELETION_DATE_OFFSET_DAYS_FOR_TRANSMITTED_DEPOSIT));
        }

        addBaseSearchCriteriaParametersToQuery(query, searchCriteria);
    }

    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    public Collection<SurveyTableBriDto> getSurveyTableBriDtos(EntityManager entityManager, Set<UUID> paginatedIds, String orderBy, StringBuilder sql) {
        sql.insert(0, "SELECT DISTINCT survey.id, survey.identifier, survey.shom_id, survey.producer_organization, survey.owner_organization, " +
            "survey.bri_internal_comment, survey.name_and_location, survey.last_update, survey.start_date, survey.end_date, survey.compliant, " +
            "survey.last_update as expected_deletion_date, spr.status as test_processing_status, spr.processing_date as test_processing_date, " +
            "survey.transmission_status, survey.last_update_transmission_date\n"
        );

        sql.append("LEFT JOIN survey_processing_result_list sprl ON sprl.survey_id = survey.id\n");
        sql.append("LEFT JOIN survey_processing_result spr ON spr.id = sprl.processing_result_id\n");

        sql.append("WHERE survey.id IN (:surveyIds)\n");
        sql.append("AND spr.processing_date IN (" +
            "select max(processing_date) from survey_processing_result spr join survey_processing_result_list sprl on sprl.processing_result_id = spr.id " +
            "group by sprl.survey_id)\n");

        sql.append(orderBy);

        final Query query = entityManager.createNativeQuery(sql.toString(), SURVEY_ADMIN_TABLE_BRI_VIEW_TUPLE_MAPPING);
        query.setParameter("surveyIds", paginatedIds);

        final LinkedHashMap<UUID, SurveyTableBriDto> mapDto = new LinkedHashMap<>();
        query.getResultList().forEach(tuple -> map(mapDto, (SurveyTableBriTuple) tuple));


        return mapDto.values();
    }

    private void map(LinkedHashMap<UUID, SurveyTableBriDto> mapDto, SurveyTableBriTuple tuple) {
        add(mapDto, tuple);
    }

    private void add(LinkedHashMap<UUID, SurveyTableBriDto> mapDto, SurveyTableBriTuple tuple) {
        final SurveyTableBriDto surveyTableDto = SurveyTableBriDto.builder()
            .id(tuple.getId())
            .identifier(tuple.getIdentifier())
            .shomId(tuple.getShomId())
            .producerOrganization(tuple.getProducerOrganization())
            .ownerOrganization(tuple.getOwnerOrganization())
            .briInternalComment(tuple.getBriInternalComment())
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
}
