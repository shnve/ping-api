package fr.shom.ping.utils;

import fr.shom.ping.exception.ConvertException;
import fr.shom.ping.model.dto.BoundingBoxDto;
import java.util.Arrays;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import static org.locationtech.jts.geom.Geometry.TYPENAME_POLYGON;

public final class GeoUtil {

    private GeoUtil() {
    }

    // coordinates lon/lat
    public static Geometry convertCoordinatesFromEpsg4326To3857(String shape, boolean invertCoordinates, String... pos) throws ConvertException {
        return convertCoordinatesFromAnyEpsgTo3857("EPSG:4326", shape, invertCoordinates, pos);
    }

    // coordinates lon/lat
    public static Geometry convertCoordinatesFromEpsg4326To3857(String shape, String... pos) throws ConvertException {
        return convertCoordinatesFromAnyEpsgTo3857("EPSG:4326", shape, false, pos);
    }

    public static Geometry convertCoordinatesFromAnyEpsgTo3857(String sourceEpsg, String shape, boolean invertCoordinates, String... pos)
        throws ConvertException {
        final CoordinateReferenceSystem targetCRS;
        final CoordinateReferenceSystem sourceCRS;
        final MathTransform transform;
        try {
            targetCRS = CRS.decode("EPSG:3857");
            sourceCRS = CRS.decode(sourceEpsg);
            transform = CRS.findMathTransform(sourceCRS, targetCRS, false);
        } catch (FactoryException e) {
            throw new ConvertException(e);
        }
        final int srid = Integer.parseInt(SurveyUtil.getEpsgCode(sourceEpsg).replace("EPSG:", "").trim());
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), srid);
        Geometry geometry;
        switch (shape) {
            case Point.TYPENAME_POINT:
                geometry = geometryFactory.createPoint(
                    new Coordinate(Double.parseDouble(pos[invertCoordinates ? 1 : 0]), Double.parseDouble(pos[invertCoordinates ? 0 : 1]))
                );
                break;

            case TYPENAME_POLYGON:
                geometry = buildPolygon(geometryFactory, invertCoordinates, pos);
                break;

            default:
                throw new ConvertException("Unknown shape " + shape);
        }
        geometry.setSRID(srid);
        try {
            final Geometry transformed = JTS.transform(geometry, transform);
            transformed.setSRID(3857);
            return transformed;
        } catch (TransformException e) {
            throw new ConvertException(e);
        }
    }

    public static Geometry convertBoundingBoxToPolygon(String sourceEpsg, boolean coordinatesXY, BoundingBoxDto boundingBoxDto) throws ConvertException {
        Double[] pos;
        if (coordinatesXY) {
            pos = new Double[]{
                // NW point
                boundingBoxDto.getNwCorner().getX(),
                boundingBoxDto.getNwCorner().getY(),

                // NE point
                boundingBoxDto.getSeCorner().getX(),
                boundingBoxDto.getNwCorner().getY(),

                // SE point
                boundingBoxDto.getSeCorner().getX(),
                boundingBoxDto.getSeCorner().getY(),

                // SW point
                boundingBoxDto.getNwCorner().getX(),
                boundingBoxDto.getSeCorner().getY(),

                // repeat NW point to close geometry
                boundingBoxDto.getNwCorner().getX(),
                boundingBoxDto.getNwCorner().getY()
            };
        } else {
            pos = new Double[]{
                // NW point
                boundingBoxDto.getNwCorner().getY(),
                boundingBoxDto.getNwCorner().getX(),

                // NE point
                boundingBoxDto.getNwCorner().getY(),
                boundingBoxDto.getSeCorner().getX(),

                // SE point
                boundingBoxDto.getSeCorner().getY(),
                boundingBoxDto.getSeCorner().getX(),

                // SW point
                boundingBoxDto.getSeCorner().getY(),
                boundingBoxDto.getNwCorner().getX(),

                // repeat NW point to close geometry
                boundingBoxDto.getNwCorner().getY(),
                boundingBoxDto.getNwCorner().getX()
            };
        }

        final String[] coordinates = Arrays.stream(pos).map(String::valueOf).toArray(String[]::new);

        if (sourceEpsg.contains("EPSG:3857")) {
            // if already 3857, no transformation required
            final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 3857);
            return buildPolygon(geometryFactory, coordinates);
        }
        return convertCoordinatesFromAnyEpsgTo3857(sourceEpsg, TYPENAME_POLYGON, !coordinatesXY, coordinates);
    }

    public static Geometry buildPolygon(GeometryFactory geometryFactory, String... pos) throws ConvertException {
        return buildPolygon(geometryFactory, false, pos);
    }

    public static Geometry buildPolygon(GeometryFactory geometryFactory, boolean invertCoordinates, String... pos) throws ConvertException {
        final int size = pos.length / 2;
        final Coordinate[] coordinates = new Coordinate[size];
        int idxPos = 0;
        for (int i = 0; i < size; i++) {
            final Coordinate c = new Coordinate();
            if (invertCoordinates) {
                c.y = Double.parseDouble(pos[idxPos++]);
                c.x = Double.parseDouble(pos[idxPos++]);
            } else {
                c.x = Double.parseDouble(pos[idxPos++]);
                c.y = Double.parseDouble(pos[idxPos++]);
            }

            coordinates[i] = c;
        }

        // assume that linear ring is compliant with its definition, last point is equal to first point
        // see https://yandex.com/dev/maps/ymapsml/doc/1.x/ref/reference/gml-LinearRing.html
        LinearRing linearRing;

        final LineString lineString = geometryFactory.createLineString(coordinates);
        if (lineString.isClosed()) {
            try {
                linearRing = geometryFactory.createLinearRing(coordinates);
            } catch (IllegalArgumentException e) {
                throw new ConvertException(e);
            }
        } else {
            // add missing coordinates to close geometry
            final CoordinateSequence sequence = lineString.getCoordinateSequence();
            final Coordinate[] array = new Coordinate[sequence.size() + 1];
            for (int i = 0; i < sequence.size(); i++) {
                array[i] = sequence.getCoordinate(i);
                array[array.length - 1] = sequence.getCoordinate(0);
            }
            linearRing = geometryFactory.createLinearRing(array);
        }

        return geometryFactory.createPolygon(linearRing);
    }
}
