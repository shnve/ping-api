package fr.shom.ping.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static fr.shom.ping.validation.ValidGeoregulationPartRestrictionValidator.FIELD_ERROR_MESSAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target({TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = ValidGeoregulationPartRestrictionValidator.class)
public @interface ValidGeoregulationPartRestriction {
    String message() default FIELD_ERROR_MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
