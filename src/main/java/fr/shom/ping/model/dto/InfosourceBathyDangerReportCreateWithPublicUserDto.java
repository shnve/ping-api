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
@Schema(name = "Infosource Create Dto (Bathy Danger Report) (Public User)")
public class InfosourceBathyDangerReportCreateWithPublicUserDto extends InfosourceBathyDangerReportCreateDto {
    @Serial
    private static final long serialVersionUID = -3348523726395306296L;

    public InfosourceBathyDangerReportCreateWithPublicUserDto(Infosource infosource) {
        super(infosource);
    }

    public Infosource toInfosource(List<Attachment> attachments, PublicUser publicUser) {
        return super.toInfosourceBuilder(attachments)
            .issuingPublicUser(publicUser)
            .build();
    }
}
