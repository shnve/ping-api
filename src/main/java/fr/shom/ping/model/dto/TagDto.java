package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Tag;
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
@Schema(name = "Tag Dto Model")
public class TagDto extends AbstractTagDto {

    @Schema(description = ID_DESCRIPTION)
    private UUID id;

    public TagDto(Tag tag) {
        this.id = tag.getId();
        this.value = tag.getValue();
        this.maritimeServiceId = tag.getMaritimeService().getId();
    }
}
