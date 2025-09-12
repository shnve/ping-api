package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.utils.PingStringUtil.REGEX_PATTERN_NULL_OR_NOT_BLANK;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
@Schema(name = "Update infosource comment Dto Model")
public class UpdateInfosourceCommentDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -5303342361801513660L;

    @Pattern(regexp = REGEX_PATTERN_NULL_OR_NOT_BLANK, flags = { Pattern.Flag.DOTALL }, message = "validation.infosource.internal-comment.null-or-not-blank")
    @NoHtml
    private String comment;
}
