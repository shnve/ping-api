package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.ActionType;
import fr.shom.ping.model.enums.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static jakarta.persistence.EnumType.STRING;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "Infosource Action Model")
public class InfosourceAction implements Serializable {
    @Serial
    private static final long serialVersionUID = 4247146552012087502L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @OneToOne(optional = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotNull(message = "validation.action.infosource.not-null")
    @Schema(description = "Infosource linked to action")
    private Infosource infosource;

    @Column(nullable = false)
    @Enumerated(STRING)
    @NotNull(message = "validation.action.type.not-null")
    @Schema(description = "Action type")
    private ActionType type;

    @Schema(description = "Comment")
    private String comment;

    @Schema(description = "User UUID")
    private UUID userId;

    @Enumerated(STRING)
    @Schema(description = "Public or pro user")
    private UserType userType;

    @OneToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Schema(description = "Service concerned")
    private MaritimeService service;

    @Column(nullable = false)
    @NotNull(message = "validation.action.date.not-null")
    @Schema(description = "Date/time of action (UTC)")
    private Instant date;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
        date = now();
    }
}
