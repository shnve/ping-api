package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "Survey ENC Model")
@Embeddable
public class SurveyEnc implements Serializable {

    @Serial
    private static final long serialVersionUID = 1719343500817789982L;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Float updateNumber;

    @Column(nullable = false)
    private Instant updateDate;

    private String comments;
}
