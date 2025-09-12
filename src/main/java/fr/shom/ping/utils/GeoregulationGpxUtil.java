package fr.shom.ping.utils;

import fr.shom.ping.PingApiTranslationsRegistry;
import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.model.Feature;
import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.Georegulation;
import fr.shom.ping.model.GeoregulationPart;
import fr.shom.ping.model.GeoregulationPartTypeEntity;
import fr.shom.ping.properties.GpxProperties;
import fr.shom.ping.properties.SubscriptionNotificationProperties;
import io.jenetics.jpx.Bounds;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.Metadata;
import io.jenetics.jpx.WayPoint;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.servlet.http.HttpServletResponse;
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
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

@Component
public class GeoregulationGpxUtil extends GpxUtil {

    @Autowired
    private PingApiTranslationsRegistry translationsRegistry;

    @Autowired
    private GpxProperties gpxProperties;

    @Autowired
    private SubscriptionNotificationProperties subscriptionNotificationProperties;

    private GeoregulationGpxUtil() {
    }

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public void generateGpx(Georegulation georegulation, String lang, String generatedGpxFolder, HttpServletResponse response) throws ServiceException {
        final String gpxDirectoryPathname = Path.of(generatedGpxFolder, FilenameUtils.getName(GeoregulationUtil.getNumber(georegulation))).toString();
        deleteCurrentGpxDirectory(gpxDirectoryPathname);
        createNewGpxDirectory(gpxDirectoryPathname);
        buildGpxFile(georegulation, lang, gpxDirectoryPathname);
        final String filename = getGpxFilename(georegulation);
        response.addHeader("Content-Disposition", "attachment;filename=" + filename);
        // Copy the stream to the response's output stream.
        try {
            IOUtils.copy(FileUtils.openInputStream(Path.of(gpxDirectoryPathname, FilenameUtils.getName(filename)).toFile()), response.getOutputStream());
        } catch (IOException e) {
            throw new ServiceException("Failed to download gpx file ! " + e.getMessage());
        }
    }

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public void buildGpxFile(Georegulation georegulation, String lang, String generatedGpxFolder) throws ServiceException {
        final LinkedHashMap<GeoregulationPart, List<Geometry>> featurePartGeometriesMap = new LinkedHashMap<>();
        georegulation.getParts().stream().sorted()
            .forEach(featurePart -> {
                final List<Geometry> geometries = featurePart.getFeatures().stream()
                    .map(Feature::getGeometry)
                    .collect(toList());
                featurePartGeometriesMap.put(featurePart, geometries);
            });
        final String filename = FilenameUtils.getName(getGpxFilename(georegulation));
        final String fileFullPath = Path.of(generatedGpxFolder, filename).toString();
        generateGpx(georegulation, lang, featurePartGeometriesMap, fileFullPath);
    }

    public void generateGpx(Georegulation georegulation, String lang, Map<GeoregulationPart, List<Geometry>> featurePartGeometriesMap, String fileFullPath)
        throws ServiceException {
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

        final String accessLink = subscriptionNotificationProperties.getEmailFrontLinkRoot() +
            subscriptionNotificationProperties.getEmailAccessGeoregulationLink() + georegulation.getId();
        final GPX.Builder builder = GPX.builder()
            .metadata(Metadata.builder()
                .name(GeoregulationUtil.getNumber(georegulation))
                .desc(generateMetadataDescription(georegulation, lang))
                .author(georegulation.getIssuingService().getName())
                .bounds(bounds)
                .addLink(accessLink)
                .build());

        final int nbParts = georegulation.getParts().size();
        addTracks(builder, featurePartGeometriesMap, georegulation, lang, accessLink, nbParts);

        final GPX gpx = builder.build();

        writeGpxFile(gpx, fileFullPath);
    }

    public String generateMetadataDescription(Georegulation georegulation, String lang) {
        final boolean internationalLanguage = isInternationalLanguage(lang);
        final StringBuilder description = new StringBuilder(internationalLanguage ? gpxProperties.getEn() : gpxProperties.getFr());
        description.append(GPX_LINE_SEPARATOR);
        description.append(georegulation.getType());
        description.append(GPX_LINE_SEPARATOR);
        return description.toString();
    }

    public String generateTrackDescription(GeoregulationPart featurePart, String lang) {
        final boolean internationalLanguage = isInternationalLanguage(lang);
        final StringBuilder sbTrackDescription = new StringBuilder();
        final boolean hasGeoregulationPartType = !featurePart.getTypes().isEmpty();
        if (hasGeoregulationPartType) {
            // add georegulation type details
            final String warningHazardTypeDetailsLine = featurePart.getTypes().stream()
                .map(type -> translationsRegistry.getGeoregulationPartTypeTranslation(type, internationalLanguage ?
                    GeoregulationPartTypeEntity::getInternationalTranslation : GeoregulationPartTypeEntity::getTranslation))
                .collect(Collectors.joining(" - "));
            sbTrackDescription.append(warningHazardTypeDetailsLine).append(GPX_LINE_SEPARATOR);
        }

        final boolean hasCalendarPeriods = !featurePart.getCalendarPeriods().isEmpty();
        if (hasCalendarPeriods) {
            // add calendar periods
            final String dateRangesLines = featurePart.getCalendarPeriods().stream()
                .sorted(comparing(FixedDateRange::getPosition))
                .map(fixedDateRange -> generateFixedDateRange(fixedDateRange, internationalLanguage, true))
                .collect(Collectors.joining(GPX_LINE_SEPARATOR));
            sbTrackDescription.append(hasGeoregulationPartType ? dateRangesLines : dateRangesLines.substring(SPACES_FOR_FEATURE_INDENT))
                .append(GPX_LINE_SEPARATOR);
        }

        return sbTrackDescription.toString();
    }

    public void addTracks(GPX.Builder builder, Map<GeoregulationPart, List<Geometry>> featurePartGeometriesMap, Georegulation georegulation, String lang,
        String accessLink, int nbParts) {
        featurePartGeometriesMap.forEach((featurePart, geometries) -> {
            final String trackName = GeoregulationUtil.getNumber(georegulation) + " #" + featurePart.getPosition() + "/" + nbParts;
            final String description = generateTrackDescription(featurePart, lang);
            addTrackByGeometryType(builder, geometries, Point.class, trackName, description, accessLink);
            addTrackByGeometryType(builder, geometries, LineString.class, trackName, description, accessLink);
            addTrackByGeometryType(builder, geometries, Polygon.class, trackName, description, accessLink);
        });
    }

    public static String getGpxFilename(Georegulation georegulation) {
        return GeoregulationUtil.getNumber(georegulation) + ".gpx";
    }
}
