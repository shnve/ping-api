package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.EMPTY_LIST_EXAMPLE;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Study Dto Model")
public abstract class AbstractStudyDto {
    @NotNull(message = "validation.study.folder-id.not-null")
    @Schema(description = "Folder id", requiredMode = Schema.RequiredMode.REQUIRED)
    protected UUID folderId;

    @NotNull(message = "validation.study.infonaut-number.not-null")
    @Schema(description = "Infonaut Number", requiredMode = Schema.RequiredMode.REQUIRED)
    @NoHtml
    protected String number;

    @Schema(description = "List of notions Ids", example = EMPTY_LIST_EXAMPLE)
    @Builder.Default
    protected Set<UUID> notionsIds = new HashSet<>();

    @Schema(description = "List of attachments Ids", example = EMPTY_LIST_EXAMPLE)
    @Builder.Default
    protected Set<UUID> attachmentsIds = new HashSet<>();

    @Schema(description = "References")
    private Set<@Valid StudyReferenceDto> references;
}
