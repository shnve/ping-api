package fr.shom.ping.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * PISAC (18-nov-2026): propuesta de traducción automática para el campo «Información» de advertencias y
 * regulaciones. Usa un LibreTranslate propio (contenedor {@code ping_libretranslate}, solo en 127.0.0.1): el texto no
 * sale del servidor y no hay costo por uso.
 *
 * <p>La traducción es una <b>propuesta</b>: el operador la revisa antes de guardar. Se traduce línea por línea para
 * conservar la estructura del aviso (saltos de línea, líneas vacías y coordenadas en su propia línea).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationService {

    public static final Set<String> SUPPORTED_LANGUAGES = Set.of("es", "en");
    public static final int MAX_LENGTH = 10_000;

    private final ObjectMapper objectMapper;

    @Value("${ping.translation.url:http://localhost:5000}")
    private String translationUrl;

    @Value("${ping.translation.enabled:true}")
    private boolean enabled;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .proxy(HttpClient.Builder.NO_PROXY)
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    public String translate(String text, String source, String target) {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "translation.disabled");
        }
        if (text == null || text.isBlank()) {
            return "";
        }
        if (text.length() > MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "translation.too-long");
        }
        if (!SUPPORTED_LANGUAGES.contains(source) || !SUPPORTED_LANGUAGES.contains(target) || source.equals(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "translation.unsupported-language");
        }

        // Solo se envían las líneas con texto; las vacías se conservan en su sitio.
        final String[] lines = text.replace("\r\n", "\n").split("\n", -1);
        final List<Integer> indexes = new ArrayList<>();
        final List<String> toTranslate = new ArrayList<>();
        final List<Boolean> wasUpperCase = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].isBlank()) {
                indexes.add(i);
                final boolean upper = isMostlyUpperCase(lines[i]);
                wasUpperCase.add(upper);
                toTranslate.add("es".equals(source) ? prepareSpanishLine(lines[i], upper) : lines[i]);
            }
        }
        if (toTranslate.isEmpty()) {
            return text;
        }

        final List<String> translated = callLibreTranslate(toTranslate, source, target);
        for (int i = 0; i < indexes.size() && i < translated.size(); i++) {
            String line = SPACED_SLASH.matcher(translated.get(i)).replaceAll("$1/$2");
            if ("en".equals(target)) {
                line = applyEnglishFixes(line);
            }
            // Los avisos náuticos se redactan en mayúsculas (estilo NAVTEX): se respeta el formato de origen.
            lines[indexes.get(i)] = wasUpperCase.get(i) ? line.toUpperCase(java.util.Locale.ROOT) : line;
        }
        return String.join("\n", lines);
    }

    // ---- Preparación del texto en español ----
    // El modelo traduce mal el texto en MAYÚSCULAS (lo toma como siglas). Se pasa a minúsculas conservando siglas,
    // códigos con cifras y topónimos, y se sustituyen antes los términos náuticos que el modelo no conoce
    // (el modelo deja intactas las palabras que ya están en inglés).

    private static final java.util.regex.Pattern SPACED_SLASH = java.util.regex.Pattern.compile("(\\d) / (\\d)");

    /** Siglas que se mantienen en mayúsculas. */
    private static final Set<String> ACRONYMS = Set.of(
        "VE", "UTC", "HLV", "NAVAREA", "NAVTEX", "SHN", "OCHINA", "INEA", "AIS", "GPS", "VHF", "UHF", "MF", "HF", "MHZ", "KHZ",
        "ECDIS", "WGS84", "WGS", "NM", "MN", "I", "II", "III", "IV", "V", "N", "S", "E", "W", "NE", "NW", "SE", "SW", "PDVSA");

    /** Palabras tras las que viene un topónimo (se capitaliza el nombre para que el modelo no lo traduzca). */
    private static final java.util.regex.Pattern TOPONYM = java.util.regex.Pattern.compile(
        "\\b(punta|isla|islas|islote|cabo|bah[ií]a|puerto|ensenada|laguna|r[ií]o|cayo|cayos|archipi[eé]lago|pen[ií]nsula|"
            + "bajo|banco|morro|playa|golfo de|canal de|estrecho de|boca de|lago de)\\s+((?:(?:de|del|la|las|los|el)\\s+)?[\\p{L}]+)",
        java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE);

    /** Términos náuticos (español en minúsculas → inglés), del más largo al más corto. */
    private static final List<String[]> GLOSSARY = List.of(
        new String[]{"se recomienda a los navegantes navegar con precaución", "mariners are advised to navigate with caution"},
        new String[]{"se recomienda navegar con precaución", "mariners are advised to navigate with caution"},
        new String[]{"cancelar este aviso", "cancel this message"},
        new String[]{"cancelar esta advertencia", "cancel this message"},
        new String[]{"boya de recalada", "landfall buoy"},
        new String[]{"boya cardinal norte", "north cardinal buoy"},
        new String[]{"boya cardinal sur", "south cardinal buoy"},
        new String[]{"boya cardinal este", "east cardinal buoy"},
        new String[]{"boya cardinal oeste", "west cardinal buoy"},
        new String[]{"boya lateral de babor", "port lateral buoy"},
        new String[]{"boya lateral de estribor", "starboard lateral buoy"},
        new String[]{"boya de peligro aislado", "isolated danger buoy"},
        new String[]{"boya de aguas seguras", "safe water buoy"},
        new String[]{"boya especial", "special mark buoy"},
        new String[]{"ejercicios de tiro", "firing exercises"},
        new String[]{"operaciones de dragado", "dredging operations"},
        new String[]{"levantamiento hidrográfico", "hydrographic survey"},
        new String[]{"fuera de posición", "off position"},
        new String[]{"fuera de servicio", "out of service"},
        new String[]{"luz apagada", "light unlit"},
        new String[]{"apagada", "unlit"},
        new String[]{"apagado", "unlit"},
        new String[]{"baliza", "beacon"},
        new String[]{"fondeadero", "anchorage"},
        new String[]{"naufragio", "wreck"},
        new String[]{"pecio", "wreck"},
        new String[]{"derrelicto", "derelict"},
        new String[]{"estribor", "starboard"},
        new String[]{"babor", "port"},
        new String[]{"millas náuticas", "nautical miles"},
        new String[]{"cables", "cables"});

    /**
     * Correcciones sobre el inglés ya traducido: términos que el modelo traduce de forma genérica y que en los avisos
     * náuticos tienen una forma fija (el modelo traduce mal si se le mezclan palabras inglesas dentro de la frase).
     */
    private static final List<String[]> ENGLISH_FIXES = List.of(
        new String[]{"(the )?sailors are (informed|warned|advised)", "mariners are advised"},
        new String[]{"(the )?navigators are (informed|warned|advised)", "mariners are advised"},
        new String[]{"\\bsailors\\b", "mariners"},
        new String[]{"\\bnavigators\\b", "mariners"},
        new String[]{"\\bsail with caution\\b", "navigate with caution"},
        new String[]{"\\bsail with care\\b", "navigate with caution"},
        new String[]{"\\bis stranded\\b", "is aground"},
        new String[]{"\\bstranded\\b", "aground"},
        new String[]{"\\bmacolla tip\\b", "Punta Macolla"});

    static String applyEnglishFixes(String line) {
        String result = line;
        for (String[] fix : ENGLISH_FIXES) {
            final java.util.regex.Matcher m = java.util.regex.Pattern.compile(fix[0],
                java.util.regex.Pattern.CASE_INSENSITIVE).matcher(result);
            final StringBuilder sb = new StringBuilder();
            while (m.find()) {
                String replacement = fix[1];
                // Conserva la mayúscula inicial de la frase
                if (Character.isUpperCase(m.group().charAt(0)) && Character.isLowerCase(replacement.charAt(0))) {
                    replacement = Character.toUpperCase(replacement.charAt(0)) + replacement.substring(1);
                }
                m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
            }
            m.appendTail(sb);
            result = sb.toString();
        }
        return result;
    }

    static boolean isMostlyUpperCase(String line) {
        long letters = line.chars().filter(Character::isLetter).count();
        long upper = line.chars().filter(Character::isUpperCase).count();
        return letters >= 4 && upper * 100 / letters >= 80;
    }

    static String prepareSpanishLine(String line, boolean upper) {
        String result = line;
        if (upper) {
            final StringBuilder sb = new StringBuilder();
            for (String token : line.split("(?<=\\s)|(?=\\s)")) {
                final String bare = token.replaceAll("[^\\p{L}\\p{N}]", "");
                final boolean keep = ACRONYMS.contains(bare.toUpperCase(java.util.Locale.ROOT)) || bare.chars().anyMatch(Character::isDigit);
                sb.append(keep ? token : token.toLowerCase(java.util.Locale.forLanguageTag("es")));
            }
            result = sb.toString();
            // Primera letra de la línea y tras punto, en mayúscula
            result = capitalizeSentences(result);
        }
        // Topónimos: «punta macolla» → «Punta Macolla»
        final java.util.regex.Matcher m = TOPONYM.matcher(result);
        final StringBuilder out = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(capitalizeWords(m.group(1)) + " " + capitalizeWords(m.group(2))));
        }
        m.appendTail(out);
        result = out.toString();
        // Glosario náutico
        for (String[] entry : GLOSSARY) {
            result = java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(entry[0]) + "\\b",
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE).matcher(result).replaceAll(entry[1]);
        }
        return result;
    }

    private static String capitalizeSentences(String s) {
        final StringBuilder sb = new StringBuilder(s);
        boolean capitalizeNext = true;
        for (int i = 0; i < sb.length(); i++) {
            final char c = sb.charAt(i);
            if (capitalizeNext && Character.isLetter(c)) {
                sb.setCharAt(i, Character.toUpperCase(c));
                capitalizeNext = false;
            } else if (c == '.' || c == ':' || c == ';') {
                capitalizeNext = true;
            } else if (Character.isLetterOrDigit(c)) {
                capitalizeNext = false;
            }
        }
        return sb.toString();
    }

    private static String capitalizeWords(String s) {
        final StringBuilder sb = new StringBuilder();
        for (String w : s.split(" ")) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            if (Set.of("de", "del", "la", "las", "los", "el").contains(w.toLowerCase(java.util.Locale.ROOT)) && !sb.isEmpty()) {
                sb.append(w.toLowerCase(java.util.Locale.ROOT));
            } else {
                sb.append(w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase(java.util.Locale.ROOT));
            }
        }
        return sb.toString();
    }

    private List<String> callLibreTranslate(List<String> lines, String source, String target) {
        final long start = System.currentTimeMillis();
        try {
            final Map<String, Object> body = new LinkedHashMap<>();
            body.put("q", lines);
            body.put("source", source);
            body.put("target", target);
            body.put("format", "text");

            final HttpRequest request = HttpRequest.newBuilder(URI.create(translationUrl + "/translate"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Traducción: LibreTranslate respondió {}: {}", response.statusCode(), response.body());
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "translation.unavailable");
            }

            final JsonNode translatedText = objectMapper.readTree(response.body()).get("translatedText");
            final List<String> result = new ArrayList<>();
            if (translatedText != null && translatedText.isArray()) {
                translatedText.forEach(node -> result.add(node.asText()));
            } else if (translatedText != null) {
                result.add(translatedText.asText());
            }
            log.info("Traducción {}→{}: {} líneas en {} ms", source, target, lines.size(), System.currentTimeMillis() - start);
            return result;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "translation.unavailable");
        } catch (Exception e) {
            log.warn("Traducción: servicio no disponible en {} ({})", translationUrl, e.toString());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "translation.unavailable");
        }
    }
}
