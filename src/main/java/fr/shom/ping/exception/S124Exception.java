package fr.shom.ping.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serial;
import lombok.EqualsAndHashCode;
import org.springframework.web.bind.annotation.ResponseStatus;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@EqualsAndHashCode(callSuper = true)
@ResponseStatus(BAD_REQUEST)
@JsonInclude(NON_EMPTY)
public class S124Exception extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -5157061728135393351L;

    public S124Exception() {
        super();
    }

    public S124Exception(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    @Override
    public String getLocalizedMessage() {
        return null;
    }
}
