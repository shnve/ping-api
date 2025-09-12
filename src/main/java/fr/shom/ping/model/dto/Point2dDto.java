package fr.shom.ping.model.dto;

import java.io.Serial;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Point2dDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 7721780522049142107L;

    private double x;

    private double y;
}
