package fr.shom.ping.utils;

import fr.shom.ping.exception.ServiceException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.jts.JTS;
import org.hibernate.query.NativeQuery;

import static fr.shom.ping.utils.SchemaUtil.GEOREGULATION_PART_FEATURE_TABLE;
import static fr.shom.ping.utils.SchemaUtil.GEOREGULATION_PART_TABLE;

public final class GeoregulationShpUtil extends ShpUtil {

    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    public static void generateShp(EntityManager entityManager, UUID georegulationId, String generatedShapefileFolder, HttpServletResponse response)
        throws ServiceException {
        Query query = entityManager.createNativeQuery(
            "SELECT " + GEOREGULATION_PART_FEATURE_TABLE + ".geom as the_geom " +
                "FROM " + GEOREGULATION_PART_FEATURE_TABLE + " " +
                "INNER JOIN " + GEOREGULATION_PART_TABLE + " ON " + GEOREGULATION_PART_TABLE + ".id = " + GEOREGULATION_PART_FEATURE_TABLE + ".part_id " +
                "WHERE " + GEOREGULATION_PART_TABLE + ".georegulation_id = :georegulationId ");

        query.setParameter("georegulationId", georegulationId);
        List<Geometry> geometries = query.unwrap(NativeQuery.class).getResultList();

        generateShp(generatedShapefileFolder, georegulationId, geometries.stream().map(JTS::to).toList(), response);
    }
}
