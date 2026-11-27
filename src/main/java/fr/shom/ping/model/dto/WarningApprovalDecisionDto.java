package fr.shom.ping.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "Warning Approval Decision DTO")
public class WarningApprovalDecisionDto {

    @Schema(description = "Observation or reason for returning/rejecting or requesting cancellation")
    private String comment;
}
