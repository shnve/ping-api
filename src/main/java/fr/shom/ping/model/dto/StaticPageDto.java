package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.StaticPage;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.UUID;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.INTERNATIONAL_TITLE_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.PATH_DESCRIPTION;
import static fr.shom.ping.SwaggerConfig.Property.TITLE_DESCRIPTION;
import static fr.shom.ping.utils.PingStringUtil.blobAsString;
import static fr.shom.ping.utils.PingStringUtil.stringAsBlob;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
@Builder
@Schema(name = "Static page DTO")
public class StaticPageDto implements Serializable {

    @Id
    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID id;

    @NotEmpty(message = "validation.static-page.title.not-empty")
    @Schema(description = TITLE_DESCRIPTION, required = true)
    @NoHtml
    private String title;

    @NotEmpty(message = "validation.static-page.international-title.not-empty")
    @Schema(description = INTERNATIONAL_TITLE_DESCRIPTION, required = true)
    @NoHtml
    private String internationalTitle;

    @NotEmpty(message = "validation.static-page.path.not-empty")
    @Pattern(regexp = "^[a-z\\d-]+$", message = "validation.static-page.path.invalid-pattern")
    @Schema(description = PATH_DESCRIPTION, required = true)
    @NoHtml
    private String path;

    @Builder.Default
    private Boolean locked = false;

    private String content;

    private String internationalContent;

    public StaticPageDto(StaticPage staticPage) {
        id = staticPage.getId();
        title = staticPage.getTitle();
        internationalTitle = staticPage.getInternationalTitle();
        path = staticPage.getPath();
        locked = staticPage.getLocked();
        content = blobAsString(staticPage.getContent());
        internationalContent = blobAsString(staticPage.getInternationalContent());
    }

    public StaticPage toStaticPage() {
        return StaticPage.builder()
            .id(id)
            .title(title)
            .internationalTitle(internationalTitle)
            .path(path)
            .locked(locked)
            .content(stringAsBlob(content))
            .internationalContent(stringAsBlob(internationalContent))
            .build();
    }
}
