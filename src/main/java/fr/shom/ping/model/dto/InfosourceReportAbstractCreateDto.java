package fr.shom.ping.model.dto;

import fr.shom.ping.model.Attachment;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.InfosourcePart;
import fr.shom.ping.model.enums.InfosourceNature;
import fr.shom.ping.validation.NoHtml;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
@Schema(name = "Infosource Create Dto")
public class InfosourceReportAbstractCreateDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 6076913672312529626L;

    @NotEmpty(message = "validation.infosource.localisation.not-empty")
    @Builder.Default
    protected Set<@Valid FeatureDto> localisation = new HashSet<>();

    @NotEmpty(message = "validation.infosource.description.not-empty")
    @NotBlank(message = "validation.infosource.description.not-blank")
    @NoHtml
    protected String description;

    @Builder.Default
    protected List<UUID> attachmentIds = new ArrayList<>();

    @NoHtml
    protected String timezone;

    protected InfosourceNature nature;

    public InfosourceReportAbstractCreateDto(Infosource infosource) {
        final InfosourcePart infosourcePart = infosource.getInfosourceParts().first();
        this.localisation = infosourcePart.getLocalisation().stream().map(FeatureDto::new).collect(Collectors.toSet());
        this.description = infosourcePart.getDescription();
        this.attachmentIds = infosource.getAttachments().stream().map(Attachment::getId).collect(Collectors.toList());
        this.timezone = infosource.getTimezone();
        this.nature = infosource.getNature();
    }
}
