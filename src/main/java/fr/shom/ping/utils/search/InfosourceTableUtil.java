package fr.shom.ping.utils.search;

import fr.shom.ping.model.dto.InfosourceRecipientDto;
import fr.shom.ping.model.dto.InfosourceTableDto;
import fr.shom.ping.model.dto.ReferenceFolderStudyDto;
import fr.shom.ping.model.enums.InfosourceTableSortProperty;
import fr.shom.ping.model.predicate.InfosourceSearchCriteria;
import fr.shom.ping.model.tuple.InfosourceTableTuple;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static fr.shom.ping.utils.InfosourceUtil.buildNaturesOrderedArraySqlCase;
import static fr.shom.ping.utils.InfosourceUtil.buildStatusOrderedArraySqlCase;
import static fr.shom.ping.utils.SchemaUtil.FOLDER_TABLE;
import static fr.shom.ping.utils.SchemaUtil.INFOSOURCE_PART_TABLE;
import static fr.shom.ping.utils.SchemaUtil.INFOSOURCE_RECIPIENT_TABLE;
import static fr.shom.ping.utils.SchemaUtil.INFOSOURCE_TABLE_VIEW_TUPLE_MAPPING;
import static fr.shom.ping.utils.SchemaUtil.MARITIME_SERVICE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.NOTION_TABLE;
import static fr.shom.ping.utils.SchemaUtil.PUBLIC_USER_TABLE;
import static fr.shom.ping.utils.SchemaUtil.STUDY_REFERENCE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.STUDY_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_INFOSOURCE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_TABLE;
import static fr.shom.ping.utils.search.WarningSearchUtil.LEFT_JOIN;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.util.ObjectUtils.isEmpty;

public final class InfosourceTableUtil {

    private InfosourceTableUtil() {
    }

    public static PageImpl<InfosourceTableDto> find(EntityManager entityManager, InfosourceSearchCriteria searchCriteria, Pageable pageable,
        UUID publicUserId, String lang) {
        String orderBy = orderBy(searchCriteria, pageable, lang);
        List<UUID> ids = selectInfosourceIds(entityManager, searchCriteria, orderBy, publicUserId);
        if (ids.isEmpty()) {
            return new PageImpl<>(emptyList(), pageable, 0);
        }

        Set<UUID> paginatedIds = paginateIds(ids, pageable);
        List<InfosourceTableDto> paginatedInfosources = new ArrayList<>(paginateInfosources(entityManager, paginatedIds, orderBy(searchCriteria,
            pageable, lang, true), lang));

        return new PageImpl<>(paginatedInfosources, pageable, ids.size());
    }

    // We ignore "findsecbugs:SQL_INJECTION_JPA" sonar warning as we use setParameter with our NativeQuery to prevent SQL Injection
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    private static Collection<InfosourceTableDto> paginateInfosources(EntityManager entityManager, Set<UUID> paginatedIds, String orderBy, String lang) {
        StringBuilder sql = from();
        final String natureOrderedArray = buildNaturesOrderedArraySqlCase(lang);
        final String statusOrderedArray = buildStatusOrderedArraySqlCase(lang);
        sql.insert(0, "SELECT DISTINCT infos.id, infos.number, infos.nature, infos.creation_date, infos.last_update, infos.validation_time, " +
            "'' as comment, infos.status, infos.issuing_public_user_id, fp.description, ir.status as action_type, " +
            "ir.service_id as recipient_id, recipient.name as recipient_name, recipient.information as recipient_information, recipient.localized_information" +
            " as recipient_localized_information, " + natureOrderedArray + " as nature_order, " + statusOrderedArray + " as status_order, " +
            "CONCAT(warning.name_of_series, ' ', warning.warning_number, '/', (warning.year - 2000)) as warning_number, " +
            "sr.study_id as reference_study_id, sr.type as reference_type, sr.value as reference_value\n"
        );
        sql.append("WHERE infos.id IN (:infosourceIds)\n");
        sql.append(orderBy);

        Query query = entityManager.createNativeQuery(sql.toString(), INFOSOURCE_TABLE_VIEW_TUPLE_MAPPING);
        query.setParameter("infosourceIds", paginatedIds);

        LinkedHashMap<UUID, InfosourceTableDto> mapDto = new LinkedHashMap<>();
        query.getResultList().forEach(tuple -> map(mapDto, (InfosourceTableTuple) tuple));

        return mapDto.values();
    }

    private static void map(LinkedHashMap<UUID, InfosourceTableDto> mapDto, InfosourceTableTuple tuple) {
        if (mapDto.containsKey(tuple.getId())) {
            update(mapDto, tuple);
        } else {
            add(mapDto, tuple);
        }
    }

    private static void update(LinkedHashMap<UUID, InfosourceTableDto> mapDto, InfosourceTableTuple tuple) {
        InfosourceTableDto currentInfosource = mapDto.get(tuple.getId());

        if (!isEmpty(tuple.getRecipientId())) {
            InfosourceRecipientDto recipientDto = currentInfosource.getRecipients().stream()
                .filter(infosourceRecipientDto -> infosourceRecipientDto.getId().equals(tuple.getRecipientId()))
                .findFirst()
                .orElse(InfosourceRecipientDto.builder()
                    .id(tuple.getRecipientId())
                    .name(tuple.getRecipientName())
                    .actionType(tuple.getActionType())
                    .information(tuple.getRecipientInformation())
                    .localizedInformation(tuple.getRecipientLocalizedInformation())
                    .build()
                );
            currentInfosource.getRecipients().add(recipientDto);
        }

        if (!isEmpty(tuple.getWarningNumber()) && !StringUtils.equals(tuple.getWarningNumber(), " /")) {
            currentInfosource.getWarnings().add(tuple.getWarningNumber());
        }

        if (!isEmpty(tuple.getReferenceStudyId())) {
            ReferenceFolderStudyDto referenceFolderStudyDto = ReferenceFolderStudyDto.builder()
                .studyId(tuple.getReferenceStudyId())
                .type(tuple.getReferenceType())
                .value(tuple.getReferenceValue())
                .build();
            currentInfosource.getReferenceFolderStudies().add(referenceFolderStudyDto);
        }
    }

    private static void add(LinkedHashMap<UUID, InfosourceTableDto> mapDto, InfosourceTableTuple tuple) {
        InfosourceTableDto infosourceTableDto = InfosourceTableDto.builder()
            .id(tuple.getId())
            .number(tuple.getNumber())
            .nature(tuple.getNature())
            .status(tuple.getStatus())
            .comment(tuple.getComment())
            .creationDate(tuple.getCreationDate())
            .lastUpdate(tuple.getLastUpdate())
            .validationTime(tuple.getValidationTime())
            .description(tuple.getDescription())
            .issuingPublicUserId(tuple.getIssuingPublicUserId())
            .build();

        mapDto.put(tuple.getId(), infosourceTableDto);
        update(mapDto, tuple);
    }

    private static Set<UUID> paginateIds(List<UUID> ids, Pageable pageable) {
        return ids.stream()
            .skip(pageable.getPageNumber() * pageable.getPageSize())
            .limit(pageable.getPageSize())
            .collect(toSet());
    }

    private static String orderBy(InfosourceSearchCriteria searchCriteria, Pageable pageable, String lang) {
        return orderBy(searchCriteria, pageable, lang, false);
    }

    private static String orderBy(InfosourceSearchCriteria searchCriteria, Pageable pageable, String lang, boolean useForFinalSelect) {
        String orderProperty;
        Sort.Direction direction;
        switch (searchCriteria.getState()) {
            case DRAFT:
                orderProperty = "infos.last_update";
                direction = DESC;
                break;
            default:
                orderProperty = "infos.validation_time";
                direction = DESC;
        }

        Sort sortParams = pageable.getSort();
        // We only support one sort property
        if (!sortParams.isEmpty() && sortParams.stream().count() == 1) {
            Sort.Order sortParam = sortParams.stream().findFirst().get();
            InfosourceTableSortProperty sortProperty = InfosourceTableSortProperty.fromProperty(sortParam.getProperty());
            direction = sortParam.getDirection();
            orderProperty = switch (sortProperty) {
                case NUMBER -> "infos.number";
                case NATURE -> useForFinalSelect ? "nature_order" : buildNaturesOrderedArraySqlCase(lang);
                case LAST_UPDATE -> "infos.last_update";
                case VALIDATION_TIME -> "infos.validation_time";
                default -> useForFinalSelect ? "status_order" : buildStatusOrderedArraySqlCase(lang);
            };
        }

        return "ORDER BY " + orderProperty + " " + direction.name();
    }

    // We ignore "findsecbugs:SQL_INJECTION_JPA" sonar warning as we use setParameter with our NativeQuery to prevent SQL Injection
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    private static List<UUID> selectInfosourceIds(EntityManager entityManager, InfosourceSearchCriteria searchCriteria, String orderBy, UUID publicUserId) {
        StringBuilder sql = from();
        sql.insert(0, "SELECT infos.id\n");
        addCriteriaSelectionToSql(sql, searchCriteria);
        sql.append("GROUP BY infos.id\n");
        sql.append(orderBy);

        Query query = entityManager.createNativeQuery(sql.toString());

        addSearchCriteriaParametersToQuery(query, publicUserId);

        // Why we unwrap and addScalar : https://github.com/spring-projects/spring-data-jpa/issues/1796
        return query.unwrap(NativeQuery.class)
            .addScalar("id", StandardBasicTypes.UUID)
            .getResultList();
    }

    private static StringBuilder from() {
        StringBuilder queryStringBuilder = new StringBuilder();

        queryStringBuilder.append("FROM infosource infos\n");
        queryStringBuilder.append(LEFT_JOIN + INFOSOURCE_PART_TABLE + " fp ON infos.id = fp.infosource_id AND fp.position = 1\n");
        queryStringBuilder.append(LEFT_JOIN + INFOSOURCE_RECIPIENT_TABLE + " ir ON infos.id = ir.infosource_id\n");
        queryStringBuilder.append(LEFT_JOIN + MARITIME_SERVICE_TABLE + " recipient ON recipient.id = ir.service_id\n");
        queryStringBuilder.append(LEFT_JOIN + WARNING_INFOSOURCE_TABLE + " warning_infos ON warning_infos.infosource_id = infos.id\n");
        queryStringBuilder.append(LEFT_JOIN + WARNING_TABLE + " warning ON warning_infos.warning_id = warning.id AND warning.status = 'PUBLISHED'\n");
        queryStringBuilder.append(LEFT_JOIN + NOTION_TABLE + " notion_infos ON notion_infos.infosource_id = infos.id AND notion_infos.type = 'INFOSOURCE'\n");
        queryStringBuilder.append(LEFT_JOIN + FOLDER_TABLE + " folder_infos ON notion_infos.folder_id = folder_infos.id AND folder_infos.status = 'OPEN'\n");
        queryStringBuilder.append(LEFT_JOIN + STUDY_TABLE + " s ON s.folder_id = folder_infos.id\n");
        queryStringBuilder.append(LEFT_JOIN + STUDY_REFERENCE_TABLE + " sr ON sr.study_id = s.id\n");

        return queryStringBuilder;
    }

    public static void addCriteriaSelectionToSql(StringBuilder queryStringBuilder, InfosourceSearchCriteria searchCriteria) {
        queryStringBuilder.append(LEFT_JOIN + PUBLIC_USER_TABLE + " pu ON infos.issuing_public_user_id = pu.id\n");

        queryStringBuilder.append("WHERE 1=1\n");

        queryStringBuilder.append("AND pu.id = :issuingPublicUserId\n");

        if (!isEmpty(searchCriteria.getState())) {
            switch (searchCriteria.getState()) {
                case DRAFT:
                    queryStringBuilder.append("AND infos.status = 'DRAFT'\n");
                    break;
                case SENT:
                    queryStringBuilder.append("AND infos.status IN ('SENT','OPENED','REJECTED','TAKEN')\n");
                    break;
                default:
                    break;
            }
        }
    }

    public static void addSearchCriteriaParametersToQuery(Query query, UUID publicUserId) {
        query.setParameter("issuingPublicUserId", publicUserId);
    }
}
