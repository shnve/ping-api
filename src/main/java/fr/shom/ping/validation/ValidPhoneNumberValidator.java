package fr.shom.ping.validation;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    @Override
    public void initialize(ValidPhoneNumber constraint) {
        // Default initialize override
    }

    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (phoneNumber == null) {
            return true;
        }

        try {
            PhoneNumberUtil.getInstance().parse(phoneNumber, Phonenumber.PhoneNumber.CountryCodeSource.UNSPECIFIED.name());
            return true;
        } catch (NumberParseException e) {
            return false;
        }
    }
}
