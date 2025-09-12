package fr.shom.ping.exception;

import java.io.Serial;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class ServiceException extends Exception {
    @Serial
    private static final long serialVersionUID = 4819139845660240139L;

    public ServiceException() {
        super();
    }

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
