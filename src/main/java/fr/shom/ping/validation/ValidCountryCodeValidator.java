package fr.shom.ping.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static java.util.Arrays.asList;
import static java.util.Locale.getISOCountries;

public class ValidCountryCodeValidator implements ConstraintValidator<ValidCountryCode, String> {

    @Override
    public void initialize(ValidCountryCode constraint) {
        // Default initialize override
    }

    public boolean isValid(String obj, ConstraintValidatorContext context) {
        if (obj == null) {
            return true;
        }

        return asList(getISOCountries()).contains(obj);
    }
}
