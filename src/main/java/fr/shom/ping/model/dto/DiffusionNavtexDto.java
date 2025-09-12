package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.WarningNavtexConfig;
import fr.shom.ping.model.enums.DiffusionNavtexEmergencyLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
@Schema(name = "Diffusion NAVTEX Dto Model")
public class DiffusionNavtexDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -2602448928002914382L;

    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @Schema(description = "emergency level", required = true)
    private DiffusionNavtexEmergencyLevel emergencyLevel;

    @Schema(description = "diffusion end day")
    private Short endDay;

    @Schema(description = "diffusion end hour")
    private Short endHour;

    @Schema(description = "French Niton message number")
    private Short taNumber;

    @Schema(description = "English Niton message number")
    private Short kaNumber;

    public DiffusionNavtexDto(WarningNavtexConfig warningNavtexConfig) throws InvocationTargetException, IllegalAccessException {
        BeanUtils.copyProperties(this, warningNavtexConfig);
    }
}
