package fr.shom.ping.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum KeycloakErrorMessage {
    PASSWORD_POLICY_NOT_MET("Password policy not met"),
    USER_ALREADY_EXISTS("User exists with same username"),
    USER_ALREADY_EXISTS_WITH_EMAIL("User exists with same email"),
    UNKNOWN_ERROR("Unknown error");

    private final String message;

    @JsonValue
    public String jsonValue() {
        return message;
    }
}
