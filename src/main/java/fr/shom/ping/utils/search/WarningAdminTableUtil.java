package fr.shom.ping.utils.search;


import fr.shom.ping.model.dto.GeneralAreaDto;
import fr.shom.ping.model.dto.WarningAdminTableDto;
import fr.shom.ping.model.dto.WarningLocalityNamesDto;
import fr.shom.ping.model.dto.WarningReferenceDto;
import fr.shom.ping.model.enums.WarningAdminTableSortProperty;
import fr.shom.ping.model.enums.WarningHazardTypeDetails;
import fr.shom.ping.model.enums.WarningReferenceCategoryEnum;
import fr.shom.ping.model.predicate.WarningAdminSearchCriteria;
import fr.shom.ping.model.tuple.WarningAdminTableTuple;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import static fr.shom.ping.model.enums.WarningState.ACTIVE;
import static fr.shom.ping.model.enums.WarningState.DAY_OF_PUBLICATION;
import static fr.shom.ping.model.enums.WarningState.OUTDATED;
import static fr.shom.ping.model.enums.WarningState.TO_REVIEW;
import static fr.shom.ping.model.enums.WarningStatus.CANCELLED;
import static fr.shom.ping.utils.SchemaUtil.AFFECTED_AREA_TABLE;
import static fr.shom.ping.utils.SchemaUtil.FEATURE_PART_TABLE;
import static fr.shom.ping.utils.SchemaUtil.FIXED_DATE_RANGE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.FOLDER_TABLE;
import static fr.shom.ping.utils.SchemaUtil.GENERAL_AREA_TABLE;
import static fr.shom.ping.utils.SchemaUtil.LOCALIZED_TEXT_TABLE;
import static fr.shom.ping.utils.SchemaUtil.MARITIME_ENTITY_TABLE;
import static fr.shom.ping.utils.SchemaUtil.MARITIME_SERVICE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.NOTION_TABLE;
import static fr.shom.ping.utils.SchemaUtil.SERIE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.SUB_AREA_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_ADMIN_TABLE_VIEW_TUPLE_MAPPING;
import static fr.shom.ping.utils.SchemaUtil.WARNING_ENTITY_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_FEATURE_PART_GEOMETRY;
import static fr.shom.ping.utils.SchemaUtil.WARNING_HAZARD_TYPE_DETAILS;
import static fr.shom.ping.utils.SchemaUtil.WARNING_HAZARD_TYPE_DETAILS_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_HAZARD_TYPE_GENERAL;
import static fr.shom.ping.utils.SchemaUtil.WARNING_LOCALITY_NAMES_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_REFERENCE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_REFERENCE_WARNING_TABLE;
import static fr.shom.ping.utils.WarningUtil.convertGeneralAreaInformationToLocationNames;
import static fr.shom.ping.utils.search.WarningSearchUtil.END_OF_DAY_TIME;
import static fr.shom.ping.utils.search.WarningSearchUtil.INNER_JOIN;
import static fr.shom.ping.utils.search.WarningSearchUtil.LEFT_JOIN;
import static fr.shom.ping.utils.search.WarningSearchUtil.START_OF_DAY_TIME;
import static java.lang.Boolean.TRUE;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.util.ObjectUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

public final class WarningAdminTableUtil {

    private WarningAdminTableUtil() {
    }

    public static PageImpl<WarningAdminTableDto> find(EntityManager entityManager, WarningAdminSearchCriteria searchCriteria, Pageable pageable) {
        String orderBy = orderBy(searchCriteria, pageable);
        List<UUID> warningIds = selectWarningIds(entityManager, searchCriteria, orderBy);
        if (warningIds.isEmpty()) {
            return new PageImpl<>(emptyList(), pageable, 0);
        }

        Set<UUID> paginatedWarningIds = paginateWarningIds(warningIds, pageable);
        List<WarningAdminTableDto> paginatedWarnings = new ArrayList<>(paginateWarnings(entityManager, paginatedWarningIds, orderBy));

        return new PageImpl<>(paginatedWarnings, pageable, warningIds.size());
    }

    private static StringBuilder from() {
        StringBuilder queryStringBuilder = new StringBuilder();

        queryStringBuilder.append("FROM warning w\n");
        queryStringBuilder.append(LEFT_JOIN + GENERAL_AREA_TABLE + " g ON w.id = g.warning_id\n");
        queryStringBuilder.append(LEFT_JOIN + WARNING_LOCALITY_NAMES_TABLE + " l ON w.id = l.warning_id\n");
        queryStringBuilder.append(LEFT_JOIN + FEATURE_PART_TABLE + " fp ON w.id = fp.warning_id\n");
        queryStringBuilder.append(LEFT_JOIN + WARNING_HAZARD_TYPE_DETAILS_TABLE + " fd ON fd.feature_part_id = fp.id\n");
        queryStringBuilder.append(LEFT_JOIN + WARNING_REFERENCE_TABLE + " wr ON w.id = wr.warning_id" +
            " AND wr.category IN ('" + WarningReferenceCategoryEnum.CANCELLATION.name() + "', '" + WarningReferenceCategoryEnum.REPLACEMENT.name() + "')\n");
        queryStringBuilder.append(LEFT_JOIN + WARNING_REFERENCE_WARNING_TABLE + " wrw ON wr.id = wrw.warning_reference_id\n");
        queryStringBuilder.append(LEFT_JOIN + "warning r ON wrw.warning_id = r.id\n");
        queryStringBuilder.append(LEFT_JOIN + "(SELECT w.id, MAX(CASE " +
            "WHEN current_date >= w.estimated_publication_date AND w.status = 'DRAFT' THEN " + DAY_OF_PUBLICATION.ordinal() + " " +
            "WHEN current_date >= w.review_date OR w.force_to_review = TRUE THEN " + TO_REVIEW.ordinal() + " " +
            "WHEN current_date >= fdr.max_date_end THEN " + OUTDATED.ordinal() + " " +
            "ELSE " + ACTIVE.ordinal() + " " +
            "END) as warning_state_order " +
            "FROM warning w " +
            "LEFT JOIN warning_feature_part fp ON w.id = fp.warning_id " +
            "LEFT JOIN (SELECT MAX(date_end) as max_date_end, parent_id FROM fixed_date_range GROUP BY parent_id) fdr ON fp.id = fdr.parent_id GROUP BY w.id)" +
            " ws " +
            "ON w.id = ws.id\n");
        queryStringBuilder.append(LEFT_JOIN + WARNING_ENTITY_TABLE + " we ON w.id = we.warning_id\n");
        queryStringBuilder.append(LEFT_JOIN + MARITIME_ENTITY_TABLE + " me ON we.entity_id = me.id\n");
        queryStringBuilder.append(LEFT_JOIN + NOTION_TABLE + " no ON no.warning_id = w.id\n");
        queryStringBuilder.append(LEFT_JOIN + FOLDER_TABLE + " fo ON fo.id = no.folder_id\n");
        queryStringBuilder.append(LEFT_JOIN + MARITIME_SERVICE_TABLE + " msf ON fo.issuing_service_id = msf.id\n");
        queryStringBuilder.append(LEFT_JOIN + WARNING_FEATURE_PART_GEOMETRY + " wfpg on fp.id = wfpg.feature_part_id\n");
        queryStringBuilder.append(LEFT_JOIN + AFFECTED_AREA_TABLE + " wfpaa on fp.id = wfpaa.feature_part_id\n");
        queryStringBuilder.append(LEFT_JOIN + MARITIME_ENTITY_TABLE + " mec ON mec.type = 'SERVICE_AREA'\n");
        queryStringBuilder.append(LEFT_JOIN + WARNING_HAZARD_TYPE_GENERAL + " whtg ON whtg.enum_name = w.warning_hazard_type_general\n");
        queryStringBuilder.append(LEFT_JOIN + WARNING_HAZARD_TYPE_DETAILS + " whtd ON whtd.enum_name = fd.warning_hazard_type_detail\n");
        return queryStringBuilder;
    }

    private static String orderBy(WarningAdminSearchCriteria searchCriteria, Pageable pageable) {
        String orderProperty = "ws.warning_state_order";
        Direction direction = ASC;
        String orderByDate = ", ";
        switch (searchCriteria.getStatus()) {
            case PUBLISHED:
                orderByDate += "w.publication_time DESC";
                break;
            case CANCELLED:
                orderProperty = "w.cancellation_date";
                direction = DESC;
                orderByDate = "";
                break;
            case DRAFT:
            case MODEL:
            default:
                orderByDate += "w.creation_date DESC";
        }

        Sort sortParams = pageable.getSort();
        // We only support one sort property
        if (!sortParams.isEmpty() && sortParams.stream().count() == 1) {
            Sort.Order sortParam = sortParams.stream().findFirst().get();
            WarningAdminTableSortProperty sortProperty = WarningAdminTableSortProperty.fromProperty(sortParam.getProperty());
            direction = sortParam.getDirection();
            switch (sortProperty) {
                case NUMBER:
                    orderProperty = "w.year " + direction.name() + ", w.warning_number";
                    break;
                case PUBLICATION_DATE:
                    orderProperty = "w.publication_time";
                    orderByDate = "";
                    break;
                case CANCELLATION_DATE:
                    orderProperty = "w.cancellation_date";
                    orderByDate = "";
                    break;
                case REVIEW_DATE:
                    orderProperty = "w.review_date";
                    break;
                case ESTIMATED_PUBLICATION_DATE:
                    orderProperty = "w.estimated_publication_date";
                    break;
                case END_OF_BROADCAST_DATE:
                    orderProperty = "w.end_of_broadcast_date";
                    break;
                case ENTITIES:
                    orderProperty = "entity";
                    break;
                case FOLDERS:
                    orderProperty = "folder";
                    break;
                case STATE:
                default:
                    orderProperty = "ws.warning_state_order";
            }
        }

        return "ORDER BY " + orderProperty + " " + direction.name() + orderByDate;
    }

    // We ignore "findsecbugs:SQL_INJECTION_JPA" sonar warning as we use setParameter with our NativeQuery to prevent SQL Injection
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    private static List<UUID> selectWarningIds(EntityManager entityManager, WarningAdminSearchCriteria searchCriteria, String orderBy) {
        StringBuilder sql = from();

        final boolean orderByEntity = orderBy.contains("ORDER BY entity");
        final boolean orderByFolder = orderBy.contains("ORDER BY folder");
        final boolean addDistinct = orderByEntity || orderByFolder;

        String querySelect = "SELECT w.id as id";
        if (addDistinct) {
            querySelect = "SELECT DISTINCT id FROM (" + querySelect;
        }

        if (orderByEntity) {
            querySelect += ", me.name as entity";
        } else if (orderByFolder) {
            querySelect += ", fo.number as folder";
        }
        sql.insert(0, querySelect + "\n");

        addCriteriaSelectionToSql(sql, searchCriteria);
        sql.append("GROUP BY w.id, ws.warning_state_order");
        if (orderByEntity) {
            sql.append(", entity");
        } else if (orderByFolder) {
            sql.append(", folder");
        }
        sql.append("\n");
        sql.append(orderBy);

        if (addDistinct) {
            sql.append(") global_warning\n");
        }

        Query query = entityManager.createNativeQuery(sql.toString());

        addSearchCriteriaParametersToQuery(query, searchCriteria);

        // Why we unwrap and addScalar : https://github.com/spring-projects/spring-data-jpa/issues/1796
        return query.unwrap(NativeQuery.class)
            .addScalar("id", StandardBasicTypes.UUID)
            .getResultList();
    }

    private static Set<UUID> paginateWarningIds(List<UUID> warningIds, Pageable pageable) {
        return warningIds.stream()
            .skip(pageable.getPageNumber() * pageable.getPageSize())
            .limit(pageable.getPageSize())
            .collect(toSet());
    }

    // We ignore "findsecbugs:SQL_INJECTION_JPA" sonar warning as we use setParameter with our NativeQuery to prevent SQL Injection
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    private static Collection<WarningAdminTableDto> paginateWarnings(EntityManager entityManager, Set<UUID> paginatedWarningIds, String orderBy) {
        StringBuilder sql = from();
        sql.insert(0, "SELECT w.id, w.name_of_series, w.warning_number, w.year, w.model_name, g.id as general_area_id, g.sub_area_id as sub_area_id," +
            "g.position as general_area_position, g.name as general_area_name, g.international_name as general_area_international_name, " +
            "l.id as locality_id, l.parent_id as locality_parent_id, l.position as locality_position , l.name as locality_name, l.international_name as locality_international_name, " +
            "w.warning_hazard_type_general, fd.warning_hazard_type_detail, w.creation_date, w.last_update, w.estimated_publication_date, " +
            "w.publication_time, w.cancellation_date, w.end_of_broadcast_date, w.review_date, w.internal_comment, " +
            "ws.warning_state_order, r.name_of_series as reference_name_of_series, r.warning_number as reference_warning_number, r.year as " +
            "reference_year, " +
            "w.status, me.name as entity, " +
            "(CASE WHEN fo.number IS NOT NULL THEN " +
            "CONCAT(fo.number,'/',RIGHT(CAST(EXTRACT(YEAR FROM fo.creation_date) AS VARCHAR), 2),' - ',msf.name) ELSE CONCAT(fo.number,'') END) as folder\n");
        sql.append("WHERE w.id IN (:warningIds)\n");
        sql.append(orderBy);

        Query query = entityManager.createNativeQuery(sql.toString(), WARNING_ADMIN_TABLE_VIEW_TUPLE_MAPPING);
        query.setParameter("warningIds", paginatedWarningIds);

        LinkedHashMap<UUID, WarningAdminTableDto> warnings = new LinkedHashMap<>();
        query.getResultList().forEach(tuple -> map(warnings, (WarningAdminTableTuple) tuple));

        return warnings.values();
    }

    private static void map(LinkedHashMap<UUID, WarningAdminTableDto> warnings, WarningAdminTableTuple tuple) {
        if (warnings.containsKey(tuple.getId())) {
            update(warnings, tuple);
        } else {
            add(warnings, tuple);
        }
    }

    private static void update(LinkedHashMap<UUID, WarningAdminTableDto> warnings, WarningAdminTableTuple tuple) {
        WarningAdminTableDto currentWarning = warnings.get(tuple.getId());

        if (!isEmpty(tuple.getGeneralAreaId())) {
            GeneralAreaDto generalArea = currentWarning.getGeneralAreaSortedSet().stream()
                .filter(generalAreaDto -> generalAreaDto.getId().equals(tuple.getGeneralAreaId()))
                .findFirst()
                .orElse(GeneralAreaDto.builder()
                    .id(tuple.getGeneralAreaId())
                    .subAreaId(tuple.getSubAreaId())
                    .position(tuple.getGeneralAreaPosition())
                    .build());

            updateGeneralAreas(currentWarning, generalArea, tuple);
        }

        if (!isEmpty(tuple.getLocalityId())) {
            WarningLocalityNamesDto locality = currentWarning.getLocalitySortedList().stream()
                .filter(localityDto -> localityDto.getId().equals(tuple.getLocalityId()))
                .findFirst()
                .orElse(WarningLocalityNamesDto.builder()
                    .id(tuple.getLocalityId())
                    .parentId(tuple.getLocalityParentId())
                    .position(tuple.getLocalityPosition())
                    .name(tuple.getLocalityName())
                    .internationalName(tuple.getLocalityInternationalName())
                    .build());

            updateLocalities(currentWarning, locality, tuple);
        }

        if (hasText(tuple.getReferenceNameOfSeries())) {
            currentWarning.getReferences().add(WarningReferenceDto.builder()
                .nameOfSeries(tuple.getReferenceNameOfSeries())
                .warningNumber(tuple.getReferenceWarningNumber())
                .year(tuple.getReferenceYear())
                .build());
        }

        if (StringUtils.isNotBlank(tuple.getEntity())) {
            currentWarning.getEntities().add(tuple.getEntity());
        }

        if (StringUtils.isNotBlank(tuple.getFolder())) {
            currentWarning.getFolders().add(tuple.getFolder());
        }
    }

    private static void add(LinkedHashMap<UUID, WarningAdminTableDto> warnings, WarningAdminTableTuple tuple) {
        WarningAdminTableDto warningAdminTableDto = WarningAdminTableDto.builder()
            .id(tuple.getId())
            .nameOfSeries(tuple.getNameOfSeries())
            .warningNumber(tuple.getWarningNumber())
            .year(tuple.getYear())
            .modelName(tuple.getModelName())
            .warningHazardTypeGeneral(tuple.getWarningHazardTypeGeneral())
            .creationDate(Optional.ofNullable(tuple.getCreationDate()).map(date -> LocalDate.ofInstant(date, UTC)).orElse(null))
            .lastUpdate(Optional.ofNullable(tuple.getLastUpdate()).map(date -> LocalDate.ofInstant(date, UTC)).orElse(null))
            .estimatedPublicationDate(tuple.getEstimatedPublicationDate())
            .publicationDate(Optional.ofNullable(tuple.getPublicationDate()).map(date -> LocalDate.ofInstant(date, UTC)).orElse(null))
            .cancellationDate(Optional.ofNullable(tuple.getCancellationDate()).map(date -> LocalDate.ofInstant(date, UTC)).orElse(null))
            .endOfBroadcastDate(tuple.getEndOfBroadcastDate())
            .reviewDate(tuple.getReviewDate())
            .internalComment(tuple.getInternalComment())
            .status(tuple.getStatus())
            .build();

        if (tuple.getStatus() != CANCELLED) {
            warningAdminTableDto.setState(tuple.getState());
        }

        warnings.put(tuple.getId(), warningAdminTableDto);
        update(warnings, tuple);
    }

    private static void updateGeneralAreas(WarningAdminTableDto warning, GeneralAreaDto generalArea, WarningAdminTableTuple tuple) {
        generalArea.setLocationNames(convertGeneralAreaInformationToLocationNames(tuple.getGeneralAreaName(), tuple.getGeneralAreaInternationalName()));
        warning.getGeneralAreaSortedSet().add(generalArea);
    }

    private static void updateLocalities(WarningAdminTableDto warning, WarningLocalityNamesDto locality, WarningAdminTableTuple tuple) {
        warning.getLocalitySortedList().add(locality);
    }

    public static void addCriteriaSelectionToSql(StringBuilder queryStringBuilder, WarningAdminSearchCriteria searchCriteria) {
        if (isNotEmpty(searchCriteria.getSubAreaIds())) {
            queryStringBuilder.append(INNER_JOIN + SUB_AREA_TABLE + " sa ON g.sub_area_id = sa.id\n");
        }

        if (searchCriteria.getSerieId() != null) {
            queryStringBuilder.append(LEFT_JOIN + SERIE_TABLE + " s ON w.serie_id = s.id\n");
        }

        if (!isEmpty(searchCriteria.getDateStart()) || !isEmpty(searchCriteria.getDateEnd())) {
            queryStringBuilder.append(LEFT_JOIN + FIXED_DATE_RANGE_TABLE + " fdr ON fp.id = fdr.parent_id\n");
        }

        if (hasText(searchCriteria.getLocalityOrInformation())) {
            queryStringBuilder.append(LEFT_JOIN + LOCALIZED_TEXT_TABLE + " fl ON fp.id = fl.parent_id\n");
        }

        queryStringBuilder.append("WHERE 1=1\n");

        if (!isEmpty(searchCriteria.getSerieId())) {
            queryStringBuilder.append("AND s.id = :serieId\n");
        }

        if (!isEmpty(searchCriteria.getStatus())) {
            queryStringBuilder.append("AND w.status = :status\n");
        }

        if (searchCriteria.getToBeDone() != null) {
            String condition = "=";
            if (searchCriteria.getToBeDone() == TRUE) {
                condition = "!=";
            }
            queryStringBuilder.append("AND ws.warning_state_order ");
            queryStringBuilder.append(condition);
            queryStringBuilder.append(" :warningStateOrder\n");
        }

        if (hasText(searchCriteria.getWarningNumber())) {
            queryStringBuilder.append("AND CONCAT(w.name_of_series, ' ', w.warning_number, '/', (w.year - 2000)) ILIKE :warningNumber\n");
        }

        if (hasText(searchCriteria.getModelName())) {
            queryStringBuilder.append("AND w.model_name ILIKE :modelName\n");
        }

        if (hasText(searchCriteria.getLocalityOrInformation())) {
            queryStringBuilder.append("AND (fl.text ILIKE :localityOrInformation " +
                "OR l.name ILIKE :localityOrInformation OR l.international_name ILIKE :localityOrInformation " +
                "OR g.name ILIKE :localityOrInformation OR g.international_name ILIKE :localityOrInformation " +
                "OR w.internal_comment ILIKE :localityOrInformation " +
                "OR whtg.translation ILIKE :localityOrInformation OR whtg.international_translation ILIKE :localityOrInformation " +
                "OR whtd.translation ILIKE :localityOrInformation OR whtd.international_translation ILIKE :localityOrInformation)\n");
        }

        if (isNotEmpty(searchCriteria.getSubAreaIds())) {
            queryStringBuilder.append("AND sa.id IN (:subAreaIds)\n");
        }

        if (searchCriteria.getWarningHazardTypeGeneral() != null) {
            queryStringBuilder.append("AND w.warning_hazard_type_general = :warningHazardTypeGeneral\n");
        }

        if (isNotEmpty(searchCriteria.getWarningHazardTypeDetails())) {
            queryStringBuilder.append("AND fd.warning_hazard_type_detail IN (:warningHazardTypeDetails)\n");
        }

        if (hasText(searchCriteria.getReferenceNumber())) {
            queryStringBuilder.append("AND CONCAT(r.warning_number, '/', (r.year - 2000)) ILIKE :referenceNumber\n");
        }

        if (!isEmpty(searchCriteria.getDateStart()) || !isEmpty(searchCriteria.getDateEnd())) {
            // We don't need to check if date_start or date_end is null
            // "null + time '01:23:45'" always returns null, so there is no timestamp limit depending on which attribute is null
            queryStringBuilder.append("AND tsrange(fdr.date_start + time '" + START_OF_DAY_TIME + "', fdr.date_end + time '" + END_OF_DAY_TIME + "') && " +
                ":startToEndRange\\:\\:tsrange\n");
        }

        if (isNotEmpty(searchCriteria.getConcernedMaritimeEntityIds())) {
            queryStringBuilder.append("AND we.entity_id IN :concernedMaritimeEntityIds\n");
        }

        if (isNotEmpty(searchCriteria.getEntityCoverIds())) {
            queryStringBuilder.append("AND mec.id IN (:entityCoverIds)\n");
            queryStringBuilder.append("AND (st_intersects(mec.geom, wfpg.geom) OR st_intersects(mec.geom, wfpaa.geom))\n");
        }

        if (isNotEmpty(searchCriteria.getFolderIds())) {
            queryStringBuilder.append("AND fo.id IN :folderIds\n");
        }
    }

    public static void addSearchCriteriaParametersToQuery(Query query, WarningAdminSearchCriteria searchCriteria) {
        if (!isEmpty(searchCriteria.getSerieId())) {
            query.setParameter("serieId", searchCriteria.getSerieId());
        }

        if (!isEmpty(searchCriteria.getStatus())) {
            query.setParameter("status", searchCriteria.getStatus().name());
        }

        if (searchCriteria.getToBeDone() != null) {
            query.setParameter("warningStateOrder", ACTIVE.ordinal());
        }

        if (hasText(searchCriteria.getWarningNumber())) {
            query.setParameter("warningNumber", "%" + searchCriteria.getWarningNumber() + "%");
        }

        if (hasText(searchCriteria.getModelName())) {
            query.setParameter("modelName", "%" + searchCriteria.getModelName() + "%");
        }

        if (hasText(searchCriteria.getLocalityOrInformation())) {
            query.setParameter("localityOrInformation", "%" + searchCriteria.getLocalityOrInformation() + "%");
        }

        if (isNotEmpty(searchCriteria.getSubAreaIds())) {
            query.setParameter("subAreaIds", searchCriteria.getSubAreaIds());
        }

        if (searchCriteria.getWarningHazardTypeGeneral() != null) {
            query.setParameter("warningHazardTypeGeneral", searchCriteria.getWarningHazardTypeGeneral().name());
        }

        if (isNotEmpty(searchCriteria.getWarningHazardTypeDetails())) {
            List<String> details = searchCriteria.getWarningHazardTypeDetails().stream().map(WarningHazardTypeDetails::name).collect(toList());
            query.setParameter("warningHazardTypeDetails", details);
        }

        if (hasText(searchCriteria.getReferenceNumber())) {
            query.setParameter("referenceNumber", "%" + searchCriteria.getReferenceNumber() + "%");
        }

        if (!isEmpty(searchCriteria.getDateStart()) && isEmpty(searchCriteria.getDateEnd())) {
            query.setParameter("startToEndRange", "[" + searchCriteria.getDateStart() + " " + START_OF_DAY_TIME + ",]");
        } else if (isEmpty(searchCriteria.getDateStart()) && !isEmpty(searchCriteria.getDateEnd())) {
            query.setParameter("startToEndRange", "[," + searchCriteria.getDateEnd() + " " + END_OF_DAY_TIME + "]");
        } else if (!isEmpty(searchCriteria.getDateStart()) && !isEmpty(searchCriteria.getDateEnd())) {
            query.setParameter("startToEndRange",
                "[" + searchCriteria.getDateStart() + " " + START_OF_DAY_TIME + "," + searchCriteria.getDateEnd() + " " + END_OF_DAY_TIME + "]");
        }

        if (isNotEmpty(searchCriteria.getConcernedMaritimeEntityIds())) {
            query.setParameter("concernedMaritimeEntityIds", searchCriteria.getConcernedMaritimeEntityIds());
        }

        if (isNotEmpty(searchCriteria.getEntityCoverIds())) {
            query.setParameter("entityCoverIds", searchCriteria.getEntityCoverIds());
        }

        if (isNotEmpty(searchCriteria.getFolderIds())) {
            query.setParameter("folderIds", searchCriteria.getFolderIds());
        }
    }
}
