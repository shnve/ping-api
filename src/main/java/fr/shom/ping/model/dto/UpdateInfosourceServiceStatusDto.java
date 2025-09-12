package fr.shom.ping.model.dto;

import fr.shom.ping.model.enums.ActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "Update Infosource Service Status Dto Model")
public class UpdateInfosourceServiceStatusDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -3862913366497379712L;
    @NotNull(message = "validation.update-infosource-service-status.service-id.not-null")
    private UUID serviceId;
    @NotNull(message = "validation.update-infosource-service-status.action-type.not-null")
    private ActionType actionType;
}
