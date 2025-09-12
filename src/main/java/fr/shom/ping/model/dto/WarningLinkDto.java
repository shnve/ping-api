package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.WarningLink;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Warning link Dto Model")
public class WarningLinkDto extends AbstractWarningLinkDto {

    @Schema(description = ID_DESCRIPTION)
    private UUID id;

    public WarningLinkDto(WarningLink warningLink) {
        this.id = warningLink.getId();
        this.title = warningLink.getTitle();
        this.internationalTitle = warningLink.getInternationalTitle();
        this.url = warningLink.getUrl();
    }
}
