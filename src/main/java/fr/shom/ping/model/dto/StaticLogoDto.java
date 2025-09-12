package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.StaticLogo;
import fr.shom.ping.validation.NoHtml;
import fr.shom.ping.validation.ValidLogoHeight;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.utils.PingStringUtil.blobAsString;
import static fr.shom.ping.utils.PingStringUtil.stringAsBlob;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
public class StaticLogoDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 4483889334570764724L;

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @NotBlank(message = "validation.static-logo.key.not-blank")
    @NoHtml
    private String key;

    @NotBlank(message = "validation.static-logo.description.not-blank")
    @NoHtml
    private String description;

    @NotBlank(message = "validation.static-logo.height-with-unit.not-blank")
    @Pattern(regexp = "^\\d+px$", message = "validation.static-logo.height-with-unit.invalid-pattern")
    @NoHtml
    @ValidLogoHeight
    private String heightWithUnit;

    @NotBlank(message = "validation.static-logo.description.not-blank")
    @NoHtml
    private String content;

    @NotBlank(message = "validation.static-logo.alt-txt.not-blank")
    @NoHtml
    private String altTxt;

    @NotBlank(message = "validation.static-logo.alt-txt-international.not-blank")
    @NoHtml
    private String altTxtInternational;

    @NoHtml
    private String title;

    @NoHtml
    private String titleInternational;

    @NoHtml
    private String href;

    @Builder.Default
    private Boolean locked = false;

    public StaticLogoDto(StaticLogo staticLogo) {
        id = staticLogo.getId();
        key = staticLogo.getKey();
        description = staticLogo.getDescription();
        heightWithUnit = staticLogo.getHeightWithUnit();
        content = blobAsString(staticLogo.getContent());
        altTxt = staticLogo.getAltTxt();
        altTxtInternational = staticLogo.getAltTxtInternational();
        title = staticLogo.getTitle();
        titleInternational = staticLogo.getTitleInternational();
        href = staticLogo.getHref();
        locked = staticLogo.getLocked();
    }

    public StaticLogo toStaticLogo() {
        return StaticLogo.builder()
            .id(id)
            .key(key)
            .description(description)
            .heightWithUnit(heightWithUnit)
            .content(stringAsBlob(content))
            .altTxt(altTxt)
            .altTxtInternational(altTxtInternational)
            .title(title)
            .titleInternational(titleInternational)
            .href(href)
            .locked(locked)
            .build();
    }
}
