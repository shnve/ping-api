package fr.shom.ping.model.dto;

import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BoundingBoxDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 7366525654402488117L;

    private Point2dDto nwCorner;

    private Point2dDto seCorner;
}
