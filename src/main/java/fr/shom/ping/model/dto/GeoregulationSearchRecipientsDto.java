package fr.shom.ping.model.dto;

import fr.shom.ping.model.FixedDateRange;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Geometry;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoregulationSearchRecipientsDto implements Serializable {
    @Serial
    private static final long serialVersionUID = -7260912563666967376L;

    @NotNull(message = "validation.georegulation-search-recipients.issuing-service-id.not-null")
    private UUID issuingServiceId;

    @NotEmpty(message = "validation.georegulation-search-recipients.geometries.not-empty")
    @Default
    private List<Geometry> geometries = new ArrayList<>();

    @Default
    private List<@Valid FixedDateRange> dateRanges = new ArrayList<>();

    @NotNull(message = "validation.georegulation-search-recipients.signing-date.not-null")
    private LocalDate signingDate;
}
