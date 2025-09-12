package fr.shom.ping.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum LighthouseAndBeaconMessageDtoProperty {

    LOCAL_SEND_DATE("dateEnvoiLocal", "localSendDate", false),
    UTC_SEND_DATE("dateEnvoiUtc", "utcSendDate", false),
    NOTICE_NUMBER("numeroAvis", "noticeNumber", false),
    PREVIOUS_NOTICE_NUMBER("numeroAvisPrecedent", "previousNoticeNumber", true),
    COASTAL_SERVICE("serviceLittoral", "costalService", true),
    TERRITORIAL_DIVISION("divisionTerritorial", "territorialDivision", true),
    NATURE_CODE("codeNatureAvis", "naturalNoticeCode", true),
    INCIDENT_CODE("intituleIncident", "incidentTitle", true),
    CMS_CODE("intituleCms", "cmsTitle", true),
    INDICATION_CODE("indication", "indication", true),
    RECOVERY_TIME_CODE("delaiRetablissement", "recoveryTime", true),
    COMMENT("commentaire", "comment", true),
    LEVEL("niveau", "level", true),
    IDENTIFICATION_NUMBER("numeroIdentification", "number", true),
    CONTACT("contact", "contact", true),
    HERITAGE_NAME("nomPatrimoine", "heritageName", true),
    BAPTISM_NAME("nomBapteme", "baptismName", true),
    LIST_OF_LIGHTS("numeroLivreFeux", "fireBookNumber", true),
    MARK("marque", "mark", true),
    DEPARTMENT("departement", "department", true),
    AREA("zoneGeographique", "geographicalZone", true),
    POSITION("position", "position", false);

    private static final Map<String, String> BY_TRANSLATE_NAME = new HashMap<>();
    private static final Map<String, Boolean> BY_HAS_TO_BE_SAVED_IN_OBJECT = new HashMap<>();

    static {
        for (LighthouseAndBeaconMessageDtoProperty prop : values()) {
            BY_TRANSLATE_NAME.put(prop.keyName, prop.getTranslateName());
            BY_HAS_TO_BE_SAVED_IN_OBJECT.put(prop.keyName, prop.getHasToBeSavedInObject());
        }
    }

    public static String valueOfTranslateName(String keyName) {
        return BY_TRANSLATE_NAME.get(keyName);
    }

    public static Boolean valueOfToBeSaved(String keyName) {
        return BY_HAS_TO_BE_SAVED_IN_OBJECT.get(keyName);
    }

    private String keyName;
    private String translateName;
    private Boolean hasToBeSavedInObject;
}
