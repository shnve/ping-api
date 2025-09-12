package fr.shom.ping.utils;

import fr.shom.ping.exception.ServiceException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import static fr.shom.ping.utils.SchemaUtil.AFFECTED_AREA_TABLE;
import static fr.shom.ping.utils.SchemaUtil.FEATURE_PART_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_FEATURE_PART_GEOMETRY;
import static java.util.stream.Collectors.toList;

public final class WarningKmlUtil extends KmlUtil {
    private static final String POINT_STYLE = "<Style><IconStyle><color>ff0000ff</color><IconStyleSimpleExtensionGroup radius=\"5\" points=\"Infinity\" " +
        "strokeColor=\"#ff0000\" strokeWidth=\"2\" lineDash=\"undefined\"/></IconStyle><PolyStyle " +
        "xmlns=\"\"><outline>0</outline></PolyStyle></Style>";

    private static final String POLYGON_STYLE = "<Style><IconStyle><color>ffc39620</color><IconStyleSimpleExtensionGroup/></IconStyle><LineStyle><color" +
        ">ff3643f4</color" +
        "><width>2</width></LineStyle><PolyStyle><color>4f0000ff</color></PolyStyle></Style>";

    private static final String LINESTRING_STYLE = "<Style><LineStyle><color>ff0000ff</color></LineStyle><PolyStyle><fill>ff0000</fill></PolyStyle></Style>";

    private static final String DEFAULT_STYLE = "<Style><LineStyle><color>ffc39620</color></LineStyle><PolyStyle><fill>51c3e377</fill></PolyStyle></Style>";

    public static byte[] generateKml(EntityManager entityManager, UUID warningId) throws ServiceException {
        List<String> geometriesAsKml = getGeometriesAsKmlByWarningId(entityManager, warningId, WARNING_FEATURE_PART_GEOMETRY);
        List<String> affectedAreasAsKml = getGeometriesAsKmlByWarningId(entityManager, warningId, AFFECTED_AREA_TABLE);
        List<String> geometries = Stream.of(geometriesAsKml, affectedAreasAsKml).flatMap(Collection::stream).collect(toList());

        return generateKml(warningId, geometries, POINT_STYLE, POLYGON_STYLE, LINESTRING_STYLE, DEFAULT_STYLE);
    }

    // We ignore "findsecbugs:SQL_INJECTION_JPA" sonar warning as we use setParameter with our NativeQuery to prevent SQL Injection
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    private static List<String> getGeometriesAsKmlByWarningId(EntityManager entityManager, UUID warningId, String tableName) {
        Query query = entityManager.createNativeQuery(
            "SELECT ST_AsKML(" + tableName + ".geom) " +
                "FROM " + tableName + " " +
                "INNER JOIN " + FEATURE_PART_TABLE + " ON " + FEATURE_PART_TABLE + ".id = " + tableName + ".feature_part_id " +
                "WHERE " + FEATURE_PART_TABLE + ".warning_id = :warningId ");

        query.setParameter("warningId", warningId);

        return query.getResultList();
    }
}
