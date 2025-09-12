package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.MaritimeEntity;
import fr.shom.ping.validation.NoHtml;
import java.io.Serial;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
public class MaritimeEntityConcernedDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -5392965167176697214L;
    private UUID id;

    @NoHtml
    private String name;

    @NoHtml
    private String internationalName;

    public MaritimeEntityConcernedDto(MaritimeEntity maritimeEntity) {
        id = maritimeEntity.getId();
        name = maritimeEntity.getName();
        internationalName = maritimeEntity.getInternationalName();
    }
}
