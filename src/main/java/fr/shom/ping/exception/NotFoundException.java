package fr.shom.ping.exception;

import java.io.Serial;
import lombok.EqualsAndHashCode;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ResponseStatus(NOT_FOUND)
@EqualsAndHashCode(callSuper = true)
public class NotFoundException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 7065480957809227102L;

    public NotFoundException() {
        super();
    }

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
