package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Entity
@SuperBuilder
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "GeoregulationTypes Model")
public class GeoregulationTypes extends AbstractEnumTypes {
}
