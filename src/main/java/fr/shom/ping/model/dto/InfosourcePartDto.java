package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.shom.ping.model.Feature;
import fr.shom.ping.model.InfosourcePart;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import fr.shom.ping.validation.NoHtml;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.google.common.collect.Sets.newHashSet;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfosourcePartDto implements Serializable, Comparable<InfosourcePartDto> {
    @Serial
    private static final long serialVersionUID = 8050311049312754360L;

    @JsonIgnore
    @Default
    private Integer position = 1;

    @NoHtml
    private String description;

    @NoHtml
    private String internationalDescription;

    @Default
    private Set<Feature> localisation = newHashSet();

    @Default
    @NoHtml
    private Map<String, Object> object = new HashMap<>();

    @Override
    public int compareTo(InfosourcePartDto infosourcePartDto) {
        return position.compareTo(infosourcePartDto.getPosition());
    }

    public static InfosourcePartDto fromInfosourcePart(InfosourcePart infosourcePart) {
        return InfosourcePartDto.builder()
            .description(infosourcePart.getDescription())
            .position(infosourcePart.getPosition())
            .internationalDescription(infosourcePart.getInternationalDescription())
            .localisation(infosourcePart.getLocalisation())
            .object(infosourcePart.getObject())
            .build();
    }
}
