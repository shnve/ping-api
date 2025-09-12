package fr.shom.ping.model.dto;

import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.Notion;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.WarningAttachment;
import fr.shom.ping.model.WarningGeoregulation;
import fr.shom.ping.model.WarningLink;
import fr.shom.ping.model.enums.DiffusionNavtexSource;
import fr.shom.ping.model.enums.SerieType;
import fr.shom.ping.utils.NavtexUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.google.common.collect.Sets.newTreeSet;
import static fr.shom.ping.model.enums.WarningReferenceCategoryEnum.IN_FORCE;
import static fr.shom.ping.model.enums.WarningReferenceCategoryEnum.REPLACEMENT;
import static fr.shom.ping.model.enums.WarningReferenceCategoryEnum.SOURCE_REFERENCE;
import static fr.shom.ping.model.enums.WarningStatus.DRAFT;
import static fr.shom.ping.utils.WarningUtil.checkIfAtLeastInformationOrDetailsIsFilledForEachPart;
import static fr.shom.ping.utils.WarningUtil.checkIfAtLeastOneMandatoryLanguageIsMissing;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Schema(name = "Warning Admin Detail DTO")
public class WarningAdminDetailDto extends AbstractWarningDetailDto {

    @Serial
    private static final long serialVersionUID = -5001785478168285788L;

    private UUID serviceId;

    private Instant creationDate;

    private LocalDate estimatedPublicationDate;

    private LocalDate reviewDate;

    private LocalDate endOfBroadcastDate;

    private Instant lastUpdate;

    private SerieType serieType;

    private UUID serieId;

    @Default
    private List<WarningReferenceDto> replacedWarnings = new ArrayList<>();

    @Default
    private List<WarningReferenceDto> inforcedWarnings = new ArrayList<>();

    @Default
    private List<WarningReferenceDto> parentWarnings = new ArrayList<>();

    @Default
    private List<AttachmentDownloadDto> attachments = new ArrayList<>();

    private String internalComment;

    @Default
    private LastUpdateUserDto lastUpdateUser = new LastUpdateUserDto();

    private boolean canBePublished;

    @Default
    private Set<MaritimeEntityDto> maritimeEntities = new HashSet<>();

    @Default
    private SortedSet<FolderConcernedDto> folders = newTreeSet();

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

    private boolean forceToReview;

    private boolean canBeAddedToFolder;

    public WarningAdminDetailDto fromWarning(Warning warning, boolean canBeAddedToFolder) {
        AbstractWarningDetailDto.fromWarning(this, warning);
        serviceId = warning.getSerie().getMaritimeService().getId();
        creationDate = warning.getCreationDate();
        estimatedPublicationDate = warning.getEstimatedPublicationDate();
        reviewDate = warning.getReviewDate();
        endOfBroadcastDate = warning.getEndOfBroadcastDate();
        lastUpdate = warning.getLastUpdate();
        replacedWarnings = fillReferencesByCategory(warning, REPLACEMENT);
        inforcedWarnings = fillReferencesByCategory(warning, IN_FORCE);
        parentWarnings = fillReferencesByCategory(warning, SOURCE_REFERENCE);
        maritimeEntities = warning.getMaritimeEntities().stream().map(MaritimeEntity::asDto).collect(Collectors.toSet());
        attachments = warning.getAttachments().stream()
            .map(AttachmentDownloadDto::fromWarningAttachment)
            .collect(toList());
        internalComment = warning.getInternalComment();
        serieType = warning.getSerie().getType();
        serieId = warning.getSerie().getId();
        if (warning.getLastUpdateUser() != null) {
            lastUpdateUser.setFirstname(warning.getLastUpdateUser().getFirstname());
            lastUpdateUser.setLastname(warning.getLastUpdateUser().getLastname());
        }

        DiffusionNavtexSource navtexSource = warning.getSerie().getNavtexSource();

        // For each part, check if at least information or typeDetails is filled
        boolean atLeastInformationOrDetailsIsFilledForEachPart = checkIfAtLeastInformationOrDetailsIsFilledForEachPart(warning);

        boolean atLeastOneMandatoryLanguageIsMissing = false;

        // Feature part is not mandatory until warning is published
        if (isNotEmpty(warning.getFeatureParts())) {
            atLeastOneMandatoryLanguageIsMissing = checkIfAtLeastOneMandatoryLanguageIsMissing(warning);
        }

        canBePublished = warning.getStatus() == DRAFT
            && isNotEmpty(warning.getFeatureParts())
            && atLeastInformationOrDetailsIsFilledForEachPart
            && !atLeastOneMandatoryLanguageIsMissing
            && isNotEmpty(warning.getGeneralAreas())
            && NavtexUtil.isConfigValid(warning.getNavtexConfig(), navtexSource);

        folders = warning.getNotions().stream()
            .map(Notion::getFolder)

            .map(FolderConcernedDto::from)
            .sorted()
            .collect(toCollection(TreeSet::new));

        forceToReview = warning.getForceToReview();

        warningReferencesAsPublicsReferences = warning.getWarningsPublicsReferences().stream()
            .map(WarningReferenceAsPublicReferenceDto::fromWarningPublicReference)
            .collect(toSet());

        entitiesReferencesPublicsReferences = warning.getEntityReferences().stream()
            .map(maritimeEntityReference -> {
                MaritimeEntityDto maritimeEntity = maritimeEntities.stream()
                    .filter(maritimeEntityDto -> maritimeEntityDto.references.stream()
                        .map(MaritimeEntityReferenceDto::getId)
                        .collect(toSet())
                        .contains(maritimeEntityReference.getId()))
                    .findFirst().get();
                return new WarningMaritimeEntityReferenceDto(maritimeEntity.getName(), maritimeEntity.getInternationalName(), maritimeEntityReference.asDto());
            })
            .collect(toSet());

        georegulationPublicsReferences = warning.getGeoregulations().stream()
            .filter(WarningGeoregulation::getIsPublicReference)
            .map(warningGeoregulation -> new GeoregulationSelectDto(warningGeoregulation.getGeoregulation()))
            .collect(toSet());

        attachmentsPublicsReferences = warning.getAttachments().stream()
            .filter(WarningAttachment::getIsPublicReference)
            .map(AttachmentDownloadDto::fromWarningAttachment)
            .collect(toSet());

        warningLinksPublicsReferences = warning.getWarningLinksPublicsReferences().stream()
            .map(WarningLink::asDto)
            .collect(toSet());

        this.canBeAddedToFolder = canBeAddedToFolder;

        return this;
    }
}
