package fr.shom.ping.exception;

import java.io.Serial;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class BadRequestException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -5157061728135393351L;

    public BadRequestException() {
        super();
    }

    public BadRequestException(String message) {
        super(message);
    }
}
