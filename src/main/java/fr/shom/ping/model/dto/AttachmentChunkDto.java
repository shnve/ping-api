package fr.shom.ping.model.dto;

import fr.shom.ping.validation.ValidFileExtension;
import fr.shom.ping.validation.ValidFileSize;
import java.io.Serial;
import java.io.Serializable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import static fr.shom.ping.utils.PingStringUtil.REGEX_PATTERN_FILE_IDENTIFIER;
import static fr.shom.ping.utils.PingStringUtil.REGEX_PATTERN_FILE_NAME;

@Data
public class AttachmentChunkDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 4925934629360919051L;

    public static final String VALIDATION_INVALID_FILENAME_MSG = "validation.attachment.filename.invalid-pattern";

    public static final String VALIDATION_INVALID_RELATIVE_PATH_MSG = "validation.attachment.relative-path.invalid-pattern";

    private Integer flowChunkNumber;

    private Long flowChunkSize;

    private Long flowCurrentChunkSize;
    private Long flowTotalSize;

    @Pattern(regexp = REGEX_PATTERN_FILE_IDENTIFIER, message = "validation.attachment.flow-identifier.invalid-pattern")
    @NotEmpty
    private String flowIdentifier;

    @ValidFileExtension
    @Pattern(regexp = REGEX_PATTERN_FILE_NAME, message = VALIDATION_INVALID_FILENAME_MSG)
    @NotEmpty
    private String flowFilename;

    @Pattern(regexp = REGEX_PATTERN_FILE_NAME, message = VALIDATION_INVALID_RELATIVE_PATH_MSG)
    @NotEmpty
    private String flowRelativePath;

    private Integer flowTotalChunks;

    private MultipartFile file;
}
