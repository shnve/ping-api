package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.MaritimeEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Geometry;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.GEOMETRY_EXAMPLE;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.INTERNATIONAL_NAME_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.NAME_DESCRIPTION;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
public class MaritimeEntityToDisplayOnMapDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -4015170376581709133L;
    
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @Schema(description = GEOMETRY_DESCRIPTION, required = true, example = GEOMETRY_EXAMPLE)
    private Geometry geom;

    @Schema(description = NAME_DESCRIPTION, required = true)
    private String name;

    @Schema(description = INTERNATIONAL_NAME_DESCRIPTION)
    private String internationalName;

    public MaritimeEntityToDisplayOnMapDto(MaritimeEntity maritimeEntity) {
        id = maritimeEntity.getId();
        geom = maritimeEntity.getGeom();
        name = maritimeEntity.getName();
        internationalName = maritimeEntity.getInternationalName();
    }
}
