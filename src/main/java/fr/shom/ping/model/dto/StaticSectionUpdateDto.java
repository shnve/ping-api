package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
public class StaticSectionUpdateDto extends AbstractStaticSectionDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -4941219239571950614L;
}
