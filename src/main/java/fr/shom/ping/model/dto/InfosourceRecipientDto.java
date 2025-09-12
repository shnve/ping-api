package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.ActionType;
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
@Schema(name = "Maritime Service Infosource Recipient Dto Model")
public class InfosourceRecipientDto implements Serializable {

    @Serial
    private static final long serialVersionUID = -725957188180613536L;

    private UUID id;

    @NoHtml
    private String name;

    @NoHtml
    private String information;

    @NoHtml
    private String localizedInformation;

    private ActionType actionType;

    @NoHtml
    private String transferredBy;
}
