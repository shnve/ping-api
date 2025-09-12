package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.dto.TagCreateDto;
import fr.shom.ping.model.dto.TagDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static java.util.UUID.randomUUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "Service Tag Model")
public class Tag implements Serializable {
    @Serial
    private static final long serialVersionUID = -3599928944989462941L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @NotEmpty(message = "validation.tag.value.not-empty")
    @Column(nullable = false)
    @Schema(description = "tag value", required = true)
    private String value;

    @NotNull(message = "validation.tag.maritime-service.not-null")
    @ManyToOne(optional = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private MaritimeService maritimeService;

    @OneToMany
    @JoinTable(
        name = "infosource_tag",
        joinColumns = @JoinColumn(name = "tag_id"),
        inverseJoinColumns = @JoinColumn(name = "infosource_id"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    @JsonIgnore
    private Set<Infosource> infosources = new HashSet<>();

    @PrePersist
    public void prePersist() {
        id = randomUUID();
    }

    public Tag(TagCreateDto dto, MaritimeService maritimeService) {
        this.value = dto.getValue().trim();
        this.maritimeService = maritimeService;
    }

    public TagDto asDto() {
        return new TagDto(this);
    }
}
