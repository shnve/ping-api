package fr.shom.ping.model.dto;

import fr.shom.ping.model.Area;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.ServiceUserRole;
import fr.shom.ping.model.enums.MaritimeServiceType;
import fr.shom.ping.model.enums.ServiceNetwork;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Schema(name = "AuthenticatedServiceUserDto Model")
public class AuthenticatedServiceUserDto extends AuthenticatedProUserDto {
    @Serial
    private static final long serialVersionUID = 7242392867084890770L;

    @Schema(description = "Service ID", required = true)
    private UUID serviceId;

    @Schema(description = "Service name", required = true)
    private String serviceName;

    @Schema(description = "Role by service", required = true)
    private Set<ServiceUserRole> serviceRoles;

    @Schema(description = "Notice producer", required = true)
    private boolean noticeProducer;

    @Schema(description = "Service Network")
    private ServiceNetwork serviceNetwork;

    @Schema(description = "Service maritime type")
    private MaritimeServiceType serviceType;

    public static AuthenticatedServiceUserDto fromServiceUser(ServiceUser serviceUser) {
        return AuthenticatedServiceUserDto.builder()
            .id(serviceUser.getId())
            .title(serviceUser.getTitle())
            .firstname(serviceUser.getFirstname())
            .lastname(serviceUser.getLastname())
            .serviceId(serviceUser.getService().getId())
            .serviceName(serviceUser.getService().getName())
            .serviceNetwork(serviceUser.getService().getServiceNetwork())
            .noticeProducer(serviceUser.getService().isMaritimeNoticeProducer())
            .role(serviceUser.getRole().name())
            .serviceRoles(serviceUser.getRoles())
            .preferredAreaId(Optional.ofNullable(serviceUser.getPreferredArea()).map(Area::getId).orElse(null))
            .serviceType(serviceUser.getService().getType())
            .hasNotificationsSound(serviceUser.hasNotificationsSound())
            .build();
    }


}
