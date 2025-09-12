package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Georegulation;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.Notion;
import fr.shom.ping.model.NotionGeoregulation;
import fr.shom.ping.model.NotionInfosource;
import fr.shom.ping.model.NotionWarning;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.enums.GeoregulationStatus;
import fr.shom.ping.model.enums.NotionType;
import fr.shom.ping.model.enums.WarningStatus;
import fr.shom.ping.utils.GeoregulationUtil;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.model.enums.GlobalWarningType.NOTICE_TP;
import static java.util.Optional.ofNullable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
public class NotionDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 550403141778573022L;

    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @Schema(description = "Object id", required = true)
    private UUID objectId;

    @Schema(description = "Number", required = true)
    @NoHtml
    private String number;

    @Schema(description = "Type", required = true)
    @NoHtml
    private String type;

    @Schema(description = "Creation date", required = true)
    private Instant creationDate;

    @Schema(description = "End date", required = true)
    private Instant endDate;

    @Schema(description = "Sending date time (UTC)")
    private Instant validationTime;

    @Schema(description = "Publication date")
    private Instant publicationDate;

    @Schema(description = "Cancellation date")
    private Instant cancellationDate;

    @Schema(description = "Repeal date")
    private Instant repealDate;

    @Schema(description = "Infosource")
    private @Valid NotionInfosourceDto infosource;

    @Schema(description = "Warning")
    private @Valid NotionWarningDto warning;

    @Schema(description = "Warning T&P")
    private @Valid NotionWarningDto warningTP;

    @Schema(description = "Georegulation")
    private @Valid NotionGeoregulationDto georegulation;

    private boolean canBeAccessed;

    public static NotionDto fromNotion(Notion abstractNotion, List<UUID> serviceIdsWithOperatorOrAdminRole) {
        NotionDto notionDto = NotionDto.builder()
            .id(abstractNotion.getId())
            .build();

        String number = "";
        Instant notionCreationDate = Instant.now();
        Instant notionEndDate = Instant.now();
        Instant notionValidationTime = null;
        Instant notionPublicationDate = null;
        Instant notionCancellationDate = null;
        Instant notionRepealDate = null;
        NotionType notionType = abstractNotion.getType();

        switch (abstractNotion.getType()) {
            case INFOSOURCE:
                Infosource infosource = ((NotionInfosource) abstractNotion).getInfosource();
                notionCreationDate = infosource.getCreationDate();
                notionEndDate = infosource.getLastUpdate();
                notionValidationTime = infosource.getValidationTime();
                number = infosource.getNumber();
                notionDto.setInfosource(new NotionInfosourceDto(infosource));
                notionDto.setObjectId(infosource.getId());
                ofNullable(infosource.getIssuingService()).ifPresent(service -> notionDto.setCanBeAccessed(serviceIdsWithOperatorOrAdminRole.contains(service.getId())));
                break;
            case WARNING:
                Warning warning = ((NotionWarning) abstractNotion).getWarning();
                notionCreationDate = warning.getCreationDate();
                if (warning.getStatus() == WarningStatus.PUBLISHED) {
                    notionPublicationDate = warning.getPublicationTime();
                    notionEndDate = warning.getPublicationTime();
                } else if (warning.getStatus() == WarningStatus.CANCELLED) {
                    notionPublicationDate = warning.getPublicationTime();
                    notionCancellationDate = warning.getCancellationDate();
                    notionEndDate = warning.getCancellationDate();
                }
                if (warning.getStatus() != WarningStatus.DRAFT && warning.getStatus() != WarningStatus.MODEL) {
                    String nameOfSeries = warning.getNameOfSeries();
                    String warningNumber = warning.getWarningNumber().toString();
                    String warningYear = Year.of(warning.getYear()).format(DateTimeFormatter.ofPattern("uu"));
                    number = nameOfSeries + " " + warningNumber + "/" + warningYear;
                }
                if (warning.getSerie().getType().getGlobalWarningType() == NOTICE_TP) {
                    notionType = NotionType.WARNING_TP;
                    notionDto.setWarningTP(new NotionWarningDto(warning));
                } else {
                    notionType = NotionType.WARNING;
                    notionDto.setWarning(new NotionWarningDto(warning));
                }
                notionDto.setObjectId(warning.getId());
                notionDto.setCanBeAccessed(serviceIdsWithOperatorOrAdminRole.contains(warning.getSerie().getMaritimeService().getId()));
                break;
            case GEOREGULATION:
                Georegulation georegulation = ((NotionGeoregulation) abstractNotion).getGeoregulation();
                notionCreationDate = georegulation.getCreationDate();
                if (georegulation.getStatus() == GeoregulationStatus.PUBLISHED) {
                    notionPublicationDate = georegulation.getPublicationDate();
                    notionEndDate = georegulation.getPublicationDate();
                } else if (georegulation.getStatus() == GeoregulationStatus.REPEALED) {
                    notionPublicationDate = georegulation.getPublicationDate();
                    notionRepealDate = georegulation.getRepealDate();
                    notionEndDate = georegulation.getRepealDate();
                }
                number = GeoregulationUtil.getNumber(
                    georegulation.getNumber().getYear(),
                    georegulation.getNumber().getText(),
                    Set.copyOf(georegulation.getNumber().getAuthorityCodes()),
                    georegulation.getNumber().getDate()
                );
                notionDto.setGeoregulation(new NotionGeoregulationDto(georegulation));
                notionDto.setObjectId(georegulation.getId());
                notionDto.setCanBeAccessed(serviceIdsWithOperatorOrAdminRole.contains(georegulation.getIssuingService().getId()));
                break;
        }

        notionDto.setCreationDate(notionCreationDate);
        notionDto.setEndDate(notionEndDate);
        notionDto.setNumber(number);
        notionDto.setType(notionType.name());
        notionDto.setPublicationDate(notionPublicationDate);
        notionDto.setValidationTime(notionValidationTime);
        notionDto.setCancellationDate(notionCancellationDate);
        notionDto.setRepealDate(notionRepealDate);
        return notionDto;
    }
}
