package fr.shom.ping.utils.search;

import fr.shom.ping.model.dto.InfosourceAdminTableDto;
import fr.shom.ping.model.dto.InfosourceRecipientDto;
import fr.shom.ping.model.dto.MaritimeEntityConcernedDto;
import fr.shom.ping.model.dto.ReferenceFolderStudyDto;
import fr.shom.ping.model.enums.ActionType;
import fr.shom.ping.model.enums.InfosourceNature;
import fr.shom.ping.model.enums.InfosourceTableSortProperty;
import fr.shom.ping.model.predicate.InfosourceAdminSearchCriteria;
import fr.shom.ping.model.tuple.InfosourceAdminTableTuple;
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

import static fr.shom.ping.model.enums.InfosourceState.RECEIVED;
import static fr.shom.ping.utils.InfosourceUtil.buildNaturesOrderedArraySqlCase;
import static fr.shom.ping.utils.InfosourceUtil.buildStatusOrderedArraySqlCase;
import static fr.shom.ping.utils.SchemaUtil.FOLDER_TABLE;
import static fr.shom.ping.utils.SchemaUtil.INFOSOURCE_ADMIN_TABLE_VIEW_TUPLE_MAPPING;
import static fr.shom.ping.utils.SchemaUtil.INFOSOURCE_MARITIME_ENTITY_TABLE;
import static fr.shom.ping.utils.SchemaUtil.INFOSOURCE_PART_TABLE;
import static fr.shom.ping.utils.SchemaUtil.INFOSOURCE_RECIPIENT_TABLE;
import static fr.shom.ping.utils.SchemaUtil.INFOSOURCE_TAG_TABLE;
import static fr.shom.ping.utils.SchemaUtil.MARITIME_ENTITY_TABLE;
import static fr.shom.ping.utils.SchemaUtil.MARITIME_SERVICE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.NOTION_TABLE;
import static fr.shom.ping.utils.SchemaUtil.PRO_USER_TABLE;
import static fr.shom.ping.utils.SchemaUtil.PUBLIC_USER_TABLE;
import static fr.shom.ping.utils.SchemaUtil.STUDY_REFERENCE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.STUDY_TABLE;
import static fr.shom.ping.utils.SchemaUtil.TAG_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_INFOSOURCE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_TABLE;
import static fr.shom.ping.utils.search.WarningSearchUtil.END_OF_DAY_TIME;
import static fr.shom.ping.utils.search.WarningSearchUtil.LEFT_JOIN;
import static fr.shom.ping.utils.search.WarningSearchUtil.START_OF_DAY_TIME;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.util.ObjectUtils.isEmpty;

public final class InfosourceAdminTableUtil {

    private InfosourceAdminTableUtil() {
    }

    public static PageImpl<InfosourceAdminTableDto> find(EntityManager entityManager, InfosourceAdminSearchCriteria searchCriteria, Pageable pageable,
        UUID serviceUserId, String lang) {
        String orderBy = orderBy(searchCriteria, pageable, lang);
        List<UUID> ids = selectInfosourceIds(entityManager, searchCriteria, orderBy, serviceUserId);
        if (ids.isEmpty()) {
            return new PageImpl<>(emptyList(), pageable, 0);
        }

        Set<UUID> paginatedIds = paginateIds(ids, pageable);
        List<InfosourceAdminTableDto> paginatedInfosources = new ArrayList<>(paginateInfosources(entityManager, paginatedIds, orderBy(searchCriteria,
            pageable, lang, true), searchCriteria, lang));

        return new PageImpl<>(paginatedInfosources, pageable, ids.size());
    }


    // We ignore "findsecbugs:SQL_INJECTION_JPA" sonar warning as we use setParameter with our NativeQuery to prevent SQL Injection
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    private static Collection<InfosourceAdminTableDto> paginateInfosources(EntityManager entityManager, Set<UUID> paginatedIds, String orderBy,
        InfosourceAdminSearchCriteria searchCriteria, String lang) {
        StringBuilder sql = from();
        final String natureOrderedArray = buildNaturesOrderedArraySqlCase(lang);
        final String statusOrderedArray = buildStatusOrderedArraySqlCase(lang);
        sql.insert(0, "SELECT DISTINCT infos.id, infos.number, infos.nature, infos.creation_date, infos.last_update, infos.validation_time, " +
            "'' as comment, infos.status, infos.issuing_service_id, infos.issuing_service_user_id, fp.description, " +
            "ir.status as action_type, ir.service_id as recipient_id, recipient.name as recipient_name, recipient.information as recipient_information, " +
            "recipient.localized_information as recipient_localized_information, " + "ms_transfert.name as transferred_by, " +
            "(CASE\n" +
            "   WHEN ms_infos.name IS NULL THEN CONCAT(pub.firstname, ' ', pub.lastname)\n" +
            "   ELSE CONCAT(pro.firstname, ' ', pro.lastname, ' (', ms_infos.name, ')')\n" +
            "END) as emitter,\n" +
            "ir2.status as service_status, me_infos.id as entity_id, me_infos.name as entity_name, t.value as service_tag,\n" +
            natureOrderedArray + " as nature_order, " + statusOrderedArray + " as status_order,\n" +
            "(CASE WHEN folder_infos.number IS NOT NULL THEN " +
            "CONCAT(folder_infos.number,'/',RIGHT(CAST(EXTRACT(YEAR FROM folder_infos.creation_date) AS VARCHAR), 2),' - ',msf.name) " +
            "ELSE CONCAT(folder_infos.number,'') END) as folder_number,\n" +
            "CONCAT(warning.name_of_series, ' ', warning.warning_number, '/', (warning.year - 2000)) as warning_number, " +
            "sr.study_id as reference_study_id, sr.type as reference_type, sr.value as reference_value\n"
        );

        sql.append("LEFT JOIN maritime_service ms_transfert ON ms_transfert.id = ir.transferred_by_id\n");

        sql.append(LEFT_JOIN + TAG_TABLE + " t ON t.id = tag_infos.tag_id AND t.maritime_service_id = :issuingServiceId\n");

        sql.append("WHERE infos.id IN (:infosourceIds)\n");

        if (searchCriteria.getState() == RECEIVED) {
            sql.append("AND ir2.service_id = :issuingServiceId\n");
        }

        sql.append(orderBy);

        Query query = entityManager.createNativeQuery(sql.toString(), INFOSOURCE_ADMIN_TABLE_VIEW_TUPLE_MAPPING);
        query.setParameter("infosourceIds", paginatedIds);

        query.setParameter("issuingServiceId", searchCriteria.getIssuingServiceId());

        LinkedHashMap<UUID, InfosourceAdminTableDto> mapDto = new LinkedHashMap<>();
        query.getResultList().forEach(tuple -> map(mapDto, (InfosourceAdminTableTuple) tuple));

        return mapDto.values();
    }

    private static void map(LinkedHashMap<UUID, InfosourceAdminTableDto> mapDto, InfosourceAdminTableTuple tuple) {
        if (mapDto.containsKey(tuple.getId())) {
            update(mapDto, tuple);
        } else {
            add(mapDto, tuple);
        }
    }

    private static void update(LinkedHashMap<UUID, InfosourceAdminTableDto> mapDto, InfosourceAdminTableTuple tuple) {
        InfosourceAdminTableDto currentInfosource = mapDto.get(tuple.getId());

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
                    .transferredBy(tuple.getTransferredBy())
                    .build()
                );
            currentInfosource.getRecipients().add(recipientDto);
        }

        if (!isEmpty(tuple.getEntityId())) {
            currentInfosource.getConcernedMaritimeEntities().add(MaritimeEntityConcernedDto.builder().id(tuple.getEntityId()).name(tuple.getEntityName()).build());
        }

        if (!isEmpty(tuple.getServiceTag())) {
            currentInfosource.getTags().add(tuple.getServiceTag());
        }

        if (!isEmpty(tuple.getFolderNumber())) {
            currentInfosource.getFolders().add(tuple.getFolderNumber());
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

    private static void add(LinkedHashMap<UUID, InfosourceAdminTableDto> mapDto, InfosourceAdminTableTuple tuple) {
        InfosourceAdminTableDto infosourceAdminTableDto = InfosourceAdminTableDto.builder()
            .id(tuple.getId())
            .number(tuple.getNumber())
            .nature(tuple.getNature())
            .comment(tuple.getComment())
            .creationDate(tuple.getCreationDate())
            .lastUpdate(tuple.getLastUpdate())
            .validationTime(tuple.getValidationTime())
            .description(tuple.getDescription())
            .status(tuple.getStatus())
            .issuingServiceUserId(tuple.getIssuingServiceUserId())
            .issuingServiceId(tuple.getIssuingServiceId())
            .emitter(tuple.getEmitter())
            .serviceStatus(tuple.getServiceStatus())
            .build();

        mapDto.put(tuple.getId(), infosourceAdminTableDto);
        update(mapDto, tuple);
    }

    private static Set<UUID> paginateIds(List<UUID> ids, Pageable pageable) {
        return ids.stream()
            .skip(pageable.getPageNumber() * pageable.getPageSize())
            .limit(pageable.getPageSize())
            .collect(toSet());
    }

    private static String orderBy(InfosourceAdminSearchCriteria searchCriteria, Pageable pageable, String lang) {
        return orderBy(searchCriteria, pageable, lang, false);
    }

    private static String orderBy(InfosourceAdminSearchCriteria searchCriteria, Pageable pageable, String lang, boolean useForFinalSelect) {
        String orderProperty;
        Sort.Direction direction = switch (searchCriteria.getState()) {
            case DRAFT -> {
                orderProperty = "infos.last_update";
                yield DESC;
            }
            case SENT, RECEIVED -> {
                orderProperty = "infos.validation_time";
                yield DESC;
            }
            default -> {
                orderProperty = "infos.creation_date";
                yield DESC;
            }
        };

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
                case SERVICE_STATUS -> useForFinalSelect ? "service_status" : "infos.status";
                case EMITTER -> useForFinalSelect ? "emitter" : "infos.status";
                default -> useForFinalSelect ? "status_order" : buildStatusOrderedArraySqlCase(lang);
            };
        }

        return "ORDER BY " + orderProperty + " " + direction.name();
    }

    // We ignore "findsecbugs:SQL_INJECTION_JPA" sonar warning as we use setParameter with our NativeQuery to prevent SQL Injection
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    private static List<UUID> selectInfosourceIds(EntityManager entityManager, InfosourceAdminSearchCriteria searchCriteria, String orderBy,
        UUID serviceUserId) {
        StringBuilder sql = from();
        if (isNotEmpty(searchCriteria.getTags())) {
            sql.append(LEFT_JOIN + TAG_TABLE + " t ON t.id = tag_infos.tag_id AND t.maritime_service_id = :issuingServiceId\n");
        }
        sql.insert(0, "SELECT infos.id\n");
        addCriteriaSelectionToSql(sql, searchCriteria);
        sql.append("GROUP BY infos.id\n");
        sql.append(orderBy);

        Query query = entityManager.createNativeQuery(sql.toString());

        addSearchCriteriaParametersToQuery(query, searchCriteria, serviceUserId);

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
        queryStringBuilder.append(LEFT_JOIN + MARITIME_SERVICE_TABLE + " ms_infos ON ms_infos.id = infos.issuing_service_id\n");
        queryStringBuilder.append(LEFT_JOIN + PRO_USER_TABLE + " pro ON infos.issuing_service_user_id = pro.id\n");
        queryStringBuilder.append(LEFT_JOIN + PUBLIC_USER_TABLE + " pub ON infos.issuing_public_user_id = pub.id\n");
        queryStringBuilder.append(LEFT_JOIN + INFOSOURCE_RECIPIENT_TABLE + " ir2 ON infos.id = ir2.infosource_id\n");
        queryStringBuilder.append(LEFT_JOIN + INFOSOURCE_MARITIME_ENTITY_TABLE + " ime ON ime.infosource_id = infos.id\n");
        queryStringBuilder.append(LEFT_JOIN + MARITIME_ENTITY_TABLE + " me_infos ON me_infos.id = ime.maritime_entity_id\n");
        queryStringBuilder.append(LEFT_JOIN + INFOSOURCE_TAG_TABLE + " tag_infos ON tag_infos.infosource_id = infos.id\n");
        queryStringBuilder.append(LEFT_JOIN + NOTION_TABLE + " notion_infos ON notion_infos.infosource_id = infos.id AND notion_infos.type = 'INFOSOURCE'\n");
        queryStringBuilder.append(LEFT_JOIN + FOLDER_TABLE + " folder_infos ON notion_infos.folder_id = folder_infos.id AND folder_infos.status = 'OPEN'\n");
        queryStringBuilder.append(LEFT_JOIN + MARITIME_SERVICE_TABLE + " msf ON folder_infos.issuing_service_id = msf.id\n");
        queryStringBuilder.append(LEFT_JOIN + WARNING_INFOSOURCE_TABLE + " warning_infos ON warning_infos.infosource_id = infos.id\n");
        queryStringBuilder.append(LEFT_JOIN + WARNING_TABLE + " warning ON warning_infos.warning_id = warning.id AND warning.status = 'PUBLISHED'\n");
        queryStringBuilder.append(LEFT_JOIN + STUDY_TABLE + " s ON s.folder_id = folder_infos.id\n");
        queryStringBuilder.append(LEFT_JOIN + STUDY_REFERENCE_TABLE + " sr ON sr.study_id = s.id\n");

        return queryStringBuilder;
    }

    public static void addCriteriaSelectionToSql(StringBuilder queryStringBuilder, InfosourceAdminSearchCriteria searchCriteria) {

        queryStringBuilder.append("WHERE 1=1\n");

        if (!isEmpty(searchCriteria.getState())) {
            switch (searchCriteria.getState()) {
                case DRAFT:
                case SENT:
                    queryStringBuilder.append("AND ms_infos.id = :issuingServiceId\n");
                    break;
                case RECEIVED:
                    if (isEmpty(searchCriteria.getRecipientIds())) {
                        queryStringBuilder.append("AND recipient.id = :issuingServiceId\n");
                    }
                    break;
                default:
                    break;
            }
        }

        if (isNotEmpty(searchCriteria.getMaritimeEntityIds())) {
            queryStringBuilder.append("AND ime.maritime_entity_id IN (:maritimeEntityIds)\n");
        }

        if (isNotEmpty(searchCriteria.getTags())) {
            queryStringBuilder.append("AND t.value IN (:tags)\n");
        }

        if (isNotEmpty(searchCriteria.getRecipientIds())) {
            queryStringBuilder.append("AND recipient.id IN (:recipientIds)\n");
        }

        if (StringUtils.isNotBlank(searchCriteria.getSendingYear())) {
            queryStringBuilder.append("AND CAST(EXTRACT(YEAR FROM infos.validation_time) AS VARCHAR) = :sendingYear\n");
        }

        if (!searchCriteria.isArchived() && !searchCriteria.isRejected() && searchCriteria.getState() == RECEIVED) {
            queryStringBuilder.append("AND ir.status != 'ARCHIVE'\n");
        }

        if (!searchCriteria.isArchived() && searchCriteria.isRejected() && searchCriteria.getState() == RECEIVED) {
            queryStringBuilder.append("AND (ir.status != 'ARCHIVE' OR ir.status = 'ARCHIVE' AND infos.status = 'REJECTED')\n");
        }

        if (StringUtils.isNotBlank(searchCriteria.getNumber())) {
            queryStringBuilder.append("AND LOWER(infos.number) LIKE :infosNumber\n");
        }

        if (!isEmpty(searchCriteria.getState())) {
            switch (searchCriteria.getState()) {
                case DRAFT:
                    queryStringBuilder.append("AND infos.status = 'DRAFT'\n");
                    break;
                case SENT:
                    queryStringBuilder.append("AND infos.status IN ('SENT','OPENED','REJECTED','TAKEN')\n");
                    break;
                case RECEIVED:
                    queryStringBuilder.append("AND infos.status IN ('SENT','OPENED','TAKEN'").append(searchCriteria.isRejected() ? ",'REJECTED'" : "").append(")\n");
                    break;
                default:
                    break;
            }
        }

        if (isNotEmpty(searchCriteria.getNatures())) {
            queryStringBuilder.append("AND infos.nature IN (:natures)\n");
        }

        if (isNotEmpty(searchCriteria.getServiceStatuses())) {
            queryStringBuilder.append("AND ir.status IN (:serviceStatuses)\n");
        }

        if (!isEmpty(searchCriteria.getLastUpdatePeriodDateStart()) || !isEmpty(searchCriteria.getLastUpdatePeriodDateEnd())) {
            // We don't need to check if date_start or date_end is null
            // "null + time '01:23:45'" always returns null, so there is no timestamp limit depending on which attribute is null
            queryStringBuilder.append("AND tsrange(infos.last_update + time '" + START_OF_DAY_TIME + "', infos.last_update + time '" + END_OF_DAY_TIME + "') " +
                "&& :lastUpdateStartToEndRange\\:\\:tsrange\n");
        }

        if (!isEmpty(searchCriteria.getSendingPeriodDateStart()) || !isEmpty(searchCriteria.getSendingPeriodDateEnd())) {
            // We don't need to check if date_start or date_end is null
            // "null + time '01:23:45'" always returns null, so there is no timestamp limit depending on which attribute is null
            queryStringBuilder.append("AND tsrange(infos.validation_time + time '" + START_OF_DAY_TIME + "', infos.validation_time + time '" + END_OF_DAY_TIME
                + "') && :sendingPeriodStartToEndRange\\:\\:tsrange\n");
        }
    }

    public static void addSearchCriteriaParametersToQuery(Query query, InfosourceAdminSearchCriteria searchCriteria, UUID serviceUserId) {
        if (!isEmpty(searchCriteria.getState())) {
            switch (searchCriteria.getState()) {
                case DRAFT:
                case SENT:
                    query.setParameter("issuingServiceId", searchCriteria.getIssuingServiceId());
                    break;
                case RECEIVED:
                    if (isEmpty(searchCriteria.getRecipientIds())) {
                        query.setParameter("issuingServiceId", searchCriteria.getIssuingServiceId());
                    }
                    break;
                default:
                    break;
            }
        }

        if (isNotEmpty(searchCriteria.getTags())) {
            query.setParameter("tags", searchCriteria.getTags());
        }

        if (StringUtils.isNotBlank(searchCriteria.getNumber())) {
            query.setParameter("infosNumber", "%" + searchCriteria.getNumber().toLowerCase() + "%");
        }

        if (StringUtils.isNotBlank(searchCriteria.getSendingYear())) {
            query.setParameter("sendingYear", searchCriteria.getSendingYear());
        }

        if (isNotEmpty(searchCriteria.getMaritimeEntityIds())) {
            query.setParameter("maritimeEntityIds", searchCriteria.getMaritimeEntityIds());
        }

        if (isNotEmpty(searchCriteria.getRecipientIds())) {
            query.setParameter("recipientIds", searchCriteria.getRecipientIds());
        }

        if (isNotEmpty(searchCriteria.getNatures())) {
            List<String> details = searchCriteria.getNatures().stream().map(InfosourceNature::name).collect(toList());
            query.setParameter("natures", details);
        }

        if (isNotEmpty(searchCriteria.getServiceStatuses())) {
            List<String> details = searchCriteria.getServiceStatuses().stream().map(ActionType::name).collect(toList());
            query.setParameter("serviceStatuses", details);
        }

        if (!isEmpty(searchCriteria.getLastUpdatePeriodDateStart()) && isEmpty(searchCriteria.getLastUpdatePeriodDateEnd())) {
            query.setParameter("lastUpdateStartToEndRange", "[" + searchCriteria.getLastUpdatePeriodDateStart() + " " + START_OF_DAY_TIME + ",]");
        } else if (isEmpty(searchCriteria.getLastUpdatePeriodDateStart()) && !isEmpty(searchCriteria.getLastUpdatePeriodDateEnd())) {
            query.setParameter("lastUpdateStartToEndRange", "[," + searchCriteria.getLastUpdatePeriodDateEnd() + " " + END_OF_DAY_TIME + "]");
        } else if (!isEmpty(searchCriteria.getLastUpdatePeriodDateStart()) && !isEmpty(searchCriteria.getLastUpdatePeriodDateEnd())) {
            query.setParameter("lastUpdateStartToEndRange",
                "[" + searchCriteria.getLastUpdatePeriodDateStart() + " " + START_OF_DAY_TIME + "," + searchCriteria.getLastUpdatePeriodDateEnd() + " "
                    + END_OF_DAY_TIME + "]");
        }

        if (!isEmpty(searchCriteria.getSendingPeriodDateStart()) && isEmpty(searchCriteria.getSendingPeriodDateEnd())) {
            query.setParameter("sendingPeriodStartToEndRange", "[" + searchCriteria.getSendingPeriodDateStart() + " " + START_OF_DAY_TIME + ",]");
        } else if (isEmpty(searchCriteria.getSendingPeriodDateStart()) && !isEmpty(searchCriteria.getSendingPeriodDateEnd())) {
            query.setParameter("sendingPeriodStartToEndRange", "[," + searchCriteria.getSendingPeriodDateEnd() + " " + END_OF_DAY_TIME + "]");
        } else if (!isEmpty(searchCriteria.getSendingPeriodDateStart()) && !isEmpty(searchCriteria.getSendingPeriodDateEnd())) {
            query.setParameter("sendingPeriodStartToEndRange",
                "[" + searchCriteria.getSendingPeriodDateStart() + " " + START_OF_DAY_TIME + "," + searchCriteria.getSendingPeriodDateEnd() + " "
                    + END_OF_DAY_TIME + "]");
        }
    }
}
