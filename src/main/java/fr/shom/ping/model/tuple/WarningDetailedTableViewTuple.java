package fr.shom.ping.model.tuple;

import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.LocalizedText;
import fr.shom.ping.model.enums.WarningHazardTypeDetails;
import fr.shom.ping.model.enums.WarningHazardTypeGeneral;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.hasText;

@Getter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class WarningDetailedTableViewTuple implements Serializable {
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
    private Integer featurePosition;
    private WarningHazardTypeDetails featureWarningHazardTypeDetail;
    private LocalizedText featureInformation;
    private FixedDateRange featureFixedDateRange;
    private Instant cancellationDate;
    private Instant creationDate;
    private Instant publicationTime;
    private String referenceNameOfSeries;
    private Integer referenceWarningNumber;
    private Integer referenceYear;

    public WarningDetailedTableViewTuple(UUID id, String nameOfSeries, Integer warningNumber, Integer year, UUID generalAreaId, UUID subAreaId, Integer generalAreaPosition,
        String generalAreaName, String generalAreaInternationalName, UUID localityId, UUID localityParentId, Integer localityPosition, String localityName,
        String localityInternationalName, String warningHazardTypeGeneral, Integer featurePosition, String warningHazardTypeDetail, String featureLanguage,
        String featureText, Integer datePosition,
        LocalDate dateEnd, LocalDate dateStart, LocalTime timeOfDayEnd, LocalTime timeOfDayStart, String periodTimezone, Instant cancellationDate, Instant creationDate,
        Instant publicationTime, String referenceNameOfSeries, Integer referenceWarningNumber, Integer referenceYear) {
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
        if (hasText(warningHazardTypeGeneral)) {
            this.warningHazardTypeGeneral = WarningHazardTypeGeneral.valueOf(warningHazardTypeGeneral);
        }
        this.featurePosition = featurePosition;
        if (hasText(warningHazardTypeDetail)) {
            this.featureWarningHazardTypeDetail = WarningHazardTypeDetails.valueOf(warningHazardTypeDetail);
        }
        this.featureInformation = ofNullable(featureText).map(ignored -> new LocalizedText(featureLanguage, featureText)).orElse(null);
        if (!(dateEnd == null && dateStart == null && timeOfDayEnd == null && timeOfDayStart == null)) {
            this.featureFixedDateRange = new FixedDateRange(datePosition, dateEnd, dateStart, timeOfDayEnd, timeOfDayStart, periodTimezone);
        }
        this.cancellationDate = cancellationDate;
        this.creationDate = creationDate;
        this.publicationTime = publicationTime;
        this.referenceNameOfSeries = referenceNameOfSeries;
        this.referenceWarningNumber = referenceWarningNumber;
        this.referenceYear = referenceYear;
    }
}
