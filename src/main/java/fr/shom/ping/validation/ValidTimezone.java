package fr.shom.ping.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validate Timezone
 */
@Constraint(validatedBy = ValidTimezoneValidator.class)
@Target({FIELD})
@Retention(RUNTIME)
@Documented
public @interface ValidTimezone {
    String message() default "should be a valid timezone";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
