package fr.shom.ping.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normaliza coordenadas escritas en texto libre (campo "Información") al formato S-53
 * {@code DD-MM.mmN DDD-MM.mmW} con 2 decimales de minuto.
 *
 * <p>Formatos reconocidos (el hemisferio es obligatorio y va como sufijo):
 * <ul>
 *   <li>Grados, minutos y segundos: {@code 10°18'44.54"N}</li>
 *   <li>Grados y minutos decimales: {@code 10°18.74'N} o {@code 10°18,74'N}</li>
 *   <li>Grados decimales: {@code 10.3124°N}</li>
 *   <li>S-53 con más de 2 decimales: {@code 10-18.742N} (se redondea a 2)</li>
 * </ul>
 * Se acepta {@code O} (Oeste) como hemisferio y se convierte a {@code W}. Una latitud seguida de una
 * longitud separadas por espacio, {@code -}, {@code ,}, {@code ;} o {@code /} se une con un único espacio.
 * Los valores fuera de rango se dejan sin modificar.
 */
public final class CoordinateTextNormalizer {

    private static final String NUMBER = "\\d{1,3}(?:[.,]\\d+)?";

    /** Coordenada con símbolo de grado: DMS, DM o DD (escapes Unicode para no depender de la codificación del fuente). */
    private static final Pattern DEGREE_TOKEN = Pattern.compile(
        "(?<![\\d.])(?<deg>" + NUMBER + ")\\s*[\\u00B0\\u00BA\\u02DA]\\s*"
            + "(?:(?<min>" + NUMBER + ")\\s*['\\u2032\\u2019\\u00B4]\\s*"
            + "(?:(?<sec>" + NUMBER + ")\\s*(?:\"|\\u2033|\\u201D|\\u201C|''|\\u2032\\u2032|\\u2019\\u2019)\\s*)?)?"
            + "(?<hem>[NSEWO])(?![\\p{L}\\d])",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Coordenada S-53 con más de 2 decimales de minuto. */
    private static final Pattern S53_TOKEN = Pattern.compile(
        "(?<![\\d.])(?<deg>\\d{1,3})-(?<min>\\d{1,2}[.,]\\d{3,})(?<hem>[NSEW])(?![\\p{L}\\d])",
        Pattern.CASE_INSENSITIVE);

    /** Separador permitido entre latitud y longitud de un mismo par. */
    private static final Pattern PAIR_SEPARATOR = Pattern.compile("^\\s*(?:[-\\u2013\\u2014,;/]\\s*)?$");

    private CoordinateTextNormalizer() {
    }

    public static String normalize(final String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return normalizeS53Tokens(normalizeDegreeTokens(text));
    }

    private static String normalizeDegreeTokens(final String text) {
        final List<Token> tokens = new ArrayList<>();
        final Matcher matcher = DEGREE_TOKEN.matcher(text);
        while (matcher.find()) {
            final String formatted = format(matcher.group("deg"), matcher.group("min"), matcher.group("sec"), matcher.group("hem"));
            if (formatted != null) {
                tokens.add(new Token(matcher.start(), matcher.end(), formatted, isLatitude(matcher.group("hem"))));
            }
        }
        if (tokens.isEmpty()) {
            return text;
        }

        final StringBuilder result = new StringBuilder();
        int cursor = 0;
        for (int i = 0; i < tokens.size(); i++) {
            final Token current = tokens.get(i);
            result.append(text, cursor, current.start);
            final Token next = i + 1 < tokens.size() ? tokens.get(i + 1) : null;
            if (next != null && current.latitude && !next.latitude
                && PAIR_SEPARATOR.matcher(text.substring(current.end, next.start)).matches()) {
                result.append(current.value).append(' ').append(next.value);
                cursor = next.end;
                i++;
            } else {
                result.append(current.value);
                cursor = current.end;
            }
        }
        return result.append(text.substring(cursor)).toString();
    }

    private static String normalizeS53Tokens(final String text) {
        final Matcher matcher = S53_TOKEN.matcher(text);
        final StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            final String formatted = format(matcher.group("deg"), matcher.group("min"), null, matcher.group("hem"));
            matcher.appendReplacement(result, Matcher.quoteReplacement(formatted != null ? formatted : matcher.group()));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * @return la coordenada en formato S-53, o {@code null} si los valores no son válidos.
     */
    private static String format(final String degText, final String minText, final String secText, final String hemText) {
        final boolean latitude = isLatitude(hemText);
        final BigDecimal deg = parse(degText);
        final BigDecimal min = minText != null ? parse(minText) : BigDecimal.ZERO;
        final BigDecimal sec = secText != null ? parse(secText) : BigDecimal.ZERO;

        // Solo la última parte presente puede tener decimales
        if ((minText != null && deg.scale() > 0) || (secText != null && min.scale() > 0)) {
            return null;
        }
        if (min.compareTo(BigDecimal.valueOf(60)) >= 0 || sec.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return null;
        }

        final BigDecimal totalDegrees = deg
            .add(min.divide(BigDecimal.valueOf(60), 12, RoundingMode.HALF_UP))
            .add(sec.divide(BigDecimal.valueOf(3600), 12, RoundingMode.HALF_UP));
        if (totalDegrees.compareTo(BigDecimal.valueOf(latitude ? 90 : 180)) > 0) {
            return null;
        }

        int degrees = totalDegrees.intValue();
        BigDecimal minutes = totalDegrees.subtract(BigDecimal.valueOf(degrees))
            .multiply(BigDecimal.valueOf(60))
            .setScale(2, RoundingMode.HALF_UP);
        if (minutes.compareTo(BigDecimal.valueOf(60)) >= 0) {
            degrees++;
            minutes = BigDecimal.ZERO.setScale(2);
        }

        final String hemisphere = "O".equalsIgnoreCase(hemText) ? "W" : hemText.toUpperCase(Locale.ROOT);
        return String.format(Locale.ROOT, latitude ? "%02d-%05.2f%s" : "%03d-%05.2f%s", degrees, minutes, hemisphere);
    }

    private static boolean isLatitude(final String hemisphere) {
        return "N".equalsIgnoreCase(hemisphere) || "S".equalsIgnoreCase(hemisphere);
    }

    private static BigDecimal parse(final String value) {
        return new BigDecimal(value.replace(',', '.'));
    }

    private record Token(int start, int end, String value, boolean latitude) {
    }
}
