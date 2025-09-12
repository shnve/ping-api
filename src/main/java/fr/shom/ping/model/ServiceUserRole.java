package fr.shom.ping.model;

import fr.shom.ping.model.enums.MaritimeServiceType;
import fr.shom.ping.model.enums.ServiceRole;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Transient;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static jakarta.persistence.EnumType.STRING;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceUserRole implements Serializable {
    @Serial
    private static final long serialVersionUID = -865171364872175184L;

    @Enumerated(STRING)
    private ServiceRole serviceRole;
    private UUID serviceId;

    @Transient
    private MaritimeServiceType type;

    @Transient
    private boolean shomService;

    @Transient
    private Boolean surveyOwner;

    @Transient
    private Boolean folderManager;
}
