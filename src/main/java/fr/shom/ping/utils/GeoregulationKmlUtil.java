package fr.shom.ping.utils;

import fr.shom.ping.exception.ServiceException;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import static fr.shom.ping.utils.SchemaUtil.GEOREGULATION_PART_FEATURE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.GEOREGULATION_PART_TABLE;

public final class GeoregulationKmlUtil extends KmlUtil {
    private static final String POINT_STYLE = "<Style><IconStyle><color>7f000000</color><IconStyleSimpleExtensionGroup radius=\"5\" points=\"Infinity\" " +
        "strokeColor=\"#000000\" strokeWidth=\"2\" lineDash=\"undefined\"/></IconStyle><PolyStyle " +
        "xmlns=\"\"><outline>0</outline></PolyStyle></Style>";

    private static final String POLYGON_STYLE = "<Style><IconStyle><color>ff000000</color><IconStyleSimpleExtensionGroup/></IconStyle><LineStyle><color" +
        ">ff000000</color" +
        "><width>2</width></LineStyle><PolyStyle><color>7f000000</color></PolyStyle></Style>";

    private static final String LINESTRING_STYLE = "<Style><LineStyle><color>ff000000</color></LineStyle><PolyStyle><fill>7f000000</fill></PolyStyle></Style>";

    private static final String DEFAULT_STYLE = "<Style><LineStyle><color>ff000000</color></LineStyle><PolyStyle><fill>7f000000</fill></PolyStyle></Style>";

    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    public static byte[] generateKml(EntityManager entityManager, UUID georegulationId) throws ServiceException {
        Query query = entityManager.createNativeQuery(
            "SELECT ST_AsKML(" + GEOREGULATION_PART_FEATURE_TABLE + ".geom) " +
                "FROM " + GEOREGULATION_PART_FEATURE_TABLE + " " +
                "INNER JOIN " + GEOREGULATION_PART_TABLE + " ON " + GEOREGULATION_PART_TABLE + ".id = " + GEOREGULATION_PART_FEATURE_TABLE + ".part_id " +
                "WHERE " + GEOREGULATION_PART_TABLE + ".georegulation_id = :georegulationId ");

        query.setParameter("georegulationId", georegulationId);
        List<String> geometries = query.getResultList();

        return generateKml(georegulationId, geometries, POINT_STYLE, POLYGON_STYLE, LINESTRING_STYLE, DEFAULT_STYLE);
    }
}
