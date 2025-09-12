package fr.shom.ping.model.dto;

import fr.shom.ping.model.MaritimeService;
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
public class MaritimeServiceSharedDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 5461008074230596832L;

    private UUID id;

    @NoHtml
    private String name;

    public static MaritimeServiceSharedDto fromMaritimeService(MaritimeService service) {
        return MaritimeServiceSharedDto.builder()
            .id(service.getId())
            .name(service.getName())
            .build();
    }
}
