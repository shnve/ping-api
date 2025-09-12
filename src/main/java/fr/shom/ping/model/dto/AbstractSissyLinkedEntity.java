package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.shom.ping.model.Warning;
import java.time.Instant;
import fr.shom.ping.validation.NoHtml;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static fr.shom.ping.model.enums.WarningStatus.CANCELLED;
import static fr.shom.ping.utils.SissyUtil.LINKED_ENTITY_NUMBER_PROPERTY;
import static fr.shom.ping.utils.SissyUtil.LINKED_ENTITY_STATUS_DATE_PROPERTY;

@Data
@NoArgsConstructor
@SuperBuilder
@JsonInclude(NON_NULL)
public abstract class AbstractSissyLinkedEntity {
    private static final String WARNING_RELATIVE_PATH = "/avurnav-notice/";
    public static final String FOLDER_RELATIVE_PATH = "/folder/tables/";

    @JsonProperty(value = LINKED_ENTITY_NUMBER_PROPERTY)
    @NoHtml
    protected String number;

    @JsonProperty(value = LINKED_ENTITY_STATUS_DATE_PROPERTY)
    protected Instant statusDate;

    protected String url;

    public static AbstractSissyLinkedEntity fromWarning(AbstractSissyLinkedEntity dto, Warning warning) {
        dto.setNumber(warning.getNameOfSeries() + " " + warning.getWarningNumber() + "/" + String.valueOf(warning.getYear()).substring(2));
        dto.setStatusDate(CANCELLED == warning.getStatus() ? warning.getCancellationDate() : warning.getPublicationTime());
        dto.setUrl(WARNING_RELATIVE_PATH + warning.getId());
        return dto;
    }
}
