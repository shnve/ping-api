package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.enums.WarningStatus;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.utils.SissyUtil.LINKED_ENTITY_STATUS_PROPERTY;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
public class SissyWarningDto extends AbstractSissyLinkedEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1690252098264502479L;

    @JsonProperty(value = LINKED_ENTITY_STATUS_PROPERTY)
    private WarningStatus status;

    public static SissyWarningDto fromWarning(Warning warning) {
        final SissyWarningDto sissyWarningDto = SissyWarningDto.builder()
            .status(warning.getStatus())
            .build();
        AbstractSissyLinkedEntity.fromWarning(sissyWarningDto, warning);
        return sissyWarningDto;
    }
}
