package fr.shom.ping.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import fr.shom.ping.validation.NoHtml;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;

import static fr.shom.ping.utils.SissyUtil.LINKED_FOLDERS_PROPERTY;
import static fr.shom.ping.utils.SissyUtil.LINKED_WARNINGS_PROPERTY;
import static fr.shom.ping.utils.SissyUtil.NUMBER_JSON_PROPERTY;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SissyUpdateMessageDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 2492648565710542490L;

    @JsonProperty(value = NUMBER_JSON_PROPERTY)
    @NoHtml
    private String number;

    @Default
    @JsonProperty(value = LINKED_WARNINGS_PROPERTY)
    private List<@Valid SissyWarningDto> linkedWarnings = new ArrayList<>();

    @Default
    @JsonProperty(value = LINKED_FOLDERS_PROPERTY)
    private List<@Valid SissyFolderDto> linkedFolders = new ArrayList<>();
}
