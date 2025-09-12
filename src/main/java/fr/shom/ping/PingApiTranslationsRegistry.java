package fr.shom.ping;

import fr.shom.ping.exception.NotFoundException;
import fr.shom.ping.model.AbstractLighthouseAndBeaconMessage;
import fr.shom.ping.model.AbstractWarningHazardType;
import fr.shom.ping.model.GeoregulationPartTypeEntity;
import fr.shom.ping.model.LighthouseAndBeaconMessageEntity;
import fr.shom.ping.model.WarningHazardTypeDetailsEntity;
import fr.shom.ping.model.WarningHazardTypeGeneralEntity;
import fr.shom.ping.model.WarningTypeEntity;
import fr.shom.ping.model.enums.GeoregulationPartType;
import fr.shom.ping.repository.GeoregulationPartTypeRepository;
import fr.shom.ping.repository.LighthouseAndBeaconMessageRepository;
import fr.shom.ping.repository.WarningHazardTypeDetailsRepository;
import fr.shom.ping.repository.WarningHazardTypeGeneralRepository;
import fr.shom.ping.repository.WarningTypeRepository;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static java.util.Collections.unmodifiableList;
import static java.util.Locale.ENGLISH;

@Slf4j
@Component
@RequiredArgsConstructor
public class PingApiTranslationsRegistry {
    private final WarningHazardTypeGeneralRepository warningHazardTypeGeneralRepository;
    private final WarningHazardTypeDetailsRepository warningHazardTypeDetailsRepository;
    private final WarningTypeRepository warningTypeRepository;
    private final LighthouseAndBeaconMessageRepository lighthouseAndBeaconMessageRepository;
    private final GeoregulationPartTypeRepository georegulationPartTypeRepository;

    @Getter
    private List<WarningHazardTypeGeneralEntity> warningHazardTypeGeneralEntityList;

    @Getter
    private List<WarningHazardTypeDetailsEntity> warningHazardTypeDetailsEntityList;

    @Getter
    private List<WarningTypeEntity> warningTypeEntityList;

    @Getter
    private List<LighthouseAndBeaconMessageEntity> lighthouseAndBeaconMessageEntityList;

    private List<GeoregulationPartTypeEntity> georegulationPartTypeEntityList;

    private static final BiPredicate<AbstractWarningHazardType, String> filterByEnumName
        = (p1, p2) -> p2.equalsIgnoreCase(p1.getEnumName());
    private static final BiPredicate<AbstractLighthouseAndBeaconMessage, String> filterLightAndhouseMessageByEnumName
        = (p1, p2) -> p2.equalsIgnoreCase(p1.getEnumName());

    @PostConstruct
    public void init() {
        log.info("init PingApiTranslationsRegistry");
        this.warningHazardTypeGeneralEntityList = unmodifiableList(this.warningHazardTypeGeneralRepository.findAll());
        this.warningHazardTypeDetailsEntityList = unmodifiableList(this.warningHazardTypeDetailsRepository.findAll());
        this.warningTypeEntityList = unmodifiableList(this.warningTypeRepository.findAll());
        this.lighthouseAndBeaconMessageEntityList = unmodifiableList(this.lighthouseAndBeaconMessageRepository.findAll());
        this.georegulationPartTypeEntityList = unmodifiableList(this.georegulationPartTypeRepository.findAll());

    }

    private static AbstractWarningHazardType getWarningHazardTypeByEnumName(List<? extends AbstractWarningHazardType> list, String enumName) {
        return list.stream()
            .filter(warningHazardType -> filterByEnumName.test(warningHazardType, enumName))
            .findFirst().orElseThrow(NotFoundException::new);
    }

    private static AbstractLighthouseAndBeaconMessage getLighthouseAndBeaconTypeByEnumName(List<? extends AbstractLighthouseAndBeaconMessage> list,
        String enumName) {
        return list.stream()
            .filter(lighthouseAndBeaconType -> filterLightAndhouseMessageByEnumName.test(lighthouseAndBeaconType, enumName))
            .findFirst().orElseThrow(NotFoundException::new);
    }

    private static String getWarningHazardTypeByEnumNameAndLang(AbstractWarningHazardType warningHazardType, String lang) {
        return isInternationalLanguage(lang) ? warningHazardType.getInternationalTranslation() : warningHazardType.getTranslation();
    }

    private static String getLighthouseAndBeaconByEnumNameAndLang(AbstractLighthouseAndBeaconMessage lighthouseAndBeaconMessage, String lang) {
        return isInternationalLanguage(lang) ? lighthouseAndBeaconMessage.getInternationalTranslation() : lighthouseAndBeaconMessage.getTranslation();
    }

    public WarningHazardTypeGeneralEntity getWarningHazardTypeGeneralByEnumName(String enumName) {
        return (WarningHazardTypeGeneralEntity) getWarningHazardTypeByEnumName(this.getWarningHazardTypeGeneralEntityList(), enumName);
    }

    public WarningHazardTypeDetailsEntity getWarningHazardTypeDetailsByEnumName(String enumName) {
        return (WarningHazardTypeDetailsEntity) getWarningHazardTypeByEnumName(this.getWarningHazardTypeDetailsEntityList(), enumName);
    }

    public WarningTypeEntity getWarningTypeByEnumName(String enumName) {
        return (WarningTypeEntity) getWarningHazardTypeByEnumName(this.getWarningTypeEntityList(), enumName);
    }

    public LighthouseAndBeaconMessageEntity getLighthouseAndBeaconMessageByEnumName(String enumName) {
        return (LighthouseAndBeaconMessageEntity) getLighthouseAndBeaconTypeByEnumName(this.getLighthouseAndBeaconMessageEntityList(), enumName);
    }

    public String getWarningHazardTypeGeneralByEnumNameAndLang(String enumName, String lang) {
        return getWarningHazardTypeByEnumNameAndLang(this.getWarningHazardTypeGeneralByEnumName(enumName), lang);
    }

    public String getWarningHazardTypeDetailsByEnumNameAndLang(String enumName, String lang) {
        return getWarningHazardTypeByEnumNameAndLang(this.getWarningHazardTypeDetailsByEnumName(enumName), lang);
    }

    public String getWarningTypeByEnumNameAndLang(String enumName, String lang) {
        return getWarningHazardTypeByEnumNameAndLang(this.getWarningTypeByEnumName(enumName), lang);
    }

    public String getLighthouseAndBeaconMessageByEnumNameAndLang(String enumName, String lang) {
        return getLighthouseAndBeaconByEnumNameAndLang(this.getLighthouseAndBeaconMessageByEnumName(enumName), lang);
    }

    public static boolean isInternationalLanguage(final String lang) {
        return ENGLISH.getLanguage().equals(lang);
    }

    public String getGeoregulationPartTypeTranslation(GeoregulationPartType type, Function<GeoregulationPartTypeEntity, String> function) {
        return this.georegulationPartTypeEntityList.stream()
            .filter(entity -> entity.getEnumName().equals(type.name()))
            .findFirst()
            .map(function)
            .orElseThrow(NotFoundException::new);
    }
}
