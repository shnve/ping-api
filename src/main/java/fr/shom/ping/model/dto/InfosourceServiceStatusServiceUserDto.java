package fr.shom.ping.model.dto;

import fr.shom.ping.model.enums.ActionType;
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
public class InfosourceServiceStatusServiceUserDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 6636582715758873104L;

    private UUID serviceId;

    @NoHtml
    private String serviceName;

    private ActionType actionType;
}
