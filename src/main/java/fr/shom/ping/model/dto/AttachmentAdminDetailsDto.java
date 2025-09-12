package fr.shom.ping.model.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Attachment Admin Details Dto Model")
public class AttachmentAdminDetailsDto extends AbstractAttachmentDto {

    @Serial
    private static final long serialVersionUID = 5407216910866839906L;

    private UUID uuid;

    private Instant creationDate;
    
    private String location;
}
