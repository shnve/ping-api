package fr.shom.ping.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static fr.shom.ping.validation.AtMostTwoLanguagesValidator.FIELD_ERROR_MESSAGE;
import static fr.shom.ping.validation.AtMostTwoLanguagesValidator.FIRST_FIELD;
import static fr.shom.ping.validation.AtMostTwoLanguagesValidator.SECOND_FIELD;

@Documented
@Constraint(validatedBy = AtMostTwoLanguagesValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AtMostTwoLanguages {
    String firstLanguageField() default FIRST_FIELD;

    String secondLanguageField() default SECOND_FIELD;

    String message() default FIELD_ERROR_MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
