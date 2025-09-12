package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.utils.PingStringUtil.REGEX_PATTERN_FILE_IDENTIFIER;
import static fr.shom.ping.utils.PingStringUtil.REGEX_PATTERN_FILE_NAME;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Attachment Dto Model")
public abstract class AbstractAttachmentDto implements Serializable, Comparable<AbstractAttachmentDto> {

    @Serial
    private static final long serialVersionUID = 3118785564152410555L;

    @Pattern(regexp = REGEX_PATTERN_FILE_NAME)
    @NotEmpty
    private String filename;

    @Pattern(regexp = REGEX_PATTERN_FILE_IDENTIFIER)
    @NotEmpty
    private String identifier;

    private Long totalSize;

    @NoHtml
    private String type;

    @Builder.Default
    private Boolean isSurvey = false;

    @Builder.Default
    private Boolean isBathy = false;

    @Builder.Default
    private Boolean isTechnicalMetadata = false;

    @Override
    public int compareTo(AbstractAttachmentDto o) {
        return filename.compareTo(o.getFilename());
    }
}
