package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarningLocalityNamesDto implements Serializable, Comparable<WarningLocalityNamesDto> {
    @Serial
    private static final long serialVersionUID = -3020654736634648037L;

    @JsonIgnore
    private UUID id;

    @Default
    private Integer position = 1;

    private String name;

    private String internationalName;

    private UUID parentId;

    @Override
    public int compareTo(WarningLocalityNamesDto localityDto) {
        return position.compareTo(localityDto.getPosition());
    }
}
