package fr.shom.ping.validation;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidUrlValidator implements ConstraintValidator<ValidUrl, String> {
    protected static final String FIELD_ERROR_MESSAGE = "validation.invalid-url";

    private static final UrlValidator URL_VALIDATOR = new UrlValidator();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return StringUtils.isEmpty(value) || URL_VALIDATOR.isValid(value);
    }
}
