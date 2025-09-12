package fr.shom.ping.model;

import fr.shom.ping.model.dto.AttachmentChunkDto;
import java.io.Serial;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.web.multipart.MultipartFile;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AttachmentChunk extends AbstractAttachment {
    @Serial
    private static final long serialVersionUID = 4055827902978671276L;

    @Column(nullable = false)
    private Integer chunkNumber;

    @Column(nullable = false)
    private Long chunkSize;

    @Column(nullable = false)
    private Long currentChunkSize;

    @Column(nullable = false)
    private String relativePath;

    @Column(nullable = false)
    private Integer totalChunks;

    private String type;

    @Transient
    private MultipartFile file;

    public static AttachmentChunk fromDto(AttachmentChunkDto dto) {
        return AttachmentChunk.builder()
            .chunkNumber(dto.getFlowChunkNumber())
            .chunkSize(dto.getFlowChunkSize())
            .currentChunkSize(dto.getFlowCurrentChunkSize())
            .totalSize(dto.getFlowTotalSize())
            .identifier(dto.getFlowIdentifier())
            .filename(dto.getFlowFilename())
            .relativePath(dto.getFlowRelativePath())
            .totalChunks(dto.getFlowTotalChunks())
            .file(dto.getFile())
            .build();
    }
}
