package fr.shom.ping.validation;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.Locale.getISOLanguages;

public class ValidLanguageValidator implements ConstraintValidator<ValidLanguage, Object> {

    @Override
    public void initialize(ValidLanguage constraint) {
        // Default initialize override
    }

    /**
     * Validate that a string or a set of strings contain only languages based on ISO 3166-1
     *
     * @param obj
     *     should be a string or set of strings to validate
     * @param context
     *     context of the validator
     * @return true if obj contains only languages, false otherwise
     */
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true;
        }

        Set<String> languages;


        if (obj instanceof String string) {
            languages = new HashSet<>(singletonList(string));
        } else if (obj instanceof Set) {
            languages = (Set<String>) obj;
        } else {
            return false;
        }

        return languages.stream().allMatch(language -> stream(getISOLanguages())
            .map(Locale::new)
            .map(Locale::getLanguage)
            .anyMatch(lang -> lang.equals(language)));

    }
}
