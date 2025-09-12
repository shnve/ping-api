package fr.shom.ping.model.tuple;

import fr.shom.ping.model.enums.GeoregulationActivity;
import fr.shom.ping.model.enums.GeoregulationStatus;
import fr.shom.ping.model.enums.GeoregulationType;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import static java.util.Optional.ofNullable;

@Getter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class GeoregulationAdminTableTuple implements Serializable {

    private UUID id;
    private GeoregulationStatus status;
    private Integer mainNumberYear;
    private String mainNumberText;
    private String mainNumberAuthorityCode;
    private LocalDate mainNumberDate;
    private GeoregulationType type;
    private GeoregulationActivity activity;
    private String title;
    private String internationalTitle;
    private UUID maritimeEntityId;
    private String maritimeEntityName;
    private String maritimeEntityInternationalName;
    private String internalComment;
    private Instant lastUpdate;
    private Instant publicationDate;
    private Instant repealDate;
    private boolean isPublic;
    private boolean isNew;
    private boolean isExpired;

    public GeoregulationAdminTableTuple(UUID id, String status, Integer mainNumberYear, String mainNumberText, String mainNumberAuthorityCode,
        LocalDate mainNumberDate, String type, String activity, String title, String internationalTitle, UUID maritimeEntityId, String maritimeEntityName,
        String maritimeEntityInternationalName, String internalComment, Instant lastUpdate, Instant publicationDate, Instant repealDate, boolean isPublic,
        boolean isNew, boolean isExpired) {
        this.id = id;
        this.status = GeoregulationStatus.valueOf(status);
        this.mainNumberYear = mainNumberYear;
        this.mainNumberText = mainNumberText;
        this.mainNumberAuthorityCode = mainNumberAuthorityCode;
        this.mainNumberDate = mainNumberDate;
        this.type = GeoregulationType.valueOf(type);
        this.activity = ofNullable(activity).map(GeoregulationActivity::valueOf).orElse(null);
        this.title = title;
        this.internationalTitle = internationalTitle;
        this.maritimeEntityId = maritimeEntityId;
        this.maritimeEntityName = maritimeEntityName;
        this.maritimeEntityInternationalName = maritimeEntityInternationalName;
        this.internalComment = internalComment;
        this.lastUpdate = lastUpdate;
        this.publicationDate = publicationDate;
        this.repealDate = repealDate;
        this.isPublic = isPublic;
        this.isNew = isNew;
        this.isExpired = isExpired;
    }
}
