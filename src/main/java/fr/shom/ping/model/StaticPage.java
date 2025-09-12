package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.dto.StaticPageDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.INTERNATIONAL_TITLE_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.PATH_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.TITLE_DESCRIPTION;
import static java.util.UUID.randomUUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
public class StaticPage implements Serializable {
    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @NotEmpty(message = "validation.static-page.title.not-empty")
    @Column(nullable = false)
    @Schema(description = TITLE_DESCRIPTION, required = true)
    private String title;

    @NotEmpty(message = "validation.static-page.international-title.not-empty")
    @Column(nullable = false)
    @Schema(description = INTERNATIONAL_TITLE_DESCRIPTION, required = true)
    private String internationalTitle;

    @NotEmpty(message = "validation.static-page.path.not-empty")
    @Pattern(regexp = "^[a-z\\d-]+$", message = "validation.static-page.path.invalid-pattern")
    @Column(nullable = false)
    @Schema(description = PATH_DESCRIPTION, required = true)
    private String path;

    @Default
    private Boolean locked = false;

    @Column(columnDefinition = "BYTEA")
    private byte[] content;

    @Column(columnDefinition = "BYTEA")
    private byte[] internationalContent;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }

    public StaticPageDto asDto() {
        return new StaticPageDto(this);
    }
}
