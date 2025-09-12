package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import fr.shom.ping.validation.NoHtml;
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
public class SissyFolderDto extends AbstractSissyLinkedEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = -4790618039433599433L;

    @JsonProperty(value = LINKED_ENTITY_STATUS_PROPERTY)
    @NoHtml
    private String status;
}
