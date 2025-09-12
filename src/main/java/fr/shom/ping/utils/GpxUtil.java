package fr.shom.ping.utils;

import fr.shom.ping.exception.ServiceException;
import io.jenetics.jpx.GPX;
import io.jenetics.jpx.TrackSegment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.xmlbeans.XmlException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import static fr.shom.ping.utils.XmlUtil.prettyFormat;
import static java.nio.file.Files.createDirectories;
import static java.util.stream.Collectors.toList;

@Slf4j
public abstract class GpxUtil {

    public final static String DEFAULT_LANGUAGE = "fr";

    protected static final String GPX_LINE_SEPARATOR = "\n";
    protected static final int SPACES_FOR_FEATURE_INDENT = 1;
    protected static final String ITEM_CHIP = "- ";

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public void createNewGpxDirectory(String gpxDirectoryPathname) throws ServiceException {
        try {
            createDirectories(Path.of(FilenameUtils.getFullPath(gpxDirectoryPathname), FilenameUtils.getName(gpxDirectoryPathname)));
        } catch (IOException exception) {
            log.error("Failed to create directory %s".formatted(gpxDirectoryPathname), exception);
            throw new ServiceException("Failed to create directory %s".formatted(gpxDirectoryPathname), exception);
        }
    }

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public void deleteCurrentGpxDirectory(String gpxDirectoryPathname) throws ServiceException {
        try {
            final File currentDirectory = Path.of(FilenameUtils.getFullPath(gpxDirectoryPathname), FilenameUtils.getName(gpxDirectoryPathname)).toFile();
            if (currentDirectory.exists()) {
                FileUtils.deleteDirectory(currentDirectory);
            }
        } catch (IOException exception) {
            throw new ServiceException("Failed to delete current directory : " + exception.getMessage());
        }
    }

    @SuppressWarnings({"findsecbugs:PATH_TRAVERSAL_IN", "findsecbugs:PATH_TRAVERSAL_OUT"})
    public void writeGpxFile(GPX gpx, String filePath) throws ServiceException {
        try (FileOutputStream fos = new FileOutputStream(Path.of(FilenameUtils.getFullPath(filePath), FilenameUtils.getName(filePath)).toString())) {
            fos.write(prettyFormat(GPX.writer().toString(gpx), 2).getBytes());
        } catch (XmlException | IOException exception) {
            throw new ServiceException("Failed to create gpx file : " + exception.getMessage());
        }
    }

    public static Point[] convertCoordinatesFromEpsg3857To4326(double xMin, double yMin, double xMax, double yMax) throws FactoryException,
        TransformException {
        final CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");
        final CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:3857");
        final MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, false);
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 3857);
        final Point minPoint = geometryFactory.createPoint(new Coordinate(xMin, yMin));
        final Point minTargetPoint = (Point) JTS.transform(minPoint, transform);
        final Point maxPoint = geometryFactory.createPoint(new Coordinate(xMax, yMax));
        final Point maxTargetPoint = (Point) JTS.transform(maxPoint, transform);
        return new Point[]{minTargetPoint, maxTargetPoint};
    }

    public static Point convertCoordinatesFromEpsg3857To4326(double x, double y) throws FactoryException, TransformException {
        final CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");
        final CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:3857");
        final MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, false);
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 3857);
        final Point point = geometryFactory.createPoint(new Coordinate(x, y));
        return (Point) JTS.transform(point, transform);
    }

    public void addTrackByGeometryType(GPX.Builder gpxBuilder, List<? extends Geometry> geometries, Class type, String trackName, String description,
        String link) {
        final List<? extends Geometry> filteredGeometries = geometries.stream()
            .filter(geometry -> geometry.getGeometryType().equals(type.getSimpleName()))
            .collect(toList());

        if (filteredGeometries.isEmpty()) {
            return;
        }

        filteredGeometries.forEach(geometry -> {
            switch (type.getSimpleName()) {
                case Point.TYPENAME_POINT:
                    try {
                        addTrackPoint(gpxBuilder, geometry, trackName, description, link);
                    } catch (FactoryException e) {
                        throw new RuntimeException(e);
                    } catch (TransformException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case LineString.TYPENAME_LINESTRING:
                    addTrackLineString(gpxBuilder, geometry, trackName, description, link);
                    break;
                case Polygon.TYPENAME_POLYGON:
                    try {
                        addTrackPolygon(gpxBuilder, geometry, trackName, description, link);
                    } catch (FactoryException e) {
                        throw new RuntimeException(e);
                    } catch (TransformException e) {
                        throw new RuntimeException(e);
                    }
                    break;
            }
        });
    }

    public void addTrackPoint(GPX.Builder gpxBuilder, Geometry geometry, String trackName, String description, String link) throws FactoryException,
        TransformException {
        final Point geometryAsPoint = (Point) geometry;
        final Point point = convertCoordinatesFromEpsg3857To4326(geometryAsPoint.getX(), geometryAsPoint.getY());

        gpxBuilder.addTrack(
            track -> track
                .name(trackName)
                .desc(description)
                .addLink(link)
                .addSegment(segment -> segment
                    .addPoint(p -> p.lat(point.getX()).lon(point.getY()))
                )
        );
    }

    public void addTrackLineString(GPX.Builder gpxBuilder, Geometry geometry, String trackName, String description, String link) {
        final LineString geometryAsLineString = (LineString) geometry;
        final TrackSegment.Builder segmentBuilder = TrackSegment.builder();
        List.of(geometryAsLineString.getCoordinates()).forEach(coordinate -> {
            final Point point;
            try {
                point = convertCoordinatesFromEpsg3857To4326(coordinate.getX(), coordinate.getY());
            } catch (FactoryException e) {
                throw new RuntimeException(e);
            } catch (TransformException e) {
                throw new RuntimeException(e);
            }
            segmentBuilder.addPoint(p -> p.lat(point.getX()).lon(point.getY()));
        });

        gpxBuilder.addTrack(
            track -> track
                .name(trackName)
                .desc(description)
                .addLink(link)
                .addSegment(segmentBuilder.build())
        );
    }

    public void addTrackPolygon(GPX.Builder gpxBuilder, Geometry geometry, String trackName, String description, String link) throws FactoryException,
        TransformException {
        final Polygon geometryAsPolygon = (Polygon) geometry;
        final TrackSegment.Builder segmentBuilder = TrackSegment.builder();
        List.of(geometryAsPolygon.getCoordinates()).forEach(coordinate -> {
            final Point point;
            try {
                point = convertCoordinatesFromEpsg3857To4326(coordinate.getX(), coordinate.getY());
            } catch (FactoryException e) {
                throw new RuntimeException(e);
            } catch (TransformException e) {
                throw new RuntimeException(e);
            }
            segmentBuilder.addPoint(p -> p.lat(point.getX()).lon(point.getY()));
        });

        final Point firstPoint = convertCoordinatesFromEpsg3857To4326(geometryAsPolygon.getCoordinates()[0].getX(),
            geometryAsPolygon.getCoordinates()[0].getY());
        segmentBuilder.addPoint(p -> p.lat(firstPoint.getX()).lon(firstPoint.getY()));

        gpxBuilder.addTrack(
            track -> track
                .name(trackName)
                .desc(description)
                .addLink(link)
                .addSegment(segmentBuilder.build())
        );
    }
}

