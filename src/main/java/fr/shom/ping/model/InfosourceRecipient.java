package fr.shom.ping.model;

import fr.shom.ping.model.dto.InfosourceRecipientDto;
import fr.shom.ping.model.enums.ActionType;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static jakarta.persistence.EnumType.STRING;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"infosource_id", "service_id"}))
public class InfosourceRecipient implements Serializable {
    @Serial
    private static final long serialVersionUID = -3087627897743026738L;

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @ToString.Exclude
    private Infosource infosource;

    @Enumerated(STRING)
    @Column(nullable = false)
    private ActionType status;

    @OneToOne(optional = false)
    @ToString.Exclude
    private MaritimeService service;

    @OneToOne
    @ToString.Exclude
    private MaritimeService transferredBy;

    @Column(nullable = false)
    private Instant date;

    @PrePersist
    public void prePersist() {
        id = randomUUID();
        date = now();
    }

    public static InfosourceRecipientDto asDto(InfosourceRecipient infosourceRecipient) {
        return InfosourceRecipientDto.builder()
            .id(infosourceRecipient.getService().getId())
            .name(infosourceRecipient.getService().getName())
            .information(infosourceRecipient.getService().getInformation())
            .actionType(infosourceRecipient.getStatus())
            .transferredBy(Optional.ofNullable(infosourceRecipient.getTransferredBy()).map(MaritimeService::getName).orElse(null))
            .build();
    }
}
