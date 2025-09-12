package fr.shom.ping.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public class ValidFileSizeValidator implements ConstraintValidator<ValidFileSize, Long> {
    private static final Integer MB = 1024 * 1024;

    public static final String FIELD_ERROR_MESSAGE = "validation.file-size-exceed";

    @Value("${ping.max-upload-file-size-in-megabytes}")
    private long maxSizeInMB;

    @Override
    public boolean isValid(Long fileSize, ConstraintValidatorContext constraintValidatorContext) {
        if (fileSize == null) {
            return true;
        }
        return fileSize < maxSizeInMB * MB;
    }
}
