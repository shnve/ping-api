package fr.shom.ping.model;

import fr.shom.ping.model.dto.AttachmentCreateDto;
import java.io.Serial;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Attachment extends AbstractAttachment {
    @Serial
    private static final long serialVersionUID = -9184403242845118284L;

    @Column(nullable = false)
    private String location;

    public static Attachment fromDto(AttachmentCreateDto dto) {
        return Attachment.builder()
            .filename(dto.getFilename())
            .identifier(dto.getIdentifier())
            .totalSize(dto.getTotalSize())
            .type(dto.getType())
            .build();
    }
}
