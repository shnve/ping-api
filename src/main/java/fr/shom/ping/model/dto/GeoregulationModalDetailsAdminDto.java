package fr.shom.ping.model.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.shom.ping.model.Georegulation;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import fr.shom.ping.validation.NoHtml;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import jakarta.validation.Valid;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.model.enums.GeoregulationStatus.PUBLISHED;
import static fr.shom.ping.model.enums.GeoregulationStatus.REPEALED;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
public class GeoregulationModalDetailsAdminDto extends GeoregulationModalDetailsDto {
    @Serial
    private static final long serialVersionUID = -5518151517658242913L;

    @Default
    private List<@Valid MaritimeEntityConcernedDto> entities = new ArrayList<>();

    @NoHtml
    private String lastUpdateUser;

    private @Valid MaritimeServiceSelectDto issuingService;

    @NoHtml
    private String internalComment;

    @JsonProperty("canBeSent")
    private Boolean canBeSent;

    public GeoregulationModalDetailsAdminDto(Georegulation georegulation, boolean hasAlreadyBeenSent, boolean canBeAddedToFolder) {
        super(georegulation, canBeAddedToFolder);
        entities = georegulation.getEntities().stream().map(MaritimeEntityConcernedDto::new).collect(toList());
        lastUpdateUser = georegulation.getLastUpdateUser().getFirstname() + " " + georegulation.getLastUpdateUser().getLastname();
        internalComment = georegulation.getInternalComment();
        issuingService = new MaritimeServiceSelectDto(georegulation.getIssuingService());
        canBeSent = asList(PUBLISHED, REPEALED).contains(georegulation.getStatus()) && !hasAlreadyBeenSent;
    }
}
