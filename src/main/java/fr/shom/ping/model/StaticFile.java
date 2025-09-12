package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.dto.StaticFileDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.io.Serializable;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.PATH_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.TITLE_DESCRIPTION;
import static java.util.UUID.randomUUID;
import static jakarta.persistence.CascadeType.ALL;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
public class StaticFile implements Serializable {
    @Serial
    private static final long serialVersionUID = -2359099200834675210L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @NotEmpty(message = "validation.static-file.path.not-empty")
    @Pattern(regexp = "^[a-z\\d-]+$", message = "validation.static-file.path.invalid-pattern")
    @Column(nullable = false)
    @Schema(description = PATH_DESCRIPTION, required = true)
    private String path;

    @NotEmpty(message = "validation.static-file.description.not-empty")
    @Column(nullable = false)
    @Schema(description = "Description", required = true)
    private String description;

    @OneToOne(cascade = ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    private Attachment attachment;

    @Builder.Default
    private Boolean locked = false;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }

    public StaticFileDto asDto() { return new StaticFileDto(this); }
}
