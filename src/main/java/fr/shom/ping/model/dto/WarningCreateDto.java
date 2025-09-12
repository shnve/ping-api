package fr.shom.ping.model.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Warning Create Dto Model")
public class WarningCreateDto extends AbstractWarningAdminDto {
    @Serial
    private static final long serialVersionUID = -875816049137856357L;

    @NotNull
    private UUID serieId;
}
