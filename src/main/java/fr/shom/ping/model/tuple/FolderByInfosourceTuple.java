package fr.shom.ping.model.tuple;

import java.time.Instant;
import java.util.UUID;
import fr.shom.ping.model.enums.FolderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FolderByInfosourceTuple {
    private UUID infosourceId;
    private UUID folderId;
    private Integer folderNumber;
    private Instant folderCreationDate;
    private Instant folderLastUpdate;
    private FolderStatus folderStatus;
}
