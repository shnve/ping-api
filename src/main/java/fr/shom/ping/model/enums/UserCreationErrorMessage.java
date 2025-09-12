package fr.shom.ping.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import fr.shom.ping.model.KeycloakError;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

import static java.util.Arrays.stream;

@Getter
@AllArgsConstructor
public enum UserCreationErrorMessage {
    PASSWORD_POLICY_NOT_MET("password", "enum.user-creation-error-message.password-policy-not-met"),
    INVALID_PASSWORD_MIN_UPPERCASE_CHARS_MESSAGE("password", "enum.user-creation-error-message.password-policy-not-met"),
    USER_ALREADY_EXISTS("email", "enum.user-creation-error-message.user-already-exists"),
    USER_ALREADY_EXISTS_WITH_EMAIL("email", "enum.user-creation-error-message.user-already-exists"),
    UNKNOWN_ERROR("user", "enum.user-creation-error-message.unknown-error");

    private final String field;
    private final String i18n;

    @JsonValue
    public String jsonValue() {
        return i18n;
    }

    public static UserCreationErrorMessage fromKeycloakError(KeycloakError keycloakError) {
        return stream(UserCreationErrorMessage.values())
            .filter(createUserErrorMessage -> Optional.ofNullable(keycloakError.getError()).orElse("").equalsIgnoreCase(createUserErrorMessage.name().replace("_", ""))
                    || Optional.ofNullable(keycloakError.getErrorMessage()).orElse(KeycloakErrorMessage.UNKNOWN_ERROR).name().equals(createUserErrorMessage.name()))
            .findFirst()
            .orElse(UNKNOWN_ERROR);
    }
}
