package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.utils.PingStringUtil.REGEX_PATTERN_NULL_OR_NOT_BLANK;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
@Schema(name = "Update survey bri internal comment Dto Model")
public class SurveyBriInternalCommentDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -2578566329449939610L;

    @Pattern(regexp = REGEX_PATTERN_NULL_OR_NOT_BLANK, flags = { Pattern.Flag.DOTALL }, message = "validation.survey.bri-internal-comment.null-or-not-blank")
    @NoHtml
    private String briInternalComment;
}
