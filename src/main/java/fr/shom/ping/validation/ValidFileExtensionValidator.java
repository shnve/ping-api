package fr.shom.ping.validation;

import fr.shom.ping.properties.PingProperties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class ValidFileExtensionValidator implements ConstraintValidator<ValidFileExtension, String> {

    public static final String FIELD_ERROR_MESSAGE = "validation.file-extension-forbidden";

    @Autowired
    private PingProperties pingProperties;

    @Override
    public boolean isValid(String filename, ConstraintValidatorContext constraintValidatorContext) {
        if (filename == null) {
            return true;
        }
        final String extension = FilenameUtils.getExtension(filename);
        final Set<String> validExtensions = Stream.of(
                pingProperties.getUploadExtensions(), pingProperties.getUploadExtensionsForSurveyBathy(), pingProperties.getUploadExtensionsForSurveyMisc()
            )
            .flatMap(x -> x.stream())
            .collect(Collectors.toSet());
        return validExtensions.contains(extension);
    }
}
