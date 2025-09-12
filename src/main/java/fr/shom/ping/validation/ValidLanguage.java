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
 * Validate language based on ISO 3166-1
 */
@Constraint(validatedBy = ValidLanguageValidator.class)
@Target({FIELD, TYPE_USE})
@Repeatable(ValidLanguage.Set.class)
@Retention(RUNTIME)
@Documented
public @interface ValidLanguage {
    String message() default "should be a valid language";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Target({TYPE_USE})
    @Retention(RUNTIME)
    @Documented
    @interface Set {

        ValidLanguage[] value();
    }
}
