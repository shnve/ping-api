package fr.shom.ping.validation;

import java.util.Collection;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.SneakyThrows;
import org.springframework.security.util.FieldUtils;

public final class AtMostTwoLanguagesValidator implements ConstraintValidator<AtMostTwoLanguages, Object> {
    protected static final String FIELD_ERROR_MESSAGE = "validation.at-most-two-languages";
    protected static final String FIRST_FIELD = "mandatoryLanguages";
    protected static final String SECOND_FIELD = "optionalLanguages";

    private String firstFields;
    private String secondFields;

    public AtMostTwoLanguagesValidator() {
        super();
        firstFields = FIRST_FIELD;
        secondFields = SECOND_FIELD;
    }

    @Override
    public void initialize(final AtMostTwoLanguages atMostTwoLanguages) {
        firstFields = atMostTwoLanguages.firstLanguageField();
        secondFields = atMostTwoLanguages.secondLanguageField();
    }

    @Override
    public boolean isValid(final Object value, final ConstraintValidatorContext context) {
        Object firstObject = this.getFieldValue(value, firstFields);
        if (firstObject == null) {
            return false;
        }
        Object secondObject = this.getFieldValue(value, secondFields);
        if (secondObject == null) {
            return false;
        }

        if (!Collection.class.isAssignableFrom(firstObject.getClass()) || !Collection.class.isAssignableFrom(secondObject.getClass())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(FIELD_ERROR_MESSAGE).addPropertyNode(firstFields).addConstraintViolation();
            return false;
        }

        int total = ((Collection<String>) firstObject).size() + ((Collection<String>) secondObject).size();

        return total <= 2;
    }

    @SneakyThrows
    private Object getFieldValue(final Object value, String field) {
        if (field == null) {
            return null;
        }
        return FieldUtils.getFieldValue(value, field);
    }
}
