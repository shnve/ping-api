package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.enums.SurveyDangerNature;
import fr.shom.ping.model.enums.SurveyDangerTypeOfValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Geometry;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static jakarta.persistence.EnumType.STRING;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Schema(name = "Survey danger Model")
@Embeddable
public class SurveyDanger implements Serializable {

    @Serial
    private static final long serialVersionUID = 475495596064619380L;

    @Column(name = "geom", nullable = false, columnDefinition = "Geometry")
    private Geometry geometry;

    @Column(nullable = false)
    @Enumerated(STRING)
    private SurveyDangerNature nature;

    @Column(nullable = false)
    @Enumerated(STRING)
    private SurveyDangerTypeOfValue typeOfValue;

    @Column(nullable = false)
    private Float value;
}
