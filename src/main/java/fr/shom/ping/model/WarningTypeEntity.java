package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serial;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.utils.SchemaUtil.WARNING_TYPE;

@Entity
@SuperBuilder
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "WarningType Model")
@Table(name = WARNING_TYPE)
public class WarningTypeEntity extends AbstractWarningHazardType {
    @Serial
    private static final long serialVersionUID = 1494174045373021601L;
}
