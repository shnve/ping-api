package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.shom.ping.model.Serie;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import fr.shom.ping.validation.NoHtml;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SerieInformationTableSubAreaDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -1812940803358321201L;

    @JsonProperty("id")
    private UUID id;

    @NoHtml
    @JsonProperty("name")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String name;

    public SerieInformationTableSubAreaDto(Serie serie) {
        id = serie.getId();
        name = serie.getName();
    }
}
