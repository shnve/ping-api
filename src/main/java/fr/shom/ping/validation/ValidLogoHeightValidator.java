package fr.shom.ping.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;

public class ValidLogoHeightValidator implements ConstraintValidator<ValidLogoHeight, String> {
    public static final String FIELD_ERROR_MESSAGE = "validation.static-logo.height-with-unit.invalid-range";

    @Value("${ping.static-logo-min-height}")
    private int minHeight;

    @Value("${ping.static-logo-max-height}")
    private int maxHeight;

    @Override
    public boolean isValid(String height, ConstraintValidatorContext constraintValidatorContext) {
        if (height == null) {
            return false;
        }

        if (!height.endsWith("px")) {
            return false;
        }

        final int value = Integer.parseInt(height.replace("px", ""));
        return value >= minHeight && value <= maxHeight;
    }
}
