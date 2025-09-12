package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.dto.MaritimeEntityReferenceCreateDto;
import fr.shom.ping.model.dto.MaritimeEntityReferenceDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.Instant;
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
import static jakarta.persistence.CascadeType.ALL;


@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "Maritime Entity Reference Model")
public class MaritimeEntityReference implements Serializable {

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @NotNull(message = "validation.maritime-entity-reference.maritime-entity.not-null")
    @OneToOne(optional = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @JsonIgnore
    private MaritimeEntity maritimeEntity;

    @NotEmpty(message = "validation.maritime-entity-reference.title.not-empty")
    @Column(nullable = false)
    @Schema(description = "maritime entity reference title", required = true)
    private String title;

    @Schema(description = "maritime entity reference international title")
    private String internationalTitle;

    @Schema(description = "maritime entity reference url")
    private String url;


    @Schema(description = "maritime entity is public", required = true)
    private boolean isPublic;

    @OneToOne(cascade = ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    private Attachment attachment;

    @Column(nullable = false)
    @Schema(description = "Creation date time (UTC)")
    private Instant creationDate;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
        creationDate = Instant.now();
    }

    public MaritimeEntityReference(MaritimeEntityReferenceCreateDto dto, MaritimeEntity maritimeEntity, Attachment attachment) {
        this.title = dto.getTitle().trim();
        if (StringUtils.isNotEmpty(dto.getInternationalTitle())) {
            this.internationalTitle = dto.getInternationalTitle().trim();
        }
        this.maritimeEntity = maritimeEntity;
        this.url = dto.getUrl();
        this.isPublic = dto.getIsPublic();
        this.attachment = attachment;
    }

    public MaritimeEntityReferenceDto asDto() {
        return new MaritimeEntityReferenceDto(this);
    }
}
