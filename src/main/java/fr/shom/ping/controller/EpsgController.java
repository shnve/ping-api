package fr.shom.ping.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@RestController
@RequestMapping("/api/epsg")
@RequiredArgsConstructor
@Tag(name = "Epsg", description = "Epsg API")
@Slf4j
public class EpsgController {
    private final EntityManager entityManager;

    @GetMapping(value = "/geometric", produces = APPLICATION_JSON)
    @Operation(summary = "Get the filtered list of geometric Epsg codes (geodesic)", tags = "Epsg")
    public List<String> getGeometricEpsgCodes() {
        final String sql = """
            select concat(srs.auth_name, ':', srs.auth_srid, ' ', \
            replace(SUBSTRING(srs.srtext, 8, POSITION(',GEOGCS' IN srs.srtext)), ',GEOGCS[', ''))
            from spatial_ref_sys srs
            where srs.auth_name = 'EPSG' and srs.auth_srid = '4326'
            order by srs.auth_srid ASC""";
        final Query query = entityManager.createNativeQuery(sql);
        return query.getResultList();
    }

    @GetMapping(value = "/projection", produces = APPLICATION_JSON)
    @Operation(summary = "Get the filtered list of projection Epsg codes", tags = "Epsg")
    public List<String> getProjectionEpsgCodes() {
        final String sql = """
            select concat(srs.auth_name, ':', srs.auth_srid, ' ', \
            replace(SUBSTRING(srs.srtext, 8, POSITION(',GEOGCS' IN srs.srtext)), ',GEOGCS[', ''))
            from spatial_ref_sys srs
            where srs.auth_name = 'EPSG' and (
            srs.auth_srid IN ('2154', '3163', '3395')
            or CAST(coalesce(srs.auth_srid, '0') AS integer) >= 3942 and CAST(coalesce(srs.auth_srid, '0') AS integer) <= 3950
            or CAST(coalesce(srs.auth_srid, '0') AS integer) >= 32601 and CAST(coalesce(srs.auth_srid, '0') AS integer) <= 32660
            or CAST(coalesce(srs.auth_srid, '0') AS integer) >= 32701 and CAST(coalesce(srs.auth_srid, '0') AS integer) <= 32760
            )
            order by srs.auth_srid ASC""";
        final Query query = entityManager.createNativeQuery(sql);
        return query.getResultList();
    }
}
