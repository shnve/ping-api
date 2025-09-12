package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static java.time.ZoneOffset.UTC;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
@Builder
@Schema(name = "Folder available for notion DTO")
public class FolderAvailableForNotionDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 5389523751261633161L;

    @Schema(description = ID_DESCRIPTION)
    private UUID id;

    private Integer number;

    @JsonIgnore
    private Instant creationDate;

    @NoHtml
    private String area;

    public FolderAvailableForNotionDto(UUID id, Integer number, Instant creationDate) {
        this.id = id;
        this.number = number;
        this.creationDate = creationDate;
    }

    public Integer getYear() {
        return creationDate.atZone(UTC).getYear();
    }

}
