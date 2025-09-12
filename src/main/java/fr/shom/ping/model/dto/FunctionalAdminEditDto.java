package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.FunctionalAdmin;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "FunctionalAdmin Edit DTO Model")
public class FunctionalAdminEditDto extends ProUserEditDto {
    @Serial
    private static final long serialVersionUID = -8471356465320539615L;

    public FunctionalAdminEditDto(FunctionalAdmin functionalAdmin) {
        super(functionalAdmin);
    }

}
