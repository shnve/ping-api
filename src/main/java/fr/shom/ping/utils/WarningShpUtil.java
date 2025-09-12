package fr.shom.ping.utils;

import fr.shom.ping.exception.ServiceException;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.jts.JTS;


import static fr.shom.ping.utils.SchemaUtil.AFFECTED_AREA_TABLE;
import static fr.shom.ping.utils.SchemaUtil.WARNING_FEATURE_PART_GEOMETRY;
import static fr.shom.ping.utils.WarningUtil.getGeometriesAsTextByWarningId;
import static java.util.stream.Collectors.toList;

public final class WarningShpUtil extends ShpUtil {

    public static void generateShp(EntityManager entityManager, UUID warningId, String generatedShapefileFolder, HttpServletResponse response)
        throws ServiceException {
        List<Geometry> geometries = getGeometriesAsTextByWarningId(entityManager, warningId, WARNING_FEATURE_PART_GEOMETRY);
        List<Geometry> affectedAreas = getGeometriesAsTextByWarningId(entityManager, warningId, AFFECTED_AREA_TABLE);
        List<org.locationtech.jts.geom.Geometry> allGeometries = Stream.of(geometries, affectedAreas).flatMap(Collection::stream).map(JTS::to).collect(toList());
        generateShp(generatedShapefileFolder, warningId, allGeometries, response);
    }
}
