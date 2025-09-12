package fr.shom.ping.model;

import fr.shom.ping.model.dto.StudyReferenceDto;
import fr.shom.ping.model.enums.StudyReferencesEnum;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static fr.shom.ping.utils.SchemaUtil.STUDY_REFERENCE_TABLE;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Reference model of folders")
@Table(name = STUDY_REFERENCE_TABLE)
public class StudyReference implements Serializable {

    @Serial
    private static final long serialVersionUID = -1019441437065621706L;

    @NotNull(message = "validation.folder.reference.type.not-null")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyReferencesEnum type;

    @NotEmpty(message = "validation.folder.reference.value.not-empty")
    @NoHtml
    @NotBlank
    @Column(nullable = false)
    private String value;

    public StudyReference(StudyReferenceDto dto) {
        this.type = dto.getType();
        this.value = dto.getValue().trim();
    }

    public StudyReferenceDto asDto() {
        return new StudyReferenceDto(this);
    }
}
