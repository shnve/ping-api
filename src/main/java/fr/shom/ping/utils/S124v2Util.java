package fr.shom.ping.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.shom.ping.PingApiTranslationsRegistry;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Feature;
import fr.shom.ping.model.FeaturePart;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.GeneralArea;
import fr.shom.ping.model.LocalizedText;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.WarningLocalityNames;
import fr.shom.ping.model.WarningReference;
import fr.shom.ping.model.dto.GmlCircleByCenterPointV2;
import fr.shom.ping.model.dto.GmlPatches;
import fr.shom.ping.model.dto.GmlPolygonPatch;
import fr.shom.ping.model.dto.GmlRadius;
import fr.shom.ping.model.dto.S100DatasetIdentificationInformation;
import fr.shom.ping.model.dto.S100v2CurveProperty;
import fr.shom.ping.model.dto.S100v2PointProperty;
import fr.shom.ping.model.dto.S100v2SurfaceProperty;
import fr.shom.ping.model.dto.S124Link;
import fr.shom.ping.model.dto.S124NWAreaAffectedDto;
import fr.shom.ping.model.dto.S124V2ExchangeSetDto;
import fr.shom.ping.model.dto.S124v2DataSetDto;
import fr.shom.ping.model.dto.S124v2FixedDateRange;
import fr.shom.ping.model.dto.S124v2Geometry;
import fr.shom.ping.model.dto.S124v2MemberDto;
import fr.shom.ping.model.dto.S124v2MessageSeriesIdentifierDto;
import fr.shom.ping.model.dto.S124v2NWPartDto;
import fr.shom.ping.model.dto.S124v2NWPreambleDto;
import fr.shom.ping.model.dto.S124v2NWReferencesDto;
import fr.shom.ping.model.dto.S124v2SubAreaDto;
import fr.shom.ping.model.dto.S124v2WarningInformation;
import fr.shom.ping.model.enums.S100DatasetPurposeType;
import fr.shom.ping.model.enums.S100TopicCategoryCode;
import fr.shom.ping.model.enums.S124ReferenceCategoryCode;
import fr.shom.ping.model.enums.WarningReferenceCategoryEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.xmlbeans.XmlException;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static fr.shom.ping.model.enums.WarningHazardTypeGeneral.IN_FORCE_BULLETIN;
import static fr.shom.ping.model.enums.WarningType.fromSerieTypeAndWarningHazardTypeGeneral;
import static fr.shom.ping.utils.DateUtil.convertDateRangeToUtc;
import static fr.shom.ping.utils.GpxUtil.convertCoordinatesFromEpsg3857To4326;
import static fr.shom.ping.utils.S100Util.getLanguageCode;
import static fr.shom.ping.utils.S124Util.INTERNATIONAL_LANGUAGE;
import static fr.shom.ping.utils.S124Util.formatNWPartId;
import static fr.shom.ping.utils.S124Util.formatNWPreambleGmlId;
import static fr.shom.ping.utils.S124Util.formatNWPreambleId;
import static fr.shom.ping.utils.XmlUtil.prettyPrintByTransformer;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;

@Slf4j
@Component
public final class S124v2Util {
    public static final String DEFAULT_LANGUAGE = "fr";
    private static final CustomGMLWriter GML_WRITER;
    public static final String SRS_NAME = "http://www.opengis.net/def/crs/EPSG/0/4326";
    private static final String DATASET_FILE_IDENTIFIER = "ID_FILE_SEE_SECOM.xml";
    private static final String DATASET_TOPIC_CATEGORY = S100TopicCategoryCode.TRANSPORTATION.getCode();
    private static final String DATASET_PURPOSE = S100DatasetPurposeType.BASE.getCode();
    private static final String UPDATE_NUMBER = "0";
    private static final XmlMapper XML_MAPPER;

    static {
        GML_WRITER = new CustomGMLWriter();
        GML_WRITER.setSrsName(SRS_NAME);
        XML_MAPPER = new XmlMapper();
        XML_MAPPER.registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Autowired
    private PingApiTranslationsRegistry translationsRegistry;

    private S124v2Util() {
    }

    public void generateGmlDatasetFile(File targetFile, Warning warning, String lang) throws ServiceException {
        final S124v2DataSetDto dataSetDto = mapWarningToS124DataSet(lang, warning, FilenameUtils.getName(targetFile.getPath()));
        writeGmlFile(dataSetDto, targetFile.getPath());
    }

    public void writeGmlFile(S124v2DataSetDto dataSetDto, String filePath) throws ServiceException {
        try (FileOutputStream fos = new FileOutputStream(Path.of(FilenameUtils.getFullPath(filePath), FilenameUtils.getName(filePath)).toString())) {
            fos.write(prettyPrintByTransformer(XML_MAPPER.writeValueAsString(dataSetDto), 4, false).getBytes());
        } catch (IOException | XmlException exception) {
            log.error("Failed to create dataset file : {}", exception.getMessage());
            throw new ServiceException("Failed to create dataset file : " + exception.getMessage());
        }
    }

    public S124V2ExchangeSetDto buildS124ExchangeSetDtoFromWarnings(List<Warning> warnings, String lang) {
        List<S124v2DataSetDto> dataSet = warnings.stream()
            .map(warning -> mapWarningToS124DataSet(lang, warning))
            .toList();

        return S124V2ExchangeSetDto.builder()
            .dataSet(dataSet)
            .build();
    }

    public S124v2DataSetDto mapWarningToS124DataSet(String lang, Warning warning) {
        return mapWarningToS124DataSet(lang, warning, DATASET_FILE_IDENTIFIER);
    }

    public S124v2DataSetDto mapWarningToS124DataSet(String lang, Warning warning, String fileIdentifier) {
        final S124v2DataSetDto dataSet = new S124v2DataSetDto();
        // Add Preamble
        final String preambleId = formatNWPreambleId(warning) + "." + getLanguageCode(lang);

        // add S100:DatasetIdentificationInformation
        dataSet.setIdentificationInformation(
            S100DatasetIdentificationInformation.builder()
                .datasetFileIdentifier(fileIdentifier)
                .datasetTitle(warning.formattedWarningFullNumber() + " - " + getLanguageCode(lang))
                .datasetReferenceDate(LocalDate.ofInstant(warning.getPublicationTime(), ZoneId.of("UTC")))
                .datasetTopicCategory(DATASET_TOPIC_CATEGORY)
                .datasetPurpose(DATASET_PURPOSE)
                .updateNumber(UPDATE_NUMBER)
                .build()
        );

        // add members
        final S124v2MemberDto memberDto = S124v2MemberDto.builder()
            .preamble(buildPreamble(lang, warning))
            .build();

        // add in-force or cancelled warnings
        buildReferences(lang, memberDto, warning);

        // Add parts and affected areas
        warning.getFeatureParts().forEach(part -> addFeaturePartAndAffectedAreaToDataSet(lang, warning, preambleId, part, memberDto));

        dataSet.getImember().add(memberDto);
        dataSet.setGmlId(preambleId);

        return dataSet;
    }

    private void addFeaturePartAndAffectedAreaToDataSet(String lang, Warning warning, String preambleId, FeaturePart part, S124v2MemberDto memberDto) {
        Map<String, String> informationByLangMap = new HashMap<>();

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

        informationByLangMap.put(lang, information);

        Set<String> optionalLanguages = warning.getSerie().getOptionalLanguages();

        part.getInformation().stream()
            .filter(localizedText -> !localizedText.getLanguage().equals(lang))
            .filter(localizedText -> optionalLanguages.contains(localizedText.getLanguage()))
            .forEach(localizedText -> {
                if (!localizedText.getText().isEmpty()) {
                    informationByLangMap.put(localizedText.getLanguage(), localizedText.getText());
                }
            });

        String partId = formatNWPartId(preambleId, part.getPosition());
        buildFeaturePart(warning, part, informationByLangMap, partId, memberDto, lang);
        buildAreaAffected(part, partId, memberDto);
    }

    private void buildFeaturePart(Warning warning, FeaturePart part, Map<String, String> informationByLangMap, String partId, S124v2MemberDto memberDto,
        String lang) {
        memberDto.getPart().add(S124v2NWPartDto.builder()
            .gmlId(partId)
            .geometries(handleGeometries(part.getGeometries(), partId))
            .fixedDateRanges(part.getFixedDateRanges().stream().map(
                dateRange -> {
                    final FixedDateRange s124FixedDateRange = dateRange.clone();
                    convertDateRangeToUtc(s124FixedDateRange);
                    return S124v2FixedDateRange.builder()
                        .dateStart(ofNullable(s124FixedDateRange.getDateStart()).map(S124v2FixedDateRange.S124Date::buildDate).orElse(null))
                        .dateEnd(ofNullable(s124FixedDateRange.getDateEnd()).map(S124v2FixedDateRange.S124Date::buildDate).orElse(null))
                        .timeOfDayStart(ofNullable(s124FixedDateRange.getTimeOfDayStart())
                            .map(fixedDateRange -> buildTimePeriod(s124FixedDateRange.getDateStart(), s124FixedDateRange.getTimeOfDayStart())).orElse(null))
                        .timeOfDayEnd(ofNullable(s124FixedDateRange.getTimeOfDayEnd())
                            .map(fixedDateRange -> buildTimePeriod(s124FixedDateRange.getDateEnd(), s124FixedDateRange.getTimeOfDayEnd())).orElse(null))
                        .build();
                }
            ).toList())
            .warningInformation(
                S124v2WarningInformation.builder()
                    .warningHazardTypeDetails(
                        part.getWarningHazardTypeDetails().stream()
                            .map(S124v2WarningInformation.S124NavwarnTypeDetails::from)
                            .toList()
                    )
                    .information(buildInformations(informationByLangMap))
                    .build()
            )
            .header(S124v2NWPartDto.S124PreambleLink.builder()
                .href(formatNWPreambleHeaderLink(warning, lang))
                .build())
            .build()
        );

        memberDto.getPreamble().getTheWarningPart().add(S124Link.builder()
            .href("#" + partId)
            .build());
    }

    private static S124v2FixedDateRange.S124Time buildTimePeriod(LocalDate date, LocalTime time) {
        return S124v2FixedDateRange.S124Time.buildTime(ZonedDateTime.of(date, time, ZoneOffset.UTC));
    }

    private static String formatNWPreambleHeaderLink(Warning warning, String lang) {
        return "#" + formatNWPreambleGmlIdByLanguageCode(lang, warning);
    }

    private List<S124v2WarningInformation.S124Information> buildInformations(Map<String, String> informationByLangMap) {
        return informationByLangMap.entrySet().stream()
            .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
            .map(entry -> S124v2WarningInformation.S124Information.builder()
                .language(getLanguageCode(entry.getKey()))
                .text(entry.getValue())
                .build())
            .toList();
    }

    private void buildAreaAffected(FeaturePart part, String partId, S124v2MemberDto memberDto) {
        final String affectedAreaId = partId + ".AR";
        final List<S124v2Geometry> affectedAreaGeometries = handleGeometries(part.getAffectedAreas(), affectedAreaId);
        if (affectedAreaGeometries.isEmpty()) {
            return;
        }
        memberDto.getAffectedArea().add(S124NWAreaAffectedDto.builder()
            .gmlId(affectedAreaId)
            .geometries(affectedAreaGeometries)
            .impacts(S124NWAreaAffectedDto.S124NWPartLink.builder()
                .href("#" + partId)
                .build())
            .build()
        );

        memberDto.getPart().stream().filter(partDto -> partDto.getGmlId().equals(partId)).findFirst()
            .ifPresent(partDto -> partDto.setAffects(S124Link.builder()
                .href("#" + affectedAreaId)
                .build())
            );
    }

    private List<Warning> getReferencedWarnings(Warning warning, WarningReferenceCategoryEnum categoryFilter) {
        return warning.getWarningReferences().stream().filter(warningReference -> warningReference.getCategory().equals(categoryFilter))
            .map(WarningReference::getWarnings).flatMap(Collection::stream).toList();
    }

    private void buildReferences(String lang, S124v2MemberDto memberDto, Warning warning) {
        // add in-force warnings referenced by current warning if warning hazard type general is IN_FORCE_BULLETIN. Must be reconfirmed with the client
        // current warning is in-force so it is also referenced as reference
        if (IN_FORCE_BULLETIN.equals(warning.getWarningHazardTypeGeneral())) {
            final List<Warning> warningsInForce = newArrayList(getReferencedWarnings(warning, WarningReferenceCategoryEnum.IN_FORCE));
            warningsInForce.add(warning); // add current warning to the list of in-force warnings
            memberDto.getReferences().add(buildReferences(lang, warningsInForce, S124ReferenceCategoryCode.IN_FORCE, warning));
            memberDto.getPreamble().getTheReferences().add(
                S124Link.builder().href("#" + S124ReferenceCategoryCode.IN_FORCE.getLabel()).build()
            );
        } else {
            // add cancelled warnings referenced by current warning
            final List<Warning> warningsCancelled = getReferencedWarnings(warning, WarningReferenceCategoryEnum.CANCELLATION);
            if (!warningsCancelled.isEmpty()) {
                memberDto.getReferences().add(buildReferences(lang, warningsCancelled, S124ReferenceCategoryCode.WARNING_CANCELLATION, warning));
                memberDto.getPreamble().getTheReferences().add(S124Link.builder()
                    .href("#" + S124ReferenceCategoryCode.WARNING_CANCELLATION.getLabel())
                    .build());
            }
        }
    }

    private S124v2NWReferencesDto buildReferences(String lang, List<Warning> warnings, S124ReferenceCategoryCode referenceCategoryCode, Warning warning) {
        final String referencesGmlId = referenceCategoryCode.getLabel();
        final S124v2NWReferencesDto referencesDto = buildNWReferencesDto(warnings, referenceCategoryCode, referencesGmlId);
        referencesDto.setTheWarning(S124Link.builder()
            .href(formatNWPreambleHeaderLink(warning, lang))
            .build());

        return referencesDto;
    }

    private S124v2NWReferencesDto buildReferences(List<Warning> warnings, S124ReferenceCategoryCode referenceCategoryCode) {
        final String referencesGmlId = referenceCategoryCode.getLabel();
        return buildNWReferencesDto(warnings, referenceCategoryCode, referencesGmlId);
    }

    private S124v2NWReferencesDto buildNWReferencesDto(List<Warning> warnings, S124ReferenceCategoryCode referenceCategoryCode,
        String referencesGmlId) {
        return S124v2NWReferencesDto.builder()
            .gmlId(referencesGmlId)
            .messageSeries(buildMessageSeriesIdentifierList(warnings))
            .noMessageOnHand(warnings.isEmpty())
            .referenceCategory(
                S124v2NWReferencesDto.S124ReferenceCategory.builder()
                    .code(referenceCategoryCode.getCode())
                    .build()
            )
            .build();
    }

    private S124v2NWPreambleDto buildPreamble(String lang, Warning warning) {
        final S124v2NWPreambleDto preamble = S124v2NWPreambleDto.builder()
            .gmlId(formatNWPreambleGmlIdByLanguageCode(lang, warning))
            .messageSeriesIdentifier(buildMessageSeriesIdentifier(warning))
            .publicationDateTime(formatDateWithoutMilliseconds(warning.getPublicationTime()))
            .generalAreas(buildGeneralAreas(lang, warning))
            .localities(buildLocalities(lang, warning))
            // if lang is en then intService is true
            .intService(INTERNATIONAL_LANGUAGE.equals(lang))
            .warningHazardTypeGeneral(S124v2NWPreambleDto.S124NavwarnTypeGeneral.from(warning.getWarningHazardTypeGeneral()))
            .build();

        if (warning.getCancellationDate() != null) {
            preamble.setCancellationDate(formatDateWithoutMilliseconds(warning.getCancellationDate()));
        }

        return preamble;
    }

    public static Instant formatDateWithoutMilliseconds(Instant instant) {
        return instant.truncatedTo(ChronoUnit.SECONDS);
    }

    @NotNull
    private static String formatNWPreambleGmlIdByLanguageCode(String lang, Warning warning) {
        return formatNWPreambleGmlId(warning) + "." + getLanguageCode(lang);
    }

    private S124v2MessageSeriesIdentifierDto buildMessageSeriesIdentifier(Warning warning) {
        return S124v2MessageSeriesIdentifierDto.builder()
            .nameOfSeries(warning.getNameOfSeries())
            .warningNumber(warning.getWarningNumber())
            .warningType(
                S124v2MessageSeriesIdentifierDto.S124WarningType.from(fromSerieTypeAndWarningHazardTypeGeneral(warning.getSerie().getType(),
                    warning.getWarningHazardTypeGeneral()))
            )
            .year(warning.getYear())
            .productionAgency(warning.getMaritimeServiceihoProducerCode())
            .country(warning.getCountry())
            .build();
    }

    private static List<S124v2SubAreaDto> buildLocalities(String lang, Warning warning) {
        return getLocalitiesOrderedByGeneralAreas(warning)
            .map(locality -> {
                String localityName;

                if (DEFAULT_LANGUAGE.equals(lang)) {
                    localityName = locality.getName();
                } else {
                    localityName = StringUtils.isBlank(locality.getInternationalName()) ? locality.getName() : locality.getInternationalName();
                }

                return S124v2SubAreaDto.builder()
                    .locationName(S124v2SubAreaDto.LocationName.builder()
                        .text(localityName)
                        .language(getLanguageCode(lang))
                        .build())
                    .build();
            })
            .toList();
    }

    /**
     * Get the localities ordered by general areas
     * All localities are sorted by their general area position and then by their own position
     * @param warning the warning
     * @return a stream of WarningLocalityNames
     */
    public static Stream<WarningLocalityNames> getLocalitiesOrderedByGeneralAreas(Warning warning) {
        return warning.getGeneralAreas().stream()
            .sorted(comparing(GeneralArea::getPosition))
            .flatMap(generalArea -> warning.getLocalities().stream()
                .filter(locality -> locality.getParent().getId().equals(generalArea.getSubArea().getId()))
                .sorted(comparing(WarningLocalityNames::getPosition)));
    }

    private static List<S124v2SubAreaDto> buildGeneralAreas(String lang, Warning warning) {
        return warning.getGeneralAreas().stream()
            .sorted(comparing(GeneralArea::getPosition))
            .map(generalArea -> S124v2SubAreaDto.builder()
                .locationName(S124v2SubAreaDto.LocationName.builder()
                    .text(getGeneralAreaLocalizedName(generalArea, lang))
                    .language(getLanguageCode(lang))
                    .build())
                .build())
            .toList();
    }

    private static String getGeneralAreaLocalizedName(GeneralArea generalArea, String lang) {
        return DEFAULT_LANGUAGE.equals(lang) ? generalArea.getName()
            : StringUtils.isBlank(generalArea.getInternationalName()) ? generalArea.getName() : generalArea.getInternationalName();
    }

    private static List<S124v2Geometry> handleGeometries(Set<Feature> geometries, String partId) {
        List<S124v2Geometry> s124Geometries = new ArrayList<>();
        geometries.forEach(withCounter((position, feature) -> {
            try {
                buildGeometry(partId, position, feature, s124Geometries);
            } catch (ServiceException e) {
                log.error("Failed to build geometry at position {}", position, e);
                throw new RuntimeException("Failed to build geometry at position " + position, e);
            }
        }));

        return s124Geometries;
    }

    private static void buildGeometry(String partId, Integer position, Feature feature, List<S124v2Geometry> s124Geometries) throws ServiceException {
        String geometryId = formatGeometryId(partId, position);
        GML_WRITER.setCustomAttributes(Map.of("gml:id", geometryId));
        Geometry geometry = feature.getGeometry();
        final Point point;
        if (geometry instanceof Point) {
            point = convertGeometryFrom3857To4326(geometry.getCoordinates()[0].x, geometry.getCoordinates()[0].y);
            S100v2PointProperty s100PointProperty = S100v2PointProperty.builder()
                .point(S100v2PointProperty.S100Point.builder()
                    .gmlId(geometryId)
                    .gmlPos(point.getX() + " " + point.getY())
                    .build())
                .build();
            s124Geometries.add(S124v2Geometry.builder().pointProperty(s100PointProperty).build());
        } else if (geometry instanceof Polygon || geometry instanceof MultiPolygon) {
            S100v2SurfaceProperty s100SurfaceProperty;
            if ((feature.getProperties() != null && feature.getProperties().get("isCircle") != null)
                && feature.getProperties().get("isCircle").equals(true)) {
                List<Number> coordinatesCenter = (List<Number>) feature.getProperties().get("center");
                point = convertGeometryFrom3857To4326(coordinatesCenter.get(0).doubleValue(), coordinatesCenter.get(1).doubleValue());
                Number radius = (Number) feature.getProperties().get("radius");
                GmlCircleByCenterPointV2 circleByCenterPoint = GmlCircleByCenterPointV2.builder()
                    .gmlPos(point.getX() + " " + point.getY())
                    .gmlRadius(GmlRadius.builder().uom("m").value(radius.toString()).build())
                    .build();
                try {
                    s100SurfaceProperty = S100v2SurfaceProperty.builder()
                        .surface(S100v2SurfaceProperty.S100Surface.builder()
                            .gmlPatches(GmlPatches.builder()
                                .polygons(singletonList(
                                    GmlPolygonPatch.builder()
                                        .exterior(
                                            GmlPolygonPatch.GmlExterior.builder()
                                                .ring(GmlPolygonPatch.GmlRing.builder()
                                                    .curveMember(GmlPolygonPatch.GmlCurveMember.builder()
                                                        .gmlCurve(GmlPolygonPatch.GmlCurve.builder()
                                                            .gmlId(geometryId + ".1")
                                                            .gmlSegments(GmlPolygonPatch.GmlSegments.builder()
                                                                .text(XML_MAPPER.writeValueAsString(circleByCenterPoint))
                                                                .build())
                                                            .build())
                                                        .build())
                                                    .build())
                                                .build()
                                        )
                                        .build()
                                ))
                                .build())
                            .gmlId(geometryId)
                            .build()
                        )
                        .build();
                } catch (JsonProcessingException e) {
                    log.error("Failed to process circle geometry: {}", e.getMessage());
                    throw new ServiceException("Failed to process circle geometry", e);
                }
            } else {
                s100SurfaceProperty = S100v2SurfaceProperty.builder()
                    .surface(S100v2SurfaceProperty.S100Surface.builder()
                        .gmlPatches(GmlPatches.builder()
                            .polygons(singletonList(
                                GmlPolygonPatch.builder()
                                    .exterior(
                                        GmlPolygonPatch.GmlExterior.builder()
                                            .linearRing(GmlPolygonPatch.GmlLinearRing.builder()
                                                .posList(Arrays.stream(geometry.getCoordinates()).map(S124v2Util::formatCoordinates).collect(Collectors.joining(" ")))
                                                .build())
                                            .build()
                                    )
                                    .build()
                            ))
                            .build())
                        .gmlId(geometryId)
                        .build()
                    )
                    .build();
            }
            s124Geometries.add(S124v2Geometry.builder().surfaceProperty(s100SurfaceProperty).build());
        } else if (geometry instanceof LineString) {
            S100v2CurveProperty s100CurveProperty = S100v2CurveProperty.builder()
                .curve(S100v2CurveProperty.S100Curve.builder()
                    .gmlSegments(S100v2CurveProperty.GmlSegments.builder()
                        .gmlLineStringSegment(S100v2CurveProperty.GmlLineStringSegment.builder()
                            .posList(Arrays.stream(geometry.getCoordinates()).map(S124v2Util::formatCoordinates).collect(Collectors.joining(" ")))
                            .build())
                        .build())
                    .gmlId(geometryId)
                    .build()
                )
                .build();
            s124Geometries.add(S124v2Geometry.builder().curveProperty(s100CurveProperty).build());
        }
    }

    private static String formatCoordinates(Coordinate coordinate) {
        Point coordinatePoint;
        try {
            coordinatePoint = convertGeometryFrom3857To4326(coordinate.x, coordinate.y);
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        }
        return coordinatePoint.getX() + " " + coordinatePoint.getY();
    }

    private static Point convertGeometryFrom3857To4326(double x, double y) throws ServiceException {
        final Point point;
        try {
            point = convertCoordinatesFromEpsg3857To4326(x, y);
        } catch (FactoryException | TransformException e) {
            log.error("Failed to convert coordinates from EPSG:3857 to EPSG:4326", e);
            throw new ServiceException("Failed to convert coordinates from EPSG:3857 to EPSG:4326", e);
        }

        return point;
    }

    private static String formatGeometryId(String preambleId, Integer position) {
        return preambleId + "." + position;
    }

    public S124V2ExchangeSetDto buildS124ExchangeSetDtoFromInForceWarnings(List<Warning> warnings) {
        return S124V2ExchangeSetDto.builder()
            .dataSet(singletonList(mapInForceWarningToS124DataSet(warnings)))
            .build();
    }

    private S124v2DataSetDto mapInForceWarningToS124DataSet(List<Warning> warnings) {
        final S124v2DataSetDto dataSet = new S124v2DataSetDto();
        // Add References
        dataSet.getImember().add(
            S124v2MemberDto.builder()
                .references(singletonList(buildReferences(warnings, S124ReferenceCategoryCode.IN_FORCE)))
                .build()
        );
        return dataSet;
    }

    private List<S124v2MessageSeriesIdentifierDto> buildMessageSeriesIdentifierList(List<Warning> warnings) {
        return warnings.stream().map(this::buildMessageSeriesIdentifier).toList();
    }

    public static <T> Consumer<T> withCounter(BiConsumer<Integer, T> consumer) {
        final AtomicInteger counter = new AtomicInteger(1);
        return item -> consumer.accept(counter.getAndIncrement(), item);
    }
}
