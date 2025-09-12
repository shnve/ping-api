package fr.shom.ping.model.dto;

import fr.shom.ping.model.StudyReference;
import fr.shom.ping.model.enums.StudyReferencesEnum;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Reference model of folders")
@ToString(callSuper = true)
public class StudyReferenceDto {

    @NotNull(message = "validation.folder.reference.type.not-null")
    private StudyReferencesEnum type;

    @NotEmpty(message = "validation.folder.reference.value.not-empty")
    @NoHtml
    @NotBlank
    private String value;

    public StudyReferenceDto(StudyReference study) {
        this.type = study.getType();
        this.value = study.getValue();
    }
}
