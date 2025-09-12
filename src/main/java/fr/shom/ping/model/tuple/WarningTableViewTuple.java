package fr.shom.ping.model.tuple;

import fr.shom.ping.model.enums.WarningHazardTypeDetails;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static org.springframework.util.StringUtils.hasText;

@Getter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class WarningTableViewTuple implements Serializable {
    @Serial
    private static final long serialVersionUID = -98538876655551581L;

    private UUID id;
    private String nameOfSeries;
    private Integer warningNumber;
    private Integer year;
    private UUID generalAreaId;
    private UUID subAreaId;
    private Integer generalAreaPosition;
    private String generalAreaName;
    private String generalAreaInternationalName;
    private UUID localityId;
    private UUID localityParentId;
    private Integer localityPosition;
    private String localityName;
    private String localityInternationalName;
    private WarningHazardTypeGeneral warningHazardTypeGeneral;
    private WarningHazardTypeDetails warningHazardTypeDetail;

    public WarningTableViewTuple(UUID id, String nameOfSeries, Integer warningNumber, Integer year, UUID generalAreaId, UUID subAreaId, Integer generalAreaPosition,
        String generalAreaName, String generalAreaInternationalName, UUID localityId, UUID localityParentId, Integer localityPosition, String localityName,
        String localityInternationalName, String warningHazardTypeGeneral, String warningHazardTypeDetail) {
        this.id = id;
        this.nameOfSeries = nameOfSeries;
        this.warningNumber = warningNumber;
        this.year = year;
        this.generalAreaId = generalAreaId;
        this.subAreaId = subAreaId;
        this.generalAreaPosition = generalAreaPosition;
        this.generalAreaName = generalAreaName;
        this.generalAreaInternationalName = generalAreaInternationalName;
        this.localityId = localityId;
        this.localityParentId = localityParentId;
        this.localityPosition = localityPosition;
        this.localityName = localityName;
        this.localityInternationalName = localityInternationalName;
        this.warningHazardTypeGeneral = WarningHazardTypeGeneral.valueOf(warningHazardTypeGeneral);
        if (hasText(warningHazardTypeDetail)) {
            this.warningHazardTypeDetail = WarningHazardTypeDetails.valueOf(warningHazardTypeDetail);
        }
    }
}
