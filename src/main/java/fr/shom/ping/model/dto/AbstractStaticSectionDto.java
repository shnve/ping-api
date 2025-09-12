package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Static section Dto Model")
public abstract class AbstractStaticSectionDto {
    protected String content;

    protected String internationalContent;
}
