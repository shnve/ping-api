package fr.shom.ping.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@MappedSuperclass
@SuperBuilder
public class AbstractEnumTypes implements Serializable {
    @Serial
    private static final long serialVersionUID = -6811913508859004241L;

    @Id
    @NotNull(message = "validation.enum-name.not-null")
    @Schema(description = "Name of enum", required = true)
    @Column(nullable = false)
    private String enumName;

    @Schema(description = "Translation FR of enum")
    private String translation;

    @Schema(description = "Translation EN of enum")
    private String internationalTranslation;
}
