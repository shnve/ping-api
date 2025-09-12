package fr.shom.ping.model.dto;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import fr.shom.ping.validation.NoHtml;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LighthouseAndBeaconInfosourcePartMessageDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 2683931229587389730L;

    @NoHtml
    private Map<String, Object> object;
}
