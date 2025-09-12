package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.SerieType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Warning Admin Detail Dto Model")
public class WarningAdminEditDto extends AbstractWarningAdminDto {
    @Serial
    private static final long serialVersionUID = -644260559427426824L;

    private UUID id;

    @NotNull
    private UUID serieId;

    private SerieType serieType;

    private Instant creationDate;

    private Instant lastUpdate;

    private AuthenticatedProUserDto lastUpdateUser;

    @Default
    private Map<String, Set<SubAreaDetailsDto>> selectedLocalities = new HashMap<>();

    @Default
    private Set<AttachmentAdminDetailsDto> attachmentFiles = new HashSet<>();

    private String serieName;
}
