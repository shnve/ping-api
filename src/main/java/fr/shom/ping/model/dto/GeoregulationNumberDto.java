package fr.shom.ping.model.dto;

import fr.shom.ping.model.GeoregulationNumber;
import fr.shom.ping.model.MaritimeService;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import fr.shom.ping.validation.NoHtml;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoregulationNumberDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 6019289191665451551L;

    @NotNull(message = "validation.georegulation.number.year.not-null")
    private Integer year;

    @NotNull(message = "validation.georegulation.number.text.not-empty")
    @NoHtml
    private String text;

    @NotEmpty(message = "validation.georegulation.number.authority-id.not-null")
    @Default
    private List<UUID> authorityIds = new ArrayList<>();

    @NotNull(message = "validation.georegulation.number.date.not-null")
    private LocalDate date;

    public GeoregulationNumberDto(GeoregulationNumber georegulationNumber) {
        this.year = georegulationNumber.getYear();
        this.text = georegulationNumber.getText();
        this.date = georegulationNumber.getDate();
        this.authorityIds = georegulationNumber.getAuthorities().stream().map(MaritimeService::getId).collect(Collectors.toList());
        if (georegulationNumber.getAuthorities().isEmpty()) {
            this.authorityIds = new ArrayList<>(georegulationNumber.getAuthorityIds());
        }
    }

    public static GeoregulationNumberDto fromGeoregulationNumber(GeoregulationNumber georegulationNumber) {
        return new GeoregulationNumberDto(georegulationNumber);
    }
}
