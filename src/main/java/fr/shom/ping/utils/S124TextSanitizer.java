package fr.shom.ping.utils;

import fr.shom.ping.model.Feature;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import static fr.shom.ping.utils.GpxUtil.convertCoordinatesFromEpsg3857To4326;

/**
 * PISAC: utilidades de texto y coordenadas para S-124 y TELEX.
 * <ul>
 *   <li>En archivos GML S-124 (español e inglés), el campo {@code <S124:text>} no debe incluir
 *       coordenadas porque la geometría ya viaja estructurada en {@code <S124:geometry>}.</li>
 *   <li>En TELEX (y NAVTEX), si el texto del mensaje no incluye coordenadas, se formatean desde
 *       las geometrías de la parte en formato S-53 para que siempre acompañen al mensaje.</li>
 * </ul>
 */
@Slf4j
public final class S124TextSanitizer {

    private static final String NUMBER = "\\d{1,3}(?:[.,]\\d+)?";

    /** Coordenada en formato S-53 (DD-MM.mmN / DDD-MM.mmW, con o sin decimales). */
    private static final String S53_COORD = "(?<![\\d.])\\d{1,3}-\\d{1,2}(?:[.,]\\d+)?\\s*[NSEWO](?![\\p{L}\\d])";

    /** Coordenada con símbolo de grado (DMS, DM o DD). */
    private static final String DEGREE_COORD = "(?<![\\d.])" + NUMBER + "\\s*[\\u00B0\\u00BA\\u02DA]\\s*"
        + "(?:" + NUMBER + "\\s*['\\u2032\\u2019\\u00B4]\\s*"
        + "(?:" + NUMBER + "\\s*(?:\"|\\u2033|\\u201D|\\u201C|''|\\u2032\\u2032|\\u2019\\u2019)\\s*)?)?"
        + "[NSEWO](?![\\p{L}\\d])";

    private static final String ANY_COORD = "(?:" + S53_COORD + "|" + DEGREE_COORD + ")";

    private static final String PAIR_SEP = "(?:\\s*[-\\u2013\\u2014,;/]\\s*|\\s+)";

    /** Par latitud/longitud (o coordenada suelta con su separador). */
    private static final String COORD_PAIR = ANY_COORD + "(?:" + PAIR_SEP + ANY_COORD + ")?";

    /** Referencia opcional al datum pegada a una coordenada (ej. WGS-84, (WGS-84 datum), DATUM WGS84, DATUMGWS-84, GS84 DATUM). */
    private static final String OPTIONAL_DATUM =
        "(?:[\\s,]*(?:\\(\\s*(?:datum\\s*)?(?:wgs|gws|gs)[\\s-]*84(?:\\s*datum)?\\s*\\)"
            + "|(?:datum\\s*)?(?:wgs|gws|gs)[\\s-]*84(?:\\s*datum)?"
            + "|datum\\s*(?:wgs|gws|gs)[\\s-]*84))?";

    private static final Pattern ANY_COORD_PATTERN = Pattern.compile(ANY_COORD, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Línea compuesta únicamente por coordenadas (más numeración/viñetas/separadores/datum opcionales). */
    private static final Pattern COORD_ONLY_LINE = Pattern.compile(
        "^\\s*(?:(?:[\\-*\\u2022]|[A-Za-z0-9]{1,3}[.)])\\s*)?"
            + ANY_COORD
            + "(?:(?:\\s*(?:[-\\u2013\\u2014,;/:&]|y|and)\\s*|\\s+)" + ANY_COORD + ")*"
            + OPTIONAL_DATUM
            + "\\s*[.,;:]?\\s*$",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    /** Línea que solo contiene el datum (ej. "DATUM WGS84") justo después de un bloque de coordenadas. */
    private static final Pattern DATUM_ONLY_LINE = Pattern.compile(
        "^\\s*\\(?\\s*(?:datum\\s*)?(?:wgs|gws|gs)[\\s-]*84(?:\\s*datum)?\\s*\\)?\\s*[.,;:]?\\s*$",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    /** Línea completa de geometría circular generada por el asistente (centro y radio ya van en CircleByCenterPoint). */
    private static final Pattern STANDALONE_CIRCLE_LINE = Pattern.compile(
        "^\\s*(?:(?:[áa]rea(?:\\s+afectada)?|affected\\s+area|area)\\s+(?:dentro\\s+de\\s+un\\s+radio\\s+de|within\\s+a\\s+radius\\s+of)"
            + "\\s+\\d+(?:[.,]\\d+)?\\s*(?:mn|nm|metros?|meters?|m)\\s+(?:de|of)\\s+)"
            + COORD_PAIR + OPTIONAL_DATUM + "\\s*[.,;:]?\\s*$",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    /** Encabezado puro que introduce un bloque de coordenadas en las líneas siguientes. */
    private static final Pattern PURE_COORD_HEADER_LINE = Pattern.compile(
        "^\\s*(?:"
            + "(?:[áa]rea(?:\\s+afectada)?\\s+delimitada\\s+por"
            + "|l[íi]nea(?:\\s+afectada)?\\s+que\\s+une"
            + "|(?:ubicad[oa]s?|localizad[oa]s?|situad[oa]s?)?\\s*en\\s+(?:las?\\s+)?(?:siguientes?\\s+)?posici(?:[óo]n|ones)(?:\\s+geogr[áa]fic[áaíi]?s?)?"
            + "|posici(?:[óo]n|ones)(?:\\s+geogr[áa]fic[áaíi]?s?)?"
            + "|coordenadas?)"
            + "|"
            + "(?:(?:affected\\s+)?area\\s+bounded\\s+by"
            + "|(?:affected\\s+)?line\\s+joining"
            + "|(?:located\\s+)?(?:in|at|to)\\s+(?:the\\s+)?(?:following\\s+)?(?:geographic\\s+)?(?:positions?|coordinates?)"
            + "|(?:geographic\\s+)?positions?"
            + "|coordinates?)"
            + ")\\s*:?\\s*$",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    /** Frase final de una línea que introduce un bloque de coordenadas en las líneas siguientes. */
    private static final Pattern TRAILING_COORD_INTRO_ES = Pattern.compile(
        "(?:[;,\\s]+(?:ubicad[oa]s?|localizad[oa]s?|situad[oa]s?)?\\s*(?:dentro\\s+del?|en\\s+(?:el|la|las|los))?\\s*"
            + "(?:(?:[áa]rea|zona)\\s+delimitada\\s+por|l[íi]nea\\s+que\\s+une)"
            + "(?:\\s+(?:las\\s+siguientes\\s+coordenadas|los\\s+siguientes\\s+puntos|las\\s+siguientes\\s+posiciones))?"
            + "|[;,\\s]+(?:ubicad[oa]s?|localizad[oa]s?|situad[oa]s?)?\\s*en\\s+(?:las?\\s+)?(?:siguientes?\\s+)?(?:posici(?:[óo]n|ones)(?:\\s+geogr[áa]fic[áaíi]?s?)?|coordenadas?)"
            + ")\\s*:?\\s*$",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern TRAILING_COORD_INTRO_EN = Pattern.compile(
        "(?:(?:[;,]\\s*they\\s+were)?[;,\\s]+(?:located\\s+)?(?:within|in|at|to)\\s+(?:the\\s+)?(?:area\\s+bounded\\s+by|line\\s+joining)"
            + "(?:\\s+(?:the\\s+)?(?:following\\s+)?(?:coordinates?|positions?|points?))?"
            + "|(?:[;,]\\s*they\\s+were)?[;,\\s]+(?:located\\s+)?(?:at|in|to)\\s+(?:the\\s+)?(?:following\\s+)?(?:geographic\\s+)?(?:positions?|coordinates?)"
            + ")\\s*:?\\s*$",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    /** Preposición colgante al final de la línea previa cuando la línea siguiente era un encabezado puro eliminado (ej. "... en el\nárea delimitada por:"). */
    private static final Pattern TRAILING_DANGLING_PREPOSITION = Pattern.compile(
        "\\s+(?:en\\s+el|en\\s+la|en\\s+los|en\\s+las|dentro\\s+del|in\\s+the|within\\s+the|at\\s+the)\\s*$",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    /** Cláusula inline con coordenada (español o inglés), tolerando salto de línea entre "ubicado/located" y "en posición/at position". */
    private static final Pattern INLINE_COORD_CLAUSE = Pattern.compile(
        "(?<leadComma>,)?\\s*"
            + "(?:"
            + "(?:(?:ubicad[oa]s?|localizad[oa]s?|situad[oa]s?)\\s+)?(?:[áa]rea\\s+afectada\\s+)?en\\s+(?:la\\s+)?posici(?:[óo]n|on)(?:\\s+geogr[áa]fic[áaíi]?)?\\s*:?\\s*"
            + "|(?:(?:ubicad[oa]s?|localizad[oa]s?|situad[oa]s?)\\s+en\\s+)"
            + "|(?:located\\s+)?(?:at|in|to)\\s+(?:(?:the\\s+)?(?:geographic\\s+)?position\\s*:?\\s*)?"
            + "|(?:affected\\s+area\\s+in\\s+position\\s+)"
            + ")"
            + COORD_PAIR + OPTIONAL_DATUM
            + "(?<trailComma>\\s*,)?",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    /** Cualquier par de coordenadas (o coordenada suelta) restante con datum opcional. */
    private static final Pattern BARE_COORD_WITH_DATUM = Pattern.compile(
        COORD_PAIR + OPTIONAL_DATUM,
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final double METERS_PER_NAUTICAL_MILE = 1852.0;

    private S124TextSanitizer() {
    }

    /**
     * Indica si el texto ya contiene al menos una coordenada geográfica (S-53 o con símbolo de grado).
     */
    public static boolean containsCoordinates(final String text) {
        return StringUtils.isNotBlank(text) && ANY_COORD_PATTERN.matcher(text).find();
    }

    /**
     * Elimina las coordenadas geográficas (y sus cláusulas introductorias / datum asociados) de un texto
     * para poblar {@code <S124:text>} en español o inglés, ya que la geometría se codifica en {@code <S124:geometry>}.
     */
    public static String stripCoordinates(final String text) {
        if (text == null || text.isEmpty() || !containsCoordinates(text)) {
            return text;
        }

        // 1. Procesar bloques multilínea (líneas de solo coordenadas, círculos, datum y sus encabezados)
        final String[] rawLines = text.split("\\r?\\n", -1);
        final List<String> keptLines = new ArrayList<>();
        boolean lastRemovedWasCoord = false;

        for (String rawLine : rawLines) {
            if (COORD_ONLY_LINE.matcher(rawLine).matches() || STANDALONE_CIRCLE_LINE.matcher(rawLine).matches()) {
                if (!lastRemovedWasCoord && !keptLines.isEmpty()) {
                    trimPrecedingCoordinateIntro(keptLines);
                }
                lastRemovedWasCoord = true;
                continue;
            }
            if (lastRemovedWasCoord && DATUM_ONLY_LINE.matcher(rawLine).matches()) {
                continue;
            }
            lastRemovedWasCoord = false;
            keptLines.add(rawLine);
        }

        String result = String.join("\n", keptLines);

        // 2. Eliminar cláusulas inline con coordenadas (ej. ", located at 10-29.40N 068-00.52W (WGS-84 datum),")
        result = removeInlineCoordinateClauses(result);

        // 3. Eliminar cualquier coordenada suelta restante
        result = BARE_COORD_WITH_DATUM.matcher(result).replaceAll("");

        // 4. Normalizar espacios, puntuación y líneas vacías
        return cleanupAfterStripping(result);
    }

    private static void trimPrecedingCoordinateIntro(final List<String> keptLines) {
        final int lastIdx = keptLines.size() - 1;
        final String prev = keptLines.get(lastIdx);

        if (PURE_COORD_HEADER_LINE.matcher(prev).matches()) {
            keptLines.remove(lastIdx);
            if (!keptLines.isEmpty()) {
                final int prevIdx = keptLines.size() - 1;
                final String beforeHeader = TRAILING_DANGLING_PREPOSITION.matcher(keptLines.get(prevIdx)).replaceFirst("");
                keptLines.set(prevIdx, beforeHeader);
            }
            return;
        }

        String trimmed = TRAILING_COORD_INTRO_ES.matcher(prev).replaceFirst("");
        trimmed = TRAILING_COORD_INTRO_EN.matcher(trimmed).replaceFirst("");
        keptLines.set(lastIdx, trimmed);
    }

    private static String removeInlineCoordinateClauses(final String input) {
        final Matcher matcher = INLINE_COORD_CLAUSE.matcher(input);
        final StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            final boolean hasLeadComma = matcher.group("leadComma") != null;
            final boolean hasTrailComma = matcher.group("trailComma") != null;
            final boolean spannedNewline = matcher.group().contains("\n");
            // Si había coma antes y después (inciso entre comas), quitamos ambas comas.
            // Si solo había una coma (p. ej. ", at position X." o "X at position Y, is..."), no dejamos coma huérfana.
            final String replacement;
            if (spannedNewline) {
                replacement = "\n";
            } else if (hasLeadComma && hasTrailComma) {
                replacement = " ";
            } else {
                replacement = " ";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String cleanupAfterStripping(final String input) {
        final String[] lines = input.split("\\r?\\n", -1);
        final List<String> cleaned = new ArrayList<>();
        for (String line : lines) {
            String s = line
                .replaceAll("[ \\t]{2,}", " ")
                .replaceAll("\\s+([.,;:])", "$1")
                .replaceAll("([.,;:])\\s*\\1+", "$1")
                .trim();
            if (!s.isEmpty() && !s.matches("^[.,;:]+$")) {
                cleaned.add(s);
            }
        }
        return String.join("\n", cleaned).trim();
    }

    /**
     * Formatea las geometrías de una parte en texto S-53 (DD-MM.mmN DDD-MM.mmW) para anexarlas al TELEX
     * cuando el mensaje escrito no incluye coordenadas explícitamente.
     */
    public static String formatGeometriesForText(final Set<Feature> geometries, final boolean internationalLanguage) {
        if (geometries == null || geometries.isEmpty()) {
            return "";
        }
        final List<String> lines = new ArrayList<>();
        for (Feature feature : geometries) {
            if (feature == null || feature.getGeometry() == null) {
                continue;
            }
            final Geometry geometry = feature.getGeometry();
            if (geometry instanceof Point) {
                final Coordinate c = geometry.getCoordinates()[0];
                final String coord = formatCoordinateS53(c.x, c.y);
                if (coord != null) {
                    lines.add((internationalLanguage ? "IN POSITION " : "EN POSICION ") + coord);
                }
            } else if (geometry instanceof Polygon || geometry instanceof MultiPolygon) {
                if (feature.getProperties() != null && Boolean.TRUE.equals(feature.getProperties().get("isCircle"))) {
                    @SuppressWarnings("unchecked")
                    final List<Number> center = (List<Number>) feature.getProperties().get("center");
                    final Number radius = (Number) feature.getProperties().get("radius");
                    if (center != null && center.size() >= 2) {
                        final String coord = formatCoordinateS53(center.get(0).doubleValue(), center.get(1).doubleValue());
                        if (coord != null && radius != null) {
                            final String radiusNm = String.format(Locale.ROOT, "%.1f", radius.doubleValue() / METERS_PER_NAUTICAL_MILE);
                            lines.add(internationalLanguage
                                ? "AREA WITHIN A RADIUS OF " + radiusNm + " NM OF " + coord
                                : "AREA DENTRO DE UN RADIO DE " + radiusNm + " MN DE " + coord);
                        } else if (coord != null) {
                            lines.add((internationalLanguage ? "IN POSITION " : "EN POSICION ") + coord);
                        }
                    }
                } else {
                    final Coordinate[] coords = geometry.getCoordinates();
                    final int len = coords.length > 3 ? coords.length - 1 : coords.length;
                    final List<String> formatted = new ArrayList<>();
                    for (int i = 0; i < len; i++) {
                        final String coord = formatCoordinateS53(coords[i].x, coords[i].y);
                        if (coord != null) {
                            formatted.add(coord);
                        }
                    }
                    if (!formatted.isEmpty()) {
                        lines.add(internationalLanguage ? "AREA BOUNDED BY:" : "AREA DELIMITADA POR:");
                        lines.addAll(formatted);
                    }
                }
            } else if (geometry instanceof LineString) {
                final Coordinate[] coords = geometry.getCoordinates();
                final List<String> formatted = new ArrayList<>();
                for (Coordinate c : coords) {
                    final String coord = formatCoordinateS53(c.x, c.y);
                    if (coord != null) {
                        formatted.add(coord);
                    }
                }
                if (!formatted.isEmpty()) {
                    lines.add(internationalLanguage ? "LINE JOINING:" : "LINEA QUE UNE:");
                    lines.addAll(formatted);
                }
            }
        }
        return String.join("\n", lines);
    }

    private static String formatCoordinateS53(final double x, final double y) {
        final double lat;
        final double lon;
        if (Math.abs(x) > 180.0 || Math.abs(y) > 90.0) {
            try {
                // GeoTools CRS.decode("EPSG:4326") devuelve lat en X y lon en Y
                final Point p = convertCoordinatesFromEpsg3857To4326(x, y);
                lat = p.getX();
                lon = p.getY();
            } catch (FactoryException | TransformException e) {
                log.warn("No se pudo convertir coordenada ({}, {}) a EPSG:4326 para TELEX: {}", x, y, e.getMessage());
                return null;
            }
        } else {
            // Ya está en lon/lat (EPSG:4326)
            lon = x;
            lat = y;
        }
        return formatGps(lat, true) + " " + formatGps(lon, false);
    }

    private static String formatGps(final double deg, final boolean isLat) {
        final String dir = isLat ? (deg >= 0 ? "N" : "S") : (deg >= 0 ? "E" : "W");
        final double absDeg = Math.abs(deg);
        int degrees = (int) Math.floor(absDeg);
        BigDecimal minutes = BigDecimal.valueOf((absDeg - degrees) * 60.0).setScale(2, RoundingMode.HALF_UP);
        if (minutes.compareTo(BigDecimal.valueOf(60)) >= 0) {
            degrees++;
            minutes = BigDecimal.ZERO.setScale(2);
        }
        return String.format(Locale.ROOT, isLat ? "%02d-%05.2f%s" : "%03d-%05.2f%s", degrees, minutes, dir);
    }
}
