package fr.shom.ping.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import fr.shom.ping.model.Feature;
import fr.shom.ping.model.FeaturePart;
import fr.shom.ping.model.LocalizedText;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.dto.GmlCircleByCenterPoint;
import fr.shom.ping.model.dto.GmlRadius;
import fr.shom.ping.model.dto.S100CurveProperty;
import fr.shom.ping.model.dto.S100PointProperty;
import fr.shom.ping.model.dto.S100SurfaceProperty;
import fr.shom.ping.model.dto.S124DataSetDto;
import fr.shom.ping.model.dto.S124ExchangeSetDto;
import fr.shom.ping.model.dto.S124FixedDateRange;
import fr.shom.ping.model.dto.S124Geometry;
import fr.shom.ping.model.dto.S124MemberDto;
import fr.shom.ping.model.dto.S124MessageSeriesIdentifierDto;
import fr.shom.ping.model.dto.S124NWPartDto;
import fr.shom.ping.model.dto.S124NWPreambleDto;
import fr.shom.ping.model.dto.S124NWReferencesDto;
import fr.shom.ping.model.dto.S124SubAreaDto;
import fr.shom.ping.model.dto.S124WarningInformation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.gml2.GMLWriter;
import org.springframework.stereotype.Component;

import static fr.shom.ping.model.enums.WarningType.fromSerieTypeAndWarningHazardTypeGeneral;
import static fr.shom.ping.utils.DateUtil.convertDateRangeToUtc;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toUnmodifiableList;

@Component
@Slf4j
public final class S124Util {
    public static final String DEFAULT_LANGUAGE = "fr";
    public static final String INTERNATIONAL_LANGUAGE = "en";
    private static final GMLWriter GML_WRITER;
    private static final String SRS_NAME = "EPSG:3857";
    private static final String NUM_ARC = "1";

    static {
        GML_WRITER = new GMLWriter();
        GML_WRITER.setSrsName(SRS_NAME);
    }

    private S124Util() {
    }

    public static S124ExchangeSetDto buildS124ExchangeSetDtoFromWarnings(List<Warning> warnings, String lang) {
        List<S124DataSetDto> dataSet = warnings.stream()
            .map(warning -> mapWarningToS124DataSet(lang, warning))
            .toList();

        return S124ExchangeSetDto.builder()
            .dataSet(dataSet)
            .build();
    }

    private static S124DataSetDto mapWarningToS124DataSet(String lang, Warning warning) {
        S124DataSetDto dataSet = new S124DataSetDto();
        // Add Preamble
        final String preambleId = formatNWPreambleId(warning);
        dataSet.getImember().add(S124MemberDto.builder()
            .preamble(buildPreamble(lang, warning, preambleId))
            .build());
        // Add parts
        warning.getFeatureParts().forEach(part -> addFeaturePartToDataSet(lang, warning, dataSet, preambleId, part));

        return dataSet;
    }

    private static void addFeaturePartToDataSet(String lang, Warning warning, S124DataSetDto dataSet, String preambleId, FeaturePart part) {
        Optional<LocalizedText> informationByRequestedLanguage = part.getInformation().stream()
            .filter(localizedText -> localizedText.getLanguage().equals(lang))
            .findFirst();
        Optional<LocalizedText> informationByDefaultLanguage =
            part.getInformation().stream()
                .filter(localizedText -> localizedText.getLanguage().equals(DEFAULT_LANGUAGE))
                .findFirst();

        String information = null;
        if (informationByRequestedLanguage.isPresent()) {
            information = informationByRequestedLanguage.get().getText();
        } else if (informationByDefaultLanguage.isPresent()) {
            information = informationByDefaultLanguage.get().getText();
        }

        String partId = formatNWPartId(preambleId, part.getPosition());
        dataSet.getImember().add(buildFeaturePart(warning, part, information, partId));
    }

    private static S124MemberDto buildFeaturePart(Warning warning, FeaturePart part, String information, String partId) {
        return S124MemberDto.builder()
            .part(S124NWPartDto.builder()
                .gmlId(partId)
                .id(partId)
                .geometries(handleGeometries(part.getGeometries()))
                .affectedAreas(handleGeometries(part.getAffectedAreas()))
                .fixedDateRanges(part.getFixedDateRanges().stream().map(
                    dateRange -> {
                        convertDateRangeToUtc(dateRange);
                        return S124FixedDateRange.builder()
                            .timeOfDayStart(dateRange.getTimeOfDayStart())
                            .timeOfDayEnd(dateRange.getTimeOfDayEnd())
                            .dateStart(S124FixedDateRange.S124Date.builder().date(dateRange.getDateStart()).build())
                            .dateEnd(S124FixedDateRange.S124Date.builder().date(dateRange.getDateEnd()).build())
                            .build();
                    }
                ).toList())
                .warningInformation(S124WarningInformation.builder()
                    .warningHazardTypeDetails(part.getWarningHazardTypeDetails().stream().toList())
                    .information(information)
                    .build())
                .header(S124NWPartDto.S124PreambleLink.builder()
                    .href("#" + formatNWPreambleGmlId(warning))
                    .build())
                .build())
            .build();
    }

    private static S124NWPreambleDto buildPreamble(String lang, Warning warning, String preambleId) {
        return S124NWPreambleDto.builder()
            .gmlId(formatNWPreambleGmlId(warning))
            .id(preambleId)
            .messageSeriesIdentifier(buildMessageSeriesIdentifier(warning))
            .publicationDate(warning.getPublicationTime())
            .generalAreas(buildGeneralAreas(warning))
            .localities(buildLocalities(lang, warning))
            .warningHazardTypeGeneral(warning.getWarningHazardTypeGeneral())
            .partLinks(buildLinks(warning, preambleId))
            .build();
    }

    private static List<S124NWPreambleDto.S124PartLink> buildLinks(Warning warning, String preambleId) {
        return warning.getFeatureParts().stream()
            .map(part -> S124NWPreambleDto.S124PartLink.builder()
                .href("#" + formatNWPartId(preambleId, part.getPosition()))
                .build())
            .collect(toUnmodifiableList());
    }

    private static S124MessageSeriesIdentifierDto buildMessageSeriesIdentifier(Warning warning) {
        return S124MessageSeriesIdentifierDto.builder()
            .nameOfSeries(warning.getNameOfSeries())
            .warningNumber(warning.getWarningNumber())
            .warningType(fromSerieTypeAndWarningHazardTypeGeneral(warning.getSerie().getType(), warning.getWarningHazardTypeGeneral()))
            .year(warning.getYear())
            .productionAgency(warning.getProductionAgency())
            .country(warning.getCountry())
            .build();
    }

    private static List<S124SubAreaDto> buildLocalities(String lang, Warning warning) {
        return warning.getLocalities().stream()
            .map(locality -> {
                String localityName;

                if (DEFAULT_LANGUAGE.equals(lang)) {
                    localityName = locality.getName();
                } else {
                    localityName = Objects.toString(locality.getInternationalName(), locality.getName());
                }

                return S124SubAreaDto.builder()
                    .locationName(S124SubAreaDto.LocationName.builder()
                        .text(localityName)
                        .build())
                    .build();
            })
            .toList();
    }

    private static List<S124SubAreaDto> buildGeneralAreas(Warning warning) {
        return warning.getGeneralAreas().stream()
            .map(generalArea -> S124SubAreaDto.builder()
                .locationName(S124SubAreaDto.LocationName.builder()
                    .text(generalArea.getName())
                    .build())
                .build())
            .collect(toUnmodifiableList());
    }

    private static List<S124Geometry> handleGeometries(Set<Feature> geometries) {
        List<S124Geometry> s124Geometries = new ArrayList<>();
        geometries.forEach(feature -> {
            Geometry geometry = feature.getGeometry();
            if (geometry instanceof Point) {
                S100PointProperty s100PointProperty = S100PointProperty.builder()
                    .point(GML_WRITER.write(geometry))
                    .build();
                s124Geometries.add(S124Geometry.builder().pointProperty(s100PointProperty).build());
            } else if (geometry instanceof Polygon || geometry instanceof MultiPolygon) {
                S100SurfaceProperty s100SurfaceProperty;
                if ((feature.getProperties() != null && feature.getProperties().get("isCircle") != null)
                    && feature.getProperties().get("isCircle").equals(true)) {
                    List<Number> coordinatesCenter = (List<Number>) feature.getProperties().get("center");
                    Number radius = (Number) feature.getProperties().get("radius");
                    XmlMapper xmlMapper = new XmlMapper();
                    GmlCircleByCenterPoint circleByCenterPoint = GmlCircleByCenterPoint.builder()
                        .srsName(SRS_NAME)
                        .numArc(NUM_ARC)
                        .gmlPos(" " + coordinatesCenter.get(0) + ',' + coordinatesCenter.get(1) + " ")
                        .gmlRadius(GmlRadius.builder().uom("m").value(" " + radius.toString() + " ").build())
                        .build();
                    try {
                        s100SurfaceProperty = S100SurfaceProperty.builder()
                            .surface(xmlMapper.writeValueAsString(circleByCenterPoint))
                            .build();
                    } catch (JsonProcessingException e) {
                        log.error("Failed to process circle geometry : {}", e.getMessage());
                        throw new RuntimeException(e);
                    }
                } else {
                    s100SurfaceProperty = S100SurfaceProperty.builder()
                        .surface(GML_WRITER.write(geometry))
                        .build();
                }
                s124Geometries.add(S124Geometry.builder().surfaceProperty(s100SurfaceProperty).build());
            } else if (geometry instanceof LineString) {
                S100CurveProperty s100CurveProperty = S100CurveProperty.builder()
                    .curve(GML_WRITER.write(geometry))
                    .build();
                s124Geometries.add(S124Geometry.builder().curveProperty(s100CurveProperty).build());
            }
        });

        return s124Geometries;
    }

    public static String formatNWPreambleGmlId(Warning warning) {
        return "PR." + formatWarningId(warning);
    }

    public static String formatNWPreambleId(Warning warning) {
        return "NW." + formatWarningId(warning);
    }

    public static String formatNWPartId(String preambleId, Integer position) {
        return preambleId + "." + position;
    }

    public static String formatWarningId(Warning warning) {
        return formatNameOfSeries(warning.getNameOfSeries()) + "." + warning.getWarningNumber() + "." + warning.getYear();
    }

    public static String formatNameOfSeries(String nameOfSeries) {
        return nameOfSeries.toUpperCase().replace(" ", "_");
    }

    public static S124ExchangeSetDto buildS124ExchangeSetDtoFromInForceWarnings(List<Warning> warnings) {
        return S124ExchangeSetDto.builder()
            .dataSet(singletonList(mapInForceWarningToS124DataSet(warnings)))
            .build();
    }

    private static S124DataSetDto mapInForceWarningToS124DataSet(List<Warning> warnings) {
        final S124DataSetDto dataSet = new S124DataSetDto();
        // Add References
        dataSet.getImember().add(
            S124MemberDto.builder()
                .references(buildReferences(warnings))
                .build()
        );
        return dataSet;
    }

    private static S124NWReferencesDto buildReferences(List<Warning> warnings) {
        return S124NWReferencesDto.builder()
            .messageSeries(buildMessageSeriesIdentifierList(warnings))
            .noMessageOnHand(warnings.isEmpty())
            .referenceCategory("in-force")
            .theWarning(S124NWReferencesDto.S124WarningLink.builder().build())
            .build();
    }

    private static List<S124MessageSeriesIdentifierDto> buildMessageSeriesIdentifierList(List<Warning> warnings) {
        return warnings.stream().map(S124Util::buildMessageSeriesIdentifier).toList();
    }
}
