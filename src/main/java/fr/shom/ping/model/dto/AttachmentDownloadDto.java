package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.WarningAttachment;
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
public class AttachmentDownloadDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -5828346985589849387L;

    private UUID id;

    @NoHtml
    private String filename;

    private Long totalSize;

    public AttachmentDownloadDto(Attachment attachment) {
        id = attachment.getId();
        filename = attachment.getFilename();
        totalSize = attachment.getTotalSize();
    }

    public static AttachmentDownloadDto fromWarningAttachment(WarningAttachment warningAttachment) {
        return new AttachmentDownloadDto(warningAttachment.getAttachment());
    }
}
