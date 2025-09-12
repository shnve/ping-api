package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(value = NON_NULL)
public class EnumTypeWithTranslationDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 5910053377476285926L;

    private String enumName;

    private String translation;

    private String internationalTranslation;
}
