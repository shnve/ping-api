package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.ServiceUser;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.stream.Collectors.toList;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Infosource Create Dto (Activity Forecast) (Service User)")
public class InfosourceActivityForecastCreateWithServiceUserDto extends AbstractInfosourceActivityForecastCreateDto {
    @Serial
    private static final long serialVersionUID = -6815093992919967523L;

    @NotNull(message = "validation.infosource.activity-forecast.service-id.not-null")
    private UUID serviceId;

    @Default
    private List<UUID> concernedMaritimeEntityIds = new ArrayList<>();

    private String internalComment;

    public InfosourceActivityForecastCreateWithServiceUserDto(Infosource infosource) {
        super(infosource);
        this.serviceId = infosource.getIssuingService().getId();
        this.concernedMaritimeEntityIds = infosource.getEntities().stream().map(MaritimeEntity::getId).collect(toList());
        this.internalComment = infosource.getComment();
    }

    public Infosource toInfosource(List<Attachment> attachments, ServiceUser serviceUser, MaritimeService service, List<MaritimeEntity> entities) {
        return super.toInfosourceBuilder(attachments)
            .issuingService(service)
            .issuingServiceUser(serviceUser)
            .entities(new HashSet<>(entities))
            .comment(this.internalComment)
            .build();
    }
}
