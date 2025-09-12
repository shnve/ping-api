package fr.shom.ping.exception;

import java.io.Serial;
import lombok.EqualsAndHashCode;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@ResponseStatus(FORBIDDEN)
@EqualsAndHashCode(callSuper = true)
public class ForbiddenException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -7952543120810912695L;

    public ForbiddenException() {
        super();
    }

    public ForbiddenException(String message) {
        super(message);
    }
}
