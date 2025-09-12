package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.dto.StaticSectionDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static java.util.UUID.randomUUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
public class StaticSection implements Serializable {
    @Serial
    private static final long serialVersionUID = 6987498366080405559L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @NotEmpty(message = "validation.static-section.key.not-empty")
    @Pattern(regexp = "^[a-z\\d-]+$", message = "validation.static-section.path.invalid-pattern")
    @Column(nullable = false, name = "section_key")
    private String key;

    @NotEmpty(message = "validation.static-section.location.not-empty")
    @NotBlank(message = "validation.static-section.location.not-blank")
    @Column(nullable = false)
    private String location;

    @NotEmpty(message = "validation.static-section.path.not-empty")
    @Pattern(regexp = "^[a-z\\d-/]+$", message = "validation.static-section.path.invalid-pattern")
    @Column(nullable = false)
    private String path;

    @NotEmpty(message = "validation.static-section.name.not-empty")
    @NotBlank(message = "validation.static-section.name.not-blank")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "BYTEA")
    private byte[] content;

    @Column(columnDefinition = "BYTEA")
    private byte[] internationalContent;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }

    public StaticSectionDto asDto() {
        return new StaticSectionDto(this);
    }
}
