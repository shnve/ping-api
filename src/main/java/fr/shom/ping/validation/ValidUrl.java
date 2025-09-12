package fr.shom.ping.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static fr.shom.ping.validation.ValidUrlValidator.FIELD_ERROR_MESSAGE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = ValidUrlValidator.class)
public @interface ValidUrl {
    String message() default FIELD_ERROR_MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
