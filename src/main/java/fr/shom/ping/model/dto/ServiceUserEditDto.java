package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.model.enums.ServiceRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "ServiceUser Edit DTO Model")
public class ServiceUserEditDto extends ProUserEditDto {
    @Serial
    private static final long serialVersionUID = -6331866873106149303L;

    private UUID serviceId;
    private ServiceRole serviceRole;

    public ServiceUserEditDto(ServiceUser serviceUser) {
        super(serviceUser);
        serviceId = serviceUser.getService().getId();
        serviceRole = serviceUser.getMainRole();
    }
}
