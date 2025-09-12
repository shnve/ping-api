package fr.shom.ping.model.dto;

import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.model.ServiceUser;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static fr.shom.ping.utils.PingStringUtil.REGEX_PATTERN_NULL_OR_NOT_BLANK;
import static java.util.stream.Collectors.toList;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@Schema(name = "Infosource Create Dto (Future Updates) (Service User)")
public class InfosourceFutureUpdatesCreateWithServiceUserDto extends InfosourceFutureUpdatesAbstractCreateDto implements InfosourceCreateReport {
    @Serial
    private static final long serialVersionUID = 8136362692913916935L;

    @NotNull(message = "validation.infosource.future-updates.service-id.not-null")
    protected UUID serviceId;

    @Default
    private List<UUID> concernedMaritimeEntityIds = new ArrayList<>();

    @Pattern(regexp = REGEX_PATTERN_NULL_OR_NOT_BLANK, flags = { Pattern.Flag.DOTALL }, message = "validation.infosource.future-updates.internal-comment.null-or-not-blank")
    @NoHtml
    protected String internalComment;

    public InfosourceFutureUpdatesCreateWithServiceUserDto(Infosource infosource) {
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
            .comment(internalComment)
            .build();
    }
}
