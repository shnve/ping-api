package fr.shom.ping.model.tuple;

import fr.shom.ping.model.enums.SerieType;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SerieTuple implements Serializable {
    @Serial
    private static final long serialVersionUID = 9009421700937996762L;

    private UUID id;
    private String name;
    private String internationalName;
    private SerieType type;
    private UUID subAreaId;
    private String subAreaName;
    private String subAreaInternationalName;
}
