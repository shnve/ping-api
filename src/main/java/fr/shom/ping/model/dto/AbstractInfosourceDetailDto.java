package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.PublicUser;
import fr.shom.ping.model.ServiceUser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.google.common.collect.Sets.newTreeSet;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
public class AbstractInfosourceDetailDto extends AbstractInfosourceDto {

    @Default
    private SortedSet<InfosourcePartDto> featureParts = newTreeSet();

    private String issuingUserFirstname;

    private String issuingUserLastname;

    private String issuingUserEmail;

    private String issuingUserMobilePhoneNumber;

    private String issuingUserPhoneNumber;

    @Default
    private List<@Valid PhoneNumberDto> issuingUserPhoneNumbers = new ArrayList<>();

    @Default
    private Set<AttachmentDownloadDto> attachments = new HashSet<>();

    public static AbstractInfosourceDetailDto fromInfosource(AbstractInfosourceDetailDto dto, Infosource infosource) {
        AbstractInfosourceDto.fromInfosource(dto, infosource);

        dto.setFeatureParts(new TreeSet<>(
            infosource.getInfosourceParts().stream()
                .map(InfosourcePartDto::fromInfosourcePart)
                .collect(toList())));

        PublicUser issuingPublicUser = infosource.getIssuingPublicUser();
        if (issuingPublicUser != null) {
            dto.setIssuingUserFirstname(issuingPublicUser.getFirstname());
            dto.setIssuingUserLastname(issuingPublicUser.getLastname());
            dto.setIssuingUserEmail(issuingPublicUser.getEmail());
            dto.setIssuingUserMobilePhoneNumber(issuingPublicUser.getMobilePhoneNumber());
            dto.setIssuingUserPhoneNumber(issuingPublicUser.getPhoneNumber());
        } else if (infosource.getIssuingServiceUser() != null) {
            ServiceUser serviceUser = infosource.getIssuingServiceUser();
            dto.setIssuingUserFirstname(serviceUser.getFirstname());
            dto.setIssuingUserLastname(serviceUser.getLastname());
            dto.setIssuingUserEmail(serviceUser.getEmail());
            dto.setIssuingUserPhoneNumbers(Optional.ofNullable(infosource.getIssuingServiceUser().getPhoneNumberList()).orElse(emptyList()).stream()
                .map(PhoneNumberDto::new)
                .collect(toList()));
        }
        dto.setAttachments(infosource.getAttachments().stream()
            .map(attachment -> AttachmentDownloadDto.builder()
                .id(attachment.getId())
                .filename(attachment.getFilename())
                .totalSize(attachment.getTotalSize())
                .build()).collect(Collectors.toSet()));

        return dto;
    }
}
