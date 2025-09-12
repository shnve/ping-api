package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.StudyReferencesEnum;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(NON_NULL)
@Schema(name = "Reference Folder Study Dto Model")
public class ReferenceFolderStudyDto implements Serializable {

    @Serial
    private static final long serialVersionUID = -2076132130111056739L;

    private UUID studyId;

    private StudyReferencesEnum type;

    @NoHtml
    private String value;
}
