package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.ProUser;
import fr.shom.ping.model.enums.UserStatus;
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
@Schema(name = "ProUser Search DTO Model")
public class ProUserSearchDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 3563573630874564348L;

    private UUID id;

    @NoHtml
    private String firstname;

    @NoHtml
    private String lastname;

    private String email;

    private UserStatus status;

    public static ProUserSearchDto fromProUser(ProUser proUser) {
        return ProUserSearchDto.builder().id(proUser.getId()).lastname(proUser.getLastname()).firstname(proUser.getFirstname())
            .email(proUser.getEmail()).status(proUser.getStatus()).build();
    }
}
