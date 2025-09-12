package fr.shom.ping.model.mapper;

import fr.shom.ping.model.GeneralArea;
import fr.shom.ping.model.LocalizedText;
import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.MaritimeEntityReference;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.SubArea;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.WarningAttachment;
import fr.shom.ping.model.WarningGeoregulation;
import fr.shom.ping.model.WarningLink;
import fr.shom.ping.model.WarningLocalityNames;
import fr.shom.ping.model.WarningReference;
import fr.shom.ping.model.dto.AttachmentAdminDetailsDto;
import fr.shom.ping.model.dto.AuthenticatedServiceUserDto;
import fr.shom.ping.model.dto.FeatureDto;
import fr.shom.ping.model.dto.GeneralAreaDto;
import fr.shom.ping.model.dto.InfosourceReferenceDto;
import fr.shom.ping.model.dto.SubAreaDetailsDto;
import fr.shom.ping.model.dto.WarningAdminEditDto;
import fr.shom.ping.model.dto.WarningFeaturePartCreateDto;
import fr.shom.ping.model.dto.WarningLinkDto;
import fr.shom.ping.model.dto.WarningReferenceAsPublicReferenceDto;
import fr.shom.ping.model.enums.WarningReferenceCategoryEnum;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class WarningToWarningAdminDetailDtoMapper implements Function<Warning, WarningAdminEditDto> {

    @Override
    public WarningAdminEditDto apply(Warning warning) {
        WarningAdminEditDto warningAdminEditDto = new WarningAdminEditDto();
        warningAdminEditDto.setServiceId(warning.getSerie().getMaritimeService().getId());
        warningAdminEditDto.setStatus(warning.getStatus());
        warningAdminEditDto.setId(warning.getId());
        warningAdminEditDto.setSerieId(warning.getSerie().getId());
        warningAdminEditDto.setSerieName(warning.getNameOfSeries());
        warningAdminEditDto.setWarningNumber(warning.getWarningNumber());
        warningAdminEditDto.setYear(warning.getYear());
        warningAdminEditDto.setSerieType(warning.getSerie().getType());

        warningAdminEditDto.setModelName(warning.getModelName());

        // order general areas by position
        warningAdminEditDto.setGeneralAreas(
            warning.getGeneralAreas().stream()
                .map(GeneralArea::toDto).sorted(GeneralAreaDto::compareTo)
                .collect(Collectors.toCollection(LinkedHashSet::new))
        );

        // order localities by position
        warningAdminEditDto.setLocalityNames(warning.getLocalities().stream().map(WarningLocalityNames::toDto).collect(toSet()));

        // filter areas by id from generalAreaIds
        SortedSet<GeneralArea> generalAreas = warning.getGeneralAreas();
        Set<GeneralArea> selectedGeneralAreas = generalAreas.stream().filter(
            generalArea -> warningAdminEditDto.getGeneralAreas().stream().anyMatch(generalAreaDto -> generalAreaDto.getId().equals(generalArea.getSubArea().getId()))
        ).collect(toSet());

        Map<String, Set<SubAreaDetailsDto>> selectedLocalitiesByAreaId = new HashMap<>();

        // group localities names by parent sector
        selectedGeneralAreas.forEach(generalArea -> {
            SubArea subArea = generalArea.getSubArea();
            Set<WarningLocalityNames> localities = warning.getLocalities().stream()
                .filter(warningLocalityNames -> warningLocalityNames.getParent().getId().equals(subArea.getId()))
                .collect(toSet());
            Set<SubAreaDetailsDto> subAreaDetailsDtos = localities.stream()
                .map(locality -> SubAreaDetailsDto.builder()
                    .name(locality.getName())
                    .internationalName(ofNullable(locality.getInternationalName()).orElse(locality.getName()))
                    .build())
                .collect(toSet());
            selectedLocalitiesByAreaId.put(subArea.getId().toString(), subAreaDetailsDtos);
        });
        warningAdminEditDto.setSelectedLocalities(selectedLocalitiesByAreaId);

        warningAdminEditDto.setTypeGeneral(warning.getWarningHazardTypeGeneral());

        // links
        warningAdminEditDto.setCancelledWarningIds(getReferencesByCategory(warning, WarningReferenceCategoryEnum.CANCELLATION));
        warningAdminEditDto.setReplacedWarningIds(getReferencesByCategory(warning, WarningReferenceCategoryEnum.REPLACEMENT));
        warningAdminEditDto.setParentWarningIds(getReferencesByCategory(warning, WarningReferenceCategoryEnum.SOURCE_REFERENCE));
        warningAdminEditDto.setInfosourcesIds(getLinkedInfosources(warning).stream().map(InfosourceReferenceDto::getId).collect(toList()));
        warningAdminEditDto.setInfosources(getLinkedInfosources(warning));
        warningAdminEditDto.setMaritimeEntitiesIds(getLinkedMaritimeEntities(warning));
        warningAdminEditDto.setGeoregulationIds(getGeoregulations(warning));

        // parts
        warning.getFeatureParts().forEach(featurePart -> {
            WarningFeaturePartCreateDto warningFeaturePartCreateDto = new WarningFeaturePartCreateDto();
            warningFeaturePartCreateDto.setGeometries(featurePart.getGeometries().stream().map(FeatureDto::new).collect(toSet()));
            warningFeaturePartCreateDto.setAffectedAreas(featurePart.getAffectedAreas().stream().map(FeatureDto::new).collect(toSet()));
            warningFeaturePartCreateDto.setTypeDetails(featurePart.getWarningHazardTypeDetails());
            warningFeaturePartCreateDto.setInformation(
                featurePart.getInformation().stream().collect(Collectors.toMap(LocalizedText::getLanguage, LocalizedText::getText))
            );
            warningFeaturePartCreateDto.setDateRanges(List.copyOf(featurePart.getFixedDateRanges()));
            warningAdminEditDto.getParts().add(warningFeaturePartCreateDto);
        });


        // attachments
        warningAdminEditDto.setAttachmentIds(
            warning.getAttachments().stream().map(warningAttachment -> warningAttachment.getAttachment().getId()).collect(toList())
        );

        Set<AttachmentAdminDetailsDto> attachmentFiles = new HashSet<>();
        warning.getAttachments().forEach(warningAttachment -> {
            AttachmentAdminDetailsDto attachmentAdminDetailsDto = new AttachmentAdminDetailsDto();
            attachmentAdminDetailsDto.setUuid(warningAttachment.getAttachment().getId());
            attachmentAdminDetailsDto.setIdentifier(warningAttachment.getAttachment().getIdentifier());
            attachmentAdminDetailsDto.setFilename(warningAttachment.getAttachment().getFilename());
            attachmentAdminDetailsDto.setTotalSize(warningAttachment.getAttachment().getTotalSize());
            attachmentAdminDetailsDto.setType(warningAttachment.getAttachment().getType());
            attachmentFiles.add(attachmentAdminDetailsDto);
        });
        warningAdminEditDto.setAttachmentFiles(attachmentFiles);

        // references
        warningAdminEditDto.setWarningPublicsReferences(getWarningReferencesAsPublicReference(warning));
        warningAdminEditDto.setGeoregulationReferenceIds(getGeoregulationsReferences(warning));
        warningAdminEditDto.setAttachmentReferenceIds(getAttachmentsReferences(warning));
        warningAdminEditDto.setEntityReferenceIds(getEntitiesReferences(warning));
        warningAdminEditDto.setWarningLinksPublicsReferences(getWarningLinksPublicsReferences(warning));

        // diffusion
        warningAdminEditDto.setTelex(warning.getTelex());
        warningAdminEditDto.setNavtex(warning.getNavtex());
        warningAdminEditDto.setNavtexConfig(warning.getNavtexConfig());

        // additional infos
        warningAdminEditDto.setCreationDate(warning.getCreationDate());
        warningAdminEditDto.setEstimatedPublicationDate(warning.getEstimatedPublicationDate());
        warningAdminEditDto.setReviewDate(warning.getReviewDate());
        warningAdminEditDto.setCancellationTime(warning.getCancellationDate());
        warningAdminEditDto.setAddCancellationTimeToTelex(warning.isAddCancellationTimeToTelex());
        warningAdminEditDto.setInternalComment(warning.getInternalComment());
        warningAdminEditDto.setBathy(warning.getIsBathy());

        warningAdminEditDto.setLastUpdate(warning.getLastUpdate());

        if (warning.getLastUpdateUser() != null) {
            warningAdminEditDto.setLastUpdateUser(AuthenticatedServiceUserDto.fromServiceUser((ServiceUser) warning.getLastUpdateUser()));
        }
        return warningAdminEditDto;
    }

    private static Predicate<WarningReference> predicateByCategory(WarningReferenceCategoryEnum category) {
        return w -> w.getCategory().equals(category);
    }

    public static List<UUID> getReferencesByCategory(Warning warning, WarningReferenceCategoryEnum category) {
        return warning.getWarningReferences().stream()
            .filter(predicateByCategory(category))
            .map(WarningReference::getWarnings)
            .flatMap(w -> w.stream())
            .map(Warning::getId)
            .collect(toList());
    }

    private static Set<InfosourceReferenceDto> getLinkedInfosources(Warning warning) {
        return warning.getInfosources().stream()
            .map(InfosourceReferenceDto::fromInfosource)
            .collect(toSet());
    }

    private static List<UUID> getLinkedMaritimeEntities(Warning warning) {
        return warning.getMaritimeEntities().stream()
            .map(MaritimeEntity::getId)
            .collect(toList());
    }

    private static List<UUID> getGeoregulations(Warning warning) {
        return warning.getGeoregulations().stream()
            .map(warningGeoregulation -> warningGeoregulation.getGeoregulation().getId())
            .collect(toList());
    }

    private static Set<WarningReferenceAsPublicReferenceDto> getWarningReferencesAsPublicReference(Warning warning) {
        return warning.getWarningsPublicsReferences().stream()
            .map(WarningReferenceAsPublicReferenceDto::fromWarningPublicReference)
            .collect(toSet());
    }

    private static Set<UUID> getGeoregulationsReferences(Warning warning) {
        return warning.getGeoregulations().stream()
            .filter(WarningGeoregulation::getIsPublicReference)
            .map(warningGeoregulation -> warningGeoregulation.getGeoregulation().getId())
            .collect(toSet());
    }

    private static Set<UUID> getAttachmentsReferences(Warning warning) {
        return warning.getAttachments().stream()
            .filter(WarningAttachment::getIsPublicReference)
            .map(warningAttachment -> warningAttachment.getAttachment().getId())
            .collect(toSet());
    }

    private static Set<UUID> getEntitiesReferences(Warning warning) {
        return warning.getEntityReferences().stream()
            .map(MaritimeEntityReference::getId)
            .collect(toSet());
    }

    private static Set<WarningLinkDto> getWarningLinksPublicsReferences(Warning warning) {
        return warning.getWarningLinksPublicsReferences().stream().map(WarningLink::asDto).collect(toSet());
    }
}
