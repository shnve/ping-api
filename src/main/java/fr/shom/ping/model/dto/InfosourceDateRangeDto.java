package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
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
@Schema(name = "Infosource Dto Model")
public class InfosourceDateRangeDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 8440978392893102473L;

    @Schema(description = "Date end")
    private LocalDate end;

    @Schema(description = "Date start")
    private LocalDate start;
}
