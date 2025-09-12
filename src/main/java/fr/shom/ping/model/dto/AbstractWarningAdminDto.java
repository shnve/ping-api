package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.shom.ping.model.WarningNavtexConfig;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
import fr.shom.ping.model.enums.WarningStatus;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Warning Dto Model")
public abstract class AbstractWarningAdminDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 5453170206212208108L;

    private UUID serviceId;

    private WarningStatus status;

    // Part 1
    @NotEmpty
    @Default
    private Set<GeneralAreaDto> generalAreas = new HashSet<>();

    @Default
    private boolean useAsModel = false;

    @NoHtml
    private String modelName;

    @Default
    private Set<WarningLocalityNamesDto> localityNames = new HashSet<>();

    @NotNull
    private WarningHazardTypeGeneral typeGeneral;

    private Integer warningNumber;

    private Integer year;

    private Instant cancellationTime;

    private boolean addCancellationTimeToTelex;

    // Part 2
    @Default
    private List<UUID> cancelledWarningIds = new ArrayList<>();

    @Default
    private List<UUID> replacedWarningIds = new ArrayList<>();

    @Default
    private List<UUID> parentWarningIds = new ArrayList<>();

    @Default
    public List<UUID> infosourcesIds = new ArrayList<>();

    @Default
    public Set<@Valid InfosourceReferenceDto> infosources = new HashSet<>();

    @Default
    private List<UUID> maritimeEntitiesIds = new ArrayList<>();

    @Default
    private List<UUID> georegulationIds = new ArrayList<>();

    @Default
    private List<UUID> attachmentIds = new ArrayList<>();

    // Part 3
    @Default
    private List<@Valid WarningFeaturePartCreateDto> parts = new ArrayList<>();

    // Part 4
    @Default
    private Set<WarningReferenceAsPublicReferenceDto> warningPublicsReferences = new HashSet<>();

    @Default
    private Set<UUID> georegulationReferenceIds = new HashSet<>();

    @Default
    private Set<UUID> attachmentReferenceIds = new HashSet<>();

    @Default
    private Set<UUID> entityReferenceIds = new HashSet<>();

    @Default
    protected Set<WarningLinkDto> warningLinksPublicsReferences = new HashSet<>();

    // Part 5
    @NoHtml
    private String telex;

    @NoHtml
    private String navtex;

    @JsonDeserialize(using = EmptyToNullNavtexConfigDeserializer.class)
    private WarningNavtexConfig navtexConfig;

    // Part 6
    private LocalDate estimatedPublicationDate;

    private LocalDate reviewDate;

    @NoHtml
    private String internalComment;

    @Default
    @JsonProperty(value = "isBathy")
    private boolean isBathy = false;

    public static class EmptyToNullNavtexConfigDeserializer extends JsonDeserializer<WarningNavtexConfig> {

        @Override
        public WarningNavtexConfig deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            if (node.isEmpty()) {
                return null;
            }
            return Jackson2ObjectMapperBuilder.json().build().convertValue(node, WarningNavtexConfig.class);
        }
    }
}
