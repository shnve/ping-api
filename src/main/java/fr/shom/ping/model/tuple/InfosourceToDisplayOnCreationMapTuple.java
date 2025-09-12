package fr.shom.ping.model.tuple;

import fr.shom.ping.model.Feature;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InfosourceToDisplayOnCreationMapTuple {
    private UUID id;
    private Feature geometry;
    private String number;
}
