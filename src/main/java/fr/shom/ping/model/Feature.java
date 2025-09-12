package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.locationtech.jts.geom.Geometry;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = NON_NULL)
@Embeddable
public class Feature implements Serializable {
    @Serial
    private static final long serialVersionUID = -6837204427188995993L;

    @Default
    @Transient
    private String type = "Feature";

    @Default
    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    private Map<String, Object> properties = new HashMap<>();

    @Column(name = "geom", nullable = false, columnDefinition = "Geometry")
    private Geometry geometry;
}
