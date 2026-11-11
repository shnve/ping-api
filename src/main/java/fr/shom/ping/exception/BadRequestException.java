package fr.shom.ping.exception;

import java.io.Serial;
import lombok.EqualsAndHashCode;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

// PISAC: responde 400 (antes, sin @ResponseStatus, toda regla de negocio violada llegaba al cliente como 500)
@ResponseStatus(BAD_REQUEST)
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
