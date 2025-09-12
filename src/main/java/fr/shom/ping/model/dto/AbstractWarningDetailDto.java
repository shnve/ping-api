package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.GeneralArea;
import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.WarningAttachment;
import fr.shom.ping.model.WarningLink;
import fr.shom.ping.model.WarningLocalityNames;
import fr.shom.ping.model.WarningReference;
import fr.shom.ping.model.enums.GlobalWarningType;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
import fr.shom.ping.model.enums.WarningReferenceCategoryEnum;
import fr.shom.ping.model.enums.WarningStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.hateoas.server.core.Relation;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.Lists.newArrayList;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.model.enums.WarningReferenceCategoryEnum.CANCELLATION;
import static fr.shom.ping.utils.S124v2Util.getLocalitiesOrderedByGeneralAreas;
import static fr.shom.ping.utils.WarningUtil.convertGeneralAreaDtoSortedSetToMap;
import static fr.shom.ping.utils.WarningUtil.convertGeneralAreaInformationToLocationNames;
import static fr.shom.ping.utils.WarningUtil.convertLocalityDtoSortedSetToMap;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
@SuperBuilder
@Schema(name = "Warning Abstract Detail DTO")
@Relation(collectionRelation = "items")
public abstract class AbstractWarningDetailDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1709329227410887593L;

    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @Schema(description = "Name of series", required = true)
    private String nameOfSeries;

    @Schema(description = "Warning number", required = true)
    private Integer warningNumber;

    @Schema(description = "Year", required = true)
    private Integer year;

    @Schema(description = "Global warning type", required = true)
    private GlobalWarningType globalWarningType;

    @Default
    @JsonIgnore
    private SortedSet<GeneralAreaDto> generalAreaSortedSet = new TreeSet<>();

    @Default
    @JsonIgnore
    private List<WarningLocalityNamesDto> localitySortedList = newArrayList();

    @Schema(description = "Warning hazard type general", required = true)
    private WarningHazardTypeGeneral warningHazardTypeGeneral;

    @Default
    @Schema(description = "Feature parts")
    private SortedSet<WarningFeaturePartDto> featureParts = new TreeSet<>();

    @Schema(description = "Cancellation date")
    private Instant cancellationDate;

    @Schema(description = "Creation date")
    private Instant creationDate;

    @Schema(description = "Add cancellation time to Telex")
    private boolean addCancellationTimeToTelex;

    @Schema(description = "Publication time", required = true)
    private Instant publicationTime;

    @Default
    private Set<WarningReferenceDto> cancelledWarnings = new HashSet<>();

    protected WarningStatus status;

    @Default
    private Set<AttachmentDownloadDto> attachmentsPublicsReferences = new HashSet<>();

    @Default
    private Set<WarningLinkDto> warningLinksPublicsReferences = new HashSet<>();

    @Default
    private Set<WarningMaritimeEntityReferenceDto> entitiesReferencesPublicsReferences = new HashSet<>();

    @Default
    private Set<GeoregulationSelectDto> georegulationPublicsReferences = new HashSet<>();

    @Default
    private Set<WarningReferenceAsPublicReferenceDto> warningReferencesAsPublicsReferences = new HashSet<>();

    public static AbstractWarningDetailDto fromWarning(AbstractWarningDetailDto dto, Warning warning) {
        dto.setId(warning.getId());
        dto.setNameOfSeries(warning.getNameOfSeries());
        dto.setWarningNumber(warning.getWarningNumber());
        dto.setYear(warning.getYear());
        SortedSet<GeneralAreaDto> generalAreaDtos = new TreeSet<>();
        warning.getGeneralAreas().forEach(generalArea -> {
            generalAreaDtos.add(GeneralAreaDto.builder()
                .id(generalArea.getId())
                .subAreaId(generalArea.getSubArea().getId())
                .position(generalArea.getPosition())
                .locationNames(convertGeneralAreaInformationToLocationNames(generalArea.getName(), generalArea.getInternationalName()))
                .build());
        });
        dto.setGeneralAreaSortedSet(generalAreaDtos);

        List<WarningLocalityNamesDto> localityDtos = newArrayList();

        getLocalitiesOrderedByGeneralAreas(warning)
            .forEach(locality ->
                localityDtos.add(WarningLocalityNamesDto.builder()
                    .id(locality.getId())
                    .position(locality.getPosition())
                    .name(locality.getName())
                    .internationalName(locality.getInternationalName())
                    .parentId(locality.getParent().getId())
                    .build())
            );

        dto.setLocalitySortedList(localityDtos);
        dto.setWarningHazardTypeGeneral(warning.getWarningHazardTypeGeneral());

        warning.getFeatureParts().forEach(featurePart -> {
            WarningFeaturePartDto warningFeaturePartDto = new WarningFeaturePartDto();
            warningFeaturePartDto.setPosition(featurePart.getPosition());
            warningFeaturePartDto.setGeometries(featurePart.getGeometries());
            warningFeaturePartDto.setAffectedAreas(featurePart.getAffectedAreas());
            warningFeaturePartDto.setTypeDetails(featurePart.getWarningHazardTypeDetails());
            warningFeaturePartDto.setInformation(featurePart.getInformation());
            warningFeaturePartDto.setDateRanges(featurePart.getFixedDateRanges());
            dto.getFeatureParts().add(warningFeaturePartDto);
        });
        dto.setGlobalWarningType(warning.getSerie().getType().getGlobalWarningType());
        dto.setCancellationDate(warning.getCancellationDate());
        dto.setCreationDate(warning.getCreationDate());
        dto.setAddCancellationTimeToTelex(warning.isAddCancellationTimeToTelex());
        dto.setPublicationTime(warning.getPublicationTime());
        dto.setCancelledWarnings(new HashSet<>(fillReferencesByCategory(warning, CANCELLATION)));
        dto.setStatus(warning.getStatus());

        Set<WarningReferenceAsPublicReferenceDto> warningReferencesAsPublicsReferences = warning.getWarningsPublicsReferences().stream()
            .map(WarningReferenceAsPublicReferenceDto::fromWarningPublicReference)
            .collect(toSet());
        dto.setWarningReferencesAsPublicsReferences(warningReferencesAsPublicsReferences);

        Set<MaritimeEntityDto> maritimeEntities = warning.getMaritimeEntities().stream().map(MaritimeEntity::asDto).collect(Collectors.toSet());
        Set<WarningMaritimeEntityReferenceDto> entitiesReferencesPublicsReferences = warning.getEntityReferences().stream()
            .map(maritimeEntityReference -> {
                MaritimeEntityDto maritimeEntity = maritimeEntities.stream()
                    .filter(maritimeEntityDto -> maritimeEntityDto.references.stream()
                        .map(MaritimeEntityReferenceDto::getId)
                        .collect(toSet())
                        .contains(maritimeEntityReference.getId()))
                    .findFirst().orElseThrow(NotFoundException::new);
                return new WarningMaritimeEntityReferenceDto(maritimeEntity.getName(), maritimeEntity.getInternationalName(), maritimeEntityReference.asDto());
            })
            .collect(toSet());
        dto.setEntitiesReferencesPublicsReferences(entitiesReferencesPublicsReferences);

        Set<GeoregulationSelectDto> georegulationPublicsReferences = warning.getGeoregulations().stream()
            .map(warningGeoregulation -> new GeoregulationSelectDto(warningGeoregulation.getGeoregulation()))
            .collect(toSet());
        dto.setGeoregulationPublicsReferences(georegulationPublicsReferences);

        Set<AttachmentDownloadDto> attachmentsPublicsReferences = warning.getAttachments().stream()
            .filter(WarningAttachment::getIsPublicReference)
            .map(AttachmentDownloadDto::fromWarningAttachment)
            .collect(toSet());
        dto.setAttachmentsPublicsReferences(attachmentsPublicsReferences);

        Set<WarningLinkDto> warningLinksPublicsReferences = warning.getWarningLinksPublicsReferences().stream()
            .map(WarningLink::asDto)
            .collect(toSet());
        dto.setWarningLinksPublicsReferences(warningLinksPublicsReferences);

        return dto;
    }

    protected static List<WarningReferenceDto> fillReferencesByCategory(Warning warning, WarningReferenceCategoryEnum category) {
        return warning.getWarningReferences().stream()
            .filter(warningReference -> warningReference.getCategory() == category)
            .map(WarningReference::getWarnings)
            .flatMap(Collection::stream)
            .map(reference -> WarningReferenceDto.builder()
                .id(reference.getId())
                .nameOfSeries(reference.getNameOfSeries())
                .warningNumber(reference.getWarningNumber())
                .year(reference.getYear())
                .build())
            .collect(toList());
    }

    @Schema(description = "List of general area")
    public Map<String, Set<String>> getGeneralAreas() {
        return convertGeneralAreaDtoSortedSetToMap(generalAreaSortedSet);
    }

    @Schema(description = "List of locality")
    public Map<String, Set<String>> getLocalities() {
        return convertLocalityDtoSortedSetToMap(localitySortedList, generalAreaSortedSet);
    }
}
