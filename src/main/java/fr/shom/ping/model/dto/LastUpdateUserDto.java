package fr.shom.ping.model.dto;

import fr.shom.ping.validation.NoHtml;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LastUpdateUserDto {
    @NoHtml
    private String lastname;

    @NoHtml
    private String firstname;
}
