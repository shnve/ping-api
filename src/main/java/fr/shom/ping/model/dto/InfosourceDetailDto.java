package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Infosource;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
public class InfosourceDetailDto extends AbstractInfosourceDetailDto {

    private UUID issuingPublicUserId;

    public InfosourceDetailDto fromInfosource(Infosource infosource) {
        AbstractInfosourceDetailDto.fromInfosource(this, infosource);
        setIssuingPublicUserId(infosource.getIssuingPublicUser().getId());
        return this;
    }
}
