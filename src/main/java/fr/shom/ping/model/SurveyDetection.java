package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "Survey detection Model")
@Embeddable
public class SurveyDetection implements Serializable {
    @Serial
    private static final long serialVersionUID = 2694053359027258686L;

    public static short COLOR_LEVEL_CRITICAL = 3;

    @Column(name = "geom", nullable = false, columnDefinition = "Geometry")
    private Geometry geometry;

    @Column(nullable = false)
    private Float depth;

    @Column(nullable = false)
    private Float difference;

    @Column(nullable = false)
    private Short color;
}
