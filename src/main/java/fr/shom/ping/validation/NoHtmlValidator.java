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
        if (value == null) {
            return true;
        } else if (value instanceof String string) {
            return checkSanitization(string);
        } else if (value instanceof Map map) {
            return map.values().stream()
                .allMatch(o -> !(o instanceof String) || checkSanitization((String) o));
        } else if (value instanceof Collection list) {
            return list.stream()
                .allMatch(o -> o instanceof String s && checkSanitization(s));
        } else {
            return false;
        }

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
