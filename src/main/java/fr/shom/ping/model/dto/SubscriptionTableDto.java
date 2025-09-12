package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.hateoas.server.core.Relation;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Subscription Table Dto Model")
@Relation(collectionRelation = "items")
public class SubscriptionTableDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 6682618680955846563L;

    @Schema(description = ID_DESCRIPTION, required = true)
    protected UUID id;

    protected String title;

    protected Instant creationDate;

    protected boolean sendingMail;

    protected String areaName;

    protected String areaInternationalName;

    protected String maritimeServiceSubscriberName;

    protected String maritimeServiceSubscriberInternationalName;
}
