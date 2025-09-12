package fr.shom.ping.exception;

import fr.shom.ping.model.enums.UserCreationErrorMessage;
import java.io.Serial;
import lombok.Getter;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(BAD_REQUEST)
public class KeycloakUserCreationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 8115163886582741197L;

    @Getter
    private final UserCreationErrorMessage error;

    public KeycloakUserCreationException(UserCreationErrorMessage error) {
        super(error.getI18n());
        this.error = error;
    }
}
