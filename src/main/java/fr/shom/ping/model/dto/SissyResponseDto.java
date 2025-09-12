package fr.shom.ping.model.dto;

import fr.shom.ping.validation.NoHtml;
import lombok.Builder;
import org.springframework.http.HttpStatus;

@Builder
public class SissyResponseDto {
    private HttpStatus status;

    @NoHtml
    private String message;
}
