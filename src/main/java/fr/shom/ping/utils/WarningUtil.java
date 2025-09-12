package fr.shom.ping.utils;

import fr.shom.ping.model.FeaturePart;
import fr.shom.ping.model.LocalizedText;
import fr.shom.ping.model.Serie;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.dto.GeneralAreaDto;
import fr.shom.ping.model.dto.WarningLocalityNamesDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.geolatte.geom.Geometry;
import org.hibernate.query.NativeQuery;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static fr.shom.ping.utils.SchemaUtil.FEATURE_PART_TABLE;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.FRANCE;
import static java.util.Locale.FRENCH;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.StringUtils.hasText;

public final class WarningUtil {
    private WarningUtil() {
    }

    public static Set<LocalizedText> convertGeneralAreaInformationToLocationNames(String generalAreaName, String generalAreaInternationalName) {
        Set<LocalizedText> locationNames = new HashSet<>();
        locationNames.add(LocalizedText.builder()
            .language(FRANCE.getLanguage())
            .text(generalAreaName)
            .build());

        if (hasText(generalAreaInternationalName)) {
            locationNames.add(LocalizedText.builder()
                .language(ENGLISH.getLanguage())
                .text(generalAreaInternationalName)
                .build());
        }

        return locationNames;
    }

    public static Map<String, Set<String>> convertGeneralAreaDtoSortedSetToMap(Set<GeneralAreaDto> generalAreaSortedSet) {
        Map<String, Set<String>> generalAreas = new LinkedHashMap<>();
        generalAreaSortedSet.stream()
            .map(GeneralAreaDto::getLocationNames)
            .forEach(locationNames -> locationNames.forEach(locationName -> {
                if (generalAreas.containsKey(locationName.getLanguage())) {
                    generalAreas.get(locationName.getLanguage()).add(locationName.getText());
                } else {
                    generalAreas.put(locationName.getLanguage(), newLinkedHashSet(singletonList(locationName.getText())));
                }
            }));

        return generalAreas;
    }

    public static Map<String, Set<String>> convertLocalityDtoSortedSetToMap(List<WarningLocalityNamesDto> localitySortedList, Set<GeneralAreaDto> generalAreaSortedSet) {
        Map<String, Set<String>> localities = new LinkedHashMap<>();

        generalAreaSortedSet.stream()
            .sorted(comparing(GeneralAreaDto::getPosition))
            .flatMap(generalArea -> localitySortedList.stream()
                .filter(locality -> locality.getParentId().equals(generalArea.getSubAreaId()))
                .sorted(comparing(WarningLocalityNamesDto::getPosition)))
            .forEach(locality -> buildLocalitiesMap(locality, localities));

        return localities;
    }

    private static void buildLocalitiesMap(WarningLocalityNamesDto locality, Map<String, Set<String>> localities) {
        if (localities.containsKey(FRENCH.getLanguage())) {
            localities.get(FRENCH.getLanguage()).add(locality.getName());
        } else {
            localities.put(FRENCH.getLanguage(), newLinkedHashSet(singletonList(locality.getName())));
        }

        if (localities.containsKey(ENGLISH.getLanguage())) {
            localities.get(ENGLISH.getLanguage()).add(locality.getInternationalName());
        } else {
            localities.put(ENGLISH.getLanguage(), newLinkedHashSet(singletonList(locality.getInternationalName())));
        }
    }

    public static boolean checkIfAtLeastInformationOrDetailsIsFilledForEachPart(Warning warning) {
        return warning.getFeatureParts().stream()
            .allMatch(featurePart -> featurePart.getInformation().stream().allMatch(localizedText -> !localizedText.getText().isEmpty())
                || !featurePart.getWarningHazardTypeDetails().isEmpty());
    }

    public static boolean checkIfAtLeastOneMandatoryLanguageIsMissing(Warning warning) {
        return !warning.getSerie().getMandatoryLanguages().stream()
            .allMatch(language -> {
                List<FeaturePart> featurePartWithEmptyDetails = warning.getFeatureParts().stream()
                    .filter(featurePart -> featurePart.getWarningHazardTypeDetails().isEmpty())
                    .collect(toList());

                if (featurePartWithEmptyDetails.isEmpty()) {
                    return true;
                }

                return featurePartWithEmptyDetails.stream()
                    .map(FeaturePart::getInformation)
                    .flatMap(Collection::stream)
                    .map(LocalizedText::getLanguage)
                    .anyMatch(informationLanguage -> informationLanguage.contains(language));
            });
    }

    public static String generateUsualNumber(final Warning warning, final boolean internationalLanguage) {
        return generateUsualNumber(warning, internationalLanguage, null);
    }

    public static String generateUsualNumber(final Warning warning, final boolean internationalLanguage, String usualNumberLink) {
        final Serie serie = warning.getSerie();
        final StringBuilder usualNumber = new StringBuilder();
        final boolean hasLink = StringUtils.isNotBlank(usualNumberLink);
        if (hasLink) {
            usualNumber.append("<a href=\"" + usualNumberLink + "\">");
        }
        usualNumber.append(internationalLanguage ? Optional.ofNullable(serie.getInternationalName()).orElse(serie.getName()) : serie.getName())
            .append(" ")
            .append(warning.getWarningNumber() != null ? warning.getWarningNumber() + "/" + String.valueOf(warning.getYear()).substring(2) : "XX/AA");
        if (hasLink) {
            usualNumber.append("</a>");
        }
        return usualNumber.toString();
    }

    // We ignore "findsecbugs:SQL_INJECTION_JPA" sonar warning as we use setParameter with our NativeQuery to prevent SQL Injection
    @SuppressWarnings("findsecbugs:SQL_INJECTION_JPA")
    public static List<Geometry> getGeometriesAsTextByWarningId(EntityManager entityManager, UUID warningId, String tableName) {
        Query query = entityManager.createNativeQuery(
            "SELECT " + tableName + ".geom as the_geom " +
                "FROM " + tableName + " " +
                "INNER JOIN " + FEATURE_PART_TABLE + " ON " + FEATURE_PART_TABLE + ".id = " + tableName + ".feature_part_id " +
                "WHERE " + FEATURE_PART_TABLE + ".warning_id = :warningId ");

        query.setParameter("warningId", warningId);
        return query.unwrap(NativeQuery.class).getResultList();
    }
}
