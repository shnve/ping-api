package fr.shom.ping.model.dto;

import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.PublicUser;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@Schema(name = "Infosource Create Dto (Hazard Report) (Public User)")
public class InfosourceHazardReportCreateWithPublicUserDto extends InfosourceHazardReportAbstractCreateDto {
    @Serial
    private static final long serialVersionUID = 7413025341826728712L;

    public InfosourceHazardReportCreateWithPublicUserDto(Infosource infosource) {
        super(infosource);
    }

    public Infosource toInfosource(PublicUser publicUser, List<Attachment> attachments) {
        return super.toInfosourceBuilder(attachments)
            .issuingPublicUser(publicUser)
            .build();
    }
}
