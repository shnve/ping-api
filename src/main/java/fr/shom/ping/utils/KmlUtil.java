package fr.shom.ping.utils;

import fr.shom.ping.exception.ServiceException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class KmlUtil {
    private static final String KML_FOLDER_NAME_TOKEN = "__FOLDER_NAME__";
    private static String PLACEMARKS_TOKEN = "__PLACEMARKS_TOKEN__";

    private enum GeomType {
        POINT,
        LINESTRING,
        POLYGON,
        OTHER
    }

    private static final String KML_TEMPLATE =
        "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
            "<Document id=\"root_doc\">\n" +
            "<Folder><name>" + KML_FOLDER_NAME_TOKEN + "</name>\n" +
            PLACEMARKS_TOKEN +
            "</Folder>\n" +
            "</Document>\n" +
            "</kml>";

    private static GeomType getTypeOfGeometry(String geometry) {
        return Arrays.stream(GeomType.values())
            .filter(geomType -> geometry.toUpperCase().contains(geomType.name()))
            .findFirst()
            .orElse(GeomType.OTHER);
    }

    protected static byte[] generateKml(UUID objectId, List<String> geometries, String pointStyle, String polygonStyle, String linestringStyle,
            String defaultStyle) throws ServiceException {
        StringBuffer placemarks = new StringBuffer();

        geometries.forEach(geometry -> {
            String style = "";
            GeomType typeOfGeom = getTypeOfGeometry(geometry);

            switch (typeOfGeom) {
                case POINT:
                    style = pointStyle;
                    break;
                case POLYGON:
                    style = polygonStyle;
                    break;
                case LINESTRING:
                    style = linestringStyle;
                    break;
                default:
                    style = defaultStyle;
            }

            String placemark =
                "<Placemark>\n" +
                    style + "\n" +
                    geometry + "\n" +
                    "</Placemark>\n";

            placemarks.append(placemark);
        });

        String kml = KML_TEMPLATE;
        kml = kml.replace(KML_FOLDER_NAME_TOKEN, objectId.toString());
        kml = kml.replace(PLACEMARKS_TOKEN, placemarks.toString());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(kml.getBytes(UTF_8));
            return out.toByteArray();
        } catch (IOException exception) {
            throw new ServiceException("Failed to write kml file : " + exception.getMessage());
        }
    }
}
