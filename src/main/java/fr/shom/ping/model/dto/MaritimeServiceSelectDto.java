package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.MaritimeService;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
@Schema(name = "Maritime Service Select Dto")
public class MaritimeServiceSelectDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -4916126165633810771L;

    private UUID id;

    @NoHtml
    private String name;

    @NoHtml
    private String internationalName;

    public MaritimeServiceSelectDto(MaritimeService service) {
        id = service.getId();
        name = service.getName();
        internationalName = service.getInternationalName();
    }
}
