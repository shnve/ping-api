package fr.shom.ping;

import fr.shom.ping.exception.BindingException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.context.request.WebRequest;

@Component
public class ExtendedErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        final Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);

        final Throwable error = super.getError(webRequest);
        // In case of a BindingException raised during the bean validation
        // we add an "invalid-arguments" attribute to the response which contains
        // a map of field in error and its default error message.
        if (error instanceof BindingException exception) {
            Map<String, String> errors = new HashMap<>();
            exception.getErrors().forEach(fieldError -> {
                String fieldName = ((FieldError) fieldError).getField();
                String errorMessage = fieldError.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            });
            errorAttributes.put("invalid-arguments", errors);
        }

        return errorAttributes;
    }
}
