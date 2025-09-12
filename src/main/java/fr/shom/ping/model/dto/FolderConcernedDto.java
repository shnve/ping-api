package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Folder;
import fr.shom.ping.utils.FolderUtil;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import fr.shom.ping.validation.NoHtml;
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
public class FolderConcernedDto implements Serializable, Comparable<FolderConcernedDto> {
    @Serial
    private static final long serialVersionUID = -7795875103064167703L;

    private UUID id;

    @NoHtml
    private String number;

    public FolderConcernedDto(Folder folder) {
        id = folder.getId();
        number = FolderUtil.formatFolderNumberYearIssuingService(folder);
    }

    public static FolderConcernedDto from(Folder folder) {
        return FolderConcernedDto.builder()
            .id(folder.getId())
            .number(FolderUtil.formatFolderNumberYearIssuingService(folder))
            .build();
    }

    @Override
    public int compareTo(FolderConcernedDto folderConcernedDto) {
        return number.compareTo(folderConcernedDto.getNumber());
    }
}
