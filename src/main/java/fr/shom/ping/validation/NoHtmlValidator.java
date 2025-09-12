package fr.shom.ping.validation;

import java.util.Collection;
import java.util.Map;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

@Slf4j
public class NoHtmlValidator implements ConstraintValidator<NoHtml, Object> {

    protected static final String FIELD_ERROR_MESSAGE = "validation.no-html-content-allowed";

    private static final Cleaner CLEANER = new Cleaner(Safelist.none());

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        return switch (value) {
            case null -> true;
            case String string -> checkSanitization(string);
            case Map map -> map.values().stream()
                .allMatch(o -> !(o instanceof String) || checkSanitization((String) o));
            case Collection list -> list.stream()
                .allMatch(o -> o instanceof String s && checkSanitization(s));
            default -> false;
        };

    }

    public static boolean checkSanitization(String value) {
        final String preProcessedValue = StringUtils.normalizeSpace(value).replace("\n", "\\n").trim();
        final String sanitized = Jsoup.clean(preProcessedValue, Safelist.none());
        final String unsanitized = CLEANER.clean(Jsoup.parse(sanitized.replace("\n",""))).text();
        if (!unsanitized.equals(preProcessedValue)) {
            log.warn("original value: \n" + value);
            log.warn("preProcessedValue value: \n" + preProcessedValue);
            log.warn("sanitized: \n" + sanitized);
            log.warn("unsanitized: \n" + unsanitized);
        }
        return unsanitized.equals(preProcessedValue);
    }
}
