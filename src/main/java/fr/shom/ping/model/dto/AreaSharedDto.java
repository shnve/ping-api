package fr.shom.ping.model.dto;

import fr.shom.ping.model.Area;
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
public class AreaSharedDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1956216857887429368L;

    private UUID id;

    @NoHtml
    private String name;

    @NoHtml
    private String internationalName;

    public static AreaSharedDto fromArea(Area area) {
        return AreaSharedDto.builder()
            .id(area.getId())
            .name(area.getName())
            .internationalName(area.getInternationalName())
            .build();
    }
}
