package fr.shom.ping.utils;

import fr.shom.ping.PingApiTranslationsRegistry;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Feature;
import fr.shom.ping.model.FeaturePart;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.Warning;
import fr.shom.ping.properties.GpxProperties;
import fr.shom.ping.properties.SubscriptionNotificationProperties;
import io.jenetics.jpx.Bounds;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Metadata;
import io.jenetics.jpx.WayPoint;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static fr.shom.ping.PingApiTranslationsRegistry.isInternationalLanguage;
import static fr.shom.ping.utils.TelexUtil.generateFixedDateRange;
import static fr.shom.ping.utils.WarningUtil.generateUsualNumber;
import static fr.shom.ping.utils.ZipUtil.generateZipFileAndAttachToServletResponse;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@Component
public final class WarningGpxUtil extends GpxUtil {

    @Autowired
    private PingApiTranslationsRegistry translationsRegistry;

    @Autowired
    private GpxProperties gpxProperties;

    @Autowired
    private SubscriptionNotificationProperties subscriptionNotificationProperties;

    private static final String GPX_LINE_SEPARATOR = "\n";
    private static final int SPACES_FOR_FEATURE_INDENT = 1;

    private WarningGpxUtil() {
    }

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public void generateGpx(Warning warning, String lang, String generatedGpxFolder, HttpServletResponse response) throws ServiceException {
        final String warningDirectoryName = warning.getWarningNumber() + "-" + warning.getYear();
        final String gpxDirectoryPathname = Path.of(generatedGpxFolder, FilenameUtils.getName(warningDirectoryName)).toString();
        deleteCurrentGpxDirectory(gpxDirectoryPathname);
        createNewGpxDirectory(gpxDirectoryPathname);
        buildGpxFiles(Collections.singletonList(warning), lang, gpxDirectoryPathname);
        final String filename = getGpxFilename(warning);
        response.addHeader("Content-Disposition", "attachment;filename=" + filename);
        // Copy the stream to the response's output stream.
        try {
            IOUtils.copy(FileUtils.openInputStream(Path.of(gpxDirectoryPathname, FilenameUtils.getName(filename)).toFile()), response.getOutputStream());
        } catch (IOException e) {
            throw new ServiceException("Failed to download gpx file ! " + e.getMessage());
        }
    }

    public static String getGpxFilename(Warning warning) {
        return warning.getNameOfSeries() + "-" + warning.getWarningNumber() + "-" + warning.getYear() + ".gpx";
    }

    @SuppressWarnings({"findsecbugs:WEAK_MESSAGE_DIGEST_MD5", "findsecbugs:PATH_TRAVERSAL_IN"})
    public void generateGpx(List<Warning> warnings, String lang, String generatedGpxFolder, HttpServletResponse response) throws ServiceException {
        final String warningDirectoryName = DigestUtils.md5Hex(
            warnings.stream().map(warning -> warning.getWarningNumber() + "/" + warning.getYear()).collect(Collectors.joining("-"))
        );
        final String gpxDirectoryPathname = Path.of(generatedGpxFolder, FilenameUtils.getName(warningDirectoryName)).toString();
        deleteCurrentGpxDirectory(gpxDirectoryPathname);
        createNewGpxDirectory(gpxDirectoryPathname);
        buildGpxFiles(warnings, lang, gpxDirectoryPathname);
        generateZipFileAndAttachToServletResponse(gpxDirectoryPathname, response);
    }

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public void buildGpxFiles(List<Warning> warnings, String lang, String generatedGpxFolder) throws ServiceException {
        for (Warning warning : warnings) {
            final LinkedHashMap<FeaturePart, List<Geometry>> featurePartGeometriesMap = new LinkedHashMap<>();
            warning.getFeatureParts().stream().sorted()
                .forEach(featurePart -> {
                    final List<Geometry> geometries = featurePart.getGeometries().stream()
                        .map(Feature::getGeometry)
                        .collect(toList());
                    final List<Geometry> affectedAreas = featurePart.getAffectedAreas().stream()
                        .map(Feature::getGeometry)
                        .collect(toList());
                    final List<Geometry> allGeometries = Stream.of(geometries, affectedAreas).flatMap(Collection::stream).collect(toList());
                    featurePartGeometriesMap.put(featurePart, allGeometries);
                });
            final String filename = FilenameUtils.getName(getGpxFilename(warning));
            final String fileFullPath = Path.of(generatedGpxFolder, filename).toString();
            generateGpx(warning, lang, featurePartGeometriesMap, fileFullPath);
        }
    }

    public void generateGpx(Warning warning, String lang, Map<FeaturePart, List<Geometry>> featurePartGeometriesMap, String fileFullPath)
        throws ServiceException {
        final boolean internationalLanguage = isInternationalLanguage(lang);
        final Envelope env = new Envelope();
        featurePartGeometriesMap.values().stream().flatMap(Collection::stream)
            .forEach(geometry -> env.expandToInclude(geometry.getEnvelopeInternal()));

        Point[] boundsAsPoints;
        try {
            boundsAsPoints = convertCoordinatesFromEpsg3857To4326(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY());
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        } catch (TransformException e) {
            throw new RuntimeException(e);
        }
        final Stream<WayPoint> points = Stream.of(
            WayPoint.of(boundsAsPoints[0].getX(), boundsAsPoints[0].getY()),
            WayPoint.of(boundsAsPoints[1].getX(), boundsAsPoints[1].getY())
        );

        final Bounds bounds = points.collect(Bounds.toBounds());

        final String accessLink =
            subscriptionNotificationProperties.getEmailFrontLinkRoot() + subscriptionNotificationProperties.getEmailAccessWarningLink() + warning.getId();
        final GPX.Builder builder = GPX.builder()
            .metadata(Metadata.builder()
                .name(generateUsualNumber(warning, internationalLanguage))
                .desc(generateMetadataDescription(warning, lang))
                .author(warning.getSerie().getMaritimeService().getName())
                .bounds(bounds)
                .addLink(accessLink)
                .build());

        final int nbParts = warning.getFeatureParts().size();
        addTracks(builder, featurePartGeometriesMap, warning, lang, accessLink, nbParts);

        final GPX gpx = builder.build();

        writeGpxFile(gpx, fileFullPath);
    }

    public String generateMetadataDescription(Warning warning, String lang) {
        final boolean internationalLanguage = isInternationalLanguage(lang);
        final StringBuilder description = new StringBuilder(internationalLanguage ? gpxProperties.getEn() : gpxProperties.getFr());
        description.append(GPX_LINE_SEPARATOR);
        description.append(translationsRegistry.getWarningHazardTypeGeneralByEnumNameAndLang(warning.getWarningHazardTypeGeneral().name(), lang));
        description.append(GPX_LINE_SEPARATOR);
        return description.toString();
    }

    public String generateTrackDescription(FeaturePart featurePart, String lang) {
        final boolean internationalLanguage = isInternationalLanguage(lang);
        final StringBuilder sbTrackDescription = new StringBuilder();
        final boolean hasWarningHazardTypeDetails = !featurePart.getWarningHazardTypeDetails().isEmpty();
        if (hasWarningHazardTypeDetails) {
            // add warning hazard type details
            final String warningHazardTypeDetailsLine = featurePart.getWarningHazardTypeDetails().stream()
                .map(Enum::name)
                .sorted()
                .map(enumName -> translationsRegistry.getWarningHazardTypeDetailsByEnumNameAndLang(enumName, lang))
                .collect(Collectors.joining(" - "));
            sbTrackDescription.append(warningHazardTypeDetailsLine).append(GPX_LINE_SEPARATOR);
        } else {
            sbTrackDescription.append(
                translationsRegistry.getWarningHazardTypeGeneralByEnumNameAndLang(featurePart.getWarning().getWarningHazardTypeGeneral().name(), lang)
            );
        }

        final boolean hasDateRanges = !featurePart.getFixedDateRanges().isEmpty();
        if (hasDateRanges) {
            // add fixed date ranges
            final String dateRangesLines = featurePart.getFixedDateRanges().stream()
                .sorted(comparing(FixedDateRange::getPosition))
                .map(fixedDateRange -> generateFixedDateRange(fixedDateRange, internationalLanguage, true))
                .collect(Collectors.joining(GPX_LINE_SEPARATOR));
            sbTrackDescription.append(hasWarningHazardTypeDetails ? dateRangesLines : dateRangesLines.substring(SPACES_FOR_FEATURE_INDENT))
                .append(GPX_LINE_SEPARATOR);
        }
        return sbTrackDescription.toString();
    }

    public void addTracks(GPX.Builder builder, Map<FeaturePart, List<Geometry>> featurePartGeometriesMap, Warning warning, String lang, String accessLink,
        int nbParts) {
        final boolean internationalLanguage = isInternationalLanguage(lang);
        featurePartGeometriesMap.forEach((featurePart, geometries) -> {
            final String trackName = WarningUtil.generateUsualNumber(warning, internationalLanguage) + " #" + featurePart.getPosition() + "/" + nbParts;
            final String description = generateTrackDescription(featurePart, lang);
            addTrackByGeometryType(builder, geometries, Point.class, trackName, description, accessLink);
            addTrackByGeometryType(builder, geometries, LineString.class, trackName, description, accessLink);
            addTrackByGeometryType(builder, geometries, Polygon.class, trackName, description, accessLink);
        });
    }
}
