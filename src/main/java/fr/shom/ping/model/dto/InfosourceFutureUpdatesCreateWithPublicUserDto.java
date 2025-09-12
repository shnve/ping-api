package fr.shom.ping.model.dto;

import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.PublicUser;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@Schema(name = "Infosource Create Dto (Future Updates) (Public User)")
public class InfosourceFutureUpdatesCreateWithPublicUserDto extends InfosourceFutureUpdatesAbstractCreateDto {
    @Serial
    private static final long serialVersionUID = -5221479891959529920L;

    public InfosourceFutureUpdatesCreateWithPublicUserDto(Infosource infosource) {
        super(infosource);
    }

    public Infosource toInfosource(PublicUser publicUser, List<Attachment> attachments) {
        return super.toInfosourceBuilder(attachments)
            .issuingPublicUser(publicUser)
            .build();
    }
}
