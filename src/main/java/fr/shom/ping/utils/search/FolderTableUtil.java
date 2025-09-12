package fr.shom.ping.utils.search;

import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.ServiceUserRole;
import fr.shom.ping.model.dto.AreaSharedDto;
import fr.shom.ping.model.dto.FolderStudyDto;
import fr.shom.ping.model.dto.FolderTableDto;
import fr.shom.ping.model.dto.MaritimeEntityConcernedDto;
import fr.shom.ping.model.dto.MaritimeServiceSharedDto;
import fr.shom.ping.model.enums.FolderTableSortProperty;
import fr.shom.ping.model.predicate.FolderSearchCriteria;
import fr.shom.ping.model.tuple.FolderTableTuple;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static fr.shom.ping.utils.SchemaUtil.AREA_TABLE;
import static fr.shom.ping.utils.SchemaUtil.FOLDER_FOLLOWING_TABLE;
import static fr.shom.ping.utils.SchemaUtil.FOLDER_MARITIME_ENTITY_TABLE;
import static fr.shom.ping.utils.SchemaUtil.FOLDER_MARITIME_SERVICE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.FOLDER_TABLE_VIEW_TUPLE_MAPPING;
import static fr.shom.ping.utils.SchemaUtil.MARITIME_ENTITY_TABLE;
import static fr.shom.ping.utils.SchemaUtil.MARITIME_SERVICE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.STUDY_TABLE;
import static fr.shom.ping.utils.SchemaUtil.SUB_AREA_TABLE;
import static fr.shom.ping.utils.search.WarningSearchUtil.END_OF_DAY_TIME;
import static fr.shom.ping.utils.search.WarningSearchUtil.START_OF_DAY_TIME;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.util.ObjectUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
public final class FolderTableUtil {

    public static final String LEFT_JOIN = "LEFT JOIN ";

    public static PageImpl<FolderTableDto> find(EntityManager entityManager, FolderSearchCriteria searchCriteria, Pageable pageable, ServiceUser proUser) {
        String orderBy = orderBy(pageable);
        List<UUID> ids = selectFolderIds(entityManager, searchCriteria, orderBy, proUser);
        if (ids.isEmpty()) {
            return new PageImpl<>(emptyList(), pageable, 0);
        }

        Set<UUID> paginatedIds = paginateIds(ids, pageable);
        List<FolderTableDto> paginatedFolders = new ArrayList<>(paginateFolders(entityManager, paginatedIds, orderBy));

        return new PageImpl<>(paginatedFolders, pageable, ids.size());
    }

    // We ignore "findsecbugs:SQL_INJECTION_JPA" sonar warning as we use setParameter with our NativeQuery to prevent SQL Injection
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    private static Collection<FolderTableDto> paginateFolders(EntityManager entityManager, Set<UUID> paginatedIds, String orderBy) {
        StringBuilder sql = from();
        sql.insert(0, "SELECT DISTINCT folders.id, folders.number, ims.name as issuingService, folders.status, folders.comment, folders.object, " +
            "folders.creation_date, folders.last_update, folders.review_date, sa.name as locality, a.name as area_name, a.international_name as " +
            "area_international_name, a.id as areaId, folders.issuing_service_id, me.id as entity_id, me.name as entity_name, me.international_name as " +
            "entity_international_name, se.id as service_id, se.name as service_name, s.id as study_id, s.number as study_number\n");
        sql.append("WHERE folders.id IN (:folderIds)\n");
        sql.append(orderBy);

        Query query = entityManager.createNativeQuery(sql.toString(), FOLDER_TABLE_VIEW_TUPLE_MAPPING);
        query.setParameter("folderIds", paginatedIds);

        LinkedHashMap<UUID, FolderTableDto> mapDto = new LinkedHashMap<>();
        query.getResultList().forEach(tuple -> map(mapDto, (FolderTableTuple) tuple));

        return mapDto.values();
    }

    private static void map(LinkedHashMap<UUID, FolderTableDto> mapDto, FolderTableTuple tuple) {
        if (mapDto.containsKey(tuple.getId())) {
            update(mapDto, tuple);
        } else {
            add(mapDto, tuple);
        }
    }

    private static void update(LinkedHashMap<UUID, FolderTableDto> mapDto, FolderTableTuple tuple) {
        FolderTableDto currentFolder = mapDto.get(tuple.getId());
        if (!isEmpty(tuple.getEntityId())) {
            currentFolder.getConcernedMaritimeEntities().add(
                MaritimeEntityConcernedDto.builder()
                    .id(tuple.getEntityId())
                    .name(tuple.getEntityName())
                    .internationalName(tuple.getEntityInternationalName())
                    .build());
        }
        if (!isEmpty(tuple.getServiceId())) {
            currentFolder.getSharedMaritimeServices().add(MaritimeServiceSharedDto.builder().id(tuple.getServiceId()).name(tuple.getServiceName()).build());
        }
        if (!isEmpty(tuple.getAreaId())) {
            currentFolder.getSharedAreas().add(
                AreaSharedDto.builder()
                    .id(tuple.getAreaId())
                    .name(tuple.getAreaName())
                    .internationalName(tuple.getAreaInternationalName())
                    .build());
        }

        if (!isEmpty(tuple.getStudyId())) {
            currentFolder.getStudies().add(
                FolderStudyDto.builder()
                    .id(tuple.getStudyId())
                    .number(tuple.getStudyNumber())
                    .build());
        }
    }

    private static void add(LinkedHashMap<UUID, FolderTableDto> mapDto, FolderTableTuple tuple) {
        FolderTableDto folderTableDto = FolderTableDto.builder()
            .id(tuple.getId())
            .number(tuple.getNumber())
            .issuingService(tuple.getIssuingService())
            .status(tuple.getStatus())
            .comment(tuple.getComment())
            .object(tuple.getObject())
            .locality(tuple.getLocality())
            .creationDate(tuple.getCreationDate())
            .lastUpdate(tuple.getLastUpdate())
            .reviewDate(tuple.getReviewDate())
            .build();

        mapDto.put(tuple.getId(), folderTableDto);
        update(mapDto, tuple);
    }

    private static Set<UUID> paginateIds(List<UUID> ids, Pageable pageable) {
        return ids.stream()
            .skip(pageable.getPageNumber() * pageable.getPageSize())
            .limit(pageable.getPageSize())
            .collect(toSet());
    }

    // We ignore "findsecbugs:SQL_INJECTION_JPA" sonar warning as we use setParameter with our NativeQuery to prevent SQL Injection
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    private static List<UUID> selectFolderIds(EntityManager entityManager, FolderSearchCriteria searchCriteria, String orderBy, ServiceUser proUser) {
        StringBuilder sql = from();

        sql.insert(0, "SELECT folders.id\n");
        addCriteriaSelectionToSql(sql, searchCriteria);
        sql.append("GROUP BY folders.id\n");
        sql.append(orderBy);

        Query query = entityManager.createNativeQuery(sql.toString());

        addSearchCriteriaParametersToQuery(query, searchCriteria, proUser);

        // Why we unwrap and addScalar : https://github.com/spring-projects/spring-data-jpa/issues/1796
        return query.unwrap(NativeQuery.class)
            .addScalar("id", StandardBasicTypes.UUID)
            .getResultList();
    }

    public static void addCriteriaSelectionToSql(StringBuilder queryStringBuilder, FolderSearchCriteria searchCriteria) {
        queryStringBuilder.append("WHERE pu.id = :userId\n");
        if (!isEmpty(searchCriteria.getStatus())) {
            queryStringBuilder.append("AND folders.status = :foldersStatus\n");
        }

        if (!isEmpty(searchCriteria.getAreaId())) {
            queryStringBuilder.append("AND folders.area_id = :foldersAreaId\n");
        }

        if (!isEmpty(searchCriteria.getFollowerServiceId())) {
            queryStringBuilder.append("AND ff.maritime_service_id = :foldersFollowerServiceId\n");
        } else {
            queryStringBuilder.append("AND folders.issuing_service_id IN (:maritimeServiceIdsOnWhichUserHasRoles)\n");
        }

        if (StringUtils.isNotBlank(searchCriteria.getSecondPartNumber())) {
            queryStringBuilder.append("AND RIGHT(CAST(EXTRACT(YEAR FROM folders.creation_date) AS VARCHAR), 2) LIKE :foldersSecondPartNumber\n");
        }

        if (StringUtils.isNotBlank(searchCriteria.getNumber())) {
            queryStringBuilder.append("AND CAST(folders.number AS VARCHAR) LIKE :foldersNumber\n");
        }

        if (hasText(searchCriteria.getLocalityOrObject())) {
            queryStringBuilder.append("AND (folders.object ILIKE :foldersLocalityOrObject OR sa.name ILIKE :foldersLocalityOrObject)\n");
        }

        if (isNotEmpty(searchCriteria.getAreaIds())) {
            queryStringBuilder.append("AND folders.area_id IN (:folderAreaIds)\n");
        }

        if (isNotEmpty(searchCriteria.getMaritimeEntityIds())) {
            queryStringBuilder.append("AND fme.maritime_entity_id IN (:folderMaritimeEntityIds)\n");
        }

        if (!isEmpty(searchCriteria.getLastUpdatePeriodDateStart()) || !isEmpty(searchCriteria.getLastUpdatePeriodDateEnd())) {
            // We don't need to check if date_start or date_end is null
            // "null + time '01:23:45'" always returns null, so there is no timestamp limit depending on which attribute is null
            queryStringBuilder.append("AND tsrange(folders.last_update + time '" + START_OF_DAY_TIME + "', " +
                "folders.last_update + time '" + END_OF_DAY_TIME + "') " +
                "&& :lastUpdateStartToEndRange\\:\\:tsrange\n");
        }

        if (StringUtils.isNotBlank(searchCriteria.getLastUpdateYear())) {
            queryStringBuilder.append("AND CAST(EXTRACT(YEAR FROM folders.last_update) AS VARCHAR) = :foldersLastUpdateYear\n");
        }

        if (!isEmpty(searchCriteria.getStudyId())) {
            queryStringBuilder.append("AND s.id = :studyId\n");
        }

        queryStringBuilder.append("AND (folders.issuing_service_id IN (:serviceIdsWithOperatorOrAdminRole) OR puse.service_network = 'GOVERNMENT_SERVICE')\n");
    }

    public static void addSearchCriteriaParametersToQuery(Query query, FolderSearchCriteria searchCriteria, ServiceUser proUser) {
        if (!isEmpty(searchCriteria.getStatus())) {
            query.setParameter("foldersStatus", searchCriteria.getStatus().toString());
        }

        if (!isEmpty(searchCriteria.getAreaId())) {
            query.setParameter("foldersAreaId", searchCriteria.getAreaId());
        }

        if (!isEmpty(searchCriteria.getFollowerServiceId())) {
            query.setParameter("foldersFollowerServiceId", searchCriteria.getFollowerServiceId());
        } else {
            Set<UUID> maritimeServiceIdsOnWhichUserHasRoles = proUser.getRoles().stream().map(ServiceUserRole::getServiceId).collect(toSet());
            query.setParameter("maritimeServiceIdsOnWhichUserHasRoles", maritimeServiceIdsOnWhichUserHasRoles);
        }

        if (StringUtils.isNotBlank(searchCriteria.getSecondPartNumber())) {
            query.setParameter("foldersSecondPartNumber", "%" + searchCriteria.getSecondPartNumber() + "%");
        }

        if (StringUtils.isNotBlank(searchCriteria.getNumber())) {
            query.setParameter("foldersNumber", "%" + searchCriteria.getNumber() + "%");
        }

        if (hasText(searchCriteria.getLocalityOrObject())) {
            query.setParameter("foldersLocalityOrObject", "%" + searchCriteria.getLocalityOrObject() + "%");
        }

        if (isNotEmpty(searchCriteria.getAreaIds())) {
            query.setParameter("folderAreaIds", searchCriteria.getAreaIds());
        }

        if (isNotEmpty(searchCriteria.getMaritimeEntityIds())) {
            query.setParameter("folderMaritimeEntityIds", searchCriteria.getMaritimeEntityIds());
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

        if (StringUtils.isNotBlank(searchCriteria.getLastUpdateYear())) {
            query.setParameter("foldersLastUpdateYear", searchCriteria.getLastUpdateYear());
        }

        if (!isEmpty(searchCriteria.getStudyId())) {
            query.setParameter("studyId", searchCriteria.getStudyId());
        }

        query.setParameter("userId", proUser.getId());
        query.setParameter("serviceIdsWithOperatorOrAdminRole", proUser.getServiceIdsWithOperatorOrAdminRole());
    }

    private static StringBuilder from() {
        StringBuilder queryStringBuilder = new StringBuilder();
        queryStringBuilder.append("FROM folder folders\n");
        queryStringBuilder.append(LEFT_JOIN + SUB_AREA_TABLE + " sa ON folders.locality_id = sa.id\n");

        queryStringBuilder.append(LEFT_JOIN + AREA_TABLE + " a ON folders.area_id = a.id\n");

        queryStringBuilder.append(LEFT_JOIN + FOLDER_FOLLOWING_TABLE + " ff ON folders.id = ff.folder_id\n");

        queryStringBuilder.append(LEFT_JOIN + MARITIME_SERVICE_TABLE + " ims ON folders.issuing_service_id = ims.id\n");

        queryStringBuilder.append(LEFT_JOIN + FOLDER_MARITIME_ENTITY_TABLE + " fme ON folders.id = fme.folder_id\n");
        queryStringBuilder.append(LEFT_JOIN + MARITIME_ENTITY_TABLE + " me ON fme.maritime_entity_id = me.id\n");

        queryStringBuilder.append(LEFT_JOIN + FOLDER_MARITIME_SERVICE_TABLE + " fse ON folders.id = fse.folder_id\n");
        queryStringBuilder.append(LEFT_JOIN + MARITIME_SERVICE_TABLE + " se ON fse.maritime_service_id = se.id\n");

        queryStringBuilder.append(LEFT_JOIN + STUDY_TABLE + " s ON folders.id = s.folder_id,\n");

        queryStringBuilder.append("pro_user pu\n");
        queryStringBuilder.append(LEFT_JOIN + MARITIME_SERVICE_TABLE + " puse ON pu.service_id = puse.id\n");
        return queryStringBuilder;
    }

    private static String orderBy(Pageable pageable) {
        String orderProperty = "folders.last_update";
        Sort.Direction direction = DESC;

        Sort sortParams = pageable.getSort();
        if (!sortParams.isEmpty() && sortParams.stream().count() == 1) {
            Sort.Order sortParam = sortParams.stream().findFirst().get();
            FolderTableSortProperty sortProperty = FolderTableSortProperty.fromProperty(sortParam.getProperty());
            direction = sortParam.getDirection();
            orderProperty = switch (sortProperty) {
                case NUMBER -> "folders.number";
                case REVIEW_DATE -> "folders.review_date";
                default -> "folders.last_update";
            };
        }
        return "ORDER BY " + orderProperty + " " + direction.name();
    }
}
