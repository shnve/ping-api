package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.NoHtml;
import fr.shom.ping.validation.ValidLanguage;
import java.io.Serial;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.springframework.util.StringUtils.hasText;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Embeddable
public class LocalizedText implements Serializable {
    @Serial
    private static final long serialVersionUID = 3209358343211352923L;

    @ValidLanguage(message = "validation.localized-text.language.invalid")
    @NotNull(message = "validation.localized-text.language.not-null")
    @Column(nullable = false)
    @NoHtml
    private String language;

    @NotEmpty(message = "validation.localized-text.text.not-empty")
    @Column(nullable = false)
    @NoHtml
    private String text;

    public static boolean isEmpty(LocalizedText localizedText) {
        return !hasText(localizedText.getLanguage()) && !hasText(localizedText.getText());
    }
}
