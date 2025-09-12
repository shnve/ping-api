package fr.shom.ping.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import java.util.UUID;
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
public class ReferencesByFolderDto {

     private UUID folderId;

     private String studyNumber;

     private Set<StudyReferenceDto> studyReferences;
}
