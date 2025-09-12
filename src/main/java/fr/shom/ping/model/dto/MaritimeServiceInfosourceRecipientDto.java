package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.MaritimeServiceType;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.NAME_DESCRIPTION;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
@Builder
@Schema(name = "Maritime Service Infosource Recipient DTO")
public class MaritimeServiceInfosourceRecipientDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 5588852026495558795L;

    @Schema(description = ID_DESCRIPTION)
    private UUID id;

    @Schema(description = NAME_DESCRIPTION)
    @NoHtml
    private String name;

    @JsonIgnore
    private MaritimeServiceType type;

    @Schema(description = "Default recipient for sending contribution")
    @Builder.Default
    private boolean isDefaultService = true;

    public MaritimeServiceInfosourceRecipientDto(UUID id, String name, MaritimeServiceType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }
}
