package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.WarningPublicReference;
import fr.shom.ping.model.enums.WarningReferenceCategoryEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.SwaggerConfig.Property.ID_DESCRIPTION;

@ToString(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
@Schema(name = "Warning reference as public reference dto")
public class WarningReferenceAsPublicReferenceDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 8593223074994184236L;

    @Schema(description = ID_DESCRIPTION, required = true)
    private UUID warningId;

    @Schema(description = "Name of series")
    private String nameOfSeries;

    @Schema(description = "Warning number")
    private Integer warningNumber;

    @Schema(description = "Year")
    private Integer year;

    @Schema(description = "Warning reference category", required = true)
    private WarningReferenceCategoryEnum category;

    public WarningReferenceAsPublicReferenceDto(Warning warning, WarningReferenceCategoryEnum category) {
        this.warningId = warning.getId();
        this.nameOfSeries = warning.getNameOfSeries();
        this.warningNumber = warning.getWarningNumber();
        this.year = warning.getYear();
        this.category = category;
    }

    public static WarningReferenceAsPublicReferenceDto fromWarningPublicReference(WarningPublicReference warningPublicReference) {
        return WarningReferenceAsPublicReferenceDto.builder()
            .warningId(warningPublicReference.getWarningPublicReference().getId())
            .nameOfSeries(warningPublicReference.getWarningPublicReference().getNameOfSeries())
            .warningNumber(warningPublicReference.getWarningPublicReference().getWarningNumber())
            .year(warningPublicReference.getWarningPublicReference().getYear())
            .category(warningPublicReference.getCategory())
            .build();
    }
}

