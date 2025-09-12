package fr.shom.ping.model.tuple;

import fr.shom.ping.model.enums.SubscriptionType;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@Data
@Getter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class SubscriptionTableTuple implements Serializable {
    @Serial
    private static final long serialVersionUID = -1744147848681743572L;

    private UUID id;

    private String title;

    private Instant creationDate;

    private boolean sendingMail;

    private String areaName;

    private String areaInternationalName;

    private String maritimeServiceSubscriberName;

    private String maritimeServiceSubscriberInternationalName;

    private SubscriptionType type;

    public SubscriptionTableTuple(UUID id, String title, Instant creationDate, boolean sendingMail,
        String areaName, String areaInternationalName, String maritimeServiceSubscriberName, String maritimeServiceSubscriberInternationalName, String type) {
        this.id = id;
        this.title = title;
        this.creationDate = creationDate;
        this.sendingMail = sendingMail;
        this.areaName = areaName;
        this.areaInternationalName = areaInternationalName;
        this.maritimeServiceSubscriberName = maritimeServiceSubscriberName;
        this.maritimeServiceSubscriberInternationalName = maritimeServiceSubscriberInternationalName;
        if (StringUtils.isNotBlank(type)) {
            this.type = SubscriptionType.valueOf(type);
        }
    }
}
