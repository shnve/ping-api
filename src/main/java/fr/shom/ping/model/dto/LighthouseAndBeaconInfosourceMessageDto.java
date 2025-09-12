package fr.shom.ping.model.dto;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import fr.shom.ping.validation.NoHtml;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.Valid;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LighthouseAndBeaconInfosourceMessageDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 2093613475845687980L;

    @NoHtml
    private String number;

    private Instant validationTime;

    @NoHtml
    private String timezone;

    @NoHtml
    private Map<String, Object> object;

    private List<@Valid LighthouseAndBeaconInfosourcePartMessageDto> parts;
}
