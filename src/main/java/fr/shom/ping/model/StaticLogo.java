package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.dto.StaticLogoDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static java.util.UUID.randomUUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
public class StaticLogo implements Serializable {
    @Serial
    private static final long serialVersionUID = 8740539684684936458L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @NotBlank(message = "validation.static-logo.key.not-blank")
    @Pattern(regexp = "^[a-z\\d-]+$", message = "validation.static-logo.key.invalid-pattern")
    @Column(nullable = false, name = "logo_key")
    private String key;

    @NotBlank(message = "validation.static-logo.description.not-blank")
    @Column(nullable = false)
    private String description;

    @NotBlank(message = "validation.static-logo.height-with-unit.not-blank")
    @Column(nullable = false)
    private String heightWithUnit;

    @Column(columnDefinition = "BYTEA")
    private byte[] content;

    @NotBlank(message = "validation.static-logo.alt-txt.not-blank")
    @Column(nullable = false)
    private String altTxt;

    @NotBlank(message = "validation.static-logo.alt-txt-international.not-blank")
    @Column(nullable = false)
    private String altTxtInternational;

    private String title;

    private String titleInternational;

    private String href;

    @Builder.Default
    private Boolean locked = false;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }

    public StaticLogoDto asDto() {
        return new StaticLogoDto(this);
    }
}
