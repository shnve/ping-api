package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.PhoneNumber;
import fr.shom.ping.validation.NoHtml;
import fr.shom.ping.validation.ValidPhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import jakarta.validation.constraints.NotEmpty;
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
@Schema(name = "PhoneNumberDto Model")
public class PhoneNumberDto {

    @Schema(description = "id")
    private UUID id;

    @Schema(description = "Description", required = true)
    @NotEmpty(message = "validation.phone.description.not-empty")
    @NoHtml
    private String description;

    @Schema(description = "Phone number (Should contain country code)", example = "+33101010101")
    @ValidPhoneNumber
    @NoHtml
    private String number;

    public PhoneNumberDto(PhoneNumber phoneNumber) {
        description = phoneNumber.getDescription();
        number = phoneNumber.getNumber();
    }
}
