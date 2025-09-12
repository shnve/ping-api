package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.PublicUser;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.enums.InfosourceNature;
import fr.shom.ping.model.enums.InfosourceStatus;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.Sets.newTreeSet;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.model.enums.GlobalWarningType.NOTICE_TP;
import static java.util.stream.Collectors.toList;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
public class InfosourceReferenceDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -684287689899861947L;

    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @NoHtml
    private String number;

    private InfosourceStatus status;

    private InfosourceNature nature;

    private Instant validationTime;

    private Instant creationDate;

    private Instant lastUpdate;

    @Default
    private SortedSet<@Valid InfosourcePartDto> featureParts = newTreeSet();

    @NoHtml
    private String issuingServiceName;

    @NoHtml
    private String issuingUserFirstname;

    @NoHtml
    private String issuingUserLastname;

    @Default
    private Set<@Valid AttachmentDownloadDto> attachments = new HashSet<>();

    @Default
    private Set<@Valid WarningReferenceDto> warnings = new HashSet<>();

    @Default
    private Set<@Valid WarningReferenceDto> warningsTP = new HashSet<>();

    @NoHtml
    private String timezone;

    public static InfosourceReferenceDto fromInfosource(InfosourceReferenceDto dto, Infosource infosource) {
        dto.setId(infosource.getId());
        dto.setNumber(infosource.getNumber());
        dto.setNature(infosource.getNature());
        dto.setStatus(infosource.getStatus());
        dto.setValidationTime(infosource.getValidationTime());
        dto.setCreationDate(infosource.getCreationDate());
        dto.setLastUpdate(infosource.getLastUpdate());
        dto.setFeatureParts(new TreeSet<>(
            infosource.getInfosourceParts().stream()
                .map(InfosourcePartDto::fromInfosourcePart)
                .collect(toList())));
        dto.setWarnings(infosource.getLinkedWarnings().stream()
            .filter(w -> !w.getSerie().getType().getGlobalWarningType().equals(NOTICE_TP))
            .map(WarningReferenceDto::fromWarning)
            .collect(Collectors.toSet()));
        dto.setWarningsTP(infosource.getLinkedWarnings().stream()
            .filter(w -> w.getSerie().getType().getGlobalWarningType().equals(NOTICE_TP))
            .map(WarningReferenceDto::fromWarning)
            .collect(Collectors.toSet()));

        PublicUser issuingPublicUser = infosource.getIssuingPublicUser();
        if (issuingPublicUser != null) {
            dto.setIssuingUserFirstname(issuingPublicUser.getFirstname());
            dto.setIssuingUserLastname(issuingPublicUser.getLastname());
        } else {
            ServiceUser serviceUser = infosource.getIssuingServiceUser();
            if (serviceUser != null) {
                dto.setIssuingUserFirstname(serviceUser.getFirstname());
                dto.setIssuingUserLastname(serviceUser.getLastname());
            }
        }
        dto.setAttachments(infosource.getAttachments().stream()
            .map(attachment -> AttachmentDownloadDto.builder()
                .id(attachment.getId())
                .filename(attachment.getFilename())
                .totalSize(attachment.getTotalSize())
                .build()).collect(Collectors.toSet()));
        if (infosource.getIssuingService() != null) {
            dto.setIssuingServiceName(infosource.getIssuingService().getName());
        }
        dto.setTimezone(infosource.getTimezone());
        return dto;
    }

    public static InfosourceReferenceDto fromInfosource(Infosource infosource) {
        return fromInfosource(InfosourceReferenceDto.builder().build(), infosource);
    }
}
