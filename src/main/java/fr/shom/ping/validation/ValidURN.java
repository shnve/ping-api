package fr.shom.ping.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validate URN based on RFC 8141
 */
@Constraint(validatedBy = ValidURNValidator.class)
@Target({FIELD, TYPE_USE})
@Repeatable(ValidURN.Set.class)
@Retention(RUNTIME)
@Documented
public @interface ValidURN {
    String message() default "should be a valid URN";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Target({TYPE_USE})
    @Retention(RUNTIME)
    @Documented
    @interface Set {

        ValidURN[] value();
    }
}
