package fr.shom.ping.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validate phone number using Google LibPhoneNumber
 */
@Constraint(validatedBy = ValidPhoneNumberValidator.class)
@Target({FIELD})
@Retention(RUNTIME)
@Documented
public @interface ValidPhoneNumber {
    String message() default "validation.phone-number.invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
