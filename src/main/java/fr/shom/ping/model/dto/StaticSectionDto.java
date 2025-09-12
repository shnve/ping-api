package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.StaticSection;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.utils.PingStringUtil.blobAsString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
public class StaticSectionDto extends AbstractStaticSectionDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1184597489978913409L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @NotEmpty(message = "validation.static-section.key.not-empty")
    @NoHtml
    private String key;

    @NotEmpty(message = "validation.static-section.location.not-empty")
    @Column(nullable = false)
    private String location;

    @NotEmpty(message = "validation.static-section.path.not-empty")
    @Pattern(regexp = "^[a-z\\d-]+$", message = "validation.static-section.path.invalid-pattern")
    @NoHtml
    private String path;

    @NotEmpty(message = "validation.static-section.name.not-empty")
    @NoHtml
    private String name;

    public StaticSectionDto(StaticSection staticSection) {
        id = staticSection.getId();
        key = staticSection.getKey();
        location = staticSection.getLocation();
        path = staticSection.getPath();
        name = staticSection.getName();
        content = blobAsString(staticSection.getContent());
        internationalContent = blobAsString(staticSection.getInternationalContent());
    }
}
