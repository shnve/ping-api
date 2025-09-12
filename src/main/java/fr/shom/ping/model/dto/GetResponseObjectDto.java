package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.grad.secom.core.models.GetResponseObject;

import java.io.Serial;
import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Builder
@JsonInclude(NON_NULL)
public class GetResponseObjectDto extends GetResponseObject implements Serializable {
    @Serial
    private static final long serialVersionUID = -8534385159168743357L;
}
