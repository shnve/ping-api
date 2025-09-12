package fr.shom.ping.model.dto;

import fr.shom.ping.model.LocalizedText;
import jakarta.validation.Valid;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
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
public class GeneralAreaDto implements Serializable, Comparable<GeneralAreaDto> {
    @Serial
    private static final long serialVersionUID = -663900643514355597L;

    private UUID id;

    private UUID subAreaId;

    @Default
    private Integer position = 1;

    @Default
    private Set<@Valid LocalizedText> locationNames = new HashSet<>();

    @Override
    public int compareTo(GeneralAreaDto generalAreaDto) {
        return position.compareTo(generalAreaDto.getPosition());
    }
}
