package fr.shom.ping.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.hateoas.server.core.Relation;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@Schema(name = "Warning Detail DTO")
@Relation(collectionRelation = "items")
public class WarningDetailDto extends AbstractWarningDetailDto {

    @Serial
    private static final long serialVersionUID = 6308027529050253461L;
}
