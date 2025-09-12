package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.dto.DiffusionNavtexDto;
import fr.shom.ping.model.enums.DiffusionNavtexEmergencyLevel;
import fr.shom.ping.model.enums.DiffusionNavtexType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.utils.SchemaUtil.WARNING_NAVTEX_CONFIG;
import static java.util.UUID.randomUUID;
import static jakarta.persistence.EnumType.STRING;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "Diffusion NAVTEX Model")
@Table(name = WARNING_NAVTEX_CONFIG)
public class WarningNavtexConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = 4347921649480207138L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "warning_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Schema(description = "the warning at which the navtex config belongs to", required = true)
    private Warning warning;

    @Enumerated(STRING)
    @Schema(description = "emergency level")
    private DiffusionNavtexEmergencyLevel emergencyLevel;

    @Schema(description = "communication type")
    private DiffusionNavtexType communicationType;

    @Schema(description = "diffusion end day")
    private Short endDay;

    @Schema(description = "diffusion end hour")
    private Short endHour;
    
    @Schema(description = "Niton message number")
    private Short nitonNumber;

    @Schema(description = "Niton cancel message number")
    private Short cancelNitonNumber;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }

    public DiffusionNavtexDto asDto() throws InvocationTargetException, IllegalAccessException {
        return new DiffusionNavtexDto(this);
    }
}
