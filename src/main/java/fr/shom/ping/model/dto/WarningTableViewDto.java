package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.WarningHazardTypeDetails;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.hateoas.server.core.Relation;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.Lists.newArrayList;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.utils.WarningUtil.convertGeneralAreaDtoSortedSetToMap;
import static fr.shom.ping.utils.WarningUtil.convertLocalityDtoSortedSetToMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Relation(collectionRelation = "items")
public class WarningTableViewDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 3872151299722299485L;

    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @Schema(description = "Name of series", required = true)
    private String nameOfSeries;

    @Schema(description = "Warning number", required = true)
    private Integer warningNumber;

    @Schema(description = "Year", required = true)
    private Integer year;

    @Schema(description = "Model Name")
    private String modelName;

    @Default
    @JsonIgnore
    private SortedSet<GeneralAreaDto> generalAreaSortedSet = new TreeSet<>();

    @Default
    @JsonIgnore
    private List<WarningLocalityNamesDto> localitySortedList = newArrayList();

    @JsonIgnore
    private WarningHazardTypeGeneral warningHazardTypeGeneral;

    @JsonIgnore
    @Default
    private Set<WarningHazardTypeDetails> warningHazardTypeDetails = new HashSet<>();

    @Schema(description = "List of general area")
    @EqualsAndHashCode.Include
    public Map<String, Set<String>> getGeneralAreas() {
        return convertGeneralAreaDtoSortedSetToMap(generalAreaSortedSet);
    }

    @Schema(description = "List of locality")
    @EqualsAndHashCode.Include
    public Map<String, Set<String>> getLocalities() {
        return convertLocalityDtoSortedSetToMap(localitySortedList, generalAreaSortedSet);
    }

    @Schema(description = "Warning details", required = true)
    public String getWarningTypeDetails() {
        // If there is just one warningHazardTypeDetails, we use it as warning details
        if (warningHazardTypeDetails.size() == 1) {
            return warningHazardTypeDetails.iterator().next().name();
        }

        // Otherwise, we use the general hazard type as warning details
        return warningHazardTypeGeneral.name();
    }
}
