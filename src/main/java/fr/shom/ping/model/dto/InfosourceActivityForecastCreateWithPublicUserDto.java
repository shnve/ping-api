package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Infosource Create Dto (Activity Forecast) (Public User)")
public class InfosourceActivityForecastCreateWithPublicUserDto extends AbstractInfosourceActivityForecastCreateDto {
    @Serial
    private static final long serialVersionUID = -5053611700764928387L;

    public InfosourceActivityForecastCreateWithPublicUserDto(Infosource infosource) {
        super(infosource);
    }

    public Infosource toInfosource(PublicUser publicUser, List<Attachment> attachments) {
        return super.toInfosourceBuilder(attachments)
            .issuingPublicUser(publicUser)
            .build();
    }
}
