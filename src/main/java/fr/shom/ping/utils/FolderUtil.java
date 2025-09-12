package fr.shom.ping.utils;

import fr.shom.ping.model.Folder;
import java.time.LocalDate;
import java.time.ZoneId;

public final class FolderUtil {
    private FolderUtil() {}

    public static String formatFolderNumberYearIssuingService(Folder folder) {
        return folder.getNumber() + "/" + (LocalDate.ofInstant(folder.getCreationDate(), ZoneId.systemDefault()).getYear() % 100)
            + " - " + folder.getIssuingService().getName();
    }
}
