package fr.shom.ping.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static fr.shom.ping.validation.AtLeastOnePhoneNumberValidator.*;

@Documented
@Constraint(validatedBy = AtLeastOnePhoneNumberValidator.class)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AtLeastOnePhoneNumber {

    String firstPhoneNumber() default FIRST_PHONE_NUMBER;

    String secondPhoneNumber() default SECOND_PHONE_NUMBER;

    String message() default FIELD_ERROR_MESSAGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
