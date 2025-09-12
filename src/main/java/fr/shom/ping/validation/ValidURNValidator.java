package fr.shom.ping.validation;

import de.slub.urn.URN;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidURNValidator implements ConstraintValidator<ValidURN, String> {

    @Override
    public void initialize(ValidURN constraint) {
        // Default initialize override
    }

    /**
     * Validate that a string contains only URN based on RFC 8141
     *
     * @param urn
     *     should be a string to validate
     * @param context
     *     context of the validator
     * @return true if urn contains only URN, false otherwise
     */
    public boolean isValid(String urn, ConstraintValidatorContext context) {
        if (urn == null) {
            return true;
        }

        try {
            URN.rfc8141().parse(urn);
            return true;
        } catch (Exception ignoredException) {
            return false;
        }
    }
}
