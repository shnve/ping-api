package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.NotificationType;
import fr.shom.ping.model.tuple.NotificationTableTuple;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.utils.SchemaUtil.NOTIFICATION_TABLE_TUPLE_MAPPING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static jakarta.persistence.EnumType.STRING;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "Notification Model")
@SqlResultSetMappings(value = {
    @SqlResultSetMapping(name = NOTIFICATION_TABLE_TUPLE_MAPPING,
        classes = {
            @ConstructorResult(targetClass = NotificationTableTuple.class,
                columns = {
                    @ColumnResult(name = "id", type = UUID.class),
                    @ColumnResult(name = "type", type = String.class),
                    @ColumnResult(name = "title", type = String.class),
                    @ColumnResult(name = "read", type = Boolean.class),
                    @ColumnResult(name = "creation_date", type = Instant.class),
                    @ColumnResult(name = "read_date", type = Instant.class),
                    @ColumnResult(name = "subscription_id", type = UUID.class),
                    @ColumnResult(name = "folder_id", type = UUID.class),
                    @ColumnResult(name = "service_id", type = UUID.class),
                    @ColumnResult(name = "service_name", type = String.class),
                    @ColumnResult(name = "service_international_name", type = String.class),
                    @ColumnResult(name = "warning_id", type = UUID.class),
                    @ColumnResult(name = "georegulation_id", type = UUID.class),
                    @ColumnResult(name = "infosource_id", type = UUID.class),
                    @ColumnResult(name = "survey_id", type = UUID.class)
                })
        }
    )
})
public class Notification implements Serializable {
    @Serial
    private static final long serialVersionUID = -8875964646144352449L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @NotNull(message = "validation.notification.type.not-null")
    @Column(nullable = false)
    @Enumerated(STRING)
    @Schema(description = "Notification type")
    private NotificationType type;

    @NotNull(message = "validation.notification.title.not-null")
    @Column(nullable = false)
    @Schema(description = "Notification title")
    private String title;

    @NotNull(message = "validation.notification.read.not-null")
    @Column(nullable = false)
    @Builder.Default
    private boolean read = false;

    @OneToOne
    @ToString.Exclude
    @Schema(description = "Public user")
    private PublicUser publicUser;

    @OneToOne
    @ToString.Exclude
    @Schema(description = "Service user")
    private ServiceUser serviceUser;

    @Column(nullable = false)
    @Schema(description = "Creation date time (UTC)")
    private Instant creationDate;

    @Schema(description = "Read date time (UTC)")
    private Instant readDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @Schema(description = "Subscription")
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @Schema(description = "Folder")
    private Folder folder;

    @ManyToOne
    @ToString.Exclude
    @Schema(description = "Service")
    private MaritimeService service;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @Schema(description = "Warning")
    private Warning warning;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @Schema(description = "Georegulation")
    private Georegulation georegulation;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @Schema(description = "Infosource")
    private Infosource infosource;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @Schema(description = "Survey")
    private Survey survey;

    public UUID getUserId() {
        return serviceUser != null ? serviceUser.getId() : publicUser.getId();
    }

    @PrePersist
    public void prePersist() {
        id = randomUUID();
        creationDate = now();
    }
}
