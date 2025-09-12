package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.InfosourceRecipient;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.ServiceUserRole;
import fr.shom.ping.validation.NoHtml;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.model.enums.InfosourceStatus.DRAFT;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_ADMIN;
import static fr.shom.ping.model.enums.ServiceRole.SERVICE_OPERATOR;
import static java.util.Arrays.asList;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
public class InfosourceAdminDetailDto extends AbstractInfosourceDetailDto {

    @NoHtml
    private String issuingServiceName;

    private UUID issuingServiceId;

    private UUID issuingServiceUserId;

    private UUID issuingPublicUserId;

    private boolean canBeTransfered;

    private boolean canBeAddedToFolder;

    private String timezone;

    public InfosourceAdminDetailDto fromInfosource(Infosource infosource, ServiceUser serviceUser) {
        AbstractInfosourceDetailDto.fromInfosource(this, infosource);
        if (infosource.getIssuingService() != null) {
            this.setIssuingServiceName(infosource.getIssuingService().getName());
            this.setIssuingServiceId(infosource.getIssuingService().getId());
            if (infosource.getIssuingServiceUser() != null) {
                this.setIssuingServiceUserId(infosource.getIssuingServiceUser().getId());
            }
        } else if (infosource.getIssuingPublicUser() != null) {
            this.setIssuingPublicUserId(infosource.getIssuingPublicUser().getId());
        }

        this.timezone = infosource.getTimezone();

        this.canBeTransfered = infosource.getStatus() != DRAFT && serviceUser.getRoles().stream()
            .filter(serviceUserRole -> asList(SERVICE_OPERATOR, SERVICE_ADMIN).contains(serviceUserRole.getServiceRole()))
            .map(ServiceUserRole::getServiceId)
            .anyMatch(serviceId -> infosource.getRecipients().stream()
                .map(InfosourceRecipient::getService)
                .map(MaritimeService::getId)
                .collect(Collectors.toList())
                .contains(serviceId));

        return this;
    }
}
