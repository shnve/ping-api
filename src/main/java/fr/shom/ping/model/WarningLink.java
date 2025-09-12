package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.dto.WarningLinkCreateDto;
import fr.shom.ping.model.dto.WarningLinkDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static java.util.UUID.randomUUID;


@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "Warning link Model")
public class WarningLink implements Serializable {
    @Serial
    private static final long serialVersionUID = -6448054232378161150L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @NotNull(message = "validation.warning-link.warning.not-null")
    @OneToOne(optional = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private Warning warning;

    @NotEmpty(message = "validation.warning-link.title.not-empty")
    @Column(nullable = false)
    @Schema(description = "warning link title", required = true)
    private String title;

    @Schema(description = "warning link international title")
    private String internationalTitle;

    @Schema(description = "warning link url")
    private String url;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }

    public WarningLink(WarningLinkCreateDto dto, Warning warning) {
        this.title = dto.getTitle().trim();
        if (StringUtils.isNotEmpty(dto.getInternationalTitle())) {
            this.internationalTitle = dto.getInternationalTitle().trim();
        }
        this.warning = warning;
        this.url = dto.getUrl();
    }

    public WarningLinkDto asDto() {
        return new WarningLinkDto(this);
    }
}

