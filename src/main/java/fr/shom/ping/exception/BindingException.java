package fr.shom.ping.exception;

import java.io.Serial;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Getter
@ResponseStatus(BAD_REQUEST)
public class BindingException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 3300005925617173417L;
    private final List<ObjectError> errors;

    public BindingException(List<ObjectError> errors) {
        super("Binding exception");
        this.errors = Collections.unmodifiableList(errors);
    }
}
