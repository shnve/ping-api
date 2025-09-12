package fr.shom.ping.model.dto;

import fr.shom.ping.model.enums.StudyReferencesEnum;
import java.util.UUID;

public interface FolderReferenceDto {

     UUID getFolderId();

     String getStudyNumber();

     StudyReferencesEnum getType();

     String getValue();
}
