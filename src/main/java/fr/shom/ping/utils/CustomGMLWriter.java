package fr.shom.ping.utils;

import lombok.Setter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.gml2.GMLConstants;
import org.locationtech.jts.util.Assert;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

public class CustomGMLWriter {
    private static final String INDENT = "  ";

    private static final String COORDINATE_SEPARATOR = ",";

    private static final String TUPLE_SEPARATOR = " ";

    private int startingIndentIndex = 0;

    private int maxCoordinatesPerLine = 10;

    private boolean emitNamespace = false;

    private boolean isRootTag = false;

    private String prefix = GMLConstants.GML_PREFIX;
    private String namespace = GMLConstants.GML_NAMESPACE;
    private String srsName = null;

    @Setter
    private Map<String, String> customAttributes = new HashMap<>();

    private String[] customElements = null;

    /**
     * Creates a writer which outputs GML with default settings.
     * The defaults are:
     * <ul>
     * <li>the namespace prefix is <tt>gml:</tt>
     * <li>no namespace prefix declaration is written
     * <li>no <tt>srsName</tt> attribute is written
     * </ul>
     */
    public CustomGMLWriter() {
    }

    /**
     * Creates a writer which may emit the GML namespace prefix
     * declaration in the geometry root element.
     *
     * @param emitNamespace true if the GML namespace prefix declaration should be written
     *  in the geometry root element
     */
    public CustomGMLWriter(boolean emitNamespace) {
        this.setNamespace(emitNamespace);
    }

    public void resetCustomAttributes() {
        customAttributes = new HashMap<>();
    }

    /**
     * Specifies the namespace prefix to write on each GML tag.
     * A null or blank prefix may be used to indicate no prefix.
     * <p>
     * The default is to write <tt>gml:</tt> as the namespace prefix.
     *
     * @param prefix the namespace prefix to use (<tt>null</tt> or blank if none)
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Sets the value of the <tt>srsName</tt> attribute
     * to be written into the root geometry tag.
     * If the value is <tt>null</tt> or blank no srsName attribute will be written.
     * The provided value must be a valid XML attribute value
     * - it will not be XML-escaped.
     * <p>
     * The default is not to write the <tt>srsName</tt> attribute.
     *
     * @param srsName the srsName attribute value
     */
    public void setSrsName(String srsName) {
        this.srsName = srsName;
    }

    /**
     * Determines whether a GML namespace declaration will be written in the
     * opening tag of geometries.  Useful in XML-aware environments which
     * parse the geometries before use, such as XSLT.
     *
     * @param emitNamespace true if the GML namespace prefix declaration
     * should be written in the root geometry element
     */
    public void setNamespace(boolean emitNamespace) {
        this.emitNamespace = emitNamespace;
    }

    /**
     * Specifies a list of custom elements
     * which are written after the opening tag
     * of the root element.
     * The text contained in the string sequence should form valid XML markup.
     * The specified strings are written one per line immediately after
     * the root geometry tag line.
     * <p>
     * For instance, this is useful for adding KML-specific geometry parameters
     * such as <tt>&lt;extrude&gt;</tt>
     *
     * @param customElements a list of the custom element strings, or null if none
     */
    public void setCustomElements(String[] customElements) {
        this.customElements = customElements.clone();
    }

    /**
     * Sets the starting column index for pretty printing
     *
     * @param indent
     */
    public void setStartingIndentIndex(int indent) {
        if (indent < 0) {
            indent = 0;
        }
        startingIndentIndex = indent;
    }

    /**
     * Sets the number of coordinates printed per line.
     *
     * @param num
     */
    public void setMaxCoordinatesPerLine(int num) {
        if (num < 1) {
            throw new IndexOutOfBoundsException("Invalid coordinate count per line, must be > 0");
        }
        maxCoordinatesPerLine = num;
    }

    /**
     * Writes a {@link Geometry} in GML2 format to a String.
     *
     * @param geom
     * @return String GML2 Encoded Geometry
     */
    public String write(Geometry geom) {
        StringWriter writer = new StringWriter();
        try {
            write(geom, writer);
        } catch (IOException ex) {
            Assert.shouldNeverReachHere();
        }
        return writer.toString();
    }

    /**
     * Writes a {@link Geometry} in GML2 format into a {@link Writer}.
     *
     * @param geom Geometry to encode
     * @param writer Stream to encode to.
     * @throws IOException
     */
    public void write(Geometry geom, Writer writer) throws IOException {
        write(geom, writer, startingIndentIndex);
    }

    private void write(Geometry geom, Writer writer, int level) throws IOException {
        isRootTag = true;
        if (geom == null) {
            throw new IllegalArgumentException("Unhandled geometry type: " + ofNullable(geom).orElseThrow(NullPointerException::new).getGeometryType());
        } else if (geom instanceof Point point) {
            writePoint(point, writer, level);
        } else if (geom instanceof LineString lineString) {
            writeLineString(lineString, writer, level);
        } else if (geom instanceof Polygon polygon) {
            writePolygon(polygon, writer, level);
        } else if (geom instanceof MultiPoint multiPoint) {
            writeMultiPoint(multiPoint, writer, level);
        } else if (geom instanceof MultiLineString multiLineString) {
            writeMultiLineString(multiLineString, writer, level);
        } else if (geom instanceof MultiPolygon multiPolygon) {
            writeMultiPolygon(multiPolygon, writer, level);
        } else if (geom instanceof GeometryCollection geometryCollection) {
            writeGeometryCollection(geometryCollection, writer, startingIndentIndex);
        } else {
            throw new IllegalArgumentException("Unhandled geometry type: " + ofNullable(geom).orElseThrow(NullPointerException::new).getGeometryType());
        }
        writer.flush();
    }

    // <gml:Point><gml:coordinates>1195156.78946687,382069.533723461</gml:coordinates></gml:Point>
    private void writePoint(Point p, Writer writer, int level) throws IOException {
        startLine(level, writer);
        startGeomTag(GMLConstants.GML_POINT, p, writer);

        write(new Coordinate[] { p.getCoordinate() }, writer, level + 1);

        startLine(level, writer);
        endGeomTag(GMLConstants.GML_POINT, writer);
    }

    //<gml:LineString><gml:coordinates>1195123.37289257,381985.763974674 1195120.22369473,381964.660533343 1195118.14929823,381942.597718511</gml:coordinates></gml:LineString>
    private void writeLineString(LineString ls, Writer writer, int level) throws IOException {
        startLine(level, writer);
        startGeomTag(GMLConstants.GML_LINESTRING, ls, writer);

        write(ls.getCoordinates(), writer, level + 1);

        startLine(level, writer);
        endGeomTag(GMLConstants.GML_LINESTRING, writer);
    }

    //<gml:LinearRing><gml:coordinates>1226890.26761027,1466433.47430292 1226880.59239079,1466427.03208053...></coordinates></gml:LinearRing>
    private void writeLinearRing(LinearRing lr, Writer writer, int level) throws IOException {
        startLine(level, writer);
        startGeomTag(GMLConstants.GML_LINEARRING, lr, writer);

        write(lr.getCoordinates(), writer, level + 1);

        startLine(level, writer);
        endGeomTag(GMLConstants.GML_LINEARRING, writer);
    }

    private void writePolygon(Polygon p, Writer writer, int level) throws IOException {
        startLine(level, writer);
        startGeomTag(GMLConstants.GML_POLYGON, p, writer);

        startLine(level + 1, writer);
        startGeomTag(GMLConstants.GML_OUTER_BOUNDARY_IS, null, writer);

        writeLinearRing(p.getExteriorRing(), writer, level + 2);

        startLine(level + 1, writer);
        endGeomTag(GMLConstants.GML_OUTER_BOUNDARY_IS, writer);

        for (int t = 0; t < p.getNumInteriorRing(); t++) {
            startLine(level + 1, writer);
            startGeomTag(GMLConstants.GML_INNER_BOUNDARY_IS, null, writer);

            writeLinearRing(p.getInteriorRingN(t), writer, level + 2);

            startLine(level + 1, writer);
            endGeomTag(GMLConstants.GML_INNER_BOUNDARY_IS, writer);
        }

        startLine(level, writer);
        endGeomTag(GMLConstants.GML_POLYGON, writer);
    }

    private void writeMultiPoint(MultiPoint mp, Writer writer, int level) throws IOException {
        startLine(level, writer);
        startGeomTag(GMLConstants.GML_MULTI_POINT, mp, writer);

        for (int t = 0; t < mp.getNumGeometries(); t++) {
            startLine(level + 1, writer);
            startGeomTag(GMLConstants.GML_POINT_MEMBER, null, writer);

            writePoint((Point) mp.getGeometryN(t), writer, level + 2);

            startLine(level + 1, writer);
            endGeomTag(GMLConstants.GML_POINT_MEMBER, writer);
        }
        startLine(level, writer);
        endGeomTag(GMLConstants.GML_MULTI_POINT, writer);
    }

    private void writeMultiLineString(MultiLineString mls, Writer writer, int level) throws IOException {
        startLine(level, writer);
        startGeomTag(GMLConstants.GML_MULTI_LINESTRING, mls, writer);

        for (int t = 0; t < mls.getNumGeometries(); t++) {
            startLine(level + 1, writer);
            startGeomTag(GMLConstants.GML_LINESTRING_MEMBER, null, writer);

            writeLineString((LineString) mls.getGeometryN(t), writer, level + 2);

            startLine(level + 1, writer);
            endGeomTag(GMLConstants.GML_LINESTRING_MEMBER, writer);
        }
        startLine(level, writer);
        endGeomTag(GMLConstants.GML_MULTI_LINESTRING, writer);
    }

    private void writeMultiPolygon(MultiPolygon mp, Writer writer, int level) throws IOException {
        startLine(level, writer);
        startGeomTag(GMLConstants.GML_MULTI_POLYGON, mp, writer);

        for (int t = 0; t < mp.getNumGeometries(); t++) {
            startLine(level + 1, writer);
            startGeomTag(GMLConstants.GML_POLYGON_MEMBER, null, writer);

            writePolygon((Polygon) mp.getGeometryN(t), writer, level + 2);

            startLine(level + 1, writer);
            endGeomTag(GMLConstants.GML_POLYGON_MEMBER, writer);
        }
        startLine(level, writer);
        endGeomTag(GMLConstants.GML_MULTI_POLYGON, writer);
    }

    private void writeGeometryCollection(GeometryCollection gc, Writer writer, int level) throws IOException {
        startLine(level, writer);
        startGeomTag(GMLConstants.GML_MULTI_GEOMETRY, gc, writer);

        for (int t = 0; t < gc.getNumGeometries(); t++) {
            startLine(level + 1, writer);
            startGeomTag(GMLConstants.GML_GEOMETRY_MEMBER, null, writer);

            write(gc.getGeometryN(t), writer, level + 2);

            startLine(level + 1, writer);
            endGeomTag(GMLConstants.GML_GEOMETRY_MEMBER, writer);
        }
        startLine(level, writer);
        endGeomTag(GMLConstants.GML_MULTI_GEOMETRY, writer);
    }

    /**
     * Takes a list of coordinates and converts it to GML.<br>
     * 2d and 3d aware.
     *
     * @param coords array of coordinates
     * @throws IOException
     */
    private void write(Coordinate[] coords, Writer writer, int level) throws IOException {
        startLine(level, writer);
        startGeomTag(GMLConstants.GML_COORDINATES, null, writer);

        int dim = 2;

        if (coords.length > 0 && !Double.isNaN(coords[0].getZ())) {
             dim = 3;
        }

        boolean isNewLine = true;
        for (int i = 0; i < coords.length; i++) {
            if (isNewLine) {
                startLine(level + 1, writer);
                isNewLine = false;
            }
            if (dim == 2) {
                writer.write("" + coords[i].x);
                writer.write(COORDINATE_SEPARATOR);
                writer.write("" + coords[i].y);
            } else {
                writer.write("" + coords[i].x);
                writer.write(COORDINATE_SEPARATOR);
                writer.write("" + coords[i].y);
                writer.write(COORDINATE_SEPARATOR);
                writer.write("" + coords[i].getZ());
            }
            writer.write(TUPLE_SEPARATOR);

            // break output lines to prevent them from getting too long
            if ((i + 1) % maxCoordinatesPerLine == 0 && i < coords.length - 1) {
                writer.write("\n");
                isNewLine = true;
            }
        }

        if (!isNewLine) {
            writer.write("\n");
        }

        startLine(level, writer);
        endGeomTag(GMLConstants.GML_COORDINATES, writer);
    }

    private void startLine(int level, Writer writer) throws IOException {
        for (int i = 0; i < level; i++) {
            writer.write(INDENT);
        }
    }

    private void startGeomTag(String geometryName, Geometry g, Writer writer)
        throws IOException {
        writer.write("<" + ((prefix == null || prefix.isEmpty()) ? "" : prefix + ":"));
        writer.write(geometryName);
        writeAttributes(g, writer);
        writer.write(">\n");
        writeCustomElements(g, writer);
        isRootTag = false;
    }

    private void writeAttributes(Geometry geom, Writer writer) throws IOException {
        if (geom == null) {
            return;
        }

        if (!isRootTag) {
            return;
        }

        if (emitNamespace) {
            writer.write(" xmlns" + ((prefix == null || prefix.isEmpty()) ? "" : ":" + prefix ) + "='" + namespace + "'");
        }
        if (srsName != null && !srsName.isEmpty()) {
            writer.write(" " + GMLConstants.GML_ATTR_SRSNAME + "='" + srsName + "'");
        }

        if (!customAttributes.isEmpty()) {
            for (Map.Entry<String, String> entry : customAttributes.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                writer.write(" " + key + "='" + value + "'");
            }
        }
    }

    private void writeCustomElements(Geometry geom, Writer writer) throws IOException {
        if (geom == null || !isRootTag || customElements == null) {
            return;
        }

        for (String customElement : customElements) {
            writer.write(customElement);
            writer.write("\n");
        }
    }

    private void endGeomTag(String geometryName, Writer writer) throws IOException {
        writer.write("</" + prefix());
        writer.write(geometryName);
        writer.write(">\n");
    }

    private String prefix() {
        if (prefix == null || prefix.isEmpty()) {
            return "";
        }
        return prefix + ":";
    }
}
