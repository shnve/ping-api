package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Georegulation;
import fr.shom.ping.model.MaritimeEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import jakarta.validation.Valid;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.stream.Collectors.toList;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Georegulation Edit Dto Model")
public class GeoregulationEditDto extends GeoregulationUpdateDto {
    @Serial
    private static final long serialVersionUID = 6303618521057663900L;

    @Builder.Default
    public List<@Valid AttachmentDownloadDto> attachmentFiles = new ArrayList<>();

    public static GeoregulationEditDto from(Georegulation georegulation) {
        return GeoregulationEditDto.builder()
            .serviceId(georegulation.getIssuingService().getId())
            .type(georegulation.getType())
            .mainNumber(new GeoregulationNumberDto(georegulation.getNumber()))
            .additionalNumbers(
                georegulation.getAdditionalNumbers() == null ? new ArrayList<>() :
                    georegulation.getAdditionalNumbers().stream().map(GeoregulationNumberDto::fromGeoregulationNumber).collect(toList())
            )
            .activities(georegulation.getActivities())
            .title(georegulation.getTitle())
            .internationalTitle(georegulation.getInternationalTitle())
            .maritimeEntityIds(georegulation.getEntities().stream().map(MaritimeEntity::getId).collect(toList()))
            .summary(georegulation.getSummary())
            .internationalSummary(georegulation.getInternationalSummary())
            .isTemporary(georegulation.isTemporary())
            .isAllTerritorialSea(georegulation.isAllTerritorialSea())
            .isAnnouncedByAvurnavAvirade(georegulation.isAnnouncedByAvurnavAvirade())
            .calendarPeriods(new ArrayList<>(georegulation.getCalendarPeriods()))
            .seasonalPeriods(new ArrayList<>(georegulation.getSeasonalPeriods()))
            .hasCoastlineLimit(georegulation.isHasCoastlineLimit())
            .parts(georegulation.getParts().stream().map(GeoregulationPartCreateDto::fromGeoregulationPart).collect(toList()))
            .links(georegulation.getLinks())
            .attachmentIds(georegulation.getAttachments().stream().map(Attachment::getId).collect(toList()))
            .isPublic(georegulation.isPublic())
            .markAsNewInMonths(georegulation.getMarkAsNewInMonths())
            .internalComment(georegulation.getInternalComment())
            .timezone(georegulation.getTimezone())
            .id(georegulation.getId())
            .attachmentFiles(georegulation.getAttachments()
                .stream()
                .map(attachment -> AttachmentDownloadDto.builder()
                    .id(attachment.getId())
                    .filename(attachment.getFilename())
                    .totalSize(attachment.getTotalSize())
                    .build())
                .collect(toList()))
            .status(georegulation.getStatus())
            .build();
    }
}
