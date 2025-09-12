package fr.shom.ping.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static fr.shom.ping.validation.NoHtmlValidator.FIELD_ERROR_MESSAGE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target({ElementType.PARAMETER, FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = NoHtmlValidator.class)
public @interface NoHtml {
    String message() default FIELD_ERROR_MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
