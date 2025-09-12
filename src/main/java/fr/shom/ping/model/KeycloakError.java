package fr.shom.ping.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.shom.ping.model.enums.KeycloakErrorMessage;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

import static fr.shom.ping.utils.SissyUtil.NUMBER_JSON_PROPERTY;

@Data
public class KeycloakError implements Serializable {
    @Serial
    private static final long serialVersionUID = -4859466079905643482L;

    private KeycloakErrorMessage errorMessage;
    private String error;

    @JsonProperty(value = "error_description")
    private String errorDescription;
}
