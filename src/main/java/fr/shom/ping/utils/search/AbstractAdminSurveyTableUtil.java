package fr.shom.ping.utils.search;

import fr.shom.ping.model.predicate.SurveySearchCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import java.util.UUID;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;

import static fr.shom.ping.utils.SchemaUtil.MARITIME_SERVICE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.PRO_USER_TABLE;
import static fr.shom.ping.utils.search.WarningSearchUtil.LEFT_JOIN;

public abstract class AbstractAdminSurveyTableUtil extends AbstractSurveyTableUtil {

    public abstract void addCriteriaSelectionToSql(StringBuilder queryStringBuilder, SurveySearchCriteria searchCriteria);

    public abstract void addSearchCriteriaParametersToQuery(Query query, SurveySearchCriteria searchCriteria);

    protected StringBuilder from() {
        StringBuilder queryStringBuilder = new StringBuilder();

        queryStringBuilder.append("FROM survey survey\n");
        queryStringBuilder.append(LEFT_JOIN + MARITIME_SERVICE_TABLE + " ms_survey ON ms_survey.id = survey.issuing_service_id\n");
        queryStringBuilder.append(LEFT_JOIN + PRO_USER_TABLE + " pro ON survey.issuing_service_user_id = pro.id\n");

        return queryStringBuilder;
    }

    // We ignore "findsecbugs:SQL_INJECTION_JPA" sonar warning as we use setParameter with our NativeQuery to prevent SQL Injection
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    protected List<UUID> selectSurveyIds(EntityManager entityManager, SurveySearchCriteria searchCriteria, String orderBy) {
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
}
