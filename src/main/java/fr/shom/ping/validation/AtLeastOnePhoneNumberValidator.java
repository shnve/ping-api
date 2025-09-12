package fr.shom.ping.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.SneakyThrows;
import org.apache.commons.beanutils.BeanUtils;

import static org.springframework.util.StringUtils.hasText;

public final class AtLeastOnePhoneNumberValidator implements ConstraintValidator<AtLeastOnePhoneNumber, Object> {
    protected static final String FIELD_ERROR_MESSAGE = "validation.at-list-one-phone-number";
    protected static final String FIRST_PHONE_NUMBER = "phoneNumber";
    protected static final String SECOND_PHONE_NUMBER = "mobilePhoneNumber";

    private String firstPhoneNumber;
    private String secondPhoneNumber;

    public AtLeastOnePhoneNumberValidator() {
        super();
        firstPhoneNumber = FIRST_PHONE_NUMBER;
        secondPhoneNumber = SECOND_PHONE_NUMBER;
    }

    @Override
    public void initialize(final AtLeastOnePhoneNumber atLeastOnePhoneNumber) {
        firstPhoneNumber = atLeastOnePhoneNumber.firstPhoneNumber();
        secondPhoneNumber = atLeastOnePhoneNumber.secondPhoneNumber();
    }

    @Override
    public boolean isValid(final Object value, final ConstraintValidatorContext context) {
        Object firstObject = this.getFieldValue(value, firstPhoneNumber);
        Object secondObject = this.getFieldValue(value, secondPhoneNumber);

        String firstValue = firstObject != null ? firstObject.toString() : null;
        String secondValue = secondObject != null ? secondObject.toString() : null;

        if (hasText(firstValue) || hasText(secondValue)) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(FIELD_ERROR_MESSAGE).addPropertyNode(firstPhoneNumber).addConstraintViolation();

        return false;
    }

    @SneakyThrows
    private Object getFieldValue(final Object value, String field) {
        if (field == null) {
            return null;
        }

        return BeanUtils.getProperty(value, field);
    }
}
