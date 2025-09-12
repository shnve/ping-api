package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
public abstract class NotionDetailsDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -6828980722322518816L;

    protected UUID id;
    protected Instant creationDate;
    protected Instant lastUpdate;

    @Default
    protected Set<UUID> linkedNotionIds = new HashSet<>();
}
