package fr.shom.ping.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validate country code based on ISO 3166-1
 */
@Constraint(validatedBy = ValidCountryCodeValidator.class)
@Target({FIELD})
@Retention(RUNTIME)
@Documented
public @interface ValidCountryCode {
    String message() default "should be a valid country";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
