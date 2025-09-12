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
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@Schema(name = "Infosource Create Dto (Map Anomaly) (Public User)")
public class InfosourceMapAnomalyCreateWithPublicUserDto extends InfosourceMapAnomalyAbstractCreateDto {
    @Serial
    private static final long serialVersionUID = 3364955233513011189L;

    public InfosourceMapAnomalyCreateWithPublicUserDto(Infosource infosource) {
        super(infosource);
    }

    public Infosource toInfosource(PublicUser publicUser, List<Attachment> attachments) {
        return super.toInfosourceBuilder(attachments)
            .issuingPublicUser(publicUser)
            .build();
    }
}
