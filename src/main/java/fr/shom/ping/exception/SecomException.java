package fr.shom.ping.exception;

import java.io.Serial;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class SecomException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 7483085824534527004L;

    public SecomException() {
        super();
    }

    public SecomException(String message) {
        super(message);
    }

    public SecomException(String message, Throwable cause) {
        super(message, cause);
    }
}
