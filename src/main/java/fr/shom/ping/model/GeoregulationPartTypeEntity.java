package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.utils.SchemaUtil.GEOREGULATION_PART_TYPE;

@Entity
@SuperBuilder
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "GeoregulationPartTypes Model")
@Table(name = GEOREGULATION_PART_TYPE)
public class GeoregulationPartTypeEntity extends AbstractEnumTypes {
    @Serial
    private static final long serialVersionUID = -7075087550247751255L;
}
