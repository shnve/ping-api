package fr.shom.ping.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static fr.shom.ping.validation.ValidLogoHeightValidator.FIELD_ERROR_MESSAGE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validate logo height
 */
@Constraint(validatedBy = ValidLogoHeightValidator.class)
@Target({FIELD})
@Retention(RUNTIME)
@Documented
public @interface ValidLogoHeight {
    String message() default FIELD_ERROR_MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
